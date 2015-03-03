prowse-site - seed project  [![Build Status](https://travis-ci.org/cosm1c/prowse-site.svg?branch=master)](https://travis-ci.org/cosm1c/prowse-site)
==========================

Seed project for creating new web applications with Play! Framework.

# First Milestone - HTTP spec compliance
 * Support HTTP spec for simple read requests, see: [RFC7231](https://tools.ietf.org/html/rfc7231)
 * Support HTTP spec for conditional read requests, see: [RFC7232](https://tools.ietf.org/html/rfc7232)

## Endpoints
 * `/buildInfo` - provides JSON detailing current build
 * `/buildInfo.html` - provides HTML detailing current build

# Second Milestone - text only content from dependency injected repository
 * serve simple text content within existing html pageTemplate
 * integrate dependency injection
 * test coverage for repository contents

## Endpoints
 * `/articles/*path` - provides content from repository (`loremIpsum` only article path)

