
# To build app, image and run it
#./mvnw clean package && docker build -t kash/stord/tinyurl . && docker run --rm -d --name stord-tinyurl -p 8080:8080 kash/stord/tinyurl

FROM ubuntu:focal

# Install SQLite
RUN apt-get -y update
# RUN sudo apt-get -y upgrade
RUN apt-get install -y sqlite3 libsqlite3-dev
#RUN apt-get install -y munin-node munin

# create app dir
RUN mkdir -p /stord/tinyurl
WORKDIR /stord/tinyurl

#FROM prom/prometheus
## add prometheus conf
#ARG CONF_FILE=src/main/resources/prometheus.yml
#ADD ${CONF_FILE} /etc/prometheus/prometheus.yml

FROM openjdk:8-jdk-alpine
# copy over the Uber Jar, which is expected to be built/packaged already
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /stord/tinyurl/app.jar
ENTRYPOINT java -jar /stord/tinyurl/app.jar

EXPOSE 8080
