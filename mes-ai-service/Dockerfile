# Virtual Path MES - AI 服务镜像（CPU-only，无 torch/CUDA）
# 构建：docker build -t mes-ai-service .
# Python 版本与本地开发环境一致（README 技术栈为 3.12）
FROM python:3.12-slim

# pip 镜像源（国内服务器默认清华源，海外可用 --build-arg PIP_INDEX=https://pypi.org/simple 覆盖）
ARG PIP_INDEX=https://pypi.tuna.tsinghua.edu.cn/simple

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip -i ${PIP_INDEX} && \
    pip install --no-cache-dir -r requirements.txt -i ${PIP_INDEX}

COPY src/ ./src/
COPY knowledge/ ./knowledge/
COPY config.yaml ./config.yaml

EXPOSE 8087

# slim 镜像无 curl，用标准库 urllib 做健康检查（/api/v1/health 无需鉴权）
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=30s \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8087/api/v1/health', timeout=3)" || exit 1

CMD ["python", "-m", "src.main"]
