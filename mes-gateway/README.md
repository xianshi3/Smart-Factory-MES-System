# mes-gateway — API 网关

Spring Cloud Gateway 统一入口，全链路 JWT 鉴权与跨域管理。

## 功能

- **统一路由**：`/api/**` 前缀路由到 6 个后端服务（StripPrefix=1，AI 服务 StripPrefix=2）
- **全局 JWT 鉴权**（`JwtAuthGlobalFilter`）：除登录/注册/健康检查外，所有请求必须携带 `Authorization: Bearer <JWT>`，未携带/失效返回 401
- **用户身份透传**：校验通过后向后端注入 `X-User-Id` / `X-User-Name` / `X-User-Role` 请求头
- **CORS 白名单**：`CORS_ALLOWED_ORIGINS`（默认 `http://localhost:3000,http://localhost:5173`）+ credentials
- **WebSocket 路由**：`/api/ws/**` → 看板服务（`ws://` scheme）

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Cloud Gateway | 4.1.5 |
| jjwt (HS256/384/512) | 0.12.x |
| Spring Boot Actuator | 3.2.5 |

## 端口

`9090`（Docker 映射 `9090:9090`）

## 路由表

| 路由 | 后端服务 | StripPrefix |
|------|---------|-------------|
| `/api/auth/**` | mes-auth:8081 | 1 |
| `/api/workorder/**` | mes-workorder:8082 | 1 |
| `/api/process/**` | mes-process:8083 | 1 |
| `/api/quality/**` | mes-quality:8084 | 1 |
| `/api/dashboard/**`、`/api/alarm/**` | mes-dashboard:8085 | 1 |
| `/api/ai/**` | mes-ai-service:8087 | 2 |
| `/api/ws/**` | mes-dashboard:8085 (WebSocket) | 1 |

## 快速启动

```bash
# 本地（自动读取项目根目录 .env 的 JWT_SECRET）
mvn spring-boot:run -pl mes-gateway

# Docker
docker compose -f docker-compose.dev.yml up mes-gateway
```

## 核心配置

```yaml
jwt:
  secret: ${JWT_SECRET:}          # 必填（>= 32 字符），与所有服务一致
mes:
  security:
    whitelist: /api/auth/login,/api/auth/register,/actuator/**   # 可覆盖
```

## 目录结构

```
src/main/java/com/mes/gateway/
├── GatewayApplication.java
└── filter/
    └── JwtAuthGlobalFilter.java   # 全局 JWT 鉴权过滤器
```

## 相关文档

- [开发指南](../docs/DEVELOPMENT.md)
- [安全说明](../SECURITY.md)

*最后更新: 2026-08-15*
