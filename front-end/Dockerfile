# --- Estágio 1: build do Angular ---
FROM node:22-alpine AS build

WORKDIR /app

# Copia só os manifests primeiro para aproveitar cache do npm ci
COPY package*.json ./
RUN npm ci

# Copia o código e compila (produção por padrão)
COPY . .
ARG NG_APP_API_URL=/api
ARG NG_APP_MSAL_CLIENT_ID=146c36f9-abf3-48b0-a533-5f462e5e4eed
ARG NG_APP_MSAL_AUTHORITY=https://login.microsoftonline.com/eabe64c5-68f5-4a76-8301-9577a679e449
ARG NG_APP_MSAL_REDIRECT_URI=http://localhost:4200/
ENV NG_APP_API_URL=$NG_APP_API_URL \
    NG_APP_MSAL_CLIENT_ID=$NG_APP_MSAL_CLIENT_ID \
    NG_APP_MSAL_AUTHORITY=$NG_APP_MSAL_AUTHORITY \
    NG_APP_MSAL_REDIRECT_URI=$NG_APP_MSAL_REDIRECT_URI
RUN npm run build

# --- Estágio 2: servidor web (estáticos) + proxy para o backend ---
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/front-end/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]