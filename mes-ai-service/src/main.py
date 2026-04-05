"""MES AI Service 启动模块"""
import uvicorn
from src.app import create_app

app = create_app()

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8086)
