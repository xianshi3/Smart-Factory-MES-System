FROM openjdk:17-jdk-slim

LABEL maintainer="MES System"
LABEL version="1.0"
LABEL description="MES Dashboard Service"

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs /app/config

COPY target/mes-dashboard-1.0.0-SNAPSHOT.jar /app/app.jar

RUN echo '#!/bin/sh\n\
echo "Starting MES Dashboard Service..."\n\
exec java -Xms512m -Xmx1024m -jar /app/app.jar' > /app/start.sh && \
chmod +x /app/start.sh

EXPOSE 8085

CMD ["/app/start.sh"]