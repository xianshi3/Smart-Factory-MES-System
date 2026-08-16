"""FastAPI 应用构建模块"""
from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from src.security import verify_token
from src.schemas.schemas import ModelStatusResponse
from src.services.inference_service import InferenceService
from src.services.feature_engineering import FeatureEngineering
from src.services.conversation_store import init_db
from src.router.prediction import router as prediction_router
from src.router.llm import router as llm_router
from src.router.analysis import router as analysis_router
from src.router.agent import router as agent_router
from datetime import datetime
import yaml
import os
import logging

logger = logging.getLogger(__name__)

config_path = os.path.join(os.path.dirname(__file__), "..", "config.yaml")
with open(config_path, "r", encoding="utf-8") as f:
    config = yaml.safe_load(f)

inference_service = InferenceService(config)
feature_engineering = FeatureEngineering(config)


def create_app() -> FastAPI:
    """创建并配置 FastAPI 应用实例"""
    init_db()
    logger.info("对话历史数据库已初始化")
    app = FastAPI(
        title="MES AI Service",
        description="虚拟路径AI服务 - 质量预测与产量预测",
        version="1.0.0",
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://localhost:3000", "http://localhost:5173"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.middleware("http")
    async def attach_token_context(request, call_next):
        """把请求携带的 JWT 存入 contextvar，供 Agent 工具调用后端时透传。

        注意：不能用 FastAPI 依赖（dependencies）做这件事——依赖与端点之间
        contextvar 不传播，导致工具调用丢失 Authorization 头（401）。
        """
        auth = request.headers.get("Authorization", "")
        if auth.startswith("Bearer "):
            from src.security import set_token
            set_token(auth[len("Bearer "):].strip())
        return await call_next(request)

    # 除健康检查外，所有业务接口必须携带有效 JWT（与后端网关密钥一致）
    auth_deps = [Depends(verify_token)]

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

    app.include_router(prediction_router, dependencies=auth_deps)
    app.include_router(llm_router, dependencies=auth_deps)
    app.include_router(analysis_router, dependencies=auth_deps)
    app.include_router(agent_router, dependencies=auth_deps)

    return app
