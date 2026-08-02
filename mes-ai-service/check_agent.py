"""Agent 服务前置检查脚本 — GOAI 大赛作品验证"""

import sys
import importlib


def check_module(name, hint=""):
    try:
        importlib.import_module(name)
        print(f"  ✅ {name}")
        return True
    except ImportError:
        print(f"  ❌ {name} — {hint}")
        return False


def main():
    print("=" * 50)
    print("  GOAI 大赛 — Agent 工业助手 前置检查")
    print("=" * 50)

    print("\n📦 Python 依赖检查:")
    checks = [
        ("fastapi", "pip install fastapi"),
        ("uvicorn", "pip install uvicorn[standard]"),
        ("zhipuai", "pip install zhipuai"),
        ("httpx", "pip install httpx"),
        ("yaml", "pip install pyyaml"),
    ]
    all_ok = all(check_module(m, h) for m, h in checks)

    print("\n🔑 智谱AI API Key 检查:")
    import os
    key = os.environ.get("ZHIPU_API_KEY")
    if key:
        print(f"  ✅ 环境变量 ZHIPU_API_KEY 已设置")
    else:
        env_path = os.path.join(os.path.dirname(__file__), ".env.local")
    if os.path.exists(env_path):
        found = False
        with open(env_path) as f:
            for line in f:
                if line.startswith("ZHIPU_API_KEY="):
                    found = True
                    break
        if found:
            print(f"  ✅ .env.local 中已配置 ZHIPU_API_KEY")
        else:
            print(f"  ❌ .env.local 中未找到 ZHIPU_API_KEY")
            print(f"     请在 .env.local 中添加 ZHIPU_API_KEY=your_key")
            all_ok = False
    else:
        print(f"  ❌ 未找到 .env.local 文件")
        print(f"     请创建 .env.local 并添加 ZHIPU_API_KEY=your_key")
        all_ok = False

    print("\n🔌 MES 后端服务检查:")
    try:
        import httpx
        r = httpx.get("http://localhost:8085/dashboard/production-line/list", timeout=3)
        if r.status_code == 200:
            print(f"  ✅ mes-dashboard (8085) 运行正常")
        else:
            print(f"  ⚠️  mes-dashboard 返回状态码 {r.status_code}")
    except Exception:
        print(f"  ❌ mes-dashboard (8085) 未连接 — 请先启动后端服务")
        all_ok = False

    try:
        r = httpx.get("http://localhost:8082/workorder/page", timeout=3)
        if r.status_code == 200:
            print(f"  ✅ mes-workorder (8082) 运行正常")
    except Exception:
        print(f"  ⚠️  mes-workorder (8082) 未连接 — 工单功能不可用")

    print("\n🧠 Agent 路由检查:")
    agent_files = [
        "src/services/tools.py",
        "src/services/agent_service.py",
        "src/services/knowledge_base.py",
        "src/router/agent.py",
    ]
    base = os.path.dirname(__file__)
    for f in agent_files:
        path = os.path.join(base, f)
        if os.path.exists(path):
            print(f"  ✅ {f}")
        else:
            print(f"  ❌ {f} — 文件缺失")
            all_ok = False

    print(f"\n{'=' * 50}")
    if all_ok:
        print("  ✅ 所有检查通过！可以启动 Agent 服务")
        print(f"\n  启动命令:")
        print(f"    cd mes-ai-service && python -m uvicorn src.app:create_app --factory --host 0.0.0.0 --port 8087 --reload")
    else:
        print("  ⚠️  存在未通过项，请修复后重试")
    print(f"{'=' * 50}")


if __name__ == "__main__":
    main()
