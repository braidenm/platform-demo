# Identity Plan

This file tracks the implementation order for the identity module.
It is intentionally practical: what is done, what is next, and what is deferred.

## Current Status

### Done
- [x] `POST /v1/register-user`
- [x] `GET /v1/users/{userId}`
- [x] `POST /v1/login`
- [x] `POST /v1/refresh`
- [x] `POST /v1/logout`
- [x] `GET /v1/session`
- [x] login service ports for credential auth, token issuing, and session persistence
- [x] refresh token persistence with rotation + reuse detection semantics
- [x] resource-server compatible access token validation path for `@PreAuthorize`
- [x] public register response no longer exposes command internals
- [x] compose-backed service-level test setup for identity
- [x] CI test-infrastructure isolation via dedicated compose project/ports and bounded health waits
- [x] identity module refactor toward clearer package structure
- [x] OpenAPI/docs updated for registration + login slices

### In Progress / needs hardening
- [x] stabilize the register-user CQRS/Axon implementation so tests are consistently green
- [ ] finish tightening service/entity/repository separation after refactor
- [ ] verify idempotency across write flow, projections, and outbound event publishing
- [ ] add Kafka domain-event publication from identity domain events
- [x] ensure the first slice stays green before moving on

## Target Architecture Direction

### Registration
- registration should remain **Axon/CQRS/event-driven**
- API validates input and dispatches an Axon command
- aggregate owns write-side decisions and emits domain events
- Postgres remains the write-side persistence / event-store-backed system of record where applicable
- Mongo remains the read-model store
- registration must remain idempotent end-to-end

### Auth session model
- access tokens should be **stateless, short-lived bearer tokens**
- refresh tokens should be **stateful, persisted, rotated, and revocable**
- logout should revoke the active refresh token / session
- refresh token reuse after rotation must be rejected
- not every auth API needs to be event-driven if synchronous transactional handling is the better fit

### Provider extensibility
- keep an internal interface/port boundary so local auth can be implemented now
- later plug in providers such as Auth0 / generic OIDC without changing the core domain model
- internal user identity should remain canonical even when external providers are added

### Outbound domain events
- every relevant domain event should have one handler responsible for publishing an outbound Kafka message
- outbound publication must be idempotent / safe for replay
- other services should be able to consume these domain events as integration events

## Next APIs (Phase 1: local auth)

### 1. Login
- [x] `POST /v1/login`
- returns access token + refresh token
- likely synchronous
- should verify local credentials through a clear internal auth interface/port

### 2. Refresh
- [x] `POST /v1/refresh`
- rotates refresh token
- synchronous
- must support reuse detection and revocation semantics

### 3. Logout
- [x] `POST /v1/logout`
- revokes refresh token/session
- synchronous

### 4. Session Read
- [x] `GET /v1/session`
- returns current authenticated user/session context

## API Authorization Integration (`@PreAuthorize`)

- APIs that consume access tokens should run as Spring Security resource servers.
- Bearer access token validation should happen before controller/service code:
  - signature
  - issuer
  - audience
  - expiration
- Method-level authorization should use `@PreAuthorize` against token-derived authorities/claims.
- In local mode we can validate with a shared signing secret; for extracted/multi-service deployment we should move to asymmetric signing + JWKS distribution while keeping the same `@PreAuthorize` model.
- Refresh/session state remains identity-owned; downstream resource APIs should not perform refresh-token checks.

## Planned Auth Policy

- access token TTL: 15 minutes
- refresh token TTL: 30 days
- refresh token rotates on every successful refresh
- logout revokes active refresh token/session
- refresh token reuse after rotation is rejected

## Future Naming Direction

### Local auth
- `POST /v1/login`
- `POST /v1/refresh`
- `POST /v1/logout`
- `GET /v1/session`

### OIDC / social auth
- `POST /v1/auth/oidc/login`
- `POST /v1/auth/oidc/callback`

### Linked provider/account management
- `GET /v1/connections`
- `POST /v1/connections/{provider}`
- `DELETE /v1/connections/{provider}`

### Later machine/delegated token flows
- `POST /v1/token/client-credentials`
- `POST /v1/token/exchange`

## Deferred Until After Core Local Auth

- [ ] OIDC/social login implementation
- [ ] account connection management
- [ ] machine-to-machine token flow
- [ ] delegated auth / token exchange
- [ ] organizations and memberships
- [ ] advanced authorization APIs

## Testing Rules For This Module

- prefer service-level / slice / integration tests over mock-heavy unit tests for APIs
- for create/write APIs, test from API input to observable outcomes:
  - expected HTTP response
  - expected persisted write-side state in Postgres where applicable
  - expected projected read-side state in Mongo
  - expected Kafka message publication when applicable
  - expected follow-up GET/read endpoint response
- use randomized test data to reduce brittle hard-coded fixtures and cross-test collisions
- keep tests idempotency-aware: retries and duplicate delivery scenarios should be covered where relevant
- only use classic unit tests for pure calculation/logic helpers

## Working Rules for This Module

- API validates input and sends commands through Axon when the flow is event-driven.
- Generate IDs at the API/service boundary when needed.
- Aggregates/command handlers own write-side decision flow.
- Event handlers/listeners project read models.
- Kafka publication should happen from event handlers/listeners, not directly in the endpoint/controller path.
- Public responses should not leak internal command-processing concepts.
- Do not move to the next slice until the current slice builds and tests cleanly.
