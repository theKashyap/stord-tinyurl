
# Install SQLite
FROM ubuntu:focal
RUN apt-get -y update
# RUN sudo apt-get -y upgrade
RUN apt-get install -y sqlite3 libsqlite3-dev


RUN mkdir -p /stord/tinyurl
WORKDIR /stord/tinyurl

FROM openjdk:8-jdk-alpine
# copy over the Uber Jar, which is expected to be built/packaged already
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /stord/tinyurl/app.jar
ENTRYPOINT java -jar /stord/tinyurl/app.jar

# Not sure what's the use of this. Without -p hostPort:guestPort, no actual port forwarding happens
EXPOSE 8080
