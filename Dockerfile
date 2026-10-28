# ==========================================
# Stage 1: Build the Java application
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and POM configuration
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Copy source code and package application
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Stage 2: Production Runtime image
# ==========================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as non-root user for security
RUN addgroup --system spring && adduser --system --ingroup spring spring \
    && mkdir -p /app/upload && chown spring:spring /app/upload
USER spring:spring

# Copy built jar from build stage
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Expose default port
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=aiven \
    SERVER_ADDRESS=0.0.0.0 \
    SERVER_FORWARD_HEADERS_STRATEGY=framework \
    SESSION_COOKIE_SECURE=true \
    DEMO_PAYMENT_AUTO_TUNNEL_ENABLED=false
EXPOSE 8080

# Run Spring Boot app with dynamic port assigned by cloud host
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-Djava.awt.headless=true", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
