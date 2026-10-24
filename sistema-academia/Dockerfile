# Etapa 1: Build da aplicação
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copia arquivos de configuração de dependências para cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia código-fonte e compila gerando o jar executável
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Execução leve com JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Cria usuário sem privilégios de root por segurança
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=builder /app/target/sistema-academia-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
