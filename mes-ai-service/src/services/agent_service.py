"""Agent 编排服务 — 任务理解 + 工具调用 + 多步推理 + 知识增强"""

import json
import logging
from typing import Dict, List, Optional, Any
from datetime import datetime

from src.services.llm_service import LLmService
from src.services.tools import TOOL_DEFINITIONS, TOOL_DISPATCH
from src.services.knowledge_base import KnowledgeBase

logger = logging.getLogger(__name__)

AGENT_SYSTEM_PROMPT = """你是一个智能工厂 MES 系统的 AI 生产助理，你的职责是帮助用户完成生产管理任务。

## 能力说明
你可以调用 MES 系统的实时数据接口来获取信息，也可以查询知识库中的设备手册和质检标准。
当用户提出任务时，请分析需要哪些信息，按顺序调用工具，最终给出完整结果。

## 工具使用规则
1. 如果需要的信息当前没有，先调用工具获取
2. 一个工具的结果可能作为下一个工具的输入
3. 如果工具调用失败，告诉用户失败原因并提供备选方案
4. 多次调用时，保持上下文连贯

## 任务闭环示例
- 用户说"检查 DEV-001 设备状态，如果温度过高就创建维修工单"
  步骤: get_device_detail → 判断温度 → 如果需要则 create_work_order → 告知结果

- 用户说"查看 A 生产线的所有工位状态"
  步骤: list_production_lines → 找到 A 线 → list_workstations → 筛选结果

## 回复要求
- 用中文回复，专业且易懂
- 涉及数据时用表格或分点呈现
- 给出具体的数值和建议，不要模糊回答

## 数据分析要求（重要）
当你获取到设备列表、工单列表等数据后，请主动分析异常：
1. 温度异常：标记高于 55°C 的设备（黄色预警）和高于 70°C 的设备（红色报警）
2. 状态异常：标记 ALARM、OFFLINE、MAINTENANCE 状态的设备
3. 汇总统计：给出正常/异常设备的数量比例
4. 主动建议：如果检测到异常，主动询问用户是否需要执行后续操作（如查手册、创建工单）
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
