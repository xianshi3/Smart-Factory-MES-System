FROM openjdk:17-jdk-slim

LABEL maintainer="MES System"
LABEL version="1.0"
LABEL description="MES API Gateway Service"

RUN groupadd -r mes && useradd -r -g mes mes

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs /app/config && chown -R mes:mes /app

COPY target/mes-gateway-1.0.0-SNAPSHOT.jar /app/app.jar

USER mes

RUN echo '#!/bin/sh\n\
echo "Starting MES Gateway..."\n\
exec java -Xms256m -Xmx512m -jar /app/app.jar' > /app/start.sh && \
chmod +x /app/start.sh

EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -sf http://localhost:9090/actuator/health || exit 1

CMD ["/app/start.sh"]
