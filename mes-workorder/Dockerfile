FROM openjdk:17-jdk-slim

LABEL maintainer="MES System"
LABEL version="1.0"
LABEL description="MES WorkOrder Service"

RUN groupadd -r mes && useradd -r -g mes mes

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

# 国内服务器：Debian 源换阿里云镜像，仅装 healthcheck 需要的 curl
RUN if [ -f /etc/apt/sources.list.d/debian.sources ]; then \
        sed -i 's|deb.debian.org|mirrors.aliyun.com|g' /etc/apt/sources.list.d/debian.sources; \
    else \
        sed -i 's|deb.debian.org|mirrors.aliyun.com|g' /etc/apt/sources.list; \
    fi && \
    apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs /app/config && chown -R mes:mes /app

COPY target/mes-workorder-1.0.0-SNAPSHOT.jar /app/app.jar

USER mes

RUN echo '#!/bin/sh\n\
echo "Starting MES WorkOrder Service..."\n\
exec java -Xms512m -Xmx1024m -jar /app/app.jar' > /app/start.sh && \
chmod +x /app/start.sh

EXPOSE 8082

HEALTHCHECK --interval=60s --timeout=10s --retries=3 --start-period=90s \
  CMD curl -sf http://localhost:8082/actuator/health || exit 1

CMD ["/app/start.sh"]
