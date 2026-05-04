"""模型训练模块"""
import os
import sys
import numpy as np
import pandas as pd
import lightgbm as lgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, roc_auc_score, classification_report
import joblib
import logging
from datetime import datetime

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


def load_training_data(data_path: str = None) -> tuple:
    """加载训练数据
    
    Args:
        data_path: 数据文件路径,如果不提供则生成模拟数据
        
    Returns:
        特征矩阵X和标签向量y
    """
    if data_path and os.path.exists(data_path):
        df = pd.read_csv(data_path)
        X = df.drop('target', axis=1).values
        y = df['target'].values
        return X, y
    
    logger.info("生成模拟训练数据...")
    np.random.seed(42)
    n_samples = 10000
    
    temperature = np.random.normal(80, 15, n_samples)
    speed = np.random.normal(50, 10, n_samples)
    pressure = np.random.normal(10, 2, n_samples)
    humidity = np.random.normal(50, 10, n_samples)
    vibration = np.random.exponential(0.5, n_samples)
    
    data = {
        'temperature': temperature,
        'speed': speed,
        'pressure': pressure,
        'humidity': humidity,
        'vibration': vibration,
    }
    df = pd.DataFrame(data)
    
    target = (
        (df['temperature'] > 65) & (df['temperature'] < 95) &
        (df['speed'] > 30) & (df['speed'] < 70) &
        (df['pressure'] > 5) & (df['pressure'] < 15) &
        (df['vibration'] < 1.0)
    ).astype(int)
    
    X = df.values
    y = target.values
    
    return X, y


def feature_engineering(X: np.ndarray, feature_names: list = None) -> np.ndarray:
    """特征工程: 生成更多特征
    
    Args:
        X: 原始特征矩阵
        feature_names: 特征名称列表
        
    Returns:
        工程化后的特征矩阵
    """
    if feature_names is None:
        feature_names = ['temperature', 'speed', 'pressure', 'humidity', 'vibration']
    
    df = pd.DataFrame(X, columns=feature_names)
    
    df['temp_normalized'] = (df['temperature'] - 80) / 15
    df['speed_normalized'] = (df['speed'] - 50) / 10
    df['pressure_normalized'] = (df['pressure'] - 10) / 2
    
    df['temp_speed_interaction'] = df['temp_normalized'] * df['speed_normalized']
    df['temp_pressure_interaction'] = df['temp_normalized'] * df['pressure_normalized']
    df['speed_pressure_interaction'] = df['speed_normalized'] * df['pressure_normalized']
    
    df['temp_squared'] = df['temp_normalized'] ** 2
    df['speed_squared'] = df['speed_normalized'] ** 2
    df['pressure_squared'] = df['pressure_normalized'] ** 2
    
    return df.values


def train_lightgbm_model(
    X: np.ndarray,
    y: np.ndarray,
    params: dict = None,
    num_boost_round: int = 100,
    test_size: float = 0.2,
) -> tuple:
    """训练 LightGBM 模型
    
    Args:
        X: 特征矩阵
        y: 标签向量
        params: LightGBM 参数
        num_boost_round: 迭代轮数
        test_size: 测试集比例
        
    Returns:
        训练好的模型、测试集准确率、AUC
    """
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=test_size, random_state=42, stratify=y
    )
    
    train_data = lgb.Dataset(X_train, label=y_train)
    test_data = lgb.Dataset(X_test, label=y_test, reference=train_data)
    
    if params is None:
        params = {
            'objective': 'binary',
            'metric': ['auc', 'binary_logloss'],
            'boosting_type': 'gbdt',
            'num_leaves': 31,
            'learning_rate': 0.05,
            'feature_fraction': 0.8,
            'bagging_fraction': 0.8,
            'bagging_freq': 5,
            'verbose': -1,
            'seed': 42,
        }
    
    logger.info("开始训练 LightGBM 模型...")
    model = lgb.train(
        params,
        train_data,
        num_boost_round=num_boost_round,
        valid_sets=[train_data, test_data],
        valid_names=['train', 'valid'],
        callbacks=[lgb.early_stopping(10), lgb.log_evaluation(20)],
    )
    
    y_pred_proba = model.predict(X_test)
    y_pred = (y_pred_proba >= 0.5).astype(int)
    
    accuracy = accuracy_score(y_test, y_pred)
    auc = roc_auc_score(y_test, y_pred_proba)
    
    logger.info(f"测试集准确率: {accuracy:.4f}")
    logger.info(f"测试集 AUC: {auc:.4f}")
    logger.info("\n分类报告:\n" + classification_report(y_test, y_pred))
    
    return model, accuracy, auc


def save_model(model, output_dir: str = "models") -> dict:
    """保存模型
    
    Args:
        model: 训练好的模型
        output_dir: 输出目录
        
    Returns:
        保存的文件路径
    """
    os.makedirs(output_dir, exist_ok=True)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    pkl_path = os.path.join(output_dir, f"quality_model_{timestamp}.pkl")
    joblib.dump(model, pkl_path)
    logger.info(f"模型已保存至: {pkl_path}")
    
    try:
        onnx_path = os.path.join(output_dir, f"quality_model_{timestamp}.onnx")
        import onnx
        from skl2onnx import convert_sklearn
        from skl2onnx.common.data_types import FloatTensorType
        
        initial_type = [('float_input', FloatTensorType([None, model.num_trees()]))]
        onnx_model = convert_sklearn(model, initial_types=initial_type)
        onnx.save_model(onnx_model, onnx_path)
        logger.info(f"ONNX模型已保存至: {onnx_path}")
    except Exception as e:
        logger.warning(f"ONNX模型转换失败: {e}")
        onnx_path = None
    
    return {
        "pkl_path": pkl_path,
        "onnx_path": onnx_path,
        "timestamp": timestamp,
    }


def main():
    """主训练流程"""
    logger.info("=" * 50)
    logger.info("MES AI 模型训练脚本启动")
    logger.info("=" * 50)
    
    X, y = load_training_data()
    logger.info(f"数据加载完成: {X.shape[0]} 样本, {X.shape[1]} 特征")
    
    X_engineered = feature_engineering(X)
    logger.info(f"特征工程完成: {X_engineered.shape[1]} 特征")
    
    model, accuracy, auc = train_lightgbm_model(X_engineered, y)
    
    save_model(model)
    
    logger.info("=" * 50)
    logger.info("训练完成!")
    logger.info(f"准确率: {accuracy:.4f}, AUC: {auc:.4f}")
    logger.info("=" * 50)


if __name__ == "__main__":
    main()