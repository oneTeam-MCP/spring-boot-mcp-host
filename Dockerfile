FROM eclipse-temurin:21-jre

# Node.js, npm, npx 설치
RUN apt-get update && apt-get install -y nodejs npm \
    && ln -sf /usr/bin/npx /usr/local/bin/npx

WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]