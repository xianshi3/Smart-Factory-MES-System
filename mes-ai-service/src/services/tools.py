"""MES 工具定义层 — 将后端 API 封装为 LLM 可调用的 Tool"""

import logging
from typing import Dict, Any, List, Optional
from datetime import datetime

import httpx

logger = logging.getLogger(__name__)


MES_BASE_URL = "http://localhost:8085"
AUTH_BASE_URL = "http://localhost:8081"


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
]


async def call_list_devices(headers: Optional[Dict] = None) -> Dict:
    async with httpx.AsyncClient(base_url=MES_BASE_URL, timeout=10) as client:
        resp = await client.get("/dashboard/device/list", headers=headers or {})
        resp.raise_for_status()
        data = resp.json()
        return {"success": True, "devices": data.get("data", [])}


async def call_get_device_detail(device_code: str, headers: Optional[Dict] = None) -> Dict:
    async with httpx.AsyncClient(base_url=MES_BASE_URL, timeout=10) as client:
        resp = await client.get(f"/dashboard/device/{device_code}", headers=headers or {})
        resp.raise_for_status()
        data = resp.json()
        return {"success": True, "device": data.get("data", {})}


async def call_list_work_orders(status: Optional[str] = None, headers: Optional[Dict] = None) -> Dict:
    async with httpx.AsyncClient(base_url="http://localhost:8082", timeout=10) as client:
        params = {}
        if status:
            params["status"] = status
        resp = await client.get("/workorder/page", params=params, headers=headers or {})
        resp.raise_for_status()
        data = resp.json()
        return {"success": True, "work_orders": data.get("data", {}).get("records", [])}


async def call_create_work_order(product_name: str, quantity: int,
                                  priority: str = "MEDIUM",
                                  headers: Optional[Dict] = None) -> Dict:
    async with httpx.AsyncClient(base_url="http://localhost:8082", timeout=10) as client:
        payload = {
            "productName": product_name,
            "quantity": quantity,
            "priority": priority,
        }
        resp = await client.post("/workorder", json=payload, headers=headers or {})
        resp.raise_for_status()
        data = resp.json()
        return {"success": True, "work_order": data.get("data", {}), "message": f"工单创建成功"}


async def call_list_production_lines(headers: Optional[Dict] = None) -> Dict:
    async with httpx.AsyncClient(base_url=MES_BASE_URL, timeout=10) as client:
        resp = await client.get("/dashboard/production-line/list", headers=headers or {})
        resp.raise_for_status()
        data = resp.json()
        return {"success": True, "production_lines": data.get("data", [])}


async def call_list_workstations(headers: Optional[Dict] = None) -> Dict:
    async with httpx.AsyncClient(base_url=MES_BASE_URL, timeout=10) as client:
        resp = await client.get("/dashboard/workstation/list", headers=headers or {})
        resp.raise_for_status()
        data = resp.json()
        return {"success": True, "workstations": data.get("data", [])}


async def call_list_boms(headers: Optional[Dict] = None) -> Dict:
    async with httpx.AsyncClient(base_url=MES_BASE_URL, timeout=10) as client:
        resp = await client.get("/dashboard/bom/list", headers=headers or {})
        resp.raise_for_status()
        data = resp.json()
        return {"success": True, "boms": data.get("data", [])}


async def call_list_materials(headers: Optional[Dict] = None) -> Dict:
    async with httpx.AsyncClient(base_url=MES_BASE_URL, timeout=10) as client:
        resp = await client.get("/dashboard/material/list", headers=headers or {})
        resp.raise_for_status()
        data = resp.json()
        return {"success": True, "materials": data.get("data", [])}


async def call_get_inventory(material_id: int, headers: Optional[Dict] = None) -> Dict:
    async with httpx.AsyncClient(base_url=MES_BASE_URL, timeout=10) as client:
        resp = await client.get(f"/dashboard/inventory/list", params={"materialId": material_id},
                                 headers=headers or {})
        resp.raise_for_status()
        data = resp.json()
        return {"success": True, "inventory": data.get("data", [])}


TOOL_DISPATCH = {
    "list_devices": call_list_devices,
    "get_device_detail": call_get_device_detail,
    "list_work_orders": call_list_work_orders,
    "create_work_order": call_create_work_order,
    "list_production_lines": call_list_production_lines,
    "list_workstations": call_list_workstations,
    "list_boms": call_list_boms,
    "list_materials": call_list_materials,
    "get_inventory": call_get_inventory,
}
