"""智能分析路由模块"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime
import logging
import yaml
import os

from src.services.analysis_service import create_analysis_services
from src.services.llm_service import LLmService

logger = logging.getLogger(__name__)

config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
try:
    with open(config_path, "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)
except Exception:
    config = {"llm": {"model": "glm-4-flash"}}

llm_service = LLmService(config)
services = create_analysis_services(llm_service)

router = APIRouter(prefix="/api/v1/analysis", tags=["智能分析"])


class EnergyOptimizationRequest(BaseModel):
    """能耗优化请求"""
    device_code: str = Field(..., description="设备编码")
    current_params: Dict[str, float] = Field(..., description="当前参数")
    target_output: float = Field(..., description="目标产量")
    time_period: Optional[str] = Field("DAILY", description="时间周期")


class SPCAnalysisRequest(BaseModel):
    """SPC分析请求"""
    device_code: str = Field(..., description="设备编码")
    parameter: str = Field(..., description="参数名")
    measurements: List[float] = Field(..., description="测量值列表")


class CapacityPredictionRequest(BaseModel):
    """产能预测请求"""
    production_line_id: str = Field(..., description="生产线ID")
    product_type: str = Field(..., description="产品类型")
    start_date: str = Field(..., description="开始日期")
    days_ahead: Optional[int] = Field(7, description="预测天数")


class RootCauseAnalysisRequest(BaseModel):
    """根因分析请求"""
    quality_record_id: int = Field(..., description="质量记录ID")
    include_similar_cases: Optional[bool] = Field(True, description="包含相似案例")


class DeliveryPredictionRequest(BaseModel):
    """交期预测请求"""
    order_ids: List[int] = Field(..., description="工单ID列表")


@router.post("/energy/optimize")
async def optimize_energy(request: EnergyOptimizationRequest):
    """能耗优化接口"""
    try:
        result = services["energy"].optimize(
            device_code=request.device_code,
            current_params=request.current_params,
            target_output=request.target_output,
            time_period=request.time_period,
        )
        return {"success": True, "data": result, "timestamp": datetime.utcnow()}
    except Exception as e:
        logger.error(f"能耗优化失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/spc/analyze")
async def analyze_spc(request: SPCAnalysisRequest):
    """SPC统计过程控制分析"""
    try:
        result = services["spc"].analyze(
            device_code=request.device_code,
            parameter=request.parameter,
            measurements=request.measurements,
        )
        return {"success": True, "data": result, "timestamp": datetime.utcnow()}
    except Exception as e:
        logger.error(f"SPC分析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/capacity/predict")
async def predict_capacity(request: CapacityPredictionRequest):
    """产能预测"""
    try:
        result = services["capacity"].predict(
            production_line_id=request.production_line_id,
            product_type=request.product_type,
            start_date=request.start_date,
            days_ahead=request.days_ahead,
        )
        return {"success": True, "data": result, "timestamp": datetime.utcnow()}
    except Exception as e:
        logger.error(f"产能预测失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/root-cause/analyze")
async def analyze_root_cause(request: RootCauseAnalysisRequest):
    """质量根因分析"""
    try:
        result = services["root_cause"].analyze(
            quality_record_id=request.quality_record_id,
            include_similar_cases=request.include_similar_cases,
        )
        return {"success": True, "data": result, "timestamp": datetime.utcnow()}
    except Exception as e:
        logger.error(f"根因分析失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/delivery/predict")
async def predict_delivery(request: DeliveryPredictionRequest):
    """交期预测"""
    try:
        result = services["delivery"].predict(
            order_ids=request.order_ids,
        )
        return {"success": True, "data": result, "timestamp": datetime.utcnow()}
    except Exception as e:
        logger.error(f"交期预测失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))