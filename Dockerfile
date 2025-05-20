# Use OpenJDK base image
FROM openjdk:17-jdk-slim

# Create work directory
WORKDIR /app

# Copy the fat jar into the container
COPY target/*.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Command to run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
