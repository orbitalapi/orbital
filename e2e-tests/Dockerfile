FROM node:16-alpine

FROM mcr.microsoft.com/playwright:focal

WORKDIR /app

ENV PATH /app/node_modules/.bin:$PATH

RUN apt-get update && apt-get -y install libnss3 libatk-bridge2.0-0 libdrm-dev libxkbcommon-dev libgbm-dev libasound-dev libatspi2.0-0 libxshmfence-dev

COPY package.json /app/
COPY package-lock.json /app/
COPY playwright.config.ts /app/
COPY tests/ /app/tests/

RUN npm ci && npx playwright install

CMD wait-on $BASE_URL && npx playwright test
