"""Agent 编排服务 — 任务理解 + 工具调用 + 多步推理 + 知识增强"""

import json
import logging
from typing import Dict, List, Optional, Any
from datetime import datetime

from src.services.llm_service import LLmService
from src.services.tools import TOOL_DEFINITIONS, TOOL_DISPATCH
from src.services.knowledge_base import KnowledgeBase

logger = logging.getLogger(__name__)

AGENT_SYSTEM_PROMPT = """你是一个智能工厂 MES 系统的 AI 生产助理，集成了数字孪生能力。你的职责是帮助用户完成生产管理、设备监控、预测性维护等任务。

## 核心能力
1. **数字孪生监控**: 通过 get_device_digital_twin 获取设备完整孪生数据（3D模型、实时指标、健康评分）
2. **全工厂健康总览**: 通过 get_all_device_health 获取所有设备健康状态，识别异常
3. **告警分析**: 通过 get_device_alarms 获取设备历史告警，分析根因
4. **趋势分析**: 通过 get_device_trend 获取设备运行趋势，预判潜在故障
5. **知识增强**: 通过 query_device_docs 搜索设备手册和维修指南
6. **工单管理**: 通过 list_work_orders / create_work_order 管理工单

## 工具使用规则
1. 用户询问设备状态时，优先使用 get_device_digital_twin 获取完整数字孪生数据
2. 用户要求总览时，使用 get_all_device_health 获取全工厂健康图
3. 发现异常设备时，自动调用 get_device_alarms 和 get_device_trend 深入分析
4. 结合知识库 query_device_docs 给出维修建议
5. 必要时自动创建工单 create_work_order

## 数据分析规则（数字孪生增强版）
1. **健康评分**: 使用 get_all_device_health 的 health_score 排序，优先关注 critical 级别设备
2. **温度异常**: 标记 55°C 以上（偏高）和 70°C 以上（超标）的设备
3. **状态异常**: 标记 ALARM、OFFLINE、MAINTENANCE 状态
4. **趋势预警**: 如果设备温度持续上升，提前预警并建议维护
5. **主动闭环**: 检测到异常 → 查手册 → 创建工单，形成完整监控闭环

## 回复格式
- 用中文回复，专业清晰
- 数据用表格展示（设备名称 | 状态 | 温度 | 健康评分 | 建议）
- 异常设备标出具体数值和严重程度
- 给出可执行的操作建议（查看3D模型 / 创工单 / 查手册）

## 数字孪生闭环示例
用户: "检查工厂设备状态"
步骤: get_all_device_health → 发现 DEV-003 critical
     → get_device_digital_twin("DEV-003") → 温度78°C超标
     → get_device_alarms("DEV-003") → 查看历史
     → query_device_docs("主轴温度过高处理") → 找到维修手册
     → 建议: 立即创建维修工单
"""


class AgentService:
    """Agent 编排服务"""

    def __init__(self, llm_service: LLmService, knowledge_base: Optional[KnowledgeBase] = None):
        self.llm = llm_service
        self.kb = knowledge_base
        self.max_iterations = 8

    async def run(self, message: str, history: Optional[List[Dict]] = None,
                  session_id: Optional[str] = None) -> Dict[str, Any]:
        """执行 Agent 任务"""
        if not self.llm.is_available():
            return {
                "success": False,
                "content": "大模型服务暂不可用，请配置智谱AI API Key",
                "steps": [],
            }

        messages = [{"role": "system", "content": AGENT_SYSTEM_PROMPT}]
        if history:
            messages.extend(history[-10:])
        messages.append({"role": "user", "content": message})

        steps = []
        iteration = 0

        while iteration < self.max_iterations:
            iteration += 1

            response = self.llm.client.chat.completions.create(
                model=self.llm.model_name,
                messages=messages,
                tools=TOOL_DEFINITIONS,
                temperature=0.3,
                max_tokens=2048,
            )

            choice = response.choices[0]
            finish_reason = choice.finish_reason
            msg = choice.message

            if finish_reason == "tool_calls" and msg.tool_calls:
                messages.append({
                    "role": "assistant",
                    "content": msg.content or "",
                    "tool_calls": [
                        {
                            "id": tc.id,
                            "type": "function",
                            "function": {
                                "name": tc.function.name,
                                "arguments": tc.function.arguments,
                            },
                        }
                        for tc in msg.tool_calls
                    ],
                })

                for tc in msg.tool_calls:
                    func_name = tc.function.name
                    try:
                        args = json.loads(tc.function.arguments)
                    except json.JSONDecodeError:
                        args = {}

                    logger.info(f"Agent 调用工具: {func_name}({args})")

                    if func_name == "query_device_docs" and self.kb:
                        result = self.kb.search(args.get("query", ""))
                    elif func_name in TOOL_DISPATCH:
                        try:
                            result = await TOOL_DISPATCH[func_name](**args)
                        except Exception as e:
                            result = {"success": False, "error": str(e)}
                    else:
                        result = {"success": False, "error": f"未知工具: {func_name}"}

                    result_str = json.dumps(result, ensure_ascii=False, default=str)
                    messages.append({
                        "role": "tool",
                        "tool_call_id": tc.id,
                        "content": result_str,
                    })

                    steps.append({
                        "tool": func_name,
                        "args": args,
                        "result": result,
                    })

            elif finish_reason == "stop":
                return {
                    "success": True,
                    "content": msg.content or "",
                    "steps": steps,
                }

            else:
                return {
                    "success": True,
                    "content": msg.content or "任务已完成",
                    "steps": steps,
                }

        return {
            "success": True,
            "content": "任务步骤较多，已执行主要操作。详细结果请查看下方步骤列表。",
            "steps": steps,
        }
