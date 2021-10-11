- [stord-tinyurl](#stord-tinyurl)
  * [Design](#design)
  * [Features](#features)
  * [Explore](#explore)
  * [Run (without building)](#run--without-building-)
    + [REST call examples](#rest-call-examples)
  * [Work with source](#work-with-source)
    + [Build, test and run](#build--test-and-run)
    + [Dockerize](#dockerize)
  * [Prometheus and metrics](#prometheus-and-metrics)
- [References](#references)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>

# stord-tinyurl
A sample implementation of the TinyURL problem.

## Design
> NOTE: Implementation heavily focuses on "backend" and not so much on frontend.

Application consists of:
- A spring-boot Java microservice that exposes:
    - RESTful API for to create and get resource `/tinyurl`
    - Frontend WebUI at `/` (implemented using vanilla js in html)
- Prometheus server that collects and collates Metrics for visibility.

## Features
* A Spring Boot micro-service that provides:
  * a RESTful API to create a shorter URL from a given long URL & to get back long URL given that short URL.
  * input URL validation.
  * URL redirection for short URLs
  * A Web UI
* Unit tests in Spock with coverage
* Correlation ID for every transaction for traceability.
* Prometheus with GUI to explore Metrics.
* Dockerized

## Explore
You can:
* Run
  * Without building [from DockerHub](#run--without-building-). Requires `docker`.
  * [Build from source](#work-with-source), run tests and run app.
* Invoke [REST API from shell](#rest-call-examples) using `curl`.
* Explore code in [github](https://github.com/theKashyap/stord-tinyurl).
* [Setup Prometheus](#prometheus-and-metrics) and see Metrics.
* [Run unit tests](#build--test-and-run) to see coverage report.

## Run (without building)

An image has been pushed to DockerHub [repo thekashyap/stord.tinyurl](https://hub.docker.com/repository/docker/thekashyap/stord.tinyurl).
You can pull and run image without anything other than docker.

```
docker pull thekashyap/stord.tinyurl
docker run --rm --name stord-tinyurl -p 8080:8080 thekashyap/stord.tinyurl
```

Web UI would be accessible at http://localhost:8080

### REST call examples
```sh
# Create a new tiny URL
curl -iX POST -H 'Content-Type:application/json' http://localhost:8080/tinyurl \
      -d '{"longUrl": "https://www.wikipedia.org/"}'

# Resolve an existing tiny URL, say `2`
curl -iX GET http://localhost:8080/tinyurl/2

# Get redirected
curl -iX GET http://localhost:8080/2
```


## Work with source
### Build, test and run
```sh
# Clone
git clone git@github.com:theKashyap/stord-tinyurl.git
   # OR
git clone https://github.com/theKashyap/stord-tinyurl.git

# Compile
./mvnw clean build -DskipTests

# Run unit tests, coverage
./mvnw clean test && echo "Coverage report at: ./target/site/jacoco/index.html"

# Run app
./mvnw clean -DskipTests spring-boot:run
```
Web UI would be accessible at http://localhost:8080

### Dockerize
```sh
# Build image
docker build -t kash/stord/tinyurl .

# Build and run a container
# stop and remove if a container is already running
docker stop $(docker inspect stord-tinyurl -f '{{.Id}}')
docker rm $(docker inspect stord-tinyurl -f '{{.Id}}')
# start a new container
docker run --rm -d --name stord-tinyurl -p 8080:8080 kash/stord/tinyurl

# Compile, test, create an image and run a container
./mvnw clean test package && \
  docker build -t kash/stord/tinyurl . && \
  docker run --rm -d --name stord-tinyurl -p 8080:8080 kash/stord/tinyurl
```
Web UI would be accessible at http://localhost:8080

## Prometheus and metrics
For metrics, we
* expose our app's metrics on `/actuator/prometheus` (so tools like Splunk, Datadog, Prometheus etc can collect metrics).
* setup a Prometheus container, that collects metrics from our app regularly and provides a UI at `http://localhost:9090`.


```sh

# If App container is not running already, then start it

# prepare config file so Prometheus knows where to poll for metrics
export STORD_IP_ADDRESS=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' stord-tinyurl)
sed "s/STORD_IP_ADDRESS_PLACEHOLDER/$STORD_IP_ADDRESS/" ./src/main/resources/prometheus-template.yml > $PWD/prometheus.yml

# start Prometheus
docker run --rm -d -p 9090:9090 -v $PWD/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus --config.file=/etc/prometheus/prometheus.yml
```

Prometheus can be accessed at: http://localhost:9090

Here is [an example query/graph](http://localhost:9090/graph?g0.expr=http_server_requests_seconds_count%7Buri%20!%3D%20%22%2Factuator%2Fprometheus%22%7D&g0.tab=0&g0.stacked=0&g0.show_exemplars=0&g0.range_input=1h) shows number of http calls to various end points.

# References
- https://spring.io/guides/gs/spring-boot-docker/
- https://github.com/delight-im/ShortURL/blob/master/Java/ShortURL.java
- https://www.callicoder.com/spring-boot-actuator-metrics-monitoring-dashboard-prometheus-grafana/

