# scripts — 工具脚本

## ci-smoke.sh

CI 冒烟测试：真实启动 auth/workorder/process/quality/gateway 五个服务，逐个等待健康检查就绪，再经网关验证「登录 → 带 token 访问 → 无 token 被 401 拦截」完整链路（依赖 MySQL/Redis services）。

```bash
# GitHub Actions 中由 .github/workflows/ci.yml 调用；本地需先导出 JWT_SECRET
export JWT_SECRET=your-secret-at-least-32-chars
bash scripts/ci-smoke.sh
```

*最后更新: 2026-08-15*
