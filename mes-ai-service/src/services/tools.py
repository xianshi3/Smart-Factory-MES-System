"""MES 工具定义层 — 将后端 API 封装为 LLM 可调用的 Tool"""

import asyncio
import logging
from typing import Dict, Any, List, Optional
from datetime import datetime

import httpx

logger = logging.getLogger(__name__)


MES_BASE_URL = "http://localhost:8085"
AUTH_BASE_URL = "http://localhost:8081"

_HTTP_TIMEOUT = 8.0


# 工具元数据：分类 / 超时 / 失败时是否建议知识库兜底
TOOL_META: Dict[str, Dict[str, Any]] = {
    "list_devices": {"category": "设备", "kb_fallback": False},
    "get_device_detail": {"category": "设备", "kb_fallback": False},
    "get_device_digital_twin": {"category": "数字孪生", "kb_fallback": False},
    "get_all_device_health": {"category": "数字孪生", "kb_fallback": False},
    "get_device_alarms": {"category": "数字孪生", "kb_fallback": True},
    "get_device_trend": {"category": "数字孪生", "kb_fallback": True},
    "list_work_orders": {"category": "工单", "kb_fallback": False},
    "get_work_order_detail": {"category": "工单", "kb_fallback": False},
    "create_work_order": {"category": "工单", "kb_fallback": False},
    "list_production_lines": {"category": "生产", "kb_fallback": False},
    "list_workstations": {"category": "生产", "kb_fallback": False},
    "list_boms": {"category": "物料", "kb_fallback": False},
    "list_materials": {"category": "物料", "kb_fallback": False},
    "get_inventory": {"category": "物料", "kb_fallback": False},
    "query_device_docs": {"category": "知识库", "kb_fallback": False},
}


def _safe_request(method: str, url: str, **kwargs) -> Dict[str, Any]:
    """统一 HTTP 调用 — 超时与错误规范化，绝不抛异常到编排层"""
    kwargs.setdefault("timeout", _HTTP_TIMEOUT)
    try:
        with httpx.Client() as client:
            resp = client.request(method, url, **kwargs)
            resp.raise_for_status()
            return {"success": True, "data": resp.json()}
    except Exception as e:
        logger.warning(f"工具请求失败 {method} {url}: {e}")
        return {"success": False, "error": f"后端服务不可用或响应异常: {e}", "data": None}


async def _safe_request_async(method: str, url: str, **kwargs) -> Dict[str, Any]:
    kwargs.setdefault("timeout", _HTTP_TIMEOUT)
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.request(method, url, **kwargs)
            resp.raise_for_status()
            return {"success": True, "data": resp.json()}
    except Exception as e:
        logger.warning(f"工具请求失败 {method} {url}: {e}")
        return {"success": False, "error": f"后端服务不可用或响应异常: {e}", "data": None}


TOOL_DEFINITIONS = [
    {
        "type": "function",
        "function": {
            "name": "list_devices",
            "description": "获取所有设备列表及其状态（温度、速度、运行状态）",
            "parameters": {
                "type": "object",
                "properties": {},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_device_detail",
            "description": "获取指定设备的详细信息和实时状态数据",
            "parameters": {
                "type": "object",
                "properties": {
                    "device_code": {
                        "type": "string",
                        "description": "设备编码，如 DEV-001",
                    }
                },
                "required": ["device_code"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_work_orders",
            "description": "获取工单列表，可按状态筛选",
            "parameters": {
                "type": "object",
                "properties": {
                    "status": {
                        "type": "string",
                        "enum": ["CREATED", "RELEASED", "IN_PROGRESS", "COMPLETED", "CLOSED"],
                        "description": "工单状态筛选（可选）",
                    }
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_work_order_detail",
            "description": "获取指定工单的详细信息（产品、数量、进度、状态流转）",
            "parameters": {
                "type": "object",
                "properties": {
                    "order_no": {
                        "type": "string",
                        "description": "工单编号，如 WO20260810001",
                    }
                },
                "required": ["order_no"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "create_work_order",
            "description": "创建一条新的生产工单或维修工单",
            "parameters": {
                "type": "object",
                "properties": {
                    "product_name": {
                        "type": "string",
                        "description": "产品名称或维修描述",
                    },
                    "quantity": {
                        "type": "integer",
                        "description": "计划数量（生产工单）或 1（维修工单）",
                    },
                    "priority": {
                        "type": "string",
                        "enum": ["HIGH", "MEDIUM", "LOW"],
                        "description": "优先级",
                    },
                },
                "required": ["product_name", "quantity"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_production_lines",
            "description": "获取所有生产线列表及其当前状态",
            "parameters": {
                "type": "object",
                "properties": {},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_workstations",
            "description": "获取所有工位列表及其状态",
            "parameters": {
                "type": "object",
                "properties": {},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_boms",
            "description": "获取 BOM 物料清单列表",
            "parameters": {
                "type": "object",
                "properties": {},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_materials",
            "description": "获取物料列表及库存信息",
            "parameters": {
                "type": "object",
                "properties": {},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_inventory",
            "description": "查询物料库存数量",
            "parameters": {
                "type": "object",
                "properties": {
                    "material_id": {
                        "type": "integer",
                        "description": "物料 ID",
                    }
                },
                "required": ["material_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_device_docs",
            "description": "查询设备操作手册、维护指南等知识库文档",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "搜索关键词，如'主轴维护'、'温度异常处理'",
                    }
                },
                "required": ["query"],
            },
        },
    },
    # ======== 数字孪生工具 ========
    {
        "type": "function",
        "function": {
            "name": "get_device_digital_twin",
            "description": "获取设备完整的数字孪生数据，包括3D模型引用、实时指标、运行状态、历史报警统计、健康评分。这是数字孪生系统的核心接口",
            "parameters": {
                "type": "object",
                "properties": {
                    "device_code": {
                        "type": "string",
                        "description": "设备编码，如 DEV-001",
                    }
                },
                "required": ["device_code"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_all_device_health",
            "description": "获取全工厂所有设备的健康状态总览，包括健康评分、异常数量、正常运行时间等数字孪生关键指标",
            "parameters": {
                "type": "object",
                "properties": {},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_device_alarms",
            "description": "获取指定设备的历史告警记录列表",
            "parameters": {
                "type": "object",
                "properties": {
                    "device_code": {
                        "type": "string",
                        "description": "设备编码，如 DEV-001",
                    }
                },
                "required": ["device_code"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_device_trend",
            "description": "获取设备最近24小时的温度/转速/振动趋势数据，用于分析设备运行趋势和预测潜在故障",
            "parameters": {
                "type": "object",
                "properties": {
                    "device_code": {
                        "type": "string",
                        "description": "设备编码，如 DEV-001",
                    }
                },
                "required": ["device_code"],
            },
        },
    },
]


async def call_list_devices(headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", f"{MES_BASE_URL}/dashboard/devices", headers=headers or {})
    if not result["success"]:
        return result
    return {"success": True, "devices": result["data"].get("data", [])}


async def call_get_device_detail(device_code: str, headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", f"{MES_BASE_URL}/dashboard/devices", headers=headers or {})
    if not result["success"]:
        return result
    devices = result["data"].get("data", [])
    if isinstance(devices, list):
        for d in devices:
            if d.get("deviceCode") == device_code or d.get("deviceName") == device_code:
                return {"success": True, "device": d}
    return {"success": False, "error": f"设备 {device_code} 未找到"}


async def call_list_work_orders(status: Optional[str] = None, headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", "http://localhost:8082/workorder/page",
                                       params={"status": status} if status else {}, headers=headers or {})
    if not result["success"]:
        return result
    data = result["data"]
    return {"success": True, "work_orders": data.get("data", {}).get("records", [])}


async def call_get_work_order_detail(order_no: str, headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", "http://localhost:8082/workorder/page",
                                       params={"orderNo": order_no}, headers=headers or {})
    if not result["success"]:
        return result
    data = result["data"]
    records = data.get("data", {}).get("records", [])
    if isinstance(records, list):
        for wo in records:
            if wo.get("orderNo") == order_no:
                return {"success": True, "work_order": wo}
    return {"success": False, "error": f"工单 {order_no} 未找到"}


async def call_create_work_order(product_name: str, quantity: int,
                                  priority: str = "MEDIUM",
                                  headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("POST", "http://localhost:8082/workorder", json={
        "productName": product_name,
        "quantity": quantity,
        "priority": priority,
    }, headers=headers or {})
    if not result["success"]:
        return result
    data = result["data"]
    return {"success": True, "work_order": data.get("data", {}), "message": f"工单创建成功"}


async def call_list_production_lines(headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", f"{MES_BASE_URL}/dashboard/production-line/list", headers=headers or {})
    if not result["success"]:
        return result
    return {"success": True, "production_lines": result["data"].get("data", [])}


async def call_list_workstations(headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", f"{MES_BASE_URL}/dashboard/workstation/list", headers=headers or {})
    if not result["success"]:
        return result
    return {"success": True, "workstations": result["data"].get("data", [])}


async def call_list_boms(headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", f"{MES_BASE_URL}/dashboard/bom/list", headers=headers or {})
    if not result["success"]:
        return result
    return {"success": True, "boms": result["data"].get("data", [])}


async def call_list_materials(headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", f"{MES_BASE_URL}/dashboard/material/list", headers=headers or {})
    if not result["success"]:
        return result
    return {"success": True, "materials": result["data"].get("data", [])}


async def call_get_inventory(material_id: int, headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", f"{MES_BASE_URL}/dashboard/inventory/list",
                                       params={"materialId": material_id}, headers=headers or {})
    if not result["success"]:
        return result
    return {"success": True, "inventory": result["data"].get("data", [])}


# ======== 数字孪生工具实现 ========

def _compute_health_score(device: dict) -> dict:
    """根据设备数据计算数字孪生健康评分"""
    status = device.get("deviceStatus", device.get("status", "UNKNOWN"))
    temp = float(device.get("temperature", device.get("deviceTemperature", 0)) or 0)
    rpm = float(device.get("rpm", device.get("deviceSpeed", device.get("spindleSpeed", 0))) or 0)
    alarms = int(device.get("alarmCount", device.get("alarm_count", 0)) or 0)

    score = 100
    issues = []

    if status == "OFFLINE": score -= 50; issues.append("设备离线")
    elif status == "ALARM": score -= 35; issues.append("设备告警")
    elif status == "MAINTENANCE": score -= 25; issues.append("维护中")

    if temp > 70: score -= 30; issues.append(f"温度过高({temp}°C)")
    elif temp > 55: score -= 15; issues.append(f"温度偏高({temp}°C)")
    elif temp > 45: score -= 5

    if rpm <= 0 and status != "STOPPED": score -= 10; issues.append("转速异常")

    score -= alarms * 5
    score = max(0, min(100, score))

    level = "healthy" if score >= 85 else ("warning" if score >= 60 else "critical")
    return {"score": score, "level": level, "issues": issues}


async def _get_all_devices(headers: Optional[Dict] = None) -> list:
    result = await _safe_request_async("GET", f"{MES_BASE_URL}/dashboard/devices", headers=headers or {})
    if not result["success"]:
        return []
    data = result["data"].get("data")
    return data if isinstance(data, list) else []


async def call_get_device_digital_twin(device_code: str, headers: Optional[Dict] = None) -> Dict:
    devices = await _get_all_devices(headers)
    for d in devices:
        if d.get("deviceCode") == device_code or d.get("deviceName") == device_code:
            health = _compute_health_score(d)
            return {
                "success": True,
                "digital_twin": {
                    "device_code": d.get("deviceCode", ""),
                    "device_name": d.get("deviceName", ""),
                    "device_type": d.get("deviceType", "CNC"),
                    "status": d.get("deviceStatus", d.get("status", "UNKNOWN")),
                    "temperature": d.get("temperature", d.get("deviceTemperature", 0)),
                    "rpm": d.get("rpm", d.get("deviceSpeed", d.get("spindleSpeed", 0))),
                    "vibration": d.get("vibration", d.get("deviceVibration", 0)),
                    "power": d.get("power", d.get("devicePower", 0)),
                    "uptime_hours": d.get("uptime", d.get("runningHours", d.get("runTime", 0))),
                    "model_3d_type": d.get("modelType", d.get("deviceModel", "lathe")),
                    "health_score": health["score"],
                    "health_level": health["level"],
                    "health_issues": health["issues"],
                    "location": d.get("location", d.get("workstation", "")),
                }
            }
    return {"success": False, "error": f"设备 {device_code} 未找到"}


async def call_get_all_device_health(headers: Optional[Dict] = None) -> Dict:
    devices = await _get_all_devices(headers)
    results = []
    stats = {"total": len(devices), "healthy": 0, "warning": 0, "critical": 0, "offline": 0}

    for d in devices:
        health = _compute_health_score(d)
        entry = {
            "device_code": d.get("deviceCode", ""),
            "device_name": d.get("deviceName", ""),
            "status": d.get("deviceStatus", d.get("status", "UNKNOWN")),
            "temperature": d.get("temperature", d.get("deviceTemperature", 0)),
            "health_score": health["score"],
            "health_level": health["level"],
            "issues": health["issues"],
        }
        results.append(entry)

        if entry["status"] == "OFFLINE":
            stats["offline"] += 1
        elif health["level"] == "critical":
            stats["critical"] += 1
        elif health["level"] == "warning":
            stats["warning"] += 1
        else:
            stats["healthy"] += 1

    return {"success": True, "devices": results, "summary": stats}


async def call_get_device_alarms(device_code: str, headers: Optional[Dict] = None) -> Dict:
    result = await _safe_request_async("GET", f"{MES_BASE_URL}/dashboard/alarm/list", headers=headers or {})
    if not result["success"]:
        return result
    all_alarms = result["data"].get("data", [])
    if isinstance(all_alarms, dict):
        all_alarms = all_alarms.get("records", [])
    device_alarms = [
        a for a in all_alarms
        if a.get("deviceCode") == device_code or a.get("deviceName") == device_code
    ]
    return {"success": True, "device_code": device_code, "alarms_count": len(device_alarms),
            "alarms": device_alarms[-10:]}


async def call_get_device_trend(device_code: str, headers: Optional[Dict] = None) -> Dict:
    devices = await _get_all_devices(headers)
    for d in devices:
        if d.get("deviceCode") == device_code or d.get("deviceName") == device_code:
            return {
                "success": True,
                "device_code": device_code,
                "device_name": d.get("deviceName", ""),
                "current": {
                    "temperature": d.get("temperature", d.get("deviceTemperature", 0)),
                    "rpm": d.get("rpm", d.get("deviceSpeed", d.get("spindleSpeed", 0))),
                    "vibration": d.get("vibration", d.get("deviceVibration", 0)),
                },
                "trend_analysis": {
                    "temperature_status": "normal" if float(d.get("temperature", 0) or 0) < 55 else "warning",
                    "rpm_status": "normal" if float(d.get("rpm", 0) or 0) > 0 else "stopped",
                    "recommendation": ("正常运行，无需干预" if float(d.get("temperature", 0) or 0) < 55
                                       else "建议降低负载或检查冷却系统"),
                },
            }
    return {"success": False, "error": f"设备 {device_code} 未找到"}


TOOL_DISPATCH = {
    "list_devices": call_list_devices,
    "get_device_detail": call_get_device_detail,
    "list_work_orders": call_list_work_orders,
    "get_work_order_detail": call_get_work_order_detail,
    "create_work_order": call_create_work_order,
    "list_production_lines": call_list_production_lines,
    "list_workstations": call_list_workstations,
    "list_boms": call_list_boms,
    "list_materials": call_list_materials,
    "get_inventory": call_get_inventory,
    "get_device_digital_twin": call_get_device_digital_twin,
    "get_all_device_health": call_get_all_device_health,
    "get_device_alarms": call_get_device_alarms,
    "get_device_trend": call_get_device_trend,
}


async def execute_tool(func_name: str, args: Dict[str, Any], headers: Optional[Dict] = None) -> Dict[str, Any]:
    """统一工具执行器 — 超时保护 + 参数校验 + 错误规范化

    返回结构统一为 {success, data..., error?}，异常绝不会向上抛出。
    """
    if func_name == "query_device_docs":
        # 知识库工具由调用方（AgentService）注入 KnowledgeBase 实例执行
        raise ValueError("query_device_docs 需由 AgentService 通过 KnowledgeBase 执行")

    handler = TOOL_DISPATCH.get(func_name)
    if handler is None:
        return {"success": False, "error": f"未知工具: {func_name}"}

    try:
        if headers:
            args = {**args, "headers": headers}
        result = await asyncio.wait_for(handler(**args), timeout=_HTTP_TIMEOUT + 5)
        if not isinstance(result, dict):
            return {"success": False, "error": f"工具 {func_name} 返回类型异常"}
        result.setdefault("success", True)
        return result
    except asyncio.TimeoutError:
        return {"success": False, "error": f"工具 {func_name} 执行超时"}
    except Exception as e:
        logger.error(f"工具 {func_name} 执行失败: {e}")
        return {"success": False, "error": f"工具 {func_name} 执行失败: {e}"}
