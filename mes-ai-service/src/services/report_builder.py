"""结果交付层 — 将 Agent 执行步骤汇总为结构化交付报告

LLM 生成 JSON 结构化报告；LLM 不可用或输出不合法时，
降级为基于执行步骤的规则兜底摘要，保证交付能力不依赖大模型。
"""

import json
import logging
import re
from typing import Dict, List, Any, Optional

logger = logging.getLogger(__name__)

REPORT_SYSTEM_PROMPT = """你是 MES 系统的结果交付分析器。根据用户指令和 Agent 已执行的工具步骤，生成结构化交付报告。

要求：
1. summary: 一句话总结执行结果（中文）
2. key_points: 关键结论列表（3-6 条，中文，含具体数值）
3. tables: 数据表格列表，每项 {title, columns:[列名], rows:[[值,...]]}（无表格数据则为空数组）
4. recommendations: 可执行的建议列表（中文，针对异常给出处置动作）
5. follow_ups: 可追问用户的后续问题列表（中文，最多 3 个）

只输出 JSON，不要输出任何其他文字。JSON 结构：
{"summary": "...", "key_points": [...], "tables": [...], "recommendations": [...], "follow_ups": [...]}
"""


def _extract_json(text: str) -> Optional[Dict]:
    """从 LLM 输出中提取 JSON（容忍 ```json 代码块包裹）"""
    text = (text or "").strip()
    code_block = re.search(r"```(?:json)?\s*(.*?)```", text, re.DOTALL)
    if code_block:
        text = code_block.group(1).strip()
    try:
        parsed = json.loads(text)
        return parsed if isinstance(parsed, dict) else None
    except json.JSONDecodeError:
        brace_start = text.find("{")
        brace_end = text.rfind("}")
        if brace_start != -1 and brace_end > brace_start:
            try:
                parsed = json.loads(text[brace_start:brace_end + 1])
                return parsed if isinstance(parsed, dict) else None
            except json.JSONDecodeError:
                return None
    return None


def _rule_fallback(intent: str, user_message: str, steps: List[Dict]) -> Dict:
    """规则兜底 — 从执行步骤提取关键信息生成报告"""
    ok_steps = [s for s in steps if s.get("result", {}).get("success")]
    failed = [s for s in steps if not s.get("result", {}).get("success", True)]

    key_points: List[str] = []
    tables: List[Dict] = []
    recommendations: List[str] = []

    for s in steps:
        tool = s.get("tool", "")
        result = s.get("result", {})

        if tool == "get_all_device_health":
            summary = result.get("summary", {})
            if summary:
                key_points.append(
                    f"全工厂共 {summary.get('total', 0)} 台设备：健康 {summary.get('healthy', 0)} 台、"
                    f"警告 {summary.get('warning', 0)} 台、严重 {summary.get('critical', 0)} 台、离线 {summary.get('offline', 0)} 台"
                )
            devices = result.get("devices", [])
            if devices:
                rows = [[d.get("device_code"), d.get("status"), d.get("temperature"),
                         f"{d.get('health_score', 0)}", d.get("health_level")] for d in devices[:20]]
                tables.append({"title": "设备健康总览", "columns": ["设备编码", "状态", "温度", "健康评分", "级别"], "rows": rows})

        elif tool == "get_device_digital_twin":
            dt = result.get("digital_twin", {})
            if dt:
                issues = dt.get("health_issues", [])
                key_points.append(
                    f"{dt.get('device_code')}（{dt.get('device_name')}）状态 {dt.get('status')}，"
                    f"温度 {dt.get('temperature')}°C，健康评分 {dt.get('health_score')}"
                    + (f"，问题：{'、'.join(issues)}" if issues else "")
                )
                if dt.get("health_level") in ("warning", "critical"):
                    recommendations.append(f"建议对 {dt.get('device_code')} 安排维护检查")

        elif tool == "get_device_alarms":
            count = result.get("alarms_count", 0)
            alarms = result.get("alarms", [])
            device = result.get("device_code", "")
            if device:
                key_points.append(f"{device} 共有 {count} 条告警记录")
            if alarms:
                rows = [[a.get("alarmType") or a.get("alarmTypeName"), a.get("alarmTime") or a.get("createTime"),
                         a.get("message") or a.get("description")] for a in alarms[:10]]
                tables.append({"title": f"{device} 告警记录", "columns": ["类型", "时间", "描述"], "rows": rows})

        elif tool == "query_device_docs":
            results = result.get("results", [])
            if results:
                for r in results[:2]:
                    recommendations.append(f"参考《{r.get('title')}》：{r.get('content', '')[:80]}…")

        elif tool == "create_work_order":
            if result.get("success"):
                wo = result.get("work_order", {})
                key_points.append(f"工单创建成功：{wo.get('orderNo', '')} 产品 {wo.get('productName', '')}")
                recommendations.append("可在工单管理页面查看并下发该工单")

        elif tool == "list_work_orders":
            wos = result.get("work_orders", [])
            if wos:
                rows = [[w.get("orderNo"), w.get("productName"), w.get("status"), w.get("quantity")] for w in wos[:10]]
                tables.append({"title": "工单列表", "columns": ["工单号", "产品", "状态", "数量"], "rows": rows})

    if failed:
        key_points.append(f"有 {len(failed)} 个工具步骤执行失败（{'、'.join(s.get('tool', '') for s in failed)}）")
        recommendations.append("部分后端服务可能未启动，请检查 8082/8085 服务状态")

    if not key_points and not tables:
        key_points = ["已完成任务处理，未获取到结构化数据"]

    return {
        "summary": f"已根据指令完成处理（意图：{intent}）",
        "key_points": key_points,
        "tables": tables,
        "recommendations": recommendations,
        "follow_ups": ["需要我进一步分析某个设备吗？", "是否要创建工单安排处理？"],
    }


class ReportBuilder:
    """结果交付器 — LLM 结构化报告 + 规则兜底"""

    def __init__(self, llm_service=None):
        self.llm = llm_service

    async def build(self, intent: str, user_message: str, steps: List[Dict]) -> Dict:
        if self.llm is not None and self.llm.is_available() and steps:
            try:
                prompt = (
                    f"用户指令：{user_message}\n"
                    f"已执行步骤：{json.dumps(steps, ensure_ascii=False, default=str)[:4000]}"
                )
                resp = await _async_chat(self.llm, prompt)
                report = _extract_json(resp)
                if report:
                    return {
                        "summary": report.get("summary", ""),
                        "key_points": report.get("key_points", []),
                        "tables": report.get("tables", []),
                        "recommendations": report.get("recommendations", []),
                        "follow_ups": report.get("follow_ups", []),
                    }
            except Exception as e:
                logger.warning(f"LLM 报告生成失败，使用规则兜底: {e}")

        return _rule_fallback(intent, user_message, steps)


async def _async_chat(llm_service, prompt: str) -> str:
    """LLM 简单对话（无工具）— 同步 SDK 放入线程池"""
    import asyncio

    def _run():
        resp = llm_service.client.chat.completions.create(
            model=llm_service.model_name,
            messages=[
                {"role": "system", "content": REPORT_SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            temperature=0.2,
            max_tokens=1024,
        )
        return resp.choices[0].message.content or ""

    return await asyncio.to_thread(_run)
