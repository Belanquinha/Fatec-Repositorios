# --- Estágio 1: build da aplicação ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia só o pom.xml primeiro para aproveitar cache de dependências
COPY pom.xml ./
RUN mvn -q dependency:go-offline -DskipTests

# Copia o código e compila (gera o .jar executável)
COPY src ./src
RUN mvn -q package -DskipTests

# --- Estágio 2: imagem final (somente runtime) ---
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia o .jar gerado no estágio 1
COPY --from=build /app/target/*.jar app.jar

EXPOSE 4040

ENV DB_URL=jdbc:postgresql://postgres:5432/fatecrepository \
    POSTGRES_USER=postgres \
    POSTGRES_PASSWORD=123456 \
    JWT_SECRET=dev-secret-change-this-key-with-at-least-32-chars \
    JWT_ISSUER=fatec-repository-api \
    JWT_EXPIRATION_HOURS=24

ENTRYPOINT ["java", "-jar", "app.jar"]