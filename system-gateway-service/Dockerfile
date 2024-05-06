FROM openjdk:17-jdk-slim

ARG JAR_FILE=system-gateway-service.jar
# The Dockerfile is used on CircleCI from the root of the project, so this path is relative to the project root.
COPY ./system-gateway-service/target/${JAR_FILE} app.jar

#grafana
RUN mkdir -p /usr/local/tomcat/opentelemetry
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.0/opentelemetry-javaagent.jar  /usr/local/tomcat/opentelemetry/opentelemetry-javaagent.jar

ENTRYPOINT ["java","-javaagent:/usr/local/tomcat/opentelemetry/opentelemetry-javaagent.jar","-jar","./app.jar"]