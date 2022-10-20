# BUILD STAGE
FROM maven:3.8.6-eclipse-temurin-19-alpine AS builder

RUN mkdir "/app"

ADD ./pom.xml /app/pom.xml
ADD ./src /app/src/

ENV TESTCONTAINERS_DISABLED=true

RUN cd /app && mvn clean package

# EXECUTION STAGE
From eclipse-temurin:19-alpine

COPY --from=builder /app/target /app/

EXPOSE 8080

CMD ["java", "--module-path", "/app/classes:/app/lib", "--enable-preview", "-m", "pbouda.github.lang/pbouda.github.lang.Application"]