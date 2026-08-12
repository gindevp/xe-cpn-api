# Multi-stage build for Railway (MySQL is a separate Railway plugin)
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Full project context (see .dockerignore). Avoids missing pom-related files.
COPY . .

ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xmx1024m"

RUN chmod +x mvnw \
 && ./mvnw -B -Pprod package \
      -Dmaven.test.skip=true \
      -DskipTests \
      -Dmodernizer.skip=true \
      -Dcheckstyle.skip=true \
      -Dspotless.check.skip=true \
      -Dskip.npm=true \
      -Dskip.installnodenpm=true

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd -r -u 1001 cpn
COPY --from=build /app/target/cpn-0.0.1-SNAPSHOT.jar /app/app.jar
USER 1001
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod,demo
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar --server.port=${PORT:-8080}"]
