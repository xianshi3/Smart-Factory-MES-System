# Virtual Path MES - AI 服务镜像
# 默认 CPU-only（无 GPU 服务器）；如需完整依赖用 --build-arg DEVICE=gpu
# 构建：
#   CPU: docker build -t mes-ai-service .
#   GPU: docker build --build-arg DEVICE=gpu -t mes-ai-service .
FROM python:3.11-slim

ARG DEVICE=cpu

WORKDIR /app

# 依赖按设备选择：cpu → requirements-cpu.txt（轻量，无 torch/CUDA）
#                 gpu → requirements.txt（完整）
COPY requirements.txt requirements-cpu.txt ./
RUN if [ "$DEVICE" = "cpu" ]; then \
        pip install --no-cache-dir --upgrade pip && \
        pip install --no-cache-dir -r requirements-cpu.txt; \
    else \
        pip install --no-cache-dir --upgrade pip && \
        pip install --no-cache-dir -r requirements.txt; \
    fi

COPY src/ ./src/
COPY config.yaml ./config.yaml
RUN mkdir -p models tests

EXPOSE 8087

CMD ["python", "-m", "src.main"]
