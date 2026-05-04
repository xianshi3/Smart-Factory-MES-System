"""API 数据模型定义模块"""
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime


class QualityPredictionRequest(BaseModel):
    """质量预测请求模型"""
    model_config = {"extra": "allow"}
    
    work_order_id: int = Field(..., description="工单ID")
    product_name: str = Field(..., description="产品名称")
    device_code: str = Field(..., description="设备编码")
    temperature: float = Field(..., description="温度(°C)")
    speed: float = Field(..., description="速度(m/min)")
    pressure: float = Field(..., description="压力(MPa)")
    raw_material: Optional[str] = Field(None, description="原材料类型")
    humidity: Optional[float] = Field(None, description="湿度(%)")
    vibration: Optional[float] = Field(None, description="振动值(mm/s)")


class QualityPredictionResponse(BaseModel):
    """质量预测响应模型"""
    model_config = {"extra": "allow"}
    
    prediction_id: str = Field(..., description="预测ID")
    probability: float = Field(..., ge=0.0, le=1.0, description="合格概率")
    prediction: str = Field(..., description="预测结果 PASS/FAIL")
    confidence: float = Field(..., ge=0.0, le=1.0, description="置信度")
    factors: List[Dict[str, Any]] = Field(..., description="影响预测的关键因素")
    model_version: str = Field(..., description="模型版本号")
    timestamp: datetime = Field(..., description="预测时间戳")


class BatchPredictionRequest(BaseModel):
    """批量预测请求模型"""
    model_config = {"extra": "allow"}
    
    predictions: List[QualityPredictionRequest] = Field(..., description="预测请求列表")


class BatchPredictionResponse(BaseModel):
    """批量预测响应模型"""
    model_config = {"extra": "allow"}
    
    results: List[QualityPredictionResponse] = Field(..., description="预测结果列表")
    total_count: int = Field(..., description="总数量")
    success_count: int = Field(..., description="成功数量")


class ModelInfoResponse(BaseModel):
    """模型信息响应模型"""
    model_config = {"extra": "allow"}
    
    model_name: str = Field(..., description="模型名称")
    model_type: str = Field(..., description="模型类型")
    version: str = Field(..., description="模型版本")
    features: List[str] = Field(..., description="特征列表")
    accuracy: Optional[float] = Field(None, description="准确率")
    last_trained: Optional[datetime] = Field(None, description="最后训练时间")
    status: str = Field(..., description="模型状态")


class DeviceFaultPredictionRequest(BaseModel):
    """设备故障预测请求模型"""
    model_config = {"extra": "allow"}
    
    device_code: str = Field(..., description="设备编码")
    history_data: List[Dict[str, float]] = Field(..., description="历史运行数据")
    hours_ahead: int = Field(default=24, ge=1, le=168, description="预测小时数")


class DeviceFaultPredictionResponse(BaseModel):
    """设备故障预测响应模型"""
    model_config = {"extra": "allow"}
    
    device_code: str = Field(..., description="设备编码")
    fault_probability: float = Field(..., ge=0.0, le=1.0, description="故障概率")
    prediction: str = Field(..., description="预测结果 NORMAL/WARNING/FAULT")
    confidence: float = Field(..., ge=0.0, le=1.0, description="置信度")
    risk_factors: List[Dict[str, Any]] = Field(..., description="风险因素")
    model_version: str = Field(..., description="模型版本号")
    timestamp: datetime = Field(..., description="预测时间戳")


class ProcessParamRecommendationRequest(BaseModel):
    """工艺参数推荐请求模型"""
    model_config = {"extra": "allow"}
    
    product_type: str = Field(..., description="产品类型")
    material_properties: Optional[Dict[str, float]] = Field(None, description="材料特性")


class ProcessParamRecommendationResponse(BaseModel):
    """工艺参数推荐响应模型"""
    model_config = {"extra": "allow"}
    
    product_type: str = Field(..., description="产品类型")
    recommended_params: Dict[str, float] = Field(..., description="推荐参数")
    confidence: float = Field(..., ge=0.0, le=1.0, description="置信度")
    model_version: str = Field(..., description="模型版本号")
    timestamp: datetime = Field(..., description="推荐时间戳")


class AnomalyDetectionRequest(BaseModel):
    """异常检测请求模型"""
    model_config = {"extra": "allow"}
    
    sensor_data: Dict[str, float] = Field(..., description="传感器数据")
    device_code: str = Field(..., description="设备编码")


class AnomalyDetectionResponse(BaseModel):
    """异常检测响应模型"""
    model_config = {"extra": "allow"}
    
    is_anomaly: bool = Field(..., description="是否异常")
    anomaly_type: Optional[str] = Field(None, description="异常类型")
    anomaly_score: float = Field(..., ge=0.0, le=1.0, description="异常分数")
    details: List[Dict[str, Any]] = Field(..., description="异常详情")
    timestamp: datetime = Field(..., description="检测时间戳")