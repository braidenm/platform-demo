# Contract-First Tooling Options

This project can stay Kotlin/Spring and still be strongly contract-first.

## Recommended Baseline (for this repo now)

1. `OpenAPI 3.1` as the HTTP contract (`openapi.yaml`).
2. `openapi-generator` for server interfaces and client SDKs.
3. `Prism` (or similar) for mock server from OpenAPI.
4. `Spectral` for OpenAPI lint rules in CI.
5. `schemathesis` or REST-assured contract tests against the spec.

## Event-Focused Add-On

OpenAPI is good for the synchronous identity/auth HTTP APIs. Event contracts are cleaner with AsyncAPI once event publication becomes a first-class external concern.

- Keep OpenAPI for:
  - registration
  - login / refresh / logout / me
  - read-model queries
- Add AsyncAPI later for:
  - Kafka topics/channels
  - event payload schemas and compatibility policy

## Language/Framework Choices

### Kotlin / Spring Boot

- `springdoc-openapi`: generate and validate OpenAPI docs from code.
- `openapi-generator-maven/gradle-plugin`: generate interfaces/models.
- `spring-cloud-stream` or Kafka client for event publication when ready.

### TypeScript / Node

- `NestJS + @nestjs/swagger` or `tsoa` for contract-first HTTP.
- `zod` + `zod-to-openapi` for schema-first workflows.
- `asyncapi` tooling for event schemas.

### Go

- `oapi-codegen` for OpenAPI-first server/client generation.
- `kin-openapi` for validation in tests.

### Platform-Neutral Schema First

- `TypeSpec` can generate OpenAPI and other artifacts from one source.
- `Buf + Protobuf` if you later introduce gRPC in internal services.

## Suggested CI Gates

1. Lint OpenAPI (`spectral lint`).
2. Validate schema syntax and references.
3. Run mock-server contract tests.
4. Fail PR if breaking API changes are not versioned.
