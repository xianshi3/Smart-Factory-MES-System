"""对话历史数据模型"""
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime


class MessageResponse(BaseModel):
    id: int
    role: str
    content: str
    steps: List[Dict[str, Any]] = []
    created_at: str


class ConversationResponse(BaseModel):
    id: str
    user_id: str
    title: str
    created_at: str
    updated_at: str
    messages: List[MessageResponse] = []


class ConversationListItem(BaseModel):
    id: str
    title: str
    created_at: str
    updated_at: str


class ConversationListResponse(BaseModel):
    success: bool = True
    conversations: List[ConversationListItem]


class ConversationDetailResponse(BaseModel):
    success: bool = True
    conversation: ConversationResponse


class CreateConversationRequest(BaseModel):
    title: str = Field("新对话", max_length=100)


class CreateConversationResponse(BaseModel):
    success: bool = True
    conversation: ConversationListItem


class AddMessageRequest(BaseModel):
    role: str = Field(..., pattern="^(user|assistant)$")
    content: str = Field(..., min_length=1)
    steps: Optional[List[Dict[str, Any]]] = None
    auto_title: bool = Field(True, description="根据首条用户消息自动生成标题")


class AddMessageResponse(BaseModel):
    success: bool = True


class DeleteResponse(BaseModel):
    success: bool = True
