"""产量预测模型模块"""
import os
import pickle
import numpy as np
import xgboost as xgb
import onnxruntime as ort
from typing import Dict, List, Optional


class ProductionPredictor:
    """产量预测器，使用 XGBoost 或 ONNX 模型进行回归预测"""
    FEATURE_NAMES = [
        "avg_quantity_7d",
        "trend_slope",
        "oee_value",
        "planned_quantity",
        "order_count",
        "downtime_hours",
        "efficiency_ratio",
        "seasonality_factor",
    ]

    def __init__(self, onnx_path: Optional[str] = None):
        self.model = None
        self.onnx_session = None
        self.onnx_path = onnx_path
        self._is_onnx = False
        self._try_load(onnx_path)

    def _try_load(self, path: Optional[str]):
        if not path or not os.path.exists(path):
            return
        try:
            self._load_onnx(path)
            return
        except Exception:
            pass
        pkl_path = path.replace(".onnx", ".pkl")
        if os.path.exists(pkl_path):
            try:
                with open(pkl_path, "rb") as f:
                    self.model = pickle.load(f)
                return
            except Exception:
                pass

    def _load_onnx(self, path: str):
        self.onnx_session = ort.InferenceSession(path)
        self._is_onnx = True

    def train(self, X: np.ndarray, y: np.ndarray, **kwargs):
        """训练产量预测模型
        
        Args:
            X: 特征数据
            y: 标签数据
            **kwargs: XGBoost 训练参数
        """
        dtrain = xgb.DMatrix(X, label=y)
        params = {
            "objective": "reg:squarederror",
            "eval_metric": "rmse",
            "max_depth": 6,
            "learning_rate": 0.05,
            "subsample": 0.8,
            "colsample_bytree": 0.8,
        }
        params.update(kwargs)
        self.model = xgb.train(params, dtrain, num_boost_round=100)
        self._is_onnx = False

    def predict(self, features: List[float]) -> tuple:
        """预测产量
        
        Args:
            features: 特征列表
            
        Returns:
            元组 (预测值, 下界, 上界)
        """
        x = np.array(features, dtype=np.float32).reshape(1, -1)

        if self._is_onnx and self.onnx_session:
            input_name = self.onnx_session.get_inputs()[0].name
            result = self.onnx_session.run(None, {input_name: x})
            predicted = float(result[0][0][0])
            uncertainty = float(result[1][0][0]) if len(result) > 1 else predicted * 0.1
        elif self.model is not None:
            dmatrix = xgb.DMatrix(x)
            predicted = float(self.model.predict(dmatrix)[0])
            uncertainty = abs(predicted * 0.1)
        else:
            predicted = 1000.0
            uncertainty = predicted * 0.1

        return predicted, max(0, predicted - uncertainty), predicted + uncertainty

    def batch_predict(self, features_list: List[List[float]]) -> np.ndarray:
        """批量预测产量
        
        Args:
            features_list: 特征列表的列表
            
        Returns:
            预测结果数组
        """
        x = np.array(features_list, dtype=np.float32)
        if self._is_onnx and self.onnx_session:
            input_name = self.onnx_session.get_inputs()[0].name
            result = self.onnx_session.run(None, {input_name: x})[0]
            return result.flatten()
        elif self.model is not None:
            dmatrix = xgb.DMatrix(x)
            return self.model.predict(dmatrix)
        return np.full(len(features_list), 1000.0)



