# MES AI Service

虚拟路径MES系统 - AI服务模块，提供质量预测、产量预测、设备故障预测、工艺参数推荐、异常检测、大模型智能分析等AI能力。

## 功能特性

### 预测模块
- **质量预测**: 基于LightGBM二分类模型，预测产品合格率
- **产量预测**: 基于XGBoost回归模型，预测未来产量趋势
- **设备故障预测**: 基于历史数据分析，预测设备故障风险
- **工艺参数推荐**: 根据产品类型推荐最佳工艺参数
- **异常检测**: 实时检测传感器数据异常
- **ONNX推理**: 支持ONNX格式模型加载与高性能推理

### 大模型模块
- **智能对话**: 智谱AI GLM-4 大模型支持（结果缓存 1h + 限流 60次/分钟）
- **AI Agent**: 智能生产助理 — 六大能力完备的自主智能体：
  - **任务理解**: 规则+实体抽取的意图识别（设备监控/健康总览/告警诊断/工单/知识/分析/库存），自动提取设备编码等关键实体
  - **流程编排**: 四阶段流水线（任务理解 → 计划执行 → 知识增强 → 结果交付），执行计划注入 + 工具去重防重复调用
  - **工具调用**: 15 个 MES 工具（数字孪生/告警/趋势/工单/物料/知识库），统一执行器含超时保护、错误规范化、失败自动知识库兜底
  - **知识增强**: TF-IDF 中文检索（双字词加权+标题重排），内置 6 篇手册/标准 + `knowledge/` 目录自定义文档扩展
  - **多轮交互**: 会话焦点记忆（Redis 30 分钟），"那台设备"等指代自动继承上轮设备编码
  - **结果交付**: 结构化报告（summary/关键结论/数据表格/处置建议/后续追问），LLM 生成失败自动规则兜底
- **对话历史**: MySQL 持久化存储，多轮对话记录管理
- **分析历史**: MySQL 持久化（`ai_analysis_history`）+ Redis 最近 50 条缓存，按用户/设备隔离，支持删除
- **生产分析**: AI智能分析生产数据
- **根因分析**: AI辅助故障根因分析
- **能耗优化**: 企业级多维节能方案（真实遥测 + 参数/削峰填谷/待机/维护 + 财务测算 + 路线图）
- **SPC统计过程控制**: 8条 Western Electric 规则 + 全过程能力 + 5M1E 建议
- **产能预测**: AI预测产能趋势

### 数据处理
- **批量预测**: 支持批量预测多个工单

## 快速启动

### 环境配置

创建 `.env.local` 文件配置敏感信息：

```bash
# 智谱AI API Key（必填，否则大模型功能不可用）
ZHIPU_API_KEY=your-api-key-here

# JWT 密钥（必填，与 Java 后端一致，>= 32 字符；所有业务接口鉴权使用）
JWT_SECRET=your-jwt-secret-at-least-32-chars-long-change-me
```

> **鉴权说明**：除 `/api/v1/health` 外，所有接口必须携带 `Authorization: Bearer <JWT>`（与 Java 后端共享 `JWT_SECRET`，HS256 校验，实现见 `src/security.py`，无第三方依赖）。Agent 工具调用后端 API 时自动透传用户 token。

### Docker部署
```bash
docker build -t mes-ai-service .
docker run -p 8087:8087 mes-ai-service
```

### 本地开发
```bash
cd mes-ai-service
pip install -r requirements.txt

# 方式一：使用虚拟环境
.venv\Scripts\activate.bat
python -m src.main

# 方式二：直接运行
python -m src.main
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

### Agent API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/agent/run` | Agent 执行（多工具编排） |
| GET | `/api/v1/agent/tools` | 可用工具列表 |
| POST | `/api/v1/agent/kb/search` | 知识库搜索 |
| POST | `/api/v1/agent/conversations` | 新建对话 |
| GET | `/api/v1/agent/conversations` | 列表（按 user_id 过滤） |
| GET | `/api/v1/agent/conversations/{id}` | 详情（含消息） |
| POST | `/api/v1/agent/conversations/{id}/messages` | 保存消息 |
| DELETE | `/api/v1/agent/conversations/{id}` | 删除对话（逻辑删除） |
| POST | `/api/v1/agent/analysis` | 保存分析历史（返回记录 id） |
| GET | `/api/v1/agent/analysis` | 分析历史列表（按 user_id / type / device_code 过滤） |
| DELETE | `/api/v1/agent/analysis/{id}` | 删除分析历史（物理删除，校验 user_id） |

### 智能分析 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/analysis/energy/optimize` | 能耗优化（企业级多维策略） |
| POST | `/api/v1/analysis/spc/analyze` | SPC 统计过程控制分析 |
| POST | `/api/v1/analysis/capacity/predict` | 产能预测 |
| POST | `/api/v1/analysis/root-cause/analyze` | 质量根因分析 |
| POST | `/api/v1/analysis/delivery/predict` | 交期预测 |

### 健康检查

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/health` | 健康检查（无需鉴权） |

## 请求示例

> 除健康检查外，所有请求需携带 `Authorization: Bearer <token>`（登录接口获取，如 `POST /api/auth/login`）。

### 质量预测
```bash
curl -X POST http://localhost:8087/api/v1/predict/quality \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
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
curl -X POST http://localhost:8087/api/v1/predict/device/fault \
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
curl -X POST http://localhost:8087/api/v1/llm/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "帮我分析今天上午的生产情况",
    "context": {"date": "2026-05-05"}
  }'
```

### 生产数据分析
```bash
curl -X POST http://localhost:8087/api/v1/llm/analyze \
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
  port: 8087

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
  rate_limit: 60          # LLM调用限流（次/分钟），超限返回"服务繁忙"

database:
  host: "localhost"       # 可用 MYSQL_HOST / MYSQL_PORT / MYSQL_USERNAME / MYSQL_PASSWORD 覆盖
  port: 3306
  username: "root"
  password: "123455"
  database: "mes_db"

redis:
  host: "localhost"
  port: 6379
  db: 1
```

**Redis 缓存说明**（Redis 不可用时全部自动降级，不影响业务）：

| 用途 | Key | 说明 |
|------|-----|------|
| 分析历史缓存 | `analysis:recent:{user}:{device}` | zset 最近 50 条，TTL 300s，保存/删除双写同步 |
| LLM 结果缓存 | `llm:cache:{hash}` | 内容寻址（消息+上下文+历史+模型），命中直接复用，TTL 1h |
| LLM 限流 | `ratelimit:llm` | INCR+TTL 60s 滑动窗口，超限返回"服务繁忙" |

## 项目结构

```
mes-ai-service/
├── src/
│   ├── main.py              # 启动入口
│   ├── app.py              # FastAPI应用
│   ├── security.py         # JWT 鉴权（HS256，标准库实现，与后端共用 JWT_SECRET）
│   ├── models/             # 模型定义
│   │   ├── prediction_model.py   # 质量预测模型
│   │   ├── regression_model.py  # 产量预测模型
│   │   └── train.py           # 训练脚本
│   ├── services/            # 业务逻辑
│   │   ├── quality_predictor.py   # 质量预测服务
│   │   ├── inference_service.py # 推理服务
│   │   ├── llm_service.py      # 智谱AI大模型服务（含缓存+限流）
│   │   ├── analysis_service.py  # AI分析服务（能耗/SPC/产能/根因/交期）
│   │   ├── conversation_store.py # 对话+分析历史 MySQL 存储（含Redis缓存）
│   │   ├── redis_store.py     # Redis统一封装（降级安全）
│   │   ├── task_planner.py    # Agent任务理解（意图/实体/计划）
│   │   ├── agent_service.py   # Agent编排（四阶段流水线）
│   │   ├── report_builder.py  # Agent结果交付（结构化报告）
│   │   ├── tools.py           # Agent工具层（15个MES工具+统一执行器）
│   │   ├── knowledge_base.py  # RAG知识库（TF-IDF检索）
│   │   └── feature_engineering.py # 特征工程
│   ├── router/             # 路由
│   │   ├── prediction.py   # 预测路由
│   │   ├── llm.py        # 大模型路由
│   │   ├── analysis.py   # 分析路由
│   │   └── agent.py     # Agent 路由（含对话历史CRUD）
│   ├── schemas/
│   │   ├── schemas.py    # 核心数据模型
│   │   └── conversation.py # 对话历史模型
├── models/                # 训练模型输出
├── config.yaml            # 配置文件
├── .env.local            # 本地敏感配置（不提交）
├── requirements.txt       # Python依赖
└── Dockerfile           # Docker镜像
```

## 技术栈

| 组件 | 版本 |
|------|------|
| Python | 3.12 |
| FastAPI | 0.115 |
| LightGBM | 4.5 |
| XGBoost | 2.1 |
| ONNX Runtime | 1.19 |
| scikit-learn | 1.5 |
| 智谱AI SDK | - |
| PyMySQL | 1.1 |
| redis-py | 5.2 |
| MySQL | 8.0 |
| Redis | 7 |

## 测试

```bash
pytest tests/test_prediction.py -v
```

---

*最后更新: 2026-08-14*