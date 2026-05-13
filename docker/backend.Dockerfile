FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
RUN mvn -B -ntp dependency:go-offline
COPY backend/src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /workspace/target/tasktracker-backend.jar /app/tasktracker-backend.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/tasktracker-backend.jar"]
