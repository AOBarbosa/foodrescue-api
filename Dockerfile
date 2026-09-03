# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copia primeiro os pom.xml para aproveitar cache de dependencias
COPY pom.xml .
COPY domain/pom.xml domain/
COPY core/pom.xml core/
COPY persistence/pom.xml persistence/
COPY business/pom.xml business/
COPY rest/pom.xml rest/
RUN mvn -B -q -pl rest -am dependency:go-offline

# Copia o restante do codigo e builda o jar do modulo rest
COPY domain domain
COPY core core
COPY persistence persistence
COPY business business
COPY rest rest
RUN mvn -B -q -pl rest -am package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/rest/target/foodrescue-ai.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
