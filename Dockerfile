FROM eclipse-temurin:21.0.3_9-jre-alpine
WORKDIR /
ADD "/target/*.jar" "web-app.jar"
CMD ["sh", "-c", "java -jar $JAVA_OPTS  ./web-app.jar"]
