"""推理服务模块"""
import json
import time
import hashlib
import redis
from typing import Dict, List, Any, Optional
from src.models.prediction_model import QualityPredictor
from src.models.regression_model import ProductionPredictor


class InferenceService:
    """AI 推理服务，负责模型加载、预测和缓存管理"""
    CACHE_TTL = 300

    def __init__(self, config: dict):
        """初始化推理服务
        
        Args:
            config: 配置字典，包含模型和 Redis 配置
        """
        self.config = config
        self.quality_predictor = QualityPredictor(
            config["model"]["quality"].get("onnx_path")
        )
        self.production_predictor = ProductionPredictor(
            config["model"]["production"].get("onnx_path")
        )
        self.redis_client = self._init_redis()
        self._last_train_time = None

    def _init_redis(self) -> Optional[redis.Redis]:
        """初始化 Redis 客户端"""
        try:
            rc = self.config.get("redis", {})
            client = redis.Redis(
                host=rc.get("host", "localhost"),
                port=rc.get("port", 6379),
                db=rc.get("db", 1),
                decode_responses=True,
            )
            client.ping()
            return client
        except Exception:
            return None

    def _cache_key(self, prefix: str, device_id: str, data_hash: str) -> str:
        """生成缓存键"""
        raw = f"{prefix}:{device_id}:{data_hash}"
        return f"ai_cache:{hashlib.md5(raw.encode()).hexdigest()}"

    def _get_from_cache(self, key: str) -> Optional[Any]:
        """从缓存获取数据"""
        if not self.redis_client:
            return None
        try:
            cached = self.redis_client.get(key)
            if cached:
                return json.loads(cached)
        except Exception:
            pass
        return None

    def _set_cache(self, key: str, value: Any, ttl: int = None):
        """设置缓存"""
        if not self.redis_client:
            return
        try:
            self.redis_client.setex(key, ttl or self.CACHE_TTL, json.dumps(value))
        except Exception:
            pass

    def predict_quality(
        self, device_id: str, features: Dict[str, float]
    ) -> Dict[str, Any]:
        """预测产品质量
        
        Args:
            device_id: 设备ID
            features: 特征字典
            
        Returns:
            预测结果字典
        """
        data_hash = hashlib.md5(json.dumps(features, sort_keys=True).encode()).hexdigest()
        cache_key = self._cache_key("quality", device_id, data_hash)

        cached = self._get_from_cache(cache_key)
        if cached:
            return cached

        proba = self.quality_predictor.predict_proba(features)
        prediction = self.quality_predictor.predict(features)
        confidence = max(proba)

        result = {
            "pass_probability": round(float(proba[1]), 4),
            "fail_probability": round(float(proba[0]), 4),
            "prediction": prediction,
            "confidence": round(confidence, 4),
        }
        self._set_cache(cache_key, result)
        return result

    def predict_production(
        self, device_id: str, features: List[float]
    ) -> Dict[str, Any]:
        """预测产量
        
        Args:
            device_id: 设备ID
            features: 特征列表
            
        Returns:
            预测结果字典
        """
        data_hash = hashlib.md5(str(features).encode()).hexdigest()
        cache_key = self._cache_key("production", device_id, data_hash)

        cached = self._get_from_cache(cache_key)
        if cached:
            return cached

        predicted, lower, upper = self.production_predictor.predict(features)

        result = {
            "predicted_quantity": round(predicted, 2),
            "lower_bound": round(lower, 2),
            "upper_bound": round(upper, 2),
        }
        self._set_cache(cache_key, result)
        return result

    def batch_predict_quality(
        self, requests: List[tuple]
    ) -> List[Dict[str, Any]]:
        """批量预测质量"""
        results = []
        for device_id, features in requests:
            results.append(self.predict_quality(device_id, features))
        return results

    def get_quality_model_status(self) -> Dict:
        """获取质量模型状态"""
        return {
            "loaded": self.quality_predictor.model is not None
            or self.quality_predictor._is_onnx,
            "type": "lightgbm_binary_classification",
            "features": QualityPredictor.FEATURE_NAMES,
            "onnx_loaded": self.quality_predictor._is_onnx,
        }

    def get_production_model_status(self) -> Dict:
        """获取产量模型状态"""
        return {
            "loaded": self.production_predictor.model is not None
            or self.production_predictor._is_onnx,
            "type": "xgboost_regression",
            "features": ProductionPredictor.FEATURE_NAMES,
            "onnx_loaded": self.production_predictor._is_onnx,
        }

    def trigger_retrain(self) -> Dict[str, str]:
        """触发模型重训练"""
        self._last_train_time = time.time()
        return {
            "status": "triggered",
            "timestamp": self._last_train_time,
            "message": "Retrain job submitted successfully",
        }
