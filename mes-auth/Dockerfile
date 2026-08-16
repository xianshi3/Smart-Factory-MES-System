FROM openjdk:17-jdk-slim

LABEL maintainer="MES System"
LABEL version="1.0"
LABEL description="MES Auth Service"

RUN groupadd -r mes && useradd -r -g mes mes

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs /app/config && chown -R mes:mes /app

COPY target/mes-auth-1.0.0-SNAPSHOT.jar /app/app.jar

USER mes

RUN echo '#!/bin/sh\n\
echo "Starting MES Auth Service..."\n\
exec java -Xms256m -Xmx512m -jar /app/app.jar' > /app/start.sh && \
chmod +x /app/start.sh

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -sf http://localhost:8081/actuator/health || exit 1

CMD ["/app/start.sh"]
