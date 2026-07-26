FROM openjdk:17-jdk-slim AS builder
WORKDIR /build
COPY target/mes-common-1.0.0-SNAPSHOT.jar /build/

FROM alpine:latest
RUN mkdir -p /app/lib
COPY --from=builder /build/mes-common-1.0.0-SNAPSHOT.jar /app/lib/
VOLUME /app/lib
CMD ["echo", "mes-common is a library module, use as a volume source"]