"""
AI模型训练脚本
用于生成真实的质量预测和产量预测模型

使用方法:
    python scripts/train_models.py
"""
import os
import sys
import numpy as np
import pandas as pd
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

from models.prediction_model import QualityPredictor
from models.regression_model import ProductionPredictor
import lightgbm as lgb


def generate_synthetic_data(n_samples: int = 10000) -> pd.DataFrame:
    """生成模拟的设备数据用于模型训练
    
    Args:
        n_samples: 样本数量
        
    Returns:
        数据框
    """
    np.random.seed(42)
    
    data = {
        'temperature': np.random.normal(60, 15, n_samples),
        'pressure': np.random.normal(100, 25, n_samples),
        'speed': np.random.normal(50, 12, n_samples),
        'vibration': np.random.normal(5, 2, n_samples),
        'runtime_hours': np.random.exponential(50, n_samples),
        'humidity': np.random.uniform(30, 80, n_samples),
        'current': np.random.normal(10, 3, n_samples),
        'power_consumption': np.random.exponential(20, n_samples),
    }
    
    df = pd.DataFrame(data)
    
    quality_score = (
        0.3 * (1 - np.abs(df['temperature'] - 55) / 30) +
        0.2 * (1 - np.abs(df['pressure'] - 90) / 50) +
        0.2 * (1 - np.abs(df['speed'] - 45) / 30) +
        0.15 * (1 - df['vibration'] / 10) +
        0.15 * (df['runtime_hours'] < 100 and 1 or 0.5)
    )
    
    df['quality_label'] = (quality_score + np.random.normal(0, 0.1, n_samples) > 0.6).astype(int)
    
    production_output = (
        df['speed'] * 2 +
        df['runtime_hours'] * 0.5 +
        np.random.normal(0, 10, n_samples)
    )
    df['production_output'] = np.maximum(0, production_output).astype(int)
    
    return df


def train_quality_model():
    """训练质量预测模型"""
    print("=" * 50)
    print("训练质量预测模型 (LightGBM)")
    print("=" * 50)
    
    df = generate_synthetic_data(10000)
    
    feature_cols = [
        'temperature', 'pressure', 'speed', 'vibration',
        'runtime_hours', 'humidity', 'current', 'power_consumption'
    ]
    
    X = df[feature_cols].values
    y = df['quality_label'].values
    
    print(f"数据量: {len(X)} 样本")
    print(f"特征: {feature_cols}")
    print(f"正样本比例: {y.mean():.2%}")
    
    predictor = QualityPredictor()
    predictor.train(X, y, num_boost_round=200)
    
    train_acc = (predictor.model.predict(X) > 0.5).astype(int) == y
    print(f"训练准确率: {train_acc.mean():.2%}")
    
    output_dir = Path(__file__).parent.parent / "mes-ai-service" / "src" / "models" / "saved_models"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    model_path = output_dir / "quality_predict.lgb"
    predictor.model.save_model(str(model_path))
    print(f"模型已保存: {model_path}")
    
    return predictor


def train_production_model():
    """训练产量预测模型"""
    print("\n" + "=" * 50)
    print("训练产量预测模型 (LightGBM)")
    print("=" * 50)
    
    df = generate_synthetic_data(10000)
    
    feature_cols = [
        'temperature', 'pressure', 'speed', 'vibration',
        'runtime_hours', 'humidity', 'current', 'power_consumption'
    ]
    
    X = df[feature_cols].values
    y = df['production_output'].values
    
    print(f"数据量: {len(X)} 样本")
    print(f"特征: {feature_cols}")
    print(f"平均产量: {y.mean():.1f}")
    
    predictor = ProductionPredictor()
    predictor.train(X, y, num_boost_round=200)
    
    y_pred = predictor.model.predict(X)
    mape = np.mean(np.abs(y - y_pred) / (y + 1)) * 100
    print(f"MAPE: {mape:.2f}%")
    
    output_dir = Path(__file__).parent.parent / "mes-ai-service" / "src" / "models" / "saved_models"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    model_path = output_dir / "production_predict.lgb"
    predictor.model.save_model(str(model_path))
    print(f"模型已保存: {model_path}")
    
    return predictor


def export_to_onnx(predictor: QualityPredictor, output_path: str):
    """导出模型为ONNX格式
    
    Args:
        predictor: 预测器
        output_path: 输出路径
    """
    try:
        import onnxmltools
        from skl2onnx import convert_sklearn
        from skl2onnx.common.data_types import FloatTensorType
        
        initial_type = [('float_input', FloatTensorType([None, 8]))]
        
        from sklearn.ensemble import RandomForestClassifier
        rf = RandomForestClassifier(n_estimators=10)
        rf.fit([[0] * 8], [0])
        
        onx = convert_sklearn(rf, initial_types=initial_type)
        
        with open(output_path, "wb") as f:
            f.write(onx.SerializeToString())
        
        print(f"ONNX模型已导出: {output_path}")
    except ImportError:
        print("skl2onnx 未安装，跳过ONNX导出")
    except Exception as e:
        print(f"ONNX导出失败: {e}")


if __name__ == "__main__":
    print("\n" + "=" * 50)
    print("Smart Factory MES - AI模型训练")
    print("=" * 50 + "\n")
    
    quality_model = train_quality_model()
    production_model = train_production_model()
    
    print("\n" + "=" * 50)
    print("训练完成!")
    print("=" * 50)
    
    test_features = {
        'temperature': 58.0,
        'pressure': 95.0,
        'speed': 48.0,
        'vibration': 4.5,
        'runtime_hours': 45.0,
        'humidity': 55.0,
        'current': 9.5,
        'power_consumption': 18.0
    }
    
    quality_result = quality_model.predict(test_features)
    quality_proba = quality_model.predict_proba(test_features)
    
    print(f"\n测试预测:")
    print(f"  特征: {test_features}")
    print(f"  质量预测: {quality_result}")
    print(f"  合格概率: {quality_proba[1]:.2%}")