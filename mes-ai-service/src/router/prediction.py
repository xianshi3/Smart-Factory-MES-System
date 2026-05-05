"""预测路由模块"""
from fastapi import APIRouter, HTTPException
from datetime import datetime
from typing import List
import logging
import yaml
import os

from src.schemas.prediction import (
    QualityPredictionRequest,
    QualityPredictionResponse,
    BatchPredictionRequest,
    BatchPredictionResponse,
    ModelInfoResponse,
    DeviceFaultPredictionRequest,
    DeviceFaultPredictionResponse,
    ProcessParamRecommendationRequest,
    ProcessParamRecommendationResponse,
    AnomalyDetectionRequest,
    AnomalyDetectionResponse,
)
from src.services.quality_predictor import QualityPredictorService

logger = logging.getLogger(__name__)

config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
try:
    with open(config_path, "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)
except Exception:
    config = {"model": {"quality": {"version": "1.0.0"}}}

quality_service = QualityPredictorService(config)

router = APIRouter(prefix="/api/v1/predict", tags=["预测"])


@router.post("/quality", response_model=QualityPredictionResponse)
async def predict_quality(request: QualityPredictionRequest):
    """质量预测接口
    
    根据工单信息、工艺参数、设备状态预测产品合格概率
    
    Args:
        request: 质量预测请求
        
    Returns:
        质量预测响应
    """
    try:
        result = quality_service.predict(
            work_order_id=request.work_order_id,
            product_name=request.product_name,
            device_code=request.device_code,
            temperature=request.temperature,
            speed=request.speed,
            pressure=request.pressure,
            raw_material=request.raw_material,
            humidity=request.humidity,
            vibration=request.vibration,
        )
        return QualityPredictionResponse(**result)
    except Exception as e:
        logger.error(f"质量预测失败: {e}")
        raise HTTPException(status_code=500, detail=f"预测失败: {str(e)}")


@router.post("/batch", response_model=BatchPredictionResponse)
async def batch_predict(request: BatchPredictionRequest):
    """批量预测接口
    
    批量预测多个工单的质量结果
    
    Args:
        request: 批量预测请求
        
    Returns:
        批量预测响应
    """
    try:
        requests = [
            {
                "work_order_id": req.work_order_id,
                "product_name": req.product_name,
                "device_code": req.device_code,
                "temperature": req.temperature,
                "speed": req.speed,
                "pressure": req.pressure,
                "raw_material": req.raw_material,
                "humidity": req.humidity,
                "vibration": req.vibration,
            }
            for req in request.predictions
        ]
        results = quality_service.batch_predict(requests)
        
        return BatchPredictionResponse(
            results=[QualityPredictionResponse(**r) for r in results],
            total_count=len(results),
            success_count=len(results),
        )
    except Exception as e:
        logger.error(f"批量预测失败: {e}")
        raise HTTPException(status_code=500, detail=f"批量预测失败: {str(e)}")


@router.get("/model/info", response_model=ModelInfoResponse)
async def get_model_info():
    """获取模型信息接口
    
    Returns:
        模型信息响应
    """
    try:
        info = quality_service.get_model_info()
        return ModelInfoResponse(
            model_name=info["model_name"],
            model_type=info["model_type"],
            version=info["version"],
            features=info["features"],
            accuracy=None,
            last_trained=None,
            status=info["status"],
        )
    except Exception as e:
        logger.error(f"获取模型信息失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/device/fault", response_model=DeviceFaultPredictionResponse)
async def predict_device_fault(request: DeviceFaultPredictionRequest):
    """设备故障预测接口
    
    根据设备历史运行数据预测未来N小时故障概率
    
    Args:
        request: 设备故障预测请求
        
    Returns:
        设备故障预测响应
    """
    try:
        history = request.history_data
        if not history:
            raise HTTPException(status_code=400, detail="历史数据不能为空")
        
        import numpy as np
        temps = [h.get("temperature", 80) for h in history]
        speeds = [h.get("speed", 50) for h in history]
        
        temp_mean = np.mean(temps)
        temp_std = np.std(temps) if len(temps) > 1 else 0
        speed_mean = np.mean(speeds)
        
        fault_prob = 0.1
        risk_factors = []
        
        if temp_mean > 85:
            fault_prob += 0.2
            risk_factors.append({
                "factor": "temperature_high",
                "value": temp_mean,
                "impact": 0.2,
                "description": "温度持续偏高"
            })
        
        if temp_std > 10:
            fault_prob += 0.15
            risk_factors.append({
                "factor": "temperature_volatile",
                "value": temp_std,
                "impact": 0.15,
                "description": "温度波动大"
            })
        
        if speed_mean > 60:
            fault_prob += 0.1
            risk_factors.append({
                "factor": "speed_high",
                "value": speed_mean,
                "impact": 0.1,
                "description": "运行速度偏高"
            })
        
        if not risk_factors:
            risk_factors.append({
                "factor": "normal",
                "value": 0.0,
                "impact": 0.0,
                "description": "设备运行正常"
            })
        
        fault_prob = min(fault_prob, 0.99)
        
        prediction = "FAULT" if fault_prob > 0.5 else "NORMAL" if fault_prob < 0.3 else "WARNING"
        
        return DeviceFaultPredictionResponse(
            device_code=request.device_code,
            fault_probability=round(fault_prob, 4),
            prediction=prediction,
            confidence=round(1.0 - fault_prob * 0.5, 4),
            risk_factors=risk_factors,
            model_version="1.0.0",
            timestamp=datetime.utcnow(),
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"设备故障预测失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/process/recommend", response_model=ProcessParamRecommendationResponse)
async def recommend_process_params(request: ProcessParamRecommendationRequest):
    """工艺参数推荐接口
    
    根据产品类型和材料特性推荐最佳工艺参数
    
    Args:
        request: 工艺参数推荐请求
        
    Returns:
        工艺参数推荐响应
    """
    try:
        product_type = request.product_type
        material_props = request.material_properties or {}
        
        params = {
            "temperature": 80.0,
            "speed": 50.0,
            "pressure": 10.0,
        }
        
        if "PET" in product_type.upper():
            params["temperature"] = 75.0
            params["speed"] = 55.0
            params["pressure"] = 8.0
        elif "PP" in product_type.upper():
            params["temperature"] = 85.0
            params["speed"] = 45.0
            params["pressure"] = 12.0
        elif "PVC" in product_type.upper():
            params["temperature"] = 70.0
            params["speed"] = 40.0
            params["pressure"] = 15.0
        
        if "density" in material_props:
            if material_props["density"] > 1.0:
                params["pressure"] *= 1.1
        if "hardness" in material_props:
            if material_props["hardness"] > 80:
                params["temperature"] *= 1.05
        
        return ProcessParamRecommendationResponse(
            product_type=product_type,
            recommended_params=params,
            confidence=0.85,
            model_version="1.0.0",
            timestamp=datetime.utcnow(),
        )
    except Exception as e:
        logger.error(f"工艺参数推荐失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/anomaly", response_model=AnomalyDetectionResponse)
async def detect_anomaly(request: AnomalyDetectionRequest):
    """异常检测接口
    
    对实时传感器数据进行异常检测
    
    Args:
        request: 异常检测请求
        
    Returns:
        异常检测响应
    """
    try:
        sensor_data = request.sensor_data
        
        thresholds = {
            "temperature": (60, 100),
            "speed": (30, 70),
            "pressure": (5, 15),
            "vibration": (0.0, 1.0),
        }
        
        is_anomaly = False
        anomaly_type = None
        anomaly_score = 0.0
        details = []
        
        for sensor, value in sensor_data.items():
            if sensor in thresholds:
                min_val, max_val = thresholds[sensor]
                if value < min_val or value > max_val:
                    is_anomaly = True
                    impact = abs(value - (min_val + max_val) / 2) / ((max_val - min_val) / 2)
                    anomaly_score = max(anomaly_score, min(impact, 1.0))
                    details.append({
                        "sensor": sensor,
                        "value": value,
                        "threshold": thresholds[sensor],
                        "deviation": round(impact, 4),
                    })
                    if not anomaly_type:
                        anomaly_type = f"{sensor}_abnormal"
        
        if not details:
            details.append({
                "sensor": "all",
                "value": 0.0,
                "threshold": (0, 0),
                "deviation": 0.0,
            })
        
        return AnomalyDetectionResponse(
            is_anomaly=is_anomaly,
            anomaly_type=anomaly_type,
            anomaly_score=round(anomaly_score, 4),
            details=details,
            timestamp=datetime.utcnow(),
        )
    except Exception as e:
        logger.error(f"异常检测失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))