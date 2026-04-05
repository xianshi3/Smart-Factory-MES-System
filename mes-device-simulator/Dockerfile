FROM node:20-alpine

WORKDIR /app

COPY package.json .
RUN npm install --production

COPY src ./src
COPY .env .

ENV NODE_ENV=production

CMD ["node", "src/index.js"]