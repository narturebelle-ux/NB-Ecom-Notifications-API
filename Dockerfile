# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# Strip CRLF in case the source was uploaded from a Windows checkout (sh would fail on "\r").
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
# Separate layer so dependency downloads are cached across builds unless build.gradle changes.
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Vercel's Container runtime routes to port 80 unless the project sets PORT; server.port reads PORT.
ENV PORT=80
EXPOSE 80
# Use most of the container's memory for the heap (the JVM default is only 25%).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
