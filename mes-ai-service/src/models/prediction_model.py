"""质量预测模型模块"""
import os
import pickle
import logging
import numpy as np
import lightgbm as lgb
import onnxruntime as ort
from typing import Dict, Optional

logger = logging.getLogger(__name__)


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
        self.model = None
        self.onnx_session = None
        self.onnx_path = onnx_path
        self._is_onnx = False
        self._try_load(onnx_path)

    def _resolve_path(self, path: str) -> str:
        """相对路径基于 mes-ai-service 根目录解析，避免依赖进程工作目录"""
        if os.path.isabs(path):
            return path
        base = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        return os.path.normpath(os.path.join(base, path))

    def _try_load(self, path: Optional[str]):
        if not path:
            logger.warning("未配置模型路径，将使用默认预测")
            return
        path = self._resolve_path(path)
        if os.path.exists(path):
            try:
                self._load_onnx(path)
                logger.info("ONNX 模型加载成功: %s", path)
                return
            except Exception:
                logger.warning("ONNX 模型加载失败: %s", path)
        else:
            logger.warning("ONNX 模型文件不存在: %s，尝试 Pickle", path)
        pkl_path = path.replace(".onnx", ".pkl")
        if os.path.exists(pkl_path):
            try:
                with open(pkl_path, "rb") as f:
                    self.model = pickle.load(f)
                logger.info("Pickle 模型加载成功(onnx 缺失 fallback): %s", pkl_path)
                return
            except Exception:
                pass
        logger.warning("模型加载失败: %s，将使用默认预测", path)

    def _load_onnx(self, path: str):
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
            outputs = self.onnx_session.run(None, {input_name: x})
            # onnxmltools 输出: [label(int), probability]，概率结构可能是 ndarray / list / dict
            prob_out = outputs[-1]
            row = prob_out[0] if isinstance(prob_out, (list, tuple)) else prob_out
            if isinstance(row, dict):
                prob_pass = float(row.get(1, 0.5))
            else:
                proba = np.asarray(row).reshape(-1)
                prob_pass = float(proba[1]) if proba.size > 1 else float(proba[0])
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



