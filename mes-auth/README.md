# mes-auth — 认证与权限服务

用户认证、角色权限、菜单管理的统一服务（RBAC 模型）。

## 功能

- **登录/注册**：BCrypt 密码校验，签发 JWT（HS256/384/512 由密钥长度决定）
- **登录保护**：Redis 失败计数 + 黑名单（5 分钟 5 次锁 10 分钟），登出/改密 token 进黑名单
- **用户管理**：创建/更新（密码 BCrypt 加密）/列表/角色分配
- **角色权限**：角色 CRUD、权限分配（分配后即时清除权限缓存）
- **菜单管理**：按当前用户权限码过滤菜单（ADMIN 全量），管理员 CRUD
- **权限码查询**：`/auth/user/permissions`（前端 v-permission 指令数据源）

## 技术栈

Spring Boot 3.2.5 · MyBatis-Plus · Druid · Redis · jjwt · Knife4j

## 端口

`8081`

## 核心接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/auth/login` | 登录（返回 token + 用户信息） | 白名单 |
| POST | `/auth/register` | 注册 | 白名单 |
| GET | `/auth/info` | 当前用户信息 | Bearer |
| GET | `/auth/user/list` | 用户列表 | ADMIN |
| POST/PUT/DELETE | `/auth/user` | 创建/更新/删除用户 | ADMIN |
| GET | `/auth/menu/user` | 当前用户菜单 | Bearer |
| GET/POST/PUT/DELETE | `/auth/menu*` | 菜单管理 | ADMIN |
| GET | `/auth/role/list` | 角色列表 | Bearer |
| PUT | `/auth/role/{id}/permissions` | 分配权限 | ADMIN |
| GET | `/auth/permission/list` | 权限码列表 | ADMIN |
| GET | `/auth/user/permissions` | 当前用户权限码 | Bearer |

## 配置

```yaml
jwt:
  secret: ${JWT_SECRET:}     # 必填，从根目录 .env 自动读取
```

## 目录结构

```
src/main/java/com/mes/auth/
├── AuthApplication.java
├── controller/   # Auth/User/Role/Menu/Permission 控制器
├── service/      # AuthService（登录/注册/黑名单）
└── mapper/       # UserMapper 等
```

## 相关文档

- [数据库设计（sys_user/sys_role/sys_permission/sys_menu）](../docs/DATABASE.md)
- [安全设计](../SECURITY.md)

*最后更新: 2026-08-15*
