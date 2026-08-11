"""任务理解层 — 意图识别 + 实体抽取 + 执行计划生成

将用户自然语言指令解析为结构化任务：
  intent: 意图类型（监控/总览/诊断/工单/知识/分析/闲聊）
  entities: 关键实体（设备编码、状态、数量等）
  plan: 分步执行计划 [{step, tool, args, purpose}]
"""

import json
import logging
import re
from typing import Dict, List, Optional, Any

logger = logging.getLogger(__name__)

DEVICE_CODE_RE = re.compile(r"(?:DEV|MES|CNC)[-_]?[A-Za-z0-9]{2,12}", re.IGNORECASE)

# ============ 意图规则表 ============

_INTENT_KEYWORDS: Dict[str, List[str]] = {
    "health_overview": ["所有设备", "全部设备", "设备总览", "工厂状态", "健康", "总览", "整体", "有没有异常", "哪些设备异常", "检查工厂", "巡检"],
    "device_monitor": ["状态", "温度", "转速", "速度", "运行情况", "实时", "详情", "参数", "设备信息", "怎么样", "多少度"],
    "alarm_diagnosis": ["告警", "报警", "异常", "故障", "为什么", "原因", "诊断", "问题", "停机"],
    "work_order": ["工单", "派单", "维修单", "下单", "创建工单", "开单"],
    "knowledge_query": ["手册", "指南", "怎么处理", "怎么维护", "如何", "规范", "标准", "操作", "保养", "说明书"],
    "analysis": ["分析", "预测", "趋势", "统计", "对比", "报表", "SPC", "产能", "OEE", "良率"],
    "inventory": ["库存", "物料", "BOM", "清单", "原料"],
}

# 强信号关键词 — 命中即直接判定意图（高区分度）
_STRONG_KEYWORDS: Dict[str, List[str]] = {
    "knowledge_query": ["怎么处理", "怎么维护", "如何", "手册", "指南", "说明书", "保养", "操作规程", "标准"],
    "work_order": ["创建工单", "新建工单", "开个工单", "下工单", "派单", "维修单"],
    "health_overview": ["所有设备", "全部设备", "全工厂", "设备总览", "巡检"],
    "alarm_diagnosis": ["告警", "报警", "故障", "诊断"],
}

# 意图 → 建议工具链（编排参考）
_INTENT_TOOL_CHAIN: Dict[str, List[str]] = {
    "health_overview": ["get_all_device_health"],
    "device_monitor": ["get_device_digital_twin"],
    "alarm_diagnosis": ["get_device_alarms", "get_device_trend", "query_device_docs"],
    "work_order": ["list_work_orders", "create_work_order"],
    "knowledge_query": ["query_device_docs"],
    "analysis": ["list_devices", "get_device_trend"],
    "inventory": ["list_materials", "list_boms", "get_inventory"],
}


def _extract_device_code(message: str, focus_code: Optional[str] = None) -> Optional[str]:
    """抽取设备编码 — 显式引用优先，否则继承上轮焦点（多轮交互）"""
    matches = DEVICE_CODE_RE.findall(message)
    if matches:
        return matches[0].upper().replace("MES-", "DEV-").replace("CNC-", "DEV-")
    return focus_code


def _detect_intent(message: str) -> str:
    """规则意图识别 — 强信号优先，其次按命中关键词数打分"""
    # 强信号：命中直接判定（按优先级顺序，工单动作优先于知识检索）
    if any(kw in message for kw in _STRONG_KEYWORDS["work_order"]):
        return "work_order"
    for intent, keywords in _STRONG_KEYWORDS.items():
        if intent == "work_order":
            continue
        if any(kw in message for kw in keywords):
            return intent

    scores: Dict[str, int] = {}
    for intent, keywords in _INTENT_KEYWORDS.items():
        scores[intent] = sum(1 for kw in keywords if kw in message)
    best = max(scores.items(), key=lambda x: x[1])
    return best[0] if best[1] > 0 else "general_chat"


def _build_plan(intent: str, message: str, device_code: Optional[str]) -> List[Dict]:
    """根据意图与实体生成执行计划"""
    plan: List[Dict] = []
    step_no = 0

    def add(tool: str, args: Dict, purpose: str):
        nonlocal step_no
        step_no += 1
        plan.append({"step": step_no, "tool": tool, "args": args, "purpose": purpose})

    chain = _INTENT_TOOL_CHAIN.get(intent, [])

    if intent == "health_overview":
        add("get_all_device_health", {}, "获取全工厂设备健康总览")
        if "异常" in message or "告警" in message:
            add("get_device_alarms", {}, "查询异常设备告警记录")

    elif intent == "device_monitor":
        if device_code:
            add("get_device_digital_twin", {"device_code": device_code}, f"获取 {device_code} 数字孪生数据")
        else:
            add("list_devices", {}, "获取设备列表定位目标设备")
            add("get_device_digital_twin", {"device_code": "__query__"}, "获取设备详细数据")

    elif intent == "alarm_diagnosis":
        if device_code:
            add("get_device_alarms", {"device_code": device_code}, f"查询 {device_code} 告警记录")
            add("get_device_trend", {"device_code": device_code}, f"分析 {device_code} 运行趋势")
        else:
            add("list_devices", {}, "获取设备列表")
            add("get_device_alarms", {"device_code": "__query__"}, "查询设备告警记录")
        if any(kw in message for kw in ["怎么", "处理", "维护", "如何"]):
            add("query_device_docs", {"query": "设备异常处理"}, "检索维护指南提供处置建议")

    elif intent == "work_order":
        if any(kw in message for kw in ["查", "看", "列表", "有哪些", "状态"]):
            add("list_work_orders", {}, "查询工单列表")
        if any(kw in message for kw in ["创建", "新建", "开", "下", "维修单"]):
            add("create_work_order", {"product_name": "__query__", "quantity": 1}, "创建工单")

    elif intent == "knowledge_query":
        add("query_device_docs", {"query": message[:60]}, "检索知识库相关文档")

    elif intent == "inventory":
        if "BOM" in message.upper() or "清单" in message:
            add("list_boms", {}, "查询 BOM 清单")
        elif "库存" in message or "物料" in message:
            add("list_materials", {}, "查询物料与库存")

    elif intent == "analysis":
        if device_code:
            add("get_device_trend", {"device_code": device_code}, f"分析 {device_code} 运行趋势")
        else:
            add("list_devices", {}, "获取设备列表")
            add("get_device_trend", {"device_code": "__query__"}, "分析设备运行趋势")

    else:
        # 闲聊/其他 — 空计划，交给 LLM 自由发挥
        pass

    # 剔除重复工具调用（同一工具最多保留一次）
    seen = set()
    deduped = []
    for p in plan:
        key = (p["tool"], json.dumps(p["args"], sort_keys=True))
        if key not in seen:
            seen.add(key)
            deduped.append(p)
    for i, p in enumerate(deduped, start=1):
        p["step"] = i
    return deduped


class TaskPlanner:
    """任务理解器 — 规则快速路径 + 实体抽取"""

    def __init__(self, focus_ttl: int = 1800):
        self.focus_ttl = focus_ttl
        try:
            from src.services.redis_store import redis_store
            self.redis = redis_store
        except Exception:
            self.redis = None

    # ---- 多轮交互：会话焦点记忆 ----

    def save_focus(self, session_id: Optional[str], entities: Dict[str, Any]):
        """保存本轮实体焦点（设备编码等），供后续轮次继承"""
        if not session_id or not entities or self.redis is None:
            return
        try:
            self.redis.set_json(f"agent:focus:{session_id}", self.focus_ttl, entities)
        except Exception as e:
            logger.debug(f"保存会话焦点失败: {e}")

    def _load_focus(self, session_id: Optional[str]) -> Dict[str, Any]:
        if not session_id or self.redis is None:
            return {}
        try:
            focus = self.redis.get_json(f"agent:focus:{session_id}")
            return focus if isinstance(focus, dict) else {}
        except Exception:
            return {}

    # ---- 任务解析 ----

    def parse(self, message: str, session_id: Optional[str] = None) -> Dict[str, Any]:
        """解析用户任务 → {intent, entities, plan}"""
        prev = self._load_focus(session_id)
        device_code = _extract_device_code(message, prev.get("device_code"))
        intent = _detect_intent(message)

        entities: Dict[str, Any] = {"device_code": device_code}
        if prev.get("device_code") and not device_code:
            entities["device_code"] = prev["device_code"]
            entities["inherited_focus"] = True

        plan = _build_plan(intent, message, entities.get("device_code"))
        self.save_focus(session_id, {"device_code": entities.get("device_code")})

        return {
            "intent": intent,
            "entities": entities,
            "plan": plan,
        }

    @staticmethod
    def intent_label(intent: str) -> str:
        labels = {
            "health_overview": "设备健康总览",
            "device_monitor": "设备实时监控",
            "alarm_diagnosis": "告警诊断",
            "work_order": "工单管理",
            "knowledge_query": "知识检索",
            "analysis": "数据分析",
            "inventory": "物料库存",
            "general_chat": "常规问答",
        }
        return labels.get(intent, intent)
