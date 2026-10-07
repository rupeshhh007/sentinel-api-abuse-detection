FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-amazoncorretto-17 AS backend
WORKDIR /app
COPY pom.xml ./
COPY src ./src
COPY --from=frontend /app/frontend/dist ./frontend/dist
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

FROM amazoncorretto:17-alpine
WORKDIR /app
RUN addgroup -S sentinel && adduser -S sentinel -G sentinel
COPY --from=backend /app/target/*.jar app.jar
USER sentinel
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
