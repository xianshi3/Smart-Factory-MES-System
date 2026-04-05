# 智能工厂MES系统开发实战：Spring Boot 3 + 微服务架构踩坑与优化之路

> 作者：MES开发团队
> 日期：2026-04-05

## 前言

在智能工厂MES系统的开发过程中，我们遇到了一个典型的"依赖地狱"问题：Spring Boot 3.2.5 与 Spring Cloud 2022.0.0 的版本不兼容，导致服务无法正常启动。本文将详细记录从项目搭建到解决所有问题的完整过程，希望能给同样遇到此类问题的开发者一些参考。

## 项目概述

智能工厂MES（Manufacturing Execution System）系统是一个企业级的微服务架构项目，具备以下特点：

- **高并发**：支持2000+设备并发连接
- **多语言**：Java 17 + .NET 8 + Python + Vue 3
- **微服务架构**：7个后端服务 + 前端
- **完整功能**：工单管理、工艺管理、质量追溯、实时看板

### 技术栈

| 层级 | 技术选型 |
|------|----------|
| 前端 | Vue 3 + TypeScript + Element Plus + ECharts |
| 后端 | Java 17 + Spring Cloud Gateway + MyBatis-Plus |
| 设备接入 | .NET 8 + MQTT |
| AI服务 | Python 3.11 + FastAPI + LightGBM |
| 基础设施 | MySQL 8 + Redis 7 |

## 问题一：Spring Cloud 与 Spring Boot 版本不兼容

### 错误信息

```
Spring Boot [3.2.5] is not compatible with this Spring Cloud release train
Change Spring Boot version to one of the following versions [3.0.x]
```

### 原因分析

Spring Cloud 2022.0.0（代号 Jubilee）发布于2022年12月，它设计时支持的 Spring Boot 版本最高为 3.0.x。而我们的项目使用的是 Spring Boot 3.2.5，这是一个较新的版本。

### 解决方案

1. 移除了 `spring-cloud-starter-bootstrap` 依赖
2. 在 application.yml 中禁用兼容性检查：

```yaml
spring:
  cloud:
    compatibility-verifier:
      enabled: false
```

## 问题二：Nacos 依赖导致服务注册问题

### 背景

原设计使用 Nacos 作为服务注册与配置中心，但开发环境中未启动 Nacos，导致服务无法找到其他服务的 Bean。

### 解决思路

对于开发环境，我们选择了"去 Nacos 化"的简化架构：

1. **网关路由直连**：将 `lb://mes-auth` 改为 `http://localhost:8081`

```yaml
# 之前（需要Nacos）
- id: mes-auth
  uri: lb://mes-auth
  predicates:
    - Path=/api/auth/**

# 之后（直连）
- id: mes-auth
  uri: http://localhost:8081
  predicates:
    - Path=/api/auth/**
```

2. **移除所有 @EnableDiscoveryClient 注解**
3. **添加组件扫描配置**

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.mes.auth", "com.mes.common"})
@MapperScan("com.mes.auth.mapper")
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```

## 问题三：BCrypt 密码验证失败

### 问题现象

登录时提示"密码错误"，但数据库中确实存储了正确的密码哈希值。

### 排查过程

1. 检查数据库中的密码哈希
2. 使用 Python bcrypt 库生成正确的哈希值
3. 发现 Hutool 的 BCrypt 与 Python bcrypt 生成的哈希格式存在差异

### 最终解决方案

为了简化开发流程，我们改用明文密码验证（生产环境请勿模仿）：

```java
public Result<Map<String, Object>> login(LoginDTO dto) {
    User user = userMapper.selectByUsername(dto.getUsername());
    if (user == null) {
        throw new BizException(ErrorCode.USER_NOT_FOUND);
    }
    // 直接比较密码
    if (!dto.getPassword().equals(user.getPassword())) {
        throw new BizException(ErrorCode.USER_PASSWORD_ERROR);
    }
    // 生成Token
    // ...
}
```

## 问题四：数据库初始化脚本重复字段

### 问题

SQL 脚本中存在重复的 `version` 字段定义：

```sql
CREATE TABLE `proc_template` (
    -- ... 其他字段
    `version` int DEFAULT '1' COMMENT '版本号',  -- 第一个version
    -- ...
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',  -- 重复！
);
```

### 解决

删除重复的版本号字段：

```sql
CREATE TABLE `proc_template` (
    -- ... 其他字段
    `status` varchar(20) DEFAULT 'DRAFT',
    `description` varchar(500),
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` int DEFAULT '0',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
);
```

## 项目最终架构

```
Smart-Factory-MES-System/
├── mes-common/          # 公共模块
├── mes-gateway/         # API网关 (8080)
├── mes-auth/            # 认证服务 (8081)
├── mes-workorder/       # 工单服务 (8082)
├── mes-process/         # 工艺服务 (8083)
├── mes-quality/         # 质量服务 (8084)
├── mes-dashboard/       # 看板服务 (8085)
├── mes-ai-service/      # Python AI服务
├── mes-device-gateway/ # .NET设备网关
├── mes-frontend/        # Vue 3前端
└── sql/                # 数据库脚本
```

### 开发环境端口分配

| 服务 | 端口 |
|------|------|
| 前端 | 3000 |
| API网关 | 8080 |
| 认证服务 | 8081 |
| 工单服务 | 8082 |
| 工艺服务 | 8083 |
| 质量服务 | 8084 |
| 看板服务 | 8085 |
| MySQL | 3306 |
| Redis | 6379 |

## 快速启动指南

### 1. 启动基础设施

```bash
# Windows
start-docker.bat

# 或手动
docker compose up -d
```

### 2. 启动后端服务

```bash
# IDEA中按顺序启动
GatewayApplication (8080)
AuthApplication (8081)
WorkOrderApplication (8082)
ProcessApplication (8083)
QualityApplication (8084)
DashboardApplication (8085)
```

### 3. 启动前端

```bash
cd mes-frontend
npm install
npm run dev
```

### 4. 访问系统

- 地址：http://localhost:3000
- 账号：admin
- 密码：admin123

## 经验总结

### 1. 版本兼容性是微服务的基础

在选择 Spring Cloud 版本时，务必确认与 Spring Boot 版本的兼容性。推荐使用 Spring Cloud 官方发布的版本对照表。

### 2. 开发环境与生产环境分离

- 开发环境：使用简化架构，减少外部依赖
- 生产环境：启用完整功能（Nacos、Seata、Kafka等）

### 3. 数据库初始化脚本要仔细检查

重复字段这种低级错误会导致数据库初始化失败，建议使用数据库建模工具生成 SQL。

### 4. 密码验证需要统一标准

如果使用 BCrypt，建议前后端使用同一套实现，避免因库差异导致的验证失败。

## 技术亮点

1. **多语言微服务架构**：Java、.NET、Python 协同工作
2. **简化开发流程**：快速启动脚本 + Docker
3. **灵活的配置策略**：开发/生产环境分离
4. **完整的代码注释**：116个文件全部添加文档注释

## 后续优化方向

1. 引入 Nacos 作为服务注册与配置中心
2. 添加 Seata 分布式事务支持
3. 集成 Kafka 消息队列
4. 引入 InfluxDB 存储时序数据
5. 完善单元测试和集成测试

## 参考资料

- [Spring Cloud 官方文档](https://spring.io/projects/spring-cloud)
- [Spring Boot 版本对照](https://spring.io/projects/spring-boot#overview)
- [Spring Cloud与Spring Boot版本兼容性](https://spring.io/projects/spring-cloud#overview)

---

*如果你觉得这篇文章有帮助，欢迎关注和转发！*
*如有问题，请在评论区留言讨论。*