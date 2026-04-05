FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
COPY src/ ./src/
COPY config.yaml ./config.yaml
EXPOSE 8086
CMD ["python", "-m", "src.main"]
