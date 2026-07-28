FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY src/ ./src/
COPY config.yaml ./config.yaml
RUN mkdir -p models tests
EXPOSE 8087
CMD ["python", "-m", "src.main"]
