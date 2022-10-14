FROM --platform=linux/amd64 node:16-bullseye AS base
WORKDIR /app
COPY package*.json ./

RUN apt update
RUN apt install libvips-dev -y
RUN npm ci --only=production

COPY ./src ./src
COPY *.js .

ENV PORT=3000
ENV HOST='0.0.0.0'

EXPOSE 3000

CMD ["npm", "start"]
