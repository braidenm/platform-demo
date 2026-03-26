# Contract-First Tooling Options

This project can stay Kotlin/Spring and still be strongly contract-first.

## Recommended Baseline (for this repo now)

1. `OpenAPI 3.1` as the HTTP contract (`openapi.yaml`).
2. `openapi-generator` for server interfaces and client SDKs.
3. `Prism` (or similar) for mock server from OpenAPI.
4. `Spectral` for OpenAPI lint rules in CI.
5. `schemathesis` or REST-assured contract tests against the spec.

## Event-Focused Add-On

OpenAPI is good for command/query HTTP APIs, but event contracts are cleaner with AsyncAPI.

- Keep OpenAPI for:
  - auth endpoints
  - command submission
  - command status and read-model queries
- Add AsyncAPI for:
  - event stream topics/channels
  - event payload schemas and compatibility policy

## Language/Framework Choices

### Kotlin / Spring Boot

- `springdoc-openapi`: generate and validate OpenAPI docs from code.
- `openapi-generator-maven/gradle-plugin`: generate interfaces/DTOs.
- `spring-cloud-stream` or Kafka client for event publication.

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
4. Enforce event schema compatibility rules.
5. Fail PR if breaking API/event changes are not versioned.
