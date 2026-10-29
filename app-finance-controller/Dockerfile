# Imagem estática do frontend (ADR-014): build Angular + nginx com fallback SPA e proxy de /api.
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.29-alpine
# correções de segurança dos pacotes do sistema publicadas depois da imagem base (Trivy, F12)
RUN apk upgrade --no-cache
# Resolvido na subida do contentor (envsubst do nginx): destino das chamadas /api.
ENV API_UPSTREAM=http://api:8080
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/app-finance-controller/browser /usr/share/nginx/html
EXPOSE 80
