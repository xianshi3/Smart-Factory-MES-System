"""特征工程模块"""
import numpy as np
import pandas as pd
from typing import Dict, List, Any, Optional
from collections import deque
import statistics


class FeatureEngineering:
    """特征工程服务，负责数据预处理和特征转换"""
    def __init__(self, config: dict):
        """初始化特征工程服务
        
        Args:
            config: 配置字典
        """
        self.config = config
        feature_cfg = config.get("feature", {})
        self.window_size = feature_cfg.get("sliding_window_size", 60)
        self.aggregation_interval = feature_cfg.get("aggregation_interval", 10)
        self._windows: Dict[str, deque] = {}
        self._scaler_params: Dict[str, Dict[str, float]] = {}

    def transform_quality_features(self, raw_features: Dict[str, float]) -> Dict[str, float]:
        """转换质量预测特征
        
        Args:
            raw_features: 原始特征字典
            
        Returns:
            转换后的特征字典
        """
        transformed = {}
        for key, value in raw_features.items():
            window_key = f"quality_{key}"
            if window_key not in self._windows:
                self._windows[window_key] = deque(maxlen=self.window_size)
            self._windows[window_key].append(value)

            window_data = list(self._windows[window_key])
            if len(window_data) >= 3:
                transformed[f"{key}_mean"] = statistics.mean(window_data)
                transformed[f"{key}_std"] = (
                    statistics.stdev(window_data) if len(window_data) >= 2 else 0.0
                )
                transformed[f"{key}_max"] = max(window_data)
                transformed[f"{key}_min"] = min(window_data)
                transformed[f"{key}_range"] = max(window_data) - min(window_data)
                transformed[f"{key}_trend"] = (
                    window_data[-1] - window_data[0]
                ) / max(len(window_data) - 1, 1)

            transformed[key] = value

        return self._normalize(transformed)

    def transform_production_features(
        self, history_data: List[Dict[str, float]], days_ahead: int = 7
    ) -> List[float]:
        """转换产量预测特征
        
        Args:
            history_data: 历史数据列表
            days_ahead: 预测天数
            
        Returns:
            特征列表
        """
        df = pd.DataFrame(history_data)
        features = []

        quantity_col = None
        for col in df.columns:
            if any(k in col.lower() for k in ["quantity", "output", "yield", "产量"]):
                quantity_col = col
                break

        if quantity_col and quantity_col in df.columns:
            qty_values = df[quantity_col].values.astype(float)
            features.append(float(np.mean(qty_values[-7:])))
            if len(qty_values) >= 7:
                slope = np.polyfit(range(len(qty_values[-7:])), qty_values[-7:], 1)[0]
                features.append(float(slope))
            else:
                features.append(0.0)
        else:
            features.extend([0.0, 0.0])

        oee_col = None
        for col in df.columns:
            if "oee" in col.lower():
                oee_col = col
                break
        features.append(
            float(df[oee_col].iloc[-1]) if oee_col and oee_col in df.columns else 85.0
        )

        planned_col = None
        for col in df.columns:
            if any(k in col.lower() for k in ["plan", "target", "计划"]):
                planned_col = col
                break
        features.append(
            float(df[planned_col].iloc[-1])
            if planned_col and planned_col in df.columns
            else 1000.0
        )

        order_count = len(history_data)
        features.append(float(order_count))

        downtime_col = None
        for col in df.columns:
            if any(k in col.lower() for k in ["downtime", "stop", "停机"]):
                downtime_col = col
                break
        features.append(
            float(df[downtime_col].iloc[-1])
            if downtime_col and downtime_col in df.columns
            else 0.0
        )

        efficiency_col = None
        for col in df.columns:
            if any(k in col.lower() for k in ["efficiency", "效率"]):
                efficiency_col = col
                break
        features.append(
            float(df[efficiency_col].iloc[-1])
            if efficiency_col and efficiency_col in df.columns
            else 0.9
        )

        seasonality = (days_ahead % 12) / 12.0 * 2 * np.pi
        features.append(float(np.sin(seasonality)))

        while len(features) < 8:
            features.append(0.0)

        return features[:8]

    def extract_temporal_features(self, timestamp) -> Dict[str, float]:
        """提取时间特征
        
        Args:
            timestamp: 时间戳
            
        Returns:
            时间特征字典
        """
        if isinstance(timestamp, str):
            from datetime import datetime

            timestamp = datetime.fromisoformat(timestamp)
        return {
            "hour": timestamp.hour / 24.0,
            "day_of_week": timestamp.weekday() / 7.0,
            "day_of_month": timestamp.day / 31.0,
            "month": timestamp.month / 12.0,
            "is_weekend": 1.0 if timestamp.weekday() >= 5 else 0.0,
        }

    def _normalize(self, features: Dict[str, float]) -> Dict[str, float]:
        """特征标准化"""
        normalized = {}
        for key, value in features.items():
            if key not in self._scaler_params:
                self._scaler_params[key] = {"mean": value, "std": 1.0}
            params = self._scaler_params[key]
            std = max(params["std"], 1e-8)
            normalized[key] = (value - params["mean"]) / std
        return normalized

    def update_scaler_params(self, new_params: Dict[str, Dict[str, float]]):
        """更新标准化参数"""
        for key, params in new_params.items():
            self._scaler_params[key] = params

    def reset_windows(self):
        """重置滑动窗口数据"""
        self._windows.clear()
