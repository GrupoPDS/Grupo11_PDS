# ==================== STAGE 1: Build ====================
FROM node:20-alpine AS build

WORKDIR /app

# Copia arquivos de dependência primeiro (cache de camadas)
COPY package.json package-lock.json ./
RUN npm ci

# Copia código-fonte e gera o build de produção
COPY . .
RUN npm run build

# ==================== STAGE 2: Runtime (Nginx) ====================
FROM nginx:alpine

# Copia configuração customizada do Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copia os arquivos estáticos gerados pelo Vite
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
