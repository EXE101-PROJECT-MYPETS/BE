# ===============================
# 1️⃣ Stage build: dùng Maven + JDK 17 để build
# ===============================
FROM maven:3.9.9-eclipse-temurin-17 AS build

# Thư mục làm việc
WORKDIR /app

# Copy pom.xml trước để cache dependency
COPY pom.xml .

# Download dependency trước (tối ưu cache)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build jar (skip test cho nhanh)
RUN mvn clean package -DskipTests


# ===============================
# 2️⃣ Stage runtime: JRE nhẹ
# ===============================
FROM eclipse-temurin:17-jre-alpine

# Thư mục chạy app
WORKDIR /app

# Copy jar từ stage build
COPY --from=build /app/target/*jar app.jar

# Mở cổng Spring Boot (đổi nếu bạn dùng cổng khác)
EXPOSE 8080

# Chạy Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
