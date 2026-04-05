FROM openjdk:17-jdk-slim

LABEL maintainer="MES System"
LABEL version="1.0"
LABEL description="MES Auth Service"

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs /app/config

COPY mes-auth/target/mes-auth-1.0.0-SNAPSHOT.jar /app/app.jar

RUN echo '#!/bin/sh\n\
echo "Starting MES Auth Service..."\n\
exec java -Xms256m -Xmx512m -jar /app/app.jar' > /app/start.sh && \
chmod +x /app/start.sh

EXPOSE 8081

CMD ["/app/start.sh"]