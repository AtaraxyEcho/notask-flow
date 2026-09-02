FROM node:22-alpine AS build

WORKDIR /app

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ .

ARG VITE_API_BASE_URL=/api/v1
ARG VITE_COLLAB_WS_URL=/ws
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_COLLAB_WS_URL=${VITE_COLLAB_WS_URL}

RUN npm run build

FROM nginx:1.27-alpine

COPY backend/docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
