# --- Estágio 1: build do Angular ---
FROM node:22-alpine AS build

WORKDIR /app

# Copia só os manifests primeiro para aproveitar cache do npm ci
COPY package*.json ./
RUN npm ci

# Copia o código e compila (produção por padrão)
#
# Não há ARG/ENV de configuração: o front-end não lê nada de variável de ambiente.
# A URL da API é a constante relativa `/api` (src/environments/environment.ts) e o
# nginx deste compose é quem decide para onde ela aponta. Passar a API por build
# arg duplicaria a topologia de deploy em dois lugares, e os dois poderiam divergir
# em silêncio — o build passaria, mas o bundle chamaria o host errado.
COPY . .
RUN npm run build

# --- Estágio 2: servidor web (estáticos) + proxy para o backend ---
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/front-end/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]