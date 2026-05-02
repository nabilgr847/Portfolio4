FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN javac -cp "sqlite-jdbc-3.45.1.0.jar:slf4j-api-2.0.7.jar:slf4j-nop-2.0.7.jar:." CyberServer.java
EXPOSE 8080
CMD ["java", "-cp", "sqlite-jdbc-3.45.1.0.jar:slf4j-api-2.0.7.jar:slf4j-nop-2.0.7.jar:.", "CyberServer"]
