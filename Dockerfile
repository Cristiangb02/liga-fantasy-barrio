FROM maven:3.9.9-eclipse-temurin-21

COPY . .

RUN mvn clean package -DskipTests

ENTRYPOINT ["sh", "-c", "java -jar target/*.jar"]
