#!/usr/bin/env python
"""模型训练脚本 — 生成合成数据训练质量预测和产量预测模型，导出 ONNX 格式

Usage:
    python train_models.py
    python train_models.py --samples 5000 --output src/models/saved_models
"""
import argparse
import os
import sys
import json
import pickle
import warnings
import numpy as np

warnings.filterwarnings("ignore")

def generate_quality_data(n_samples=3000):
    """生成合成质量检测数据

    特征顺序与 QualityPredictor.FEATURE_NAMES 保持一致（8 个）:
    temperature, pressure, speed, vibration, runtime_hours, humidity, current, power_consumption
    """
    np.random.seed(42)
    temperature = np.random.uniform(20, 100, n_samples)
    pressure = np.random.uniform(5, 15, n_samples)
    speed = np.random.uniform(30, 80, n_samples)
    vibration = np.random.uniform(0, 1.5, n_samples)
    runtime_hours = np.random.uniform(1, 24, n_samples)
    humidity = np.random.uniform(30, 70, n_samples)
    current = np.random.uniform(5, 30, n_samples)
    power_consumption = np.random.uniform(100, 500, n_samples)

    X = np.column_stack([
        temperature, pressure, speed, vibration,
        runtime_hours, humidity, current, power_consumption,
    ])

    # 质量评分: 参数落在合格区间内得分高，区间外按偏离线性惩罚（向量化）
    def _band_penalty(value, lo, hi, factor, offset=0.0):
        dev = np.maximum(lo - value, 0.0) + np.maximum(value - hi, 0.0)
        return offset - factor * dev

    quality_score = (
        _band_penalty(temperature, 45, 65, 0.4, 0.6)
        + _band_penalty(pressure, 8, 12, 0.3, 0.3)
        + _band_penalty(speed, 40, 60, 0.25, 0.3)
        + _band_penalty(vibration, 0.0, 0.8, 1.5, 0.2)
        + np.random.normal(0, 0.5, n_samples)
    )
    # 转换为合格概率 (sigmoid)
    prob = 1 / (1 + np.exp(-quality_score * 1.2))
    y = (prob > 0.5).astype(int)

    return X, y, prob


def generate_production_data(n_samples=2000):
    """生成合成产量数据

    特征顺序与 ProductionPredictor.FEATURE_NAMES 保持一致（8 个）:
    avg_quantity_7d, trend_slope, oee_value, planned_quantity,
    order_count, downtime_hours, efficiency_ratio, seasonality_factor
    """
    np.random.seed(123)

    X = []
    y = []

    for _ in range(n_samples):
        avg_quantity_7d = np.random.uniform(500, 1500)
        trend_slope = np.random.uniform(-10, 10)
        oee_value = np.random.uniform(0.6, 0.95)
        planned_quantity = np.random.uniform(600, 1600)
        order_count = np.random.randint(3, 20)
        downtime_hours = np.random.uniform(0, 8)
        efficiency_ratio = np.random.uniform(0.7, 1.1)
        seasonality_factor = np.random.uniform(0.85, 1.15)

        feat = [
            avg_quantity_7d, trend_slope, oee_value, planned_quantity,
            order_count, downtime_hours, efficiency_ratio, seasonality_factor,
        ]
        X.append(feat)

        target = max(
            0,
            avg_quantity_7d
            + trend_slope * 7
            + (oee_value - 0.8) * 800
            + planned_quantity * 0.2
            - downtime_hours * 30
            + seasonality_factor * 50
            + np.random.normal(0, 20),
        )
        y.append(target)

    return np.array(X), np.array(y)


def main():
    parser = argparse.ArgumentParser(description="Train models and export to ONNX")
    parser.add_argument("--samples", type=int, default=3000, help="Number of training samples")
    parser.add_argument("--output", type=str, default=None, help="Output directory for ONNX models")
    args = parser.parse_args()

    base_dir = os.path.dirname(os.path.abspath(__file__))
    default_output = os.path.join(base_dir, "src", "models", "saved_models")
    output_dir = args.output or default_output
    os.makedirs(output_dir, exist_ok=True)

    print("=" * 60)
    print("MES AI Model Training Pipeline")
    print("=" * 60)

    # ── Quality Model (LightGBM → ONNX) ──
    print("\n[1/2] Training Quality Prediction Model (LightGBM)...")
    try:
        import lightgbm as lgb
        X_q, y_q, prob_q = generate_quality_data(args.samples)

        model_q = lgb.LGBMClassifier(
            n_estimators=100, max_depth=6, learning_rate=0.05,
            random_state=42, verbose=-1
        )
        model_q.fit(X_q, y_q)

        accuracy = model_q.score(X_q, y_q)
        print(f"  LightGBM trained. Accuracy: {accuracy:.4f}")

        qual_path = os.path.join(output_dir, "quality_predict.onnx")
        pkl_path = qual_path.replace(".onnx", ".pkl")
        # Pickle 始终保存（onnx 被 .gitignore 忽略，新环境无 onnx 时依赖 pkl）
        with open(pkl_path, "wb") as f:
            pickle.dump(model_q, f)
        print(f"  Pickle saved → {pkl_path}")
        # ONNX 尽力导出（可选加速，缺失时运行期自动 fallback pkl）
        try:
            from onnxmltools import convert_lightgbm
            from onnxmltools.convert.common.data_types import FloatTensorType
            initial_type = [("float_input", FloatTensorType([None, X_q.shape[1]]))]
            onnx_model = convert_lightgbm(model_q, initial_types=initial_type, target_opset=12)
            with open(qual_path, "wb") as f:
                f.write(onnx_model.SerializeToString())
            print(f"  ONNX exported → {qual_path}")
        except Exception as e:
            print(f"  ONNX conversion failed ({e}), pickle kept.")
    except ImportError:
        print("  lightgbm not installed. Skipping quality model.")

    # ── Production Model (XGBoost → ONNX) ──
    print("\n[2/2] Training Production Prediction Model (XGBoost)...")
    try:
        import xgboost as xgb
        X_p, y_p = generate_production_data(args.samples)

        model_p = xgb.XGBRegressor(
            n_estimators=100, max_depth=5, learning_rate=0.05,
            random_state=42, verbosity=0
        )
        model_p.fit(X_p, y_p)

        from sklearn.metrics import r2_score
        preds = model_p.predict(X_p)
        r2 = r2_score(y_p, preds)
        print(f"  XGBoost trained. R2: {r2:.4f}")

        prod_path = os.path.join(output_dir, "output_predict.onnx")
        pkl_path = prod_path.replace(".onnx", ".pkl")
        # Pickle 始终保存（onnx 被 .gitignore 忽略，新环境无 onnx 时依赖 pkl）
        with open(pkl_path, "wb") as f:
            pickle.dump(model_p, f)
        print(f"  Pickle saved → {pkl_path}")
        # ONNX 尽力导出（可选加速，缺失时运行期自动 fallback pkl）
        try:
            from onnxmltools import convert_xgboost
            from onnxmltools.convert.common.data_types import FloatTensorType
            initial_type = [("float_input", FloatTensorType([None, X_p.shape[1]]))]
            onnx_model = convert_xgboost(model_p, initial_types=initial_type, target_opset=12)
            with open(prod_path, "wb") as f:
                f.write(onnx_model.SerializeToString())
            print(f"  ONNX exported → {prod_path}")
        except Exception as e:
            print(f"  ONNX conversion failed ({e}), pickle kept.")
    except ImportError:
        print("  xgboost not installed. Skipping production model.")

    # ── Summary ──
    print("\n" + "=" * 60)
    print("Training complete!")
    print(f"Output directory: {output_dir}")
    files = os.listdir(output_dir) if os.path.exists(output_dir) else []
    for f in sorted(files):
        size = os.path.getsize(os.path.join(output_dir, f))
        print(f"  {f} ({size:,} bytes)")
    print("=" * 60)


if __name__ == "__main__":
    main()
