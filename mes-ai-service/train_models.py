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
import warnings
import numpy as np

warnings.filterwarnings("ignore")

def generate_quality_data(n_samples=3000):
    """生成合成质量检测数据"""
    np.random.seed(42)
    # 特征: [温度, 转速, 压力, 振动, 材料密度, 硬度, 重量, 加工时间, 设备编号, 炉温]
    temp = np.random.uniform(20, 100, n_samples)
    speed = np.random.uniform(30, 80, n_samples)
    pressure = np.random.uniform(5, 15, n_samples)
    vibration = np.random.uniform(0, 1.5, n_samples)
    density = np.random.uniform(0.9, 1.5, n_samples)
    hardness = np.random.uniform(40, 90, n_samples)
    weight = np.random.uniform(100, 500, n_samples)
    proc_time = np.random.uniform(5, 60, n_samples)
    device_id = np.random.randint(0, 5, n_samples)
    oven_temp = np.random.uniform(150, 250, n_samples)

    X = np.column_stack([temp, speed, pressure, vibration, density, hardness, weight, proc_time, device_id, oven_temp])

    # 质量评分: 温度适中(40-60), 转速适中(40-60), 振动低, 密度中等 得分高
    quality_score = (
        -np.abs(temp - 50) * 0.5
        - np.abs(speed - 55) * 0.3
        - vibration * 3
        - np.abs(density - 1.2) * 2
        + np.abs(hardness - 65) * 0.1
        + np.random.normal(0, 1, n_samples)
    )
    # 转换为合格概率 (sigmoid)
    prob = 1 / (1 + np.exp(-quality_score * 0.3))
    y = (prob > 0.5).astype(int)

    return X, y, prob


def generate_production_data(n_samples=2000):
    """生成合成产量数据"""
    np.random.seed(123)
    n_features = 10
    seq_len = 14

    X = []
    y = []

    for _ in range(n_samples):
        base = np.random.uniform(500, 1500)
        trend = np.random.uniform(-10, 10)
        season = np.random.uniform(0, 2 * np.pi)
        seq = []
        for t in range(seq_len):
            val = base + trend * t + 50 * np.sin(season + t * 0.3) + np.random.normal(0, 30)
            seq.append(max(0, val))
        feat = [np.mean(seq), np.std(seq), trend, base]
        # 添加更多特征
        feat.extend([
            seq[-1], seq[-2],
            np.max(seq), np.min(seq),
            np.random.uniform(0.8, 1.2),
            np.random.uniform(0, 5),
            np.random.uniform(100, 300),
        ])
        X.append(feat)
        target = max(0, base + trend * seq_len + 30 * np.sin(season + seq_len * 0.3) + np.random.normal(0, 20))
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
        try:
            from onnxmltools import convert_lightgbm
            from onnxmltools.convert.common.data_types import FloatTensorType
            initial_type = [("float_input", FloatTensorType([None, X_q.shape[1]]))]
            onnx_model = convert_lightgbm(model_q, initial_types=initial_type, target_opset=12)
            with open(qual_path, "wb") as f:
                f.write(onnx_model.SerializeToString())
            print(f"  ONNX exported → {qual_path}")
        except Exception as e:
            print(f"  ONNX conversion failed ({e}), saving pickle instead.")
            import pickle
            pkl_path = qual_path.replace(".onnx", ".pkl")
            with open(pkl_path, "wb") as f:
                pickle.dump(model_q, f)
            print(f"  Pickle saved → {pkl_path}")
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
        try:
            from onnxmltools import convert_xgboost
            from onnxmltools.convert.common.data_types import FloatTensorType
            initial_type = [("float_input", FloatTensorType([None, X_p.shape[1]]))]
            onnx_model = convert_xgboost(model_p, initial_types=initial_type, target_opset=12)
            with open(prod_path, "wb") as f:
                f.write(onnx_model.SerializeToString())
            print(f"  ONNX exported → {prod_path}")
        except Exception as e:
            print(f"  ONNX conversion failed ({e}), saving pickle instead.")
            import pickle
            pkl_path = prod_path.replace(".onnx", ".pkl")
            with open(pkl_path, "wb") as f:
                pickle.dump(model_p, f)
            print(f"  Pickle saved → {pkl_path}")
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
