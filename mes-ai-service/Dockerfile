# Virtual Path MES - AI 服务镜像（CPU-only，无 torch/CUDA）
# 构建：docker build -t mes-ai-service .
FROM python:3.11-slim

# pip 镜像源（国内服务器默认清华源，海外可用 --build-arg PIP_INDEX=https://pypi.org/simple 覆盖）
ARG PIP_INDEX=https://pypi.tuna.tsinghua.edu.cn/simple

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip -i ${PIP_INDEX} && \
    pip install --no-cache-dir -r requirements.txt -i ${PIP_INDEX}

COPY src/ ./src/
COPY config.yaml ./config.yaml
RUN mkdir -p models tests

EXPOSE 8087

CMD ["python", "-m", "src.main"]
