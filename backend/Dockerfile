# Step 1: Use an image that already has Maven + Java
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Step 2: Set the working directory
WORKDIR /app

# Step 3: Copy your code into the container
COPY . .

# Step 4: Build the JAR
RUN mvn package -DskipTests

# Step 5: Use a smaller image just to run the built app
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/Inventory_Management_System.jar app.jar

# Step 6: Expose the app’s port (for Render)
EXPOSE 8080

# Step 7: Start your app
CMD ["java", "-jar", "app.jar"]
