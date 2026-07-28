"""Agent API 路由 — 智能生产助理"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime
import logging
import yaml
import os

from src.services.llm_service import LLmService
from src.services.knowledge_base import KnowledgeBase
from src.services.agent_service import AgentService
from src.services.conversation_store import conversation_store, init_db
from src.schemas.conversation import (
    ConversationListResponse, ConversationListItem,
    ConversationDetailResponse, ConversationResponse, MessageResponse,
    CreateConversationRequest, CreateConversationResponse,
    AddMessageRequest, AddMessageResponse, DeleteResponse,
)

logger = logging.getLogger(__name__)

config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
try:
    with open(config_path, "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)
except Exception:
    config = {"llm": {"model": "glm-4-flash"}}

llm_service = LLmService(config)
kb = KnowledgeBase()
agent_service = AgentService(llm_service, knowledge_base=kb)

router = APIRouter(prefix="/api/v1/agent", tags=["AI 生产助理"])


class AgentRequest(BaseModel):
    """Agent 请求模型"""
    message: str = Field(..., description="用户消息", min_length=1, max_length=2000)
    history: Optional[List[Dict[str, str]]] = Field(None, description="对话历史")
    session_id: Optional[str] = Field(None, description="会话ID（用于保持上下文）")


class AgentStep(BaseModel):
    """Agent 执行步骤"""
    tool: str = Field(..., description="调用的工具名称")
    args: Dict[str, Any] = Field(default_factory=dict, description="工具参数")
    result: Dict[str, Any] = Field(default_factory=dict, description="工具执行结果")


class AgentResponse(BaseModel):
    """Agent 响应模型"""
    success: bool = Field(..., description="是否成功")
    content: Optional[str] = Field(None, description="回复内容")
    steps: List[AgentStep] = Field(default_factory=list, description="执行步骤")
    session_id: Optional[str] = Field(None, description="会话ID")
    timestamp: datetime = Field(..., description="时间戳")


@router.post("/run", response_model=AgentResponse)
async def run_agent(request: AgentRequest):
    """执行 Agent 任务

    接收用户自然语言指令，自动编排工具调用并返回结果。
    支持多步推理、工具调用、知识库查询。

    Args:
        request: Agent 请求（消息 + 历史记录）

    Returns:
        Agent 响应（回复内容 + 执行步骤）
    """
    try:
        if not llm_service.is_available():
            return AgentResponse(
                success=False,
                content="大模型服务暂不可用，请配置智谱AI API Key（ZHIPU_API_KEY）",
                session_id=request.session_id,
                timestamp=datetime.utcnow(),
            )

        result = await agent_service.run(
            message=request.message,
            history=request.history,
            session_id=request.session_id,
        )

        steps = []
        for s in result.get("steps", []):
            steps.append(AgentStep(tool=s.get("tool", ""), args=s.get("args", {}), result=s.get("result", {})))

        return AgentResponse(
            success=result.get("success", False),
            content=result.get("content"),
            steps=steps,
            session_id=request.session_id,
            timestamp=datetime.utcnow(),
        )

    except Exception as e:
        logger.error(f"Agent 执行失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/kb/categories")
async def get_kb_categories():
    """获取知识库分类列表"""
    return {
        "success": True,
        "categories": kb.get_all_categories(),
        "total_docs": len(kb.documents),
    }


@router.post("/kb/search")
async def search_kb(query: str, top_k: int = 3):
    """搜索知识库

    Args:
        query: 搜索关键词
        top_k: 返回结果数量

    Returns:
        匹配的知识库文档
    """
    return kb.search(query, top_k=top_k)


@router.get("/tools")
async def list_tools():
    """获取所有可用工具列表"""
    from src.services.tools import TOOL_DEFINITIONS
    return {
        "success": True,
        "tools": [
            {
                "name": t["function"]["name"],
                "description": t["function"]["description"],
            }
            for t in TOOL_DEFINITIONS
        ],
    }


# ========== 对话历史 CRUD ==========

@router.post("/conversations", response_model=CreateConversationResponse)
async def create_conversation(req: CreateConversationRequest, user_id: str = "default"):
    conv = await conversation_store.create_conversation(user_id=user_id, title=req.title)
    return CreateConversationResponse(
        conversation=ConversationListItem(
            id=conv["id"], title=conv["title"],
            created_at=conv["created_at"], updated_at=conv["updated_at"],
        )
    )


@router.get("/conversations", response_model=ConversationListResponse)
async def list_conversations(user_id: str = "default"):
    convs = await conversation_store.list_conversations(user_id=user_id)
    return ConversationListResponse(conversations=[
        ConversationListItem(id=c["id"], title=c["title"], created_at=c["created_at"], updated_at=c["updated_at"])
        for c in convs
    ])


@router.get("/conversations/{conv_id}", response_model=ConversationDetailResponse)
async def get_conversation(conv_id: str):
    conv = await conversation_store.get_conversation(conv_id)
    if not conv:
        raise HTTPException(status_code=404, detail="对话不存在")
    return ConversationDetailResponse(conversation=ConversationResponse(
        id=conv["id"], user_id=conv.get("user_id", "default"),
        title=conv["title"], created_at=conv["created_at"], updated_at=conv["updated_at"],
        messages=[MessageResponse(
            id=m["id"], role=m["role"], content=m["content"],
            steps=m.get("steps", []) or [], created_at=m["created_at"],
        ) for m in conv.get("messages", [])],
    ))


@router.post("/conversations/{conv_id}/messages", response_model=AddMessageResponse)
async def add_message(conv_id: str, req: AddMessageRequest):
    conv = await conversation_store.get_conversation(conv_id)
    if not conv:
        raise HTTPException(status_code=404, detail="对话不存在")
    await conversation_store.add_message(conv_id, req.role, req.content, req.steps)
    if req.auto_title and req.role == "user" and (not conv.get("messages")):
        title = req.content[:30] + ("..." if len(req.content) > 30 else "")
        await conversation_store.update_title(conv_id, title)
    return AddMessageResponse()


@router.delete("/conversations/{conv_id}", response_model=DeleteResponse)
async def delete_conversation(conv_id: str):
    conv = await conversation_store.get_conversation(conv_id)
    if not conv:
        raise HTTPException(status_code=404, detail="对话不存在")
    await conversation_store.delete_conversation(conv_id)
    return DeleteResponse()
