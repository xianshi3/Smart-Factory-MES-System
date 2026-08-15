# mes-common — 公共模块

被所有 Java 微服务依赖的基础组件库。

## 组件清单

### 安全（security）

| 类 | 说明 |
|-----|------|
| `JwtUtils` | JWT 签发/解析（HS256/384/512 按密钥长度）；启动校验 `JWT_SECRET` ≥ 32 字符，缺失拒绝启动 |
| `TokenAuthFilter` | 全局认证过滤器：白名单 `/auth/login,/auth/register,/actuator/`，解析 Bearer → `UserContext` |
| `SecurityInterceptor` | 权限拦截器：读取 `@RequireRole` / `@RequirePermission`（方法级优先于类级），ADMIN 全量放行 |
| `PermissionService` | 权限码查询（sys_role_permission → sys_permission），5 分钟本地缓存 + `evict`/`evictAll` 即时失效 |
| `UserContext` | ThreadLocal 用户上下文（userId/username/role） |
| `@RequireRole` / `@RequirePermission` | 权限注解 |

### 结果与异常

| 类 | 说明 |
|-----|------|
| `Result<T>` | 统一返回 `{code, message, data}`（200 成功） |
| `PageResult<T>` | 分页结果 |
| `BizException` | 业务异常（HTTP 400） |
| `ErrorCode` | 错误码枚举 |
| `GlobalExceptionHandler` | 全局异常处理：业务 400 / 未认证 401 / 其他 500（不泄露内部细节） |

### 实体与 Mapper

- `BaseEntity`：id / createTime / updateTime / deleted（物理删除）
- `User` / `Role` / `Permission` / `Menu` / `RolePermission` / `Bom` / `BomItem` / `Material` / `Inventory`（`availableQuantity` 为 DB 生成列，`@TableField(exist=false)`）/ `InventoryTransaction` / `Workstation` / `ProductionLine`
- 对应 `com.mes.common.mapper.*` Mapper 接口

### 工具

- `MesConstants` 常量 · `TimeUtils` · 雪花 ID 配置

## 单元测试

```bash
mvn test -pl mes-common
```

`JwtUtilsTest`（签发/解析/防篡改/跨密钥拒绝）+ `ResultTest` 共 10 用例。

## 使用

```xml
<dependency>
    <groupId>com.mes</groupId>
    <artifactId>mes-common</artifactId>
</dependency>
```

所有微服务启动时自动扫描（`@MapperScan("com.mes.common.mapper")` + Filter/Interceptor 自动注册）。

## 相关文档

- [安全设计](../SECURITY.md)
- [开发指南](../docs/DEVELOPMENT.md)

*最后更新: 2026-08-15*
