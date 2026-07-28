"""FastAPI 应用构建模块"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from src.schemas.schemas import ModelStatusResponse
from src.services.inference_service import InferenceService
from src.services.feature_engineering import FeatureEngineering
from src.router.prediction import router as prediction_router
from src.router.llm import router as llm_router
from src.router.analysis import router as analysis_router
from src.router.agent import router as agent_router
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
        return {"status": "healthy", "timestamp": datetime.utcnow().isoformat()}

    @app.get("/api/v1/model/status", response_model=ModelStatusResponse)
    async def model_status():
        return ModelStatusResponse(
            quality_model=inference_service.get_quality_model_status(),
            production_model=inference_service.get_production_model_status(),
            last_trained=datetime.utcnow(),
        )

    app.include_router(prediction_router)
    app.include_router(llm_router)
    app.include_router(analysis_router)
    app.include_router(agent_router)

    return app
