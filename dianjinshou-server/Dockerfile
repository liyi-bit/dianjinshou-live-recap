FROM eclipse-temurin:8-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/health || exit 1
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar", "--spring.profiles.active=prod"]
