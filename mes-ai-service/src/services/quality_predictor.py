"""质量预测服务模块"""
import json
import hashlib
import uuid
from typing import Dict, List, Any, Optional
from datetime import datetime
import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler, LabelEncoder
import redis
import logging

from src.models.prediction_model import QualityPredictor

logger = logging.getLogger(__name__)


class QualityPredictorService:
    """质量预测服务，负责特征工程、模型推理和结果解析"""

    FEATURE_COLUMNS = [
        "temperature", "speed", "pressure", "humidity", "vibration",
        "temp_normalized", "speed_normalized", "pressure_normalized",
        "temp_speed_interaction", "temp_pressure_interaction", "speed_pressure_interaction"
    ]

    MATERIAL_ENCODING = {
        "A": 0, "B": 1, "C": 2, "D": 3, "E": 4,
        "PET": 0, "PP": 1, "PVC": 2, "PE": 3, "ABS": 4
    }

    def __init__(self, config: dict):
        """初始化质量预测服务
        
        Args:
            config: 配置字典
        """
        self.config = config
        self.model = QualityPredictor(
            config.get("model", {}).get("quality", {}).get("onnx_path")
        )
        self._init_scaler()
        self._init_label_encoder()
        self.redis_client = self._init_redis()
        self.model_version = config.get("model", {}).get("quality", {}).get("version", "1.0.0")

    def _init_scaler(self):
        """初始化标准化器"""
        self.scaler = StandardScaler()
        default_params = {
            "temperature": {"mean": 80.0, "std": 15.0},
            "speed": {"mean": 50.0, "std": 10.0},
            "pressure": {"mean": 10.0, "std": 2.0},
            "humidity": {"mean": 50.0, "std": 10.0},
            "vibration": {"mean": 0.5, "std": 0.2},
        }
        for name, params in default_params.items():
            self.scaler.mean_ = np.array([params["mean"]])
            self.scaler.scale_ = np.array([params["std"]])
            self.scaler.var_ = np.array([params["std"] ** 2])
            self.scaler.n_features_in_ = 1

    def _init_label_encoder(self):
        """初始化标签编码器"""
        self.material_encoder = LabelEncoder()
        self.material_encoder.fit(list(self.MATERIAL_ENCODING.keys())[:5])

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
        except Exception as e:
            logger.warning(f"Redis 连接失败: {e}")
            return None

    def _cache_key(self, data_hash: str) -> str:
        """生成缓存键"""
        return f"quality_predict:{data_hash}"

    def _get_cache(self, key: str) -> Optional[Dict]:
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

    def _set_cache(self, key: str, value: Dict, ttl: int = 300):
        """设置缓存"""
        if not self.redis_client:
            return
        try:
            self.redis_client.setex(key, ttl, json.dumps(value, default=str))
        except Exception:
            pass

    def engineer_features(
        self,
        temperature: float,
        speed: float,
        pressure: float,
        humidity: Optional[float] = None,
        vibration: Optional[float] = None,
        raw_material: Optional[str] = None,
    ) -> Dict[str, float]:
        """特征工程: 数值归一化、编码、特征组合
        
        Args:
            temperature: 温度
            speed: 速度
            pressure: 压力
            humidity: 湿度
            vibration: 振动值
            raw_material: 原材料类型
            
        Returns:
            特征字典
        """
        features = {}
        
        temp_mean, temp_std = 80.0, 15.0
        speed_mean, speed_std = 50.0, 10.0
        pressure_mean, pressure_std = 10.0, 2.0
        
        features["temperature"] = temperature
        features["speed"] = speed
        features["pressure"] = pressure
        features["humidity"] = humidity if humidity is not None else 50.0
        features["vibration"] = vibration if vibration is not None else 0.5
        # 模型输入缺省值（对应训练分布中位：运行时长 8h / 电流 12A / 功耗 300W）
        features["runtime_hours"] = 8.0
        features["current"] = 12.0
        features["power_consumption"] = 300.0
        
        features["temp_normalized"] = (temperature - temp_mean) / temp_std
        features["speed_normalized"] = (speed - speed_mean) / speed_std
        features["pressure_normalized"] = (pressure - pressure_mean) / pressure_std
        
        features["temp_speed_interaction"] = features["temp_normalized"] * features["speed_normalized"]
        features["temp_pressure_interaction"] = features["temp_normalized"] * features["pressure_normalized"]
        features["speed_pressure_interaction"] = features["speed_normalized"] * features["pressure_normalized"]
        
        if raw_material:
            features["material_encoded"] = self.MATERIAL_ENCODING.get(raw_material.upper()[:1], 0)
            features["material_PET"] = 1.0 if raw_material.upper().startswith("PET") else 0.0
            features["material_PP"] = 1.0 if raw_material.upper().startswith("PP") else 0.0
            features["material_PVC"] = 1.0 if raw_material.upper().startswith("PVC") else 0.0
        else:
            features["material_encoded"] = 0.0
            features["material_PET"] = 0.0
            features["material_PP"] = 0.0
            features["material_PVC"] = 0.0
        
        return features

    def analyze_factors(self, features: Dict[str, float], probability: float) -> List[Dict[str, float]]:
        """分析影响预测的关键因素
        
        Args:
            features: 特征字典
            probability: 合格概率
            
        Returns:
            因素列表
        """
        factors = []
        
        if features.get("temperature", 0) > 85:
            factors.append({
                "factor": "temperature",
                "value": features["temperature"],
                "impact": -0.15,
                "description": "温度偏高,可能导致产品变形"
            })
        elif features.get("temperature", 0) < 75:
            factors.append({
                "factor": "temperature",
                "value": features["temperature"],
                "impact": -0.1,
                "description": "温度偏低,固化不完全"
            })
        
        if features.get("speed", 0) > 60:
            factors.append({
                "factor": "speed",
                "value": features["speed"],
                "impact": -0.12,
                "description": "速度过快,可能导致产品质量不稳定"
            })
        
        if features.get("pressure", 0) > 12:
            factors.append({
                "factor": "pressure",
                "value": features["pressure"],
                "impact": -0.08,
                "description": "压力过大,可能造成产品损坏"
            })
        
        if features.get("vibration", 0) > 0.8:
            factors.append({
                "factor": "vibration",
                "value": "vibration",
                "impact": -0.2,
                "description": "振动过大,设备运行不稳"
            })
        
        if not factors:
            factors.append({
                "factor": "all_normal",
                "value": 0.0,
                "impact": 0.05,
                "description": "各项参数均在正常范围内"
            })
        
        return factors[:5]

    def predict(
        self,
        work_order_id: int,
        product_name: str,
        device_code: str,
        temperature: float,
        speed: float,
        pressure: float,
        raw_material: Optional[str] = None,
        humidity: Optional[float] = None,
        vibration: Optional[float] = None,
    ) -> Dict[str, Any]:
        """质量预测推理
        
        Args:
            work_order_id: 工单ID
            product_name: 产品名称
            device_code: 设备编码
            temperature: 温度
            speed: 速度
            pressure: 压力
            raw_material: 原材料
            humidity: 湿度
            vibration: 振动值
            
        Returns:
            预测结果字典
        """
        features = self.engineer_features(
            temperature, speed, pressure, humidity, vibration, raw_material
        )
        
        data_hash = hashlib.md5(
            json.dumps(features, sort_keys=True).encode()
        ).hexdigest()
        cache_key = self._cache_key(data_hash)
        
        cached = self._get_cache(cache_key)
        if cached:
            return cached
        
        try:
            proba = self.model.predict_proba(features)
            prob_pass = float(proba[1])
            confidence = max(proba)
            
            prediction = "PASS" if prob_pass >= 0.5 else "FAIL"
            
            factors = self.analyze_factors(features, prob_pass)
            
            result = {
                "prediction_id": str(uuid.uuid4()),
                "probability": round(prob_pass, 4),
                "prediction": prediction,
                "confidence": round(confidence, 4),
                "factors": factors,
                "model_version": self.model_version,
                "timestamp": datetime.utcnow(),
            }
            
            self._set_cache(cache_key, result)
            return result
            
        except Exception as e:
            logger.error(f"预测推理失败: {e}")
            result = {
                "prediction_id": str(uuid.uuid4()),
                "probability": 0.7,
                "prediction": "PASS",
                "confidence": 0.5,
                "factors": [{"factor": "fallback", "value": 0.0, "impact": 0.0, "description": "默认预测"}],
                "model_version": self.model_version,
                "timestamp": datetime.utcnow(),
            }
            return result

    def batch_predict(
        self, requests: List[Dict]
    ) -> List[Dict[str, Any]]:
        """批量预测
        
        Args:
            requests: 请求列表
            
        Returns:
            预测结果列表
        """
        results = []
        for req in requests:
            result = self.predict(
                work_order_id=req.get("work_order_id", 0),
                product_name=req.get("product_name", ""),
                device_code=req.get("device_code", ""),
                temperature=req.get("temperature", 80.0),
                speed=req.get("speed", 50.0),
                pressure=req.get("pressure", 10.0),
                raw_material=req.get("raw_material"),
                humidity=req.get("humidity"),
                vibration=req.get("vibration"),
            )
            results.append(result)
        return results

    def get_model_info(self) -> Dict[str, Any]:
        """获取模型信息"""
        return {
            "model_name": "quality_predictor",
            "model_type": "lightgbm_binary_classification",
            "version": self.model_version,
            "features": self.FEATURE_COLUMNS,
            "status": "loaded" if self.model is not None else "not_loaded",
        }