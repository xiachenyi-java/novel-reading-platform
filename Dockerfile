# 第一阶段：构建（使用 Maven + JDK 23）
FROM maven:3.9-eclipse-temurin-23-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# 第二阶段：运行（使用 JRE 23，体积更小）
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar"]