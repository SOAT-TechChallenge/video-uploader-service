FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copia o pom.xml e baixa as dependências (isso otimiza o cache do docker)
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
EXPOSE 5005

ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]