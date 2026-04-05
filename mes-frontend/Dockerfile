FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY mes-frontend/package*.json ./

# Install dependencies
RUN npm install --registry=https://registry.npmmirror.com

# Copy source code
COPY mes-frontend/ ./

# Build production
RUN npm run build

# Production nginx image
FROM nginx:alpine

# Copy build output to nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx config
COPY mes-frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]