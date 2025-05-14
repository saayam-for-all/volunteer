# Use an official Java runtime as the base image
FROM openjdk:17-slim

# Set the working directory in the container
WORKDIR /app

# Copy your Spring Boot jar into the container
COPY target/volunteer-0.0.1-SNAPSHOT.jar app.jar


# Expose the port your app runs on (typically 8080)
EXPOSE 8080

# Define the command to run your app
CMD ["java", "-jar", "app.jar"]