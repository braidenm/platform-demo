# Identity Documentation

This folder contains the identity architecture, API-first contract, and diagrams.

## Architecture Decisions Reflected Here

- Identity stays app-agnostic (`User`, `Organization`, `Audience`, grants), while app domains keep their own business data.
- AuthN and AuthZ/FGA are separate logical boundaries inside the same module/runtime for now.
- Domain modules call authorization through an in-process contract (`AuthorizationPort`) in the monolith.
- The same contract can later be backed by an HTTP adapter when AuthZ is extracted.
- API contract is event-first: most writes are async command endpoints that emit events.

## Documents

- [Plan](./plan.md)
- [API-First Design](./api-first-design.md)
- [OpenAPI Spec](./openapi.yaml)
- [Contract-First Tooling Options](./contract-first-tooling.md)
- [SQL Seeds](./sql/README.md)
- [Bootstrap Seed SQL](./sql/bootstrap-seed.sql)

## Diagrams

- [Event Modeling](./diagrams/event-modeling.md)
- [Core Sequences](./diagrams/sequence-flows.md)
- [Diagram Sources (Mermaid + PlantUML)](./diagrams/README.md)
- [PlantUML Event Modeling](./diagrams/event-modeling.puml)
- [PlantUML Login Sequence](./diagrams/sequence-login-app-token.puml)
- [PlantUML Token Exchange Sequence](./diagrams/sequence-token-exchange.puml)
- [PlantUML API Authorization Sequence](./diagrams/sequence-api-authorization.puml)
- [PlantUML Impersonation Sequence](./diagrams/sequence-impersonation.puml)

## Package Scaffolding

Initial no-code package directories for AuthN/AuthZ separation are under:

- `../../services/identity/src/main/kotlin/com/platformdemo/identity/auth`
- `../../services/identity/src/main/kotlin/com/platformdemo/identity/authz`
