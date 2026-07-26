FROM openjdk:17-jdk-slim

LABEL maintainer="MES System"
LABEL version="1.0"
LABEL description="MES Process Service"

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs /app/config

COPY target/mes-process-1.0.0-SNAPSHOT.jar /app/app.jar

RUN echo '#!/bin/sh\n\
echo "Starting MES Process Service..."\n\
exec java -Xms256m -Xmx512m -jar /app/app.jar' > /app/start.sh && \
chmod +x /app/start.sh

EXPOSE 8083

CMD ["/app/start.sh"]