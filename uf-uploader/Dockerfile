FROM maven:3-eclipse-temurin-18-alpine AS maven-build

COPY . /var/uf

WORKDIR /var/uf

RUN mkdir build/

RUN mvn clean package

RUN cp ./source/parser/target/uffn-parser-all.jar build/ && \
    cp ./source/sockets/target/uffn-sockets-all.jar build/




FROM eclipse-temurin:18-alpine AS jdk-server

COPY --from=maven-build /var/uf/build/* /var/uf/

WORKDIR /var/uf
