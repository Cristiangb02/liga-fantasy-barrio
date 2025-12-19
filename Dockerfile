# 1. Usamos Java 17 (La versión moderna que usa tu código)
FROM eclipse-temurin:21-jdk-alpine

# 2. Copiamos tu proyecto dentro del servidor de Render
COPY . .

# 3. Damos permisos al constructor de Maven
RUN chmod +x mvnw

# 4. Construimos la aplicación (creamos el .jar)
RUN ./mvnw clean package -DskipTests

# 5. Arrancamos lo que se haya creado en la carpeta target
ENTRYPOINT ["sh", "-c", "java -jar target/*.jar"]
