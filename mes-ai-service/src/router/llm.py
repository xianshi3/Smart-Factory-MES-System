"""大模型路由模块"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime
import logging
import yaml
import os

from src.services.llm_service import LLmService

logger = logging.getLogger(__name__)

config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
try:
    with open(config_path, "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)
except Exception:
    config = {"llm": {"model": "glm-4"}}

llm_service = LLmService(config)

router = APIRouter(prefix="/api/v1/llm", tags=["大模型"])


class ChatRequest(BaseModel):
    """对话请求模型"""
    message: str = Field(..., description="用户消息")
    context: Optional[Dict[str, Any]] = Field(None, description="上下文数据")
    history: Optional[List[Dict[str, str]]] = Field(None, description="对话历史")


class ChatResponse(BaseModel):
    """对话响应模型"""
    success: bool = Field(..., description="是否成功")
    content: Optional[str] = Field(None, description="回复内容")
    message: Optional[str] = Field(None, description="错误信息")
    model: Optional[str] = Field(None, description="使用的模型")
    usage: Optional[Dict[str, int]] = Field(None, description="token使用量")
    timestamp: datetime = Field(..., description="时间戳")


class AnalyzeRequest(BaseModel):
    """分析请求模型"""
    device: Optional[Dict[str, Any]] = Field(None, description="设备信息")
    work_order: Optional[Dict[str, Any]] = Field(None, description="工单信息")
    quality: Optional[Dict[str, Any]] = Field(None, description="质量预测")
    fault: Optional[Dict[str, Any]] = Field(None, description="故障预测")


class AnalyzeResponse(BaseModel):
    """分析响应模型"""
    success: bool = Field(..., description="是否成功")
    analysis: Optional[str] = Field(None, description="分析结果")
    message: Optional[str] = Field(None, description="错误信息")
    timestamp: datetime = Field(..., description="时间戳")


class RecommendRequest(BaseModel):
    """推荐请求模型"""
    product_type: str = Field(..., description="产品类型")
    target_yield: Optional[float] = Field(0.95, description="目标良率")
    material_properties: Optional[Dict[str, float]] = Field(None, description="材料特性")


class RecommendResponse(BaseModel):
    """推荐响应模型"""
    success: bool = Field(..., description="是否成功")
    recommendation: Optional[str] = Field(None, description="推荐内容")
    message: Optional[str] = Field(None, description="错误信息")
    timestamp: datetime = Field(..., description="时间戳")


class ModelInfoResponse(BaseModel):
    """模型信息响应"""
    available: bool = Field(..., description="是否可用")
    model: str = Field(..., description="模型名称")
    provider: str = Field(..., description="提供商")
    capabilities: List[str] = Field(..., description="支持的能力")


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """通用对话接口
    
    与AI助手进行对话，可以传入上下文信息
    
    Args:
        request: 对话请求
        
    Returns:
        对话响应
    """
    try:
        result = llm_service.chat(
            message=request.message,
            context=request.context,
            history=request.history,
        )
        return ChatResponse(**result, timestamp=datetime.utcnow())
    except Exception as e:
        logger.error(f"对话失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze(request: AnalyzeRequest):
    """智能分析接口
    
    根据设备、质量、故障等数据进行智能分析
    
    Args:
        request: 分析请求
        
    Returns:
        分析响应
    """
    try:
        if not llm_service.is_available():
            return AnalyzeResponse(
                success=False,
                message="大模型服务暂不可用，请配置智谱AI API Key",
                timestamp=datetime.utcnow(),
            )
        
        context = {}
        if request.device:
            context["device"] = request.device
        if request.work_order:
            context["work_order"] = request.work_order
        if request.quality:
            context["quality"] = request.quality
        if request.fault:
            context["fault"] = request.fault
        
        prompt = "请分析以下数据并给出专业建议："
        result = llm_service.chat(prompt, context=context)
        
        return AnalyzeResponse(
            success=result.get("success", False),
            analysis=result.get("content"),
            message=result.get("message"),
            timestamp=datetime.utcnow(),
        )
    except Exception as e:
        logger.error(f"分析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/recommend", response_model=RecommendResponse)
async def recommend(request: RecommendRequest):
    """工艺参数推荐接口
    
    根据产品类型和目标良率推荐最佳工艺参数
    
    Args:
        request: 推荐请求
        
    Returns:
        推荐响应
    """
    try:
        if not llm_service.is_available():
            return RecommendResponse(
                success=False,
                message="大模型服务暂不可用，请配置智谱AI API Key",
                timestamp=datetime.utcnow(),
            )
        
        result = llm_service.recommend_process_params(
            product_type=request.product_type,
            target_yield=request.target_yield,
        )
        
        return RecommendResponse(
            success=True if result else False,
            recommendation=result,
            timestamp=datetime.utcnow(),
        )
    except Exception as e:
        logger.error(f"推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/info", response_model=ModelInfoResponse)
async def get_model_info():
    """获取大模型信息"""
    try:
        info = llm_service.get_model_info()
        return ModelInfoResponse(**info)
    except Exception as e:
        logger.error(f"获取信息失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))