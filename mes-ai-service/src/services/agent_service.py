"""Agent 编排服务 — 任务理解 → 流程编排 → 工具调用 → 知识增强 → 多轮交互 → 结果交付

执行流水线：
  阶段一 任务理解: TaskPlanner 解析意图/实体/执行计划（含上轮焦点继承）
  阶段二 流程编排: 按计划驱动的 ReAct 循环，去重防止重复调用
  阶段三 知识增强: 工具失败自动检索知识库兜底，知识类任务预注入上下文
  阶段四 结果交付: ReportBuilder 生成结构化交付报告（LLM + 规则兜底）
"""

import asyncio
import json
import logging
from datetime import datetime, timezone
from typing import Dict, List, Optional, Any

from src.services.llm_service import LLmService
from src.services.tools import TOOL_DEFINITIONS, TOOL_META, execute_tool
from src.services.knowledge_base import KnowledgeBase
from src.services.task_planner import TaskPlanner
from src.services.report_builder import ReportBuilder

logger = logging.getLogger(__name__)

AGENT_SYSTEM_PROMPT = """你是一个智能工厂 MES 系统的 AI 生产助理，集成了数字孪生能力。你的职责是帮助用户完成生产管理、设备监控、预测性维护等任务。

## 核心能力
1. **数字孪生监控**: 通过 get_device_digital_twin 获取设备完整孪生数据（3D模型、实时指标、健康评分）
2. **全工厂健康总览**: 通过 get_all_device_health 获取所有设备健康状态，识别异常
3. **告警分析**: 通过 get_device_alarms 获取设备历史告警，分析根因
4. **趋势分析**: 通过 get_device_trend 获取设备运行趋势，预判潜在故障
5. **知识增强**: 通过 query_device_docs 搜索设备手册和维修指南
6. **工单管理**: 通过 list_work_orders / get_work_order_detail / create_work_order 管理工单

## 工具使用规则
1. 用户询问设备状态时，优先使用 get_device_digital_twin 获取完整数字孪生数据
2. 用户要求总览时，使用 get_all_device_health 获取全工厂健康图
3. 发现异常设备时，自动调用 get_device_alarms 和 get_device_trend 深入分析
4. 结合知识库 query_device_docs 给出维修建议
5. 必要时自动创建工单 create_work_order
6. 同一工具不要重复调用；若工具参数不足（如缺少设备编码），先调用 list_devices 获取后再分析

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

# 允许重复调用的工具（多设备场景需要逐一查询）
_REPEATABLE_TOOLS = {"get_device_digital_twin", "get_device_alarms", "get_device_trend", "query_device_docs"}


class AgentService:
    """Agent 编排服务 — 四阶段流水线"""

    def __init__(self, llm_service: LLmService, knowledge_base: Optional[KnowledgeBase] = None):
        self.llm = llm_service
        self.kb = knowledge_base
        self.planner = TaskPlanner()
        self.report_builder = ReportBuilder(llm_service)
        self.max_iterations = 8

    async def run(self, message: str, history: Optional[List[Dict]] = None,
                  session_id: Optional[str] = None,
                  context: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """执行 Agent 任务 — 任务理解 → 编排执行 → 结果交付"""
        if not self.llm.is_available():
            return {
                "success": False,
                "content": "大模型服务暂不可用，请配置智谱AI API Key",
                "steps": [],
                "plan": [],
                "report": None,
                "intent": "general_chat",
            }

        # ============ 阶段一：任务理解 ============
        task = self.planner.parse(message, session_id)
        intent = task["intent"]
        plan = task["plan"]
        device_code = task["entities"].get("device_code")

        messages: List[Dict] = [{"role": "system", "content": AGENT_SYSTEM_PROMPT}]

        # 页面上下文注入：用户当前所在页面/筛选/数据摘要，辅助 LLM 精准回答
        if context:
            messages.append({
                "role": "system",
                "content": "## 用户当前所在页面与上下文\n"
                           + json.dumps(context, ensure_ascii=False, default=str),
            })

        # 执行计划注入（流程编排提示）
        if plan:
            plan_text = "\n".join(
                f"{p['step']}. 调用 {p['tool']} — {p['purpose']}" for p in plan
            )
            messages.append({
                "role": "system",
                "content": f"## 本轮执行计划（可按实际情况灵活调整）\n{plan_text}",
            })

        # 实体上下文注入（辅助 LLM 准确填参）
        if device_code:
            messages.append({
                "role": "system",
                "content": f"## 用户关注设备: {device_code}（涉及该设备的问题优先使用此编码）",
            })

        # 知识增强：知识类任务预注入检索上下文，即使不调用工具也有知识可用
        if intent == "knowledge_query" and self.kb:
            kb_ctx = self.kb.retrieve_context(message, top_k=3)
            if kb_ctx:
                messages.append({"role": "system", "content": kb_ctx})

        if history:
            messages.extend(history[-10:])
        messages.append({"role": "user", "content": message})

        # ============ 阶段二 + 阶段三：编排执行 + 知识增强 ============
        steps: List[Dict] = []
        executed: set = set()
        final_content: Optional[str] = None
        iteration = 0

        try:
            # 整体执行超时保护（LLM 多轮往返 + 工具调用），避免无限卡住
            final_content = await asyncio.wait_for(
                self._execute_plan(message, history, device_code, intent, plan, messages, steps, executed),
                timeout=100.0,
            )
        except asyncio.TimeoutError:
            logger.warning("Agent 编排执行超过 100s，提前返回已有结果")
            # 循环上限兜底：取最后一条 assistant 内容
            final_content = None
            for m in reversed(messages):
                if m.get("role") == "assistant" and m.get("content"):
                    final_content = m["content"]
                    break
            if final_content is None:
                final_content = "任务执行超时，请稍后重试或缩小问题范围。"
        except Exception as e:
            logger.error(f"Agent 执行失败（LLM 或工具异常）: {e}")
            return {
                "success": False,
                "content": f"AI 服务执行失败：{e}。请检查大模型服务配置（ZHIPU_API_KEY）后重试。",
                "steps": steps,
                "plan": plan,
                "report": None,
                "intent": intent,
                "intent_label": TaskPlanner.intent_label(intent),
                "session_id": None,
                "timestamp": datetime.now(timezone.utc).isoformat(),
            }

        # ============ 阶段四：结果交付 ============
        report = await self.report_builder.build(intent, message, steps)

        return {
            "success": True,
            "content": final_content,
            "steps": steps,
            "plan": plan,
            "report": report,
            "intent": intent,
            "intent_label": TaskPlanner.intent_label(intent),
        }

    async def _execute_plan(
        self,
        message: str,
        history: Optional[List[Dict]],
        device_code: Optional[str],
        intent: str,
        plan: List[Dict],
        messages: List[Dict],
        steps: List[Dict],
        executed: set,
    ) -> str:
        """ReAct 编排循环：LLM 决策 → 工具执行 → 结果回灌，直到 LLM 输出最终回复"""
        final_content: Optional[str] = None
        iteration = 0

        while iteration < self.max_iterations:
            iteration += 1

            response = self.llm.client.chat.completions.create(
                model=self.llm.model_name,
                messages=messages,
                tools=TOOL_DEFINITIONS,
                temperature=0.3,
                max_tokens=1200,
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
                        if not isinstance(args, dict):
                            args = {}
                    except json.JSONDecodeError:
                        args = {}

                    logger.info(f"Agent 调用工具: {func_name}({args})")

                    # 编排去重：非可重复工具且已执行过 → 跳过并提示
                    if func_name in executed and func_name not in _REPEATABLE_TOOLS:
                        result = {
                            "success": False,
                            "skipped": True,
                            "error": f"工具 {func_name} 已执行过，请基于已有结果继续分析",
                        }
                    else:
                        # 工具调用：知识库工具走 KnowledgeBase，其余走统一执行器
                        if func_name == "query_device_docs" and self.kb:
                            result = self.kb.search(args.get("query", ""), top_k=3)
                        else:
                            result = await execute_tool(func_name, args)

                        if result.get("success"):
                            executed.add(func_name)

                        # 知识增强：设备诊断类工具失败 → 检索知识库兜底注入
                        if (not result.get("success")
                                and TOOL_META.get(func_name, {}).get("kb_fallback")
                                and self.kb and func_name != "query_device_docs"):
                            kb_ctx = self.kb.retrieve_context(
                                args.get("device_code", "") or message, top_k=2
                            )
                            if kb_ctx:
                                messages.append({"role": "system", "content": kb_ctx})

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

            else:
                final_content = msg.content or ""
                break

        # 循环上限兜底：取最后一条 assistant 内容
        if final_content is None:
            for m in reversed(messages):
                if m.get("role") == "assistant" and m.get("content"):
                    final_content = m["content"]
                    break
        if final_content is None:
            final_content = "任务已完成，请查看下方执行步骤。"
        return final_content
