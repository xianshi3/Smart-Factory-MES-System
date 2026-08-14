"""项目根目录 .env 加载器（与 Java 服务行为一致）。

在 main.py 入口最早处调用，确保其他业务模块 import 前环境变量已就绪。
优先级：已存在的系统/IDE 环境变量 > .env 文件。
"""
import os

_PROJECT_ENV_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "..", ".env")


def load_env_file(path: str) -> None:
    """解析 KEY=VALUE 格式（支持 # 注释与引号），不覆盖已有环境变量"""
    if not os.path.isfile(path):
        return
    try:
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                key, _, value = line.partition("=")
                key = key.strip()
                if not key:
                    continue
                value = value.strip().strip('"').strip("'")
                if os.environ.get(key) is None:
                    os.environ[key] = value
    except OSError:
        pass


def load_project_env() -> None:
    load_env_file(_PROJECT_ENV_PATH)
