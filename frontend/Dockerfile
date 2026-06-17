# --- Build stage: produce the production Angular bundle ---
FROM node:22-alpine AS build
WORKDIR /build

# Cache deps: install from lockfile before copying sources.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Runtime stage: serve the static bundle via nginx ---
FROM nginx:alpine AS runtime
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
