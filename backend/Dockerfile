# ==================== STAGE 1: Build ====================
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copia apenas os arquivos de dependência primeiro (cache de camadas)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código-fonte e gera o build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==================== STAGE 2: Runtime (otimizado para Render) ====================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia apenas o JAR gerado
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Otimizações de memória para planos gratuitos (Render Free = 512MB)
# -XX:MaxRAMPercentage=75 → usa no máximo 75% da RAM disponível
# -XX:+UseSerialGC       → GC mais leve para pouca memória
# -Xss256k               → stack threads menor
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseSerialGC", \
    "-Xss256k", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
