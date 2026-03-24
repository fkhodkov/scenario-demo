FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY events-lib/pom.xml events-lib/pom.xml
COPY events-lib/src     events-lib/src
COPY scenario-service/pom.xml scenario-service/pom.xml
COPY scenario-service/src     scenario-service/src

# Maven requires ALL modules declared in parent pom.xml to exist.
# Create a stub pom for the missing sibling so validation passes.
RUN mkdir -p simulator-service && \
    echo '<project><modelVersion>4.0.0</modelVersion><parent><groupId>com.demo</groupId><artifactId>scenario-platform</artifactId><version>1.0.0</version></parent><artifactId>simulator-service</artifactId></project>' \
    > simulator-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q install -pl events-lib -am -DskipTests && \
    mvn -q package -pl scenario-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/scenario-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
