prowse-site - seed project  [![Build Status](https://travis-ci.org/cosm1c/prowse-site.svg?branch=master)](https://travis-ci.org/cosm1c/prowse-site)
==========================

Seed project for creating new web applications.

Environment variables
---------------------

Option to report metrics to a Graphite server:
 * `GRAPHITE_PORT_2003_TCP_ADDR` - Carbon line receiver host
 * `GRAPHITE_PORT_2003_TCP_PORT` - Carbon line receiver port

Example Docker run commands
---------------------------

An example when using the [Grafana Docker Dev Env Image](https://github.com/grafana/grafana-docker-dev-env)

First run Grafana container
> `docker run --rm --name graphite -p 10080:80 -p 10081:81 -p 19200:9200 -p 12003:2003 gfdev-image:latest`

Then run a packaged docker image of this project (`docker:publishLocal`):
> `docker run -p 9000:9000 --rm --link graphite:graphite prowse-site:1.0-SNAPSHOT`

***

Change History
==============

# First Milestone - HTTP spec compliance
 * Support HTTP spec for simple read requests, see: [RFC7231](https://tools.ietf.org/html/rfc7231)
 * Support HTTP spec for conditional read requests, see: [RFC7232](https://tools.ietf.org/html/rfc7232)

## Endpoints
 * `/buildInfo` - provides JSON detailing current build
 * `/buildInfo.html` - provides HTML detailing current build

***

# Second Milestone - text only content from dependency injected repository
 * serve simple text content within existing html pageTemplate
 * integrate dependency injection
 * test coverage for repository contents

## Endpoints
 * `/articles/*path` - provides content from repository (`loremIpsum` only article path)

***

# Third Milestone - metrics capture and reporting
 * use `gatling/test` for one pass performance testing
 * use `gatling/gatling-it:test` for performance testing against a deployed instance as 
   configured in `gatling/src/main/resources/gatling.conf`
