# hmpps-jobs-board-integration-api

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-jobs-board-integration-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-jobs-board-integration-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-jobs-board-integration-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://hmpps-jobs-board-integration-api-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)

# About
The **Match Jobs and Manage Applications** - Jobs Board Integration service is conceived as an independent service providing asynchronous communication with other services. For now, the only service it will be integrated with will be the Meganexus Jobs Board.

* The product page on Developer Portal: [Match Jobs and Manage Applications](https://developer-portal.hmpps.service.justice.gov.uk/products/candidate-matching-1)
* The high level design on Confluence: [Match Jobs & Manage Applications - HLD](https://dsdmoj.atlassian.net/wiki/x/34NiJgE)

## Team
This integration service is developed and supported by `Education Skills & Work` team. They can be contacted via `#education-skills-work-employment-dev` on Slack.

## Healthiness
The integration service has a `/health` endpoint which indicates the service is up and running.

# Instructions

## Running the application locally

* Run with the Spring profile `dev` on local
  * Set active profile via this environmental variable `spring.profiles.active=dev` or `SPRING_PROFILES_ACTIVE=dev`
* Run with the Spring profile `local` group on local
  * Set active profile to `local`: `spring.profiles.active=local` or `SPRING_PROFILES_ACTIVE=local`
  * The `local` group will utilise `localstack` for Integration features with message queue (`SQS`)
* API Spec:
  * Goto `http://localhost:8080/swagger-ui/index.html` to explore the OpenAPI specifications
* Checking endpoints
  * Goto `http://localhost:8080/health` to check the service is up and running

### Running with Docker

```bash
docker compose pull && docker compose up -d
```

will build the application and run it and HMPPS Auth, with a `PostgreSQL` database and `localstack` within local docker.

### Running the application in Intellij

```bash
docker compose pull && docker compose up --scale hmpps-jobs-board-integration-api=0 -d
```

will just start a docker instance of HMPPS Auth, `PostgreSQL` database, and `localstack`. The application should then be started with a `dev` or `local` active profile
in Intellij.
* supply required env var, e.g.
  * `spring.profiles.active`=`dev`
  * `SPRING_PROFILES_ACTIVE`=`local`
  * `MN_AUTH_USERNAME`
  * `MN_AUTH_PASSWORD`
  * `MN_AUTH_APP_ID`
  * `API_CLIENT_ID`
  * `API_CLIENT_SECRET`
