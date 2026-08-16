"""MES AI Service 启动模块"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 必须在任何业务模块 import 之前加载项目根目录 .env（JWT_SECRET / MYSQL_PASSWORD 等）
from src.env_loader import load_project_env  # noqa: E402
load_project_env()

import uvicorn  # noqa: E402
import yaml  # noqa: E402
from src.app import create_app  # noqa: E402

app = create_app()

if __name__ == "__main__":
    # 端口/监听地址：环境变量 AI_PORT/AI_HOST 优先，其次 config.yaml 的 server 段
    _config = {}
    try:
        _config_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "config.yaml")
        with open(_config_path, "r", encoding="utf-8") as f:
            _config = yaml.safe_load(f) or {}
    except OSError:
        pass
    _server = _config.get("server", {}) or {}
    host = os.environ.get("AI_HOST", _server.get("host", "0.0.0.0"))
    port = int(os.environ.get("AI_PORT", _server.get("port", 8087)))
    uvicorn.run(app, host=host, port=port)
