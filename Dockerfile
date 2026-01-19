# ===== BUILD STAGE =====
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy trước để cache dependency
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Copy source và build jar
COPY src ./src
RUN mvn -q clean package -DskipTests

# ===== RUN STAGE =====
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Render cấp PORT qua env
EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
