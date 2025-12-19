# 1. Usamos la imagen oficial de Maven con JAVA 21
FROM maven:3.9.9-eclipse-temurin-21

# 2. Copiamos tu proyecto dentro del servidor
COPY . .

# 3. Construimos la aplicación usando el Maven de la nube (evitamos error de wrapper)
RUN mvn clean package -DskipTests

# 4. Arrancamos lo que se haya creado en la carpeta target
ENTRYPOINT ["sh", "-c", "java -jar target/*.jar"]
