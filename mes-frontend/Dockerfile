FROM node:18-alpine AS builder

# 构建期 Vite 环境变量（docker compose build.args 传入，缺省用 .env.production）
ARG VITE_API_BASE_URL
ARG VITE_AI_SERVICE_URL
# npm 镜像源（国内服务器默认 npmmirror，海外可用 --build-arg NPM_REGISTRY=https://registry.npmjs.org 覆盖）
ARG NPM_REGISTRY=https://registry.npmmirror.com
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_AI_SERVICE_URL=${VITE_AI_SERVICE_URL}

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies（npm ci 按 lock 精确安装，--no-audit --no-fund 跳过审计/赞助请求）
RUN npm config set registry ${NPM_REGISTRY} && npm ci --no-audit --no-fund

# Copy source code
COPY . ./

# Build production
RUN npm run build

# Production nginx image
FROM nginx:alpine

# Copy build output to nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]