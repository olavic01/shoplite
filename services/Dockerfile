# Build any module:  docker build --build-arg MODULE=user-service -t shoplite/user-service .
FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /src
COPY . .
RUN mvn -q -B -pl ${MODULE} -am -DskipTests package \
    && cp ${MODULE}/target/*.jar /app.jar

FROM eclipse-temurin:17-jre
RUN useradd -r -u 1001 app
COPY --from=build /app.jar /app.jar
USER app
ENV JAVA_OPTS="-Xmx256m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app.jar"]
