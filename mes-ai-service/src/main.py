"""MES AI Service 启动模块"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 必须在任何业务模块 import 之前加载项目根目录 .env（JWT_SECRET / MYSQL_PASSWORD 等）
from src.env_loader import load_project_env  # noqa: E402
load_project_env()

import uvicorn  # noqa: E402
from src.app import create_app  # noqa: E402

app = create_app()

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8087)
