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
# Fixed heap (not MaxRAMPercentage): Railway reports the plan limit, not a real container cap.
# Total RSS ≈ Xmx + metaspace + ~300MB (threads, code cache, direct buffers) ≈ 1.6GB.
# A JAVA_OPTS variable set on Railway overrides this default.
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar --server.port=${PORT:-8080}"]
