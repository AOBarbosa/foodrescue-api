# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copia primeiro os pom.xml para aproveitar cache de dependencias
COPY pom.xml .
COPY framework/perishrescue-core/pom.xml framework/perishrescue-core/
COPY framework/perishrescue-domain/pom.xml framework/perishrescue-domain/
COPY framework/perishrescue-persistence/pom.xml framework/perishrescue-persistence/
COPY framework/perishrescue-business/pom.xml framework/perishrescue-business/
COPY framework/perishrescue-rest/pom.xml framework/perishrescue-rest/
COPY instances/foodrescue/pom.xml instances/foodrescue/
RUN mvn -B -q -pl instances/foodrescue -am dependency:go-offline

# Copia o restante do codigo e builda o jar da instancia FoodRescue
COPY framework framework
COPY instances instances
RUN mvn -B -q -pl instances/foodrescue -am package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/instances/foodrescue/target/foodrescue.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
