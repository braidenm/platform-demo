# Identity API-First Design

This document defines the public contract direction for the identity module before implementation.
Goal: keep identity reusable across multiple apps while preserving a clean extraction path.

Machine-readable contract:
- [`openapi.yaml`](./openapi.yaml)

## Status Overview

### Implemented now
- `POST /v1/register-user`
- `GET /v1/users/{userId}`
- `POST /v1/login`

### Planned next
- `POST /v1/refresh`
- `POST /v1/logout`
- `GET /v1/me`

### Planned after core local auth
- `POST /v1/auth/oidc/login`
- `POST /v1/auth/oidc/callback`
- `GET /v1/connections`
- `POST /v1/connections/{provider}`
- `DELETE /v1/connections/{provider}`

## 1. API Style

- Public base path: `/v1`
- JSON for request/response payloads
- UTC ISO-8601 timestamps
- snake_case for JSON fields
- public APIs should not leak internal command-processing concepts
- internal event emission is allowed even when the external API is synchronous

## 2. Register User (implemented)

`POST /v1/register-user`

Current behavior:
- public API is synchronous from the client's perspective
- request validation happens at the API boundary
- the write flow remains Axon/CQRS/event-driven internally
- supports `Idempotency-Key`
- returns a public response with the created user identifier
- does not expose internal command IDs or command status in the public response

Current response shape:
```json
{
  "user_id": "usr_01J9Y8TQK",
  "email": "user@example.com",
  "status": "ACTIVE",
  "created_at": "2026-03-26T21:00:00Z"
}
```

Implementation expectation:
- aggregate emits domain events
- projections update Mongo read models
- outbound integration event publishing happens through event handlers/listeners
- idempotency must hold across retries, projections, and outbound publication

## 3. Auth Session Policy (login slice implemented)

### Current login behavior
- validates local email/password credentials through an internal auth-provider port
- normalizes incoming email case before credential lookup
- rejects inactive credential status with the same `401 unauthorized` contract as invalid credentials
- returns access + refresh tokens with TTLs aligned to this contract
- persists provider-attributed session metadata sourced from the internal auth-provider adapter
- persists refresh-token-backed session state with only a token hash stored at rest
- invalid credentials return `401 unauthorized` without creating session rows

### Access token
- TTL: **15 minutes**
- short-lived bearer token
- should be stateless for normal resource authorization
- returned as `expires_in=900`

### Refresh token
- TTL: **30 days**
- stateful and persisted
- returned as `refresh_expires_in=2592000`
- rotated on every successful refresh
- previous refresh token becomes invalid after rotation
- logout revokes the active refresh token/session
- expired, revoked, or reused refresh tokens must be rejected

### Recommended model
We are aiming for a hybrid model:
- stateless access tokens
- stateful refresh/session control

This gives clean API/resource-server behavior while preserving revocation, rotation, logout, and session-management capabilities.

### Client communication
Clients should be told explicitly through API responses and docs that:
- access tokens are short-lived bearer credentials
- refresh tokens are long-lived session credentials
- refresh tokens must be replaced when `/v1/refresh` returns a new one
- clients must treat refresh token reuse failures as a signal to re-authenticate

Planned token/session response shape:
```json
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_in": 900,
  "refresh_token": "...",
  "refresh_expires_in": 2592000
}
```

## 4. Future Auth / Federation Naming

We are intentionally avoiding provider-specific concepts in the core public local-auth paths.

### Local auth APIs
- `POST /v1/login`
- `POST /v1/refresh`
- `POST /v1/logout`
- `GET /v1/me`

### OIDC / social auth APIs
- `POST /v1/auth/oidc/login`
- `POST /v1/auth/oidc/callback`

### Linked provider/account APIs
- `GET /v1/connections`
- `POST /v1/connections/{provider}`
- `DELETE /v1/connections/{provider}`

### Later service-to-service / delegated token APIs
- `POST /v1/token/client-credentials`
- `POST /v1/token/exchange`

## 5. CQRS / Eventing Direction

Current intended architecture:
- API validates input and dispatches commands through Axon where the flow is event-driven
- aggregate/command handling owns the write-side decision flow
- domain events are stored in the event store
- event handlers/listeners project data into read models
- public API responses should remain public-facing and should not expose command-processing internals
- not every auth API has to be fully event-driven if a synchronous transactional auth flow makes more sense

Recommended split:
- registration and user-lifecycle changes are strong CQRS/event-driven candidates
- login/refresh/logout may be synchronous while still emitting audit/domain events after success

For tests, deterministic processing is acceptable in the test profile if it preserves the same event flow while making slice tests reliable.

## 6. Provider Extensibility Direction

We want to hand-roll local auth first without blocking future external identity providers.

Recommended boundary:
- keep an internal interface/port for credential verification and provider-backed identity exchange
- maintain internal user IDs as the canonical identity in the platform
- treat Auth0 / generic OIDC providers as adapters that map external subjects to internal users

This should let us:
- implement local email/password now
- add Auth0 later without rewriting core identity/session concepts
- support linked accounts and provider connections in a clean way

## 7. Outbound Domain Events

In addition to projections, identity domain events should be publishable to Kafka so other services can react.

Implementation direction:
- one event handler/listener layer is responsible for translating relevant domain events into Kafka integration events
- publication must be idempotent / safe against replay
- tests should verify publication where applicable

## 8. Contract-First Delivery Flow

1. Define or update the OpenAPI contract.
2. Implement one vertical slice.
3. Keep shared concerns in the `shared` Gradle module when they are truly cross-cutting.
4. Add/maintain service-level slice tests for the implemented API.
5. Verify API response, persisted state, projected state, and Kafka publication where applicable.
6. Do not move to the next API until the current slice is green.

## 9. Out-of-Scope for Current Slice

- organizations and memberships
- audience grants
- impersonation
- domain entities outside identity/auth
- advanced authorization decision endpoints

These remain deferred until the core identity/auth slice is stable.
