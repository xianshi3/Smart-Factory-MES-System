# MES AI Service

智能工厂MES系统 - AI服务模块，提供质量预测、产量预测等AI能力。

## 功能特性

- **质量预测**: 基于LightGBM二分类模型，预测产品合格率
- **产量预测**: 基于XGBoost回归模型，预测未来产量趋势
- **ONNX推理**: 支持ONNX格式模型加载与高性能推理
- **Kafka集成**: 异步消费设备数据，实时特征工程
- **Redis缓存**: 推理结果缓存，提升响应速度

## 快速启动

### Docker部署
```bash
docker build -t mes-ai-service .
docker run -p 8086:8086 mes-ai-service
```

### 本地开发
```bash
pip install -r requirements.txt
python -m src.main
```

## API接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/predict/quality` | 质量预测 |
| POST | `/api/v1/predict/production` | 产量预测 |
| GET | `/api/v1/model/status` | 模型状态 |
| POST | `/api/v1/model/retrain` | 触发重训练 |
| GET | `/api/v1/health` | 健康检查 |

## 配置说明

配置文件 `config.yaml` 包含服务端口、模型路径、Kafka和Redis连接等信息。
