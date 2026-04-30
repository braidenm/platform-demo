# Identity Diagram Sources

This folder contains both Mermaid and PlantUML diagram versions.

## Boundary Legend

- `Identity Service (AuthN)` handles login/session/token minting.
- `Authorization Policy (AuthZ/FGA)` handles permission decisions and relation checks.
- In modular-monolith mode, policy checks are in-process through an authorization port.
- After extraction, the same port can delegate to `/identity/v1/authorize/check`.

## Event Modeling

- Mermaid: [event-modeling.md](./event-modeling.md)
- PlantUML: [event-modeling.puml](./event-modeling.puml)

## Sequence Flows

- Mermaid: [sequence-flows.md](./sequence-flows.md)
- PlantUML:
  - [sequence-login-access-refresh-session.puml](./sequence-login-access-refresh-session.puml)
  - [sequence-login-app-token.puml](./sequence-login-app-token.puml)
  - [sequence-token-exchange.puml](./sequence-token-exchange.puml)
  - [sequence-api-authorization.puml](./sequence-api-authorization.puml)
  - [sequence-impersonation.puml](./sequence-impersonation.puml)

## Rendering Notes

- Mermaid: supported directly by most Markdown viewers.
- PlantUML: install the IntelliJ PlantUML plugin and Graphviz (`dot`) on your machine.
