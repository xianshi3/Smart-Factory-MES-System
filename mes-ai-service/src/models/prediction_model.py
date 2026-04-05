"""质量预测模型模块"""
import numpy as np
import lightgbm as lgb
import onnxruntime as ort
from typing import Dict, Optional


class QualityPredictor:
    """产品质量预测器，使用 LightGBM 或 ONNX 模型进行二分类预测"""
    FEATURE_NAMES = [
        "temperature",
        "pressure",
        "speed",
        "vibration",
        "runtime_hours",
        "humidity",
        "current",
        "power_consumption",
    ]

    def __init__(self, onnx_path: Optional[str] = None):
        """初始化预测器
        
        Args:
            onnx_path: ONNX 模型文件路径，如果提供则加载 ONNX 模型
        """
        self.model = None
        self.onnx_session = None
        self.onnx_path = onnx_path
        self._is_onnx = False
        if onnx_path and os.path.exists(onnx_path):
            self._load_onnx(onnx_path)

    def _load_onnx(self, path: str):
        """加载 ONNX 模型"""
        self.onnx_session = ort.InferenceSession(path)
        self._is_onnx = True

    def train(self, X: np.ndarray, y: np.ndarray, **kwargs):
        """训练质量预测模型
        
        Args:
            X: 特征数据
            y: 标签数据
            **kwargs: LightGBM 训练参数
        """
        dtrain = lgb.Dataset(X, label=y)
        params = {
            "objective": "binary",
            "metric": ["auc", "binary_logloss"],
            "boosting_type": "gbdt",
            "num_leaves": 31,
            "learning_rate": 0.05,
            "feature_fraction": 0.8,
            "bagging_fraction": 0.8,
            "bagging_freq": 5,
            "verbose": -1,
        }
        params.update(kwargs)
        self.model = lgb.train(
            params, dtrain, num_boost_round=100, valid_sets=[dtrain]
        )
        self._is_onnx = False

    def predict_proba(self, features: Dict[str, float]) -> np.ndarray:
        """预测产品合格的概率
        
        Args:
            features: 特征字典
            
        Returns:
            概率数组 [fail_probability, pass_probability]
        """
        x = np.array(
            [features.get(name, 0.0) for name in self.FEATURE_NAMES],
            dtype=np.float32,
        ).reshape(1, -1)

        if self._is_onnx and self.onnx_session:
            input_name = self.onnx_session.get_inputs()[0].name
            result = self.onnx_session.run(None, {input_name: x})[0][0]
            if result.ndim == 0:
                prob_pass = float(result)
            else:
                prob_pass = float(result[1]) if result.shape[0] > 1 else float(result[0])
            prob_fail = 1.0 - prob_pass
            return np.array([prob_fail, prob_pass])

        if self.model is not None:
            proba = self.model.predict(x)[0]
            if isinstance(proba, np.ndarray) and len(proba) > 1:
                return proba
            prob_pass = float(proba)
            return np.array([1.0 - prob_pass, prob_pass])

        return np.array([0.3, 0.7])

    def predict(self, features: Dict[str, float]) -> str:
        """预测产品质量结果
        
        Args:
            features: 特征字典
            
        Returns:
            预测结果 "PASSED" 或 "FAILED"
        """
        proba = self.predict_proba(features)
        return "PASSED" if proba[1] >= 0.5 else "FAILED"


import os
