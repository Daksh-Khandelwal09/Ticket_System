FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY src ./src
RUN mkdir -p out && javac -d out src/servicedesk/*.java
CMD ["java", "-cp", "out", "servicedesk.WebServer"]
