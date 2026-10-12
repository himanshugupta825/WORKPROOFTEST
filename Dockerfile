
# Build the Java web application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Run the application on Tomcat 9
FROM tomcat:9.0-jdk17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /build/target/WorkProof.war /usr/local/tomcat/webapps/ROOT.war

# Railway provides PORT at runtime
EXPOSE 8080
CMD ["sh", "-c", "sed -i 's/port=\"8080\"/port=\"'\"${PORT:-8080}\"'\"/' /usr/local/tomcat/conf/server.xml && catalina.sh run"]
