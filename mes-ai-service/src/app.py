"""FastAPI 应用构建模块"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from src.schemas.schemas import (
    QualityPredictRequest,
    QualityPredictResponse,
    ProductionPredictRequest,
    ProductionPredictResponse,
    ModelStatusResponse,
)
from src.services.inference_service import InferenceService
from src.services.feature_engineering import FeatureEngineering
from datetime import datetime
import yaml
import os

config_path = os.path.join(os.path.dirname(__file__), "..", "config.yaml")
with open(config_path, "r", encoding="utf-8") as f:
    config = yaml.safe_load(f)

inference_service = InferenceService(config)
feature_engineering = FeatureEngineering(config)


def create_app() -> FastAPI:
    """创建并配置 FastAPI 应用实例"""
    app = FastAPI(
        title="MES AI Service",
        description="智能工厂AI服务 - 质量预测与产量预测",
        version="1.0.0",
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://localhost:3000", "http://localhost:5173"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.get("/api/v1/health")
    async def health_check():
        """健康检查接口"""
        return {"status": "healthy", "timestamp": datetime.utcnow().isoformat()}

    @app.post("/api/v1/predict/quality", response_model=QualityPredictResponse)
    async def predict_quality(request: QualityPredictRequest):
        """质量预测接口"""
        features = feature_engineering.transform_quality_features(request.features)
        result = inference_service.predict_quality(request.device_id, features)
        return QualityPredictResponse(
            device_id=request.device_id,
            pass_probability=result["pass_probability"],
            fail_probability=result["fail_probability"],
            prediction=result["prediction"],
            confidence=result["confidence"],
            model_version=config["model"]["quality"]["version"],
        )

    @app.post("/api/v1/predict/production", response_model=ProductionPredictResponse)
    async def predict_production(request: ProductionPredictRequest):
        """产量预测接口"""
        features = feature_engineering.transform_production_features(
            request.history_data, request.days_ahead
        )
        result = inference_service.predict_production(request.device_id, features)
        return ProductionPredictResponse(
            device_id=request.device_id,
            predicted_quantity=result["predicted_quantity"],
            lower_bound=result["lower_bound"],
            upper_bound=result["upper_bound"],
            model_version=config["model"]["production"]["version"],
        )

    @app.get("/api/v1/model/status", response_model=ModelStatusResponse)
    async def model_status():
        """获取模型状态"""
        return ModelStatusResponse(
            quality_model=inference_service.get_quality_model_status(),
            production_model=inference_service.get_production_model_status(),
            last_trained=datetime.utcnow(),
        )

    @app.post("/api/v1/model/retrain")
    async def retrain_model():
        """触发模型重训练"""
        result = inference_service.trigger_retrain()
        return {"status": "success", "message": "Retrain triggered", "result": result}

    return app
