# ---- Build stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only the code-converter folder
COPY code-converter/ ./code-converter/

# Move into project directory
WORKDIR /app/code-converter

#  Fix permission for mvnw
RUN chmod +x mvnw

# Build
RUN ./mvnw clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/code-converter/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
