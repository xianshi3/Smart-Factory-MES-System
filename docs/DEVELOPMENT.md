# Smart Factory MES System 开发文档

---

## 1. 项目概述

智能工厂制造执行系统 (MES)，基于微服务架构，支持2000+设备并发连接。

### 1.1 项目结构

```
Smart-Factory-MES-System/
├── mes-common/              # 公共模块 (Result, BaseEntity, 异常处理)
├── mes-gateway/            # API网关 (9090)
├── mes-auth/                # 认证服务 (8081)
├── mes-workorder/           # 工单服务 (8082)
├── mes-process/             # 工艺服务 (8083)
├── mes-quality/             # 质量服务 (8084)
├── mes-dashboard/           # 看板服务 (8085)
├── mes-device-gateway/      # .NET设备网关 (5000)
├── mes-ai-service/          # Python AI服务 (8086)
├── mes-device-simulator-wpf/ # WPF设备模拟器
├── mes-frontend/            # Vue 3前端 (3000)
├── sql/                      # 数据库脚本
├── start-all.bat            # 统一启动器
├── docker-compose.yml        # 基础设施配置
├── docs/                    # 项目文档
└── README.md                # 项目简介
```

### 1.2 技术栈

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + ECharts | Vue 3.5 |
| 后端 | Java 17 + Spring Cloud + MyBatis-Plus | Spring Cloud 2022.0.0 |
| 设备接入 | .NET 8 + MQTT + Kafka | .NET 8 |
| AI服务 | Python 3.11 + FastAPI + LightGBM + XGBoost | FastAPI 0.115 |
| 基础设施 | MySQL 8.0.33 + Redis 7 + Kafka 3.4 + EMQX 5.8 | - |

### 1.3 环境配置

敏感信息通过 `.env.local` 文件配置（不提交到git）：

```bash
# mes-ai-service/.env.local
ZHIPU_API_KEY=your-api-key-here
```

---

## 2. 快速开始

### 2.1 前置条件

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java运行环境 |
| Maven | 3.9+ | 项目构建 |
| Node.js | 18+ | 前端开发 |
| Python | 3.10+ | AI服务运行环境 |
| .NET | 8.0+ | 设备网关运行环境 |
| Docker | 24+ | 容器化部署 |

### 2.2 启动所有服务

```powershell
# 一键启动（推荐）
start-all.bat
```

选择 [1] Start All Services

### 2.3 单独启动服务

```powershell
start-all.bat

# 选择：
# [2] Start Docker           - 仅Docker
# [3] Start Backend          - 仅后端
# [4] Start AI Service       - 仅AI服务
# [5] Start .NET Gateway     - 仅设备网关
# [6] Start Frontend         - 仅前端
# [7] Start Device Simulator - 设备模拟器
# [8] Clean                  - 清理缓存
# [9] Stop All Services      - 停止所有
# [10] View Status           - 查看状态
```

### 2.4 启动前端

```powershell
cd mes-frontend
npm install
npm run dev
```

访问 http://localhost:3000

---

## 3. 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 3000 | Vue 3应用 |
| API网关 | 9090 | Spring Cloud Gateway（暂未使用） |
| 认证服务 | 8081 | 用户登录/注册 |
| 工单服务 | 8082 | 工单管理 |
| 工艺服务 | 8083 | 工艺模板 |
| 质量服务 | 8084 | 质检追溯 |
| 看板服务 | 8085 | OEE/WebSocket |
| AI服务 | 8086 | 质量/产量预测 |
| .NET设备网关 | 5000 | MQTT/Kafka数据接入 |
| 设备模拟器 | 8883 | 模拟2000+设备数据上报 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| MQTT | 1883 | 设备通信 |
| Kafka | 9092 | 消息队列 |

> 注意：开发环境前端直连各服务（8081-8085），或通过网关（9090）统一路由。网关已启用，
`/api/**` 路由使用 StripPrefix=1（去掉 `/api` 前缀后转发），`/ai/**` 使用 StripPrefix=0（保留前缀）。

---

## 4. 默认账号

| 服务 | 用户名 | 密码 |
|------|--------|------|
| 前端登录 | admin | admin123 |
| Nacos | nacos | nacos |
| EMQX Dashboard | admin | public |

---

## 5. 模块说明

### 5.1 Java后端模块

| 模块 | 端口 | 职责 |
|------|------|------|
| mes-auth | 8081 | 用户认证、JWT令牌 |
| mes-workorder | 8082 | 工单生命周期、报工 |
| mes-process | 8083 | 工艺模板、参数校验 |
| mes-quality | 8084 | 质检记录、追溯 |
| mes-dashboard | 8085 | 实时看板、OEE |
| mes-gateway | 9090 | API路由（暂未使用） |

### 5.2 .NET 设备网关 (mes-device-gateway)

| 组件 | 说明 |
|------|------|
| MqttConsumerService | MQTT消费服务，支持自动重连 |
| KafkaProducerService | Kafka生产者，使用Channel异步处理 |
| DataCleanseService | 数据清洗服务 |
| GatewayConfig | 网关配置类 |

**技术特点：**
- Channel<T> 高吞吐量消息队列
- MQTT 自动重连机制
- Kafka 幂等生产者
- 健康检查支持

### 5.3 公共模块 (mes-common)

| 类 | 说明 |
|-----|------|
| Result<T> | 统一返回 (code, message, data) |
| PageResult<T> | 分页结果 |
| BaseEntity | 基类实体 (id, createTime, updateTime) |
| BizException | 业务异常 |
| ErrorCode | 错误码枚举 |
| JwtUtils | JWT工具类 |
| MesConstants | 常量定义 |

### 5.3 前端页面 (mes-frontend)

| 页面 | 路由 | 功能 |
|------|------|------|
| 登录 | /login | 用户登录 |
| 首页 | /dashboard | 产量趋势、OEE图表 |
| 工单 | /workorder | 工单CRUD、报工 |
| 工艺 | /process | 模板管理、参数校验 |
| 质量 | /quality | 质检记录、正反向追溯 |
| 设备 | /device | 设备状态监控 |

---

## 6. 数据库表

### 6.1 核心表

| 表名 | 说明 |
|------|------|
| sys_user | 用户表 |
| wo_work_order | 工单表 |
| wo_work_report | 报工记录 |
| proc_template | 工艺模板 |
| proc_parameter | 工艺参数 |
| qms_quality_record | 质检记录 |
| qms_traceability | 追溯数据 |
| dash_device_status | 设备状态 |
| dash_production_stats | 生产统计 |
| dash_oee_data | OEE数据 |

---

## 7. API接口

### 7.1 认证服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /auth/login | 用户登录 |
| POST | /auth/register | 用户注册 |
| GET | /auth/info | 获取用户信息 |

### 7.2 工单服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /workorder | 创建工单 |
| GET | /workorder/{id} | 工单详情 |
| PUT | /workorder/{id} | 更新工单 |
| POST | /workorder/{id}/issue | 下发工单 |
| POST | /workorder/{id}/start | 开始生产 |
| POST | /workorder/report | 提交报工 |
| GET | /workorder/page | 分页查询 |

### 7.3 看板服务

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /overview | 生产总览 |
| GET | /devices | 设备状态 |
| GET | /production/today | 今日统计 |
| GET | /oee/calculate | OEE计算 |

---

## 8. 消息队列

### 8.1 Kafka主题

| 主题 | 说明 |
|------|------|
| mes-device-data | 设备数据 |
| mes-workorder-event | 工单事件 |
| mes-quality-event | 质量事件 |
| mes-alarm-event | 告警事件 |

### 8.2 MQTT主题

| 主题 | 说明 |
|------|------|
| mes/device/+/data | 设备数据 |
| mes/device/+/status | 设备状态 |
| mes/device/+/control | 控制指令 |

---

## 9. 注意事项

1. **首次启动**：必须执行 sql/init.sql 初始化数据库
2. **Docker命令**：Windows使用 `docker compose`（空格）
3. **前端账号**：admin / admin123
4. **AI模型**：示例模型，需真实数据训练后替换
5. **Docker仅运行基础设施**：MySQL、Redis、Kafka、Zookeeper、Nacos 使用 Docker 运行，Java 服务本地 `java -jar` 启动
6. **AI服务端口冲突**：AI 服务使用 8086 端口，与 InfluxDB 冲突。启动 AI 服务前需执行 `docker stop mes-influxdb`
7. **Docker镜像拉取**：如果 Docker 代理无法拉取镜像，需配置镜像加速器（daemon.json 中添加 registry-mirrors）

---

## 10. 常见问题

### Q1: docker 命令找不到
**A**: 安装 Docker Desktop 并重启电脑

### Q2: 前端无法登录
**A**: 检查数据库是否初始化，确认账号 admin/admin123 存在

### Q3: 微服务无法启动
**A**: 检查端口占用，确保基础设施服务正常运行

---

## 11. 版本记录

### v1.0.11 (2026-04-05)
- 前端设备监控页面对接真实API (DeviceView.vue)
- 添加Dashboard服务CORS配置
- 修复Dashboard服务JAR无法运行问题
- 数据库设备数据从4条扩展为12条

### v1.0.12 (2026-04-05)
- 启动AI服务 (端口8086)
- 前端添加AI预测功能 (设备页面)
- 添加AI服务启动/停止脚本 (start-ai.bat, stop-ai.bat)
- AI服务添加CORS配置

### v1.0.17 (2026-04-05)
- 修复 Vite 代理路径重写（405 错误）
- 添加 MyBatis-Plus 分页插件
- 修复空字符串过滤问题
- 统一前后端状态值

### v1.0.16 (2026-04-05)
- 修复前端 API 请求发错服务问题（.env 硬编码 8081）
- Vite 代理配置完善（workorder/process/quality/api）
- 前端 services.ts 去除所有硬编码 URL
- DashboardView 对接真实 API
- 密码 BCrypt 加密
- Dashboard 数据逻辑修复
- Python AI Bug 修复
- .NET 网关 MQTT 修复
- start-all.bat 路径修复

### v1.0.15 (2026-04-05)
- 密码 BCrypt 加密 (AuthService)
- 数据库种子数据密码哈希化
- Dashboard 数据逻辑修复 (概览/OEE/趋势/Redis缓存)
- Python AI Bug 修复 (config变量、import位置)
- .NET 网关 MQTT Guid 格式化修复

### v1.0.14 (2026-04-05)
- 统一启动器 start-all.bat 完善（支持 /D 参数启动 .NET 网关）
- 修复 .NET 网关 MQTT Guid 格式化 Bug
- 禁用 Kafka 幂等模式（适配无 Kafka 开发环境）
- 清理冗余 bat 文件，统一使用 start-all.bat

### v1.0.10 (2026-04-05)
- 删除冗余文件 DEVELOPMENT.html
- 完善数据库设备数据（temperature, speed, last_heartbeat）
- 添加OpenCode配置文件

### v1.0.9 (2026-04-05)
- .NET设备网关优化：Channel异步处理、自动重连、健康检查

### v1.0.8 (2026-04-05)
- 添加CORS跨域配置到workorder、process、quality服务
- 修复前端API响应处理，处理非标准响应格式
- 前端直连各服务（8081-8084），网关端口改为9090

### v1.0.7 (2026-04-05)
- 修复POM配置，添加spring-boot-maven-plugin
- 修复Quality服务路径（/quality/record/page）
- 统一API管理到services.ts

### v1.0.6 (2026-04-05)
- 创建services.ts统一管理各服务API地址
- 修复token获取方式（localStorage）
- 修复Vue文件import路径

### v1.0.2 (2026-04-04)
- 优化 Docker 配置，默认只启动 MySQL + Redis（开发友好）
- 添加简化版 docker-compose.yml
- 添加常见 Docker Hub 拉取失败解决方案

### v1.0.1 (2026-04-04)
- 添加代码注释 (116个文件)
- 创建设备模拟器
- 添加ONNX示例模型
- 修复前端配置警告
- 添加README和默认账号

### v1.0.0 (2026-04-04)
- 项目初始化
- 7个Java微服务
- .NET设备网关
- Python AI服务
- Vue 3前端

---

## 8. 核心实现模式 (2026-04-10 新增)

### 8.1 创建功能实现

后端 Service:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public Long create(CreateDTO dto) {
    Entity entity = new Entity();
    entity.setField1(dto.getField1());
    entity.setField2(dto.getField2());
    mapper.insert(entity);
    return entity.getId();
}
```

后端 Controller:
```java
@PostMapping
public Result<Long> create(@RequestBody CreateDTO dto) {
    return Result.ok(service.create(dto));
}
```

### 8.2 删除功能实现 (使用 UpdateWrapper 避免乐观锁)

后端 Service:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(Long id, Long userId) {
    Entity entity = mapper.selectById(id);
    if (entity == null) {
        throw new RuntimeException("记录不存在");
    }

    var updateWrapper = new UpdateWrapper<Entity>()
            .set("deleted", 1)
            .set("deleted_time", LocalDateTime.now())
            .set("deleted_by", userId)
            .eq("id", id);
    mapper.update(null, updateWrapper);
}
```

后端 Controller:
```java
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable Long id,
                         @RequestHeader(value = "X-User-Id", required = false) Long userId) {
    service.delete(id, userId);
    return Result.ok();
}
```

### 8.3 解决 JavaScript Long ID 精度丢失

后端实体类:
```java
@JsonSerialize(using = ToStringSerializer.class)
private Long id;
```

前端 API:
```typescript
export function deleteEntity(id: string | number) {
  return request({ url: `/entity/${id}`, method: 'delete' })
}
```

### 8.4 前后端字段名对应

| 后端字段 | 前端字段 |
|---------|---------|
| checkResult | checkResult |
| workOrderNo | workOrderNo |
| checkType | checkType |
| checkTime | checkTime |

### 8.5 前端创建对话框

```vue
<el-dialog v-model="createDialogVisible" title="新建" width="500px">
  <el-form :model="createForm" label-width="80px">
    <el-form-item label="名称">
      <el-input v-model="createForm.name" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="createDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="submitCreate">创建</el-button>
  </template>
</el-dialog>
```

---

## 常见问题与解决方案

### Q1: 删除报错 "Parameter 'MP_OPTLOCK_VERSION_ORIGINAL' not found"
**原因**: 实��类使用 @Version 注解，手动调用 updateById() 触发乐观锁
**解决**: 使用 UpdateWrapper 代替 updateById()

### Q2: 删除报错 "记录不存在" 但列表显示存在
**原因**: JavaScript Long ID 精度丢失，超过 2^53-1
**解决**: 后端添加 @JsonSerialize(using = ToStringSerializer.class)

### Q3: 前端字段有值但后端接收不到
**原因**: 前后端字段名不一致
**解决**: 统一字段名，参考 DTO 定义

### Q4: 工艺模板编辑报错 "已发布的模板不能直接修改"
**原因**: 已发布模板受保护，需要创建新版本才能修改
**解决**: 前端添加判断，DRAFT状态才允许编辑

### Q5: 数据库中文乱码
**原因**: MySQL连接字符集为latin1
**解决**: 使用 `--default-character-set=utf8mb4` 连接

### Q6: 更新数据报错 "Parameter 'MP_OPTLOCK_VERSION_ORIGINAL' not found"
**原因**: 使用updateById()触发@Version乐观锁
**解决**: 使用UpdateWrapper代替updateById()

---

## 9. 详情对话框实现

### 9.1 工单详情

```vue
<el-dialog v-model="detailVisible" title="工单详情">
  <el-descriptions :column="2" border>
    <el-descriptions-item label="工单ID">{{ detailData.id }}</el-descriptions-item>
    <el-descriptions-item label="工单编号">{{ detailData.orderNo }}</el-descriptions-item>
    <!-- 更多字段 -->
  </el-descriptions>
</el-dialog>
```

### 9.2 模板详情

类似实现，使用el-descriptions组件展示。

---

## 10. 数据库字符集修复

修复MySQL连接字符集：
```bash
docker exec mes-mysql mysql -uroot -proot --default-character-set=utf8mb4 mes_db -e "UPDATE proc_template SET ..."
```

---

## 11. 逻辑删除与唯一约束问题修复 (2026-04-12 新增)

### 问题描述

使用 MyBatis-Plus 逻辑删除 (`@TableLogic`) 时，创建新记录报错：
```
Duplicate entry '123' for key 'proc_template.uk_template_code'
```

### 问题根因

数据库唯一约束只关注单列，不考虑 `deleted` 状态。软删除的记录 (`deleted=1`) 仍然占据唯一索引。

### 解决方案

#### 1. 修改数据库唯一约束（推荐）

```sql
-- 将单列唯一索引改为复合索引
ALTER TABLE proc_template DROP INDEX uk_template_code;
ALTER TABLE proc_template ADD UNIQUE INDEX uk_template_code (template_code, deleted);
```

#### 2. 批量修复脚本

```sql
-- 所有使用逻辑删除的表都需要修复
ALTER TABLE mes_db.wo_work_order DROP INDEX uk_order_no;
ALTER TABLE mes_db.wo_work_order ADD UNIQUE INDEX uk_order_no (order_no, deleted);

ALTER TABLE mes_db.dash_device_status DROP INDEX uk_device_code;
ALTER TABLE mes_db.dash_device_status ADD UNIQUE INDEX uk_device_code (device_code, deleted);

-- ... 其他表类似
```

### 设计规范

使用逻辑删除的表，唯一约束必须包含 `deleted` 字段：

```sql
-- 正确示范
CREATE TABLE xxx (
    code VARCHAR(50),
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_code (code, deleted)
);
```

---

*最后更新：2026-07-27*

---

## 12. WebSocket 实时推送

### 后端配置

```java
// WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/dashboard")
                .setAllowedOrigins("http://localhost:3000");
    }
}
```

### 前端使用

```typescript
// utils/websocket.ts
import { wsService } from '@/utils/websocket'

// 连接
wsService.subscribe((data) => {
  devices.value = data.devices
})

// 断开
wsService.disconnect()
```

### 端点

- WebSocket: `ws://localhost:8085/ws/dashboard`
- 推送频率: 5秒/次

---

## 13. 报工功能

### 前端实现

工单页面添加"报工"按钮，弹出对话框填写：
- 报工数量
- 良品数量
- 不良数量
- 设备选择
- 备注

### 后端接口

```java
POST /workorder/report
{
  "workOrderId": 123,
  "reportQuantity": 100,
  "qualifiedQuantity": 98,
  "defectiveQuantity": 2,
  "deviceId": 1,
  "remark": ""
}
```

---

## 15. 工单完成功能

### 后端接口

```java
// WorkOrderService.java
void complete(Long id);

// WorkOrderController.java
@PostMapping("/{id}/complete")
public Result<Void> complete(@PathVariable Long id)
```

### 前端调用

```typescript
// services.ts
export function completeWorkOrder(id: number) {
  return request({ url: `/workorder/${id}/complete`, method: 'post' })
}

// WorkOrderView.vue
<el-button @click="handleComplete(row)">完成</el-button>
```

---

## 16. 设备控制功能

### 后端接口

```java
// DashboardController.java
@PostMapping("/device/{deviceId}/start")
public Result<Void> startDevice(@PathVariable Long deviceId)

@PostMapping("/device/{deviceId}/stop")  
public Result<Void> stopDevice(@PathVariable Long deviceId)
```

### 前端调用

```typescript
// services.ts
export function startDevice(deviceId: number)
export function stopDevice(deviceId: number)

// DeviceView.vue
<el-button @click="handleStart(device)">启动</el-button>
<el-button @click="handleStop(device)">停止</el-button>
```

---

## 17. 生产报表功能

### 后端接口

```java
@GetMapping("/report/production")
public Map<String, Object> getProductionReport(
    @RequestParam(required = false) String startDate,
    @RequestParam(required = false) String endDate)
```

返回数据：
```json
{
  "totalOutput": 1000,
  "totalQualified": 950,
  "qualifyRate": 95.0,
  "avgOee": 85.5,
  "dailyData": [...],
  "totalDays": 7
}
```

### 前端页面

- 路由: `/report`
- 组件: `views/report/ReportView.vue`
- 功能: 统计卡片、日趋势图表、明细表格

---

*最后更新：2026-07-27*