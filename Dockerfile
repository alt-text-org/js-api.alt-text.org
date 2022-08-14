FROM node:16 AS base
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

FROM node:16-alpine
WORKDIR /app
COPY --from=base /app .
COPY . .

EXPOSE 3000

CMD ["npm", "start"]
