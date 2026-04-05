"""API 数据模型定义模块"""
from pydantic import BaseModel, Field
from typing import Dict, List, Optional
import datetime


class QualityPredictRequest(BaseModel):
    """质量预测请求模型"""
    device_id: str = Field(..., description="设备ID")
    features: Dict[str, float] = Field(..., description="设备参数特征")


class QualityPredictResponse(BaseModel):
    """质量预测响应模型"""
    device_id: str = Field(..., description="设备ID")
    pass_probability: float = Field(..., ge=0.0, le=1.0, description="合格概率")
    fail_probability: float = Field(..., ge=0.0, le=1.0, description="不合格概率")
    prediction: str = Field(..., description="预测结果: PASSED or FAILED")
    confidence: float = Field(..., ge=0.0, le=1.0, description="置信度")
    model_version: str = Field(..., description="模型版本号")


class ProductionPredictRequest(BaseModel):
    """产量预测请求模型"""
    device_id: str = Field(..., description="设备ID")
    history_data: List[Dict[str, float]] = Field(
        ..., description="历史数据列表"
    )
    days_ahead: int = Field(default=7, ge=1, le=90, description="预测天数")


class ProductionPredictResponse(BaseModel):
    """产量预测响应模型"""
    device_id: str = Field(..., description="设备ID")
    predicted_quantity: float = Field(..., ge=0, description="预测产量")
    lower_bound: float = Field(..., ge=0, description="预测下界")
    upper_bound: float = Field(..., ge=0, description="预测上界")
    model_version: str = Field(..., description="模型版本号")


class ModelStatusResponse(BaseModel):
    """模型状态响应模型"""
    quality_model: Dict = Field(..., description="质量模型状态")
    production_model: Dict = Field(..., description="产量模型状态")
    last_trained: datetime.datetime = Field(..., description="最后训练时间")
