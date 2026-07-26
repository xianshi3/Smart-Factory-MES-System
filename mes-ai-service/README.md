# MES AI Service

智能工厂MES系统 - AI服务模块，提供质量预测、产量预测、设备故障预测、工艺参数推荐、异常检测、大模型智能分析等AI能力。

## 功能特性

### 预测模块
- **质量预测**: 基于LightGBM二分类模型，预测产品合格率
- **产量预测**: 基于XGBoost回归模型，预测未来产量趋势
- **设备故障预测**: 基于历史数据分析，预测设备故障风险
- **工艺参数推荐**: 根据产品类型推荐最佳工艺参数
- **异常检测**: 实时检测传感器数据异常
- **ONNX推理**: 支持ONNX格式模型加载与高性能推理

### 大模型模块
- **智能对话**: 智谱AI GLM-4 大模型支持
- **生产分析**: AI智能分析生产数据
- **根因分析**: AI辅助故障根因分析
- **能耗优化**: AI推荐节能方案
- **产能预测**: AI预测产能趋势

### 数据处理
- **批量预测**: 支持批量预测多个工单

## 快速启动

### 环境配置

创建 `.env.local` 文件配置敏感信息：

```bash
# 智谱AI API Key
ZHIPU_API_KEY=your-api-key-here
```

### Docker部署
```bash
docker build -t mes-ai-service .
docker run -p 8086:8086 mes-ai-service
```

### 本地开发
```bash
cd mes-ai-service
pip install -r requirements.txt

# 方式一：使用虚拟环境
.venv\Scripts\activate.bat
python src/main.py

# 方式二：直接运行
python src/main.py
```

## API接口

### 预测API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/predict/quality` | 质量预测 |
| POST | `/api/v1/predict/batch` | 批量预测 |
| GET | `/api/v1/predict/model/info` | 模型信息 |
| POST | `/api/v1/predict/device/fault` | 设备故障预测 |
| POST | `/api/v1/predict/process/recommend` | 工艺参数推荐 |
| POST | `/api/v1/predict/anomaly` | 异常检测 |
| POST | `/api/v1/predict/production` | 产量预测 |
| GET | `/api/v1/model/status` | 模型状态 |
| POST | `/api/v1/model/retrain` | 触发重训练 |

### 大模型API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/llm/chat` | 智能对话 |
| POST | `/api/v1/llm/analyze` | 生产数据分析 |
| POST | `/api/v1/predict/process/recommend` | 工艺参数推荐 |

### 健康检查

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/health` | 健康检查 |

## 请求示例

### 质量预测
```bash
curl -X POST http://localhost:8086/api/v1/predict/quality \
  -H "Content-Type: application/json" \
  -d '{
    "work_order_id": 12345,
    "product_name": "Product A",
    "device_code": "DEV001",
    "temperature": 80.0,
    "speed": 50.0,
    "pressure": 10.0,
    "raw_material": "PET"
  }'
```

### 设备故障预测
```bash
curl -X POST http://localhost:8086/api/v1/predict/device/fault \
  -H "Content-Type: application/json" \
  -d '{
    "device_code": "DEV001",
    "history_data": [
      {"temperature": 80, "speed": 50},
      {"temperature": 82, "speed": 52},
      {"temperature": 85, "speed": 55}
    ],
    "hours_ahead": 24
  }'
```

### 智能对话
```bash
curl -X POST http://localhost:8086/api/v1/llm/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "帮我分析今天上午的生产情况",
    "context": {"date": "2026-05-05"}
  }'
```

### 生产数据分析
```bash
curl -X POST http://localhost:8086/api/v1/llm/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "data_type": "quality",
    "time_range": "24h"
  }'
```

## 配置说明

配置文件 `config.yaml` 包含服务端口、模型路径、Kafka和Redis连接等信息。

```yaml
server:
  host: "0.0.0.0"
  port: 8086

model:
  quality:
    onnx_path: "src/models/saved_models/quality_predict.onnx"
    version: "1.0.0"
  production:
    onnx_path: "src/models/saved_models/output_predict.onnx"
    version: "1.0.0"

# 大模型配置（敏感信息通过环境变量 ZHIPU_API_KEY 设置）
llm:
  api_key: "${ZHIPU_API_KEY}"
  model: "glm-4-flash"

redis:
  host: "localhost"
  port: 6379
  db: 1
```

## 项目结构

```
mes-ai-service/
├── src/
│   ├── main.py              # 启动入口
│   ├── app.py              # FastAPI应用
│   ├── models/             # 模型定义
│   │   ├── prediction_model.py   # 质量预测模型
│   │   ├── regression_model.py  # 产量预测模型
│   │   └── train.py           # 训练脚本
│   ├── services/            # 业务逻辑
│   │   ├── quality_predictor.py   # 质量预测服务
│   │   ├── inference_service.py # 推理服务
│   │   ├── llm_service.py      # 智谱AI大模型服务
│   │   ├── analysis_service.py  # AI分析服务
│   │   └── feature_engineering.py # 特征工程
│   ├── router/             # 路由
│   │   ├── prediction.py   # 预测路由
│   │   ├── llm.py        # 大模型路由
│   │   └── analysis.py   # 分析路由
├── models/                # 训练模型输出
├── config.yaml            # 配置文件
├── .env.local            # 本地敏感配置（不提交）
├── requirements.txt       # Python依赖
└── Dockerfile           # Docker镜像
```

## 技术栈

| 组件 | 版本 |
|------|------|
| Python | 3.11 |
| FastAPI | 0.115 |
| LightGBM | 4.5 |
| XGBoost | 2.1 |
| ONNX Runtime | 1.19 |
| scikit-learn | 1.5 |
| 智谱AI SDK | - |

## 测试

```bash
pytest tests/test_prediction.py -v
```

---

*最后更新: 2026-05-05*