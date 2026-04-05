FROM openjdk:17-jdk-slim

LABEL maintainer="MES System"
LABEL version="1.0"
LABEL description="MES API Gateway Service"

WORKDIR /app

# Install utilities
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create directories
RUN mkdir -p /app/logs /app/config

# Copy JAR file
COPY mes-gateway/target/mes-gateway-1.0.0-SNAPSHOT.jar /app/app.jar

# Create startup script
RUN echo '#!/bin/sh\n\
echo "Starting MES Gateway..."\n\
exec java -Xms256m -Xmx512m -jar /app/app.jar' > /app/start.sh && \
chmod +x /app/start.sh

EXPOSE 8080

CMD ["/app/start.sh"]