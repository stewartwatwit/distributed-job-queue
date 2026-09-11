FROM eclipse-temurin:21-jdk AS builder

WORKDIR /backend

COPY . /backend/

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-alpine AS runtime

WORKDIR /backend

COPY --from=builder /backend/target/*.jar /backend/target/

EXPOSE 8081

CMD ["sh", "-c", "java -jar target/*.jar"]