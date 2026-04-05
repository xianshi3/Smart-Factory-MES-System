FROM openjdk:17-jdk-slim

LABEL maintainer="MES System"
LABEL version="1.0"
LABEL description="MES Common Module"

WORKDIR /app

COPY mes-common/target/mes-common-1.0.0-SNAPSHOT.jar /app/lib/

RUN mkdir -p /app/ext && \
    echo "# Common extensions" > /app/ext/README

CMD ["tail", "-f", "/dev/null"]