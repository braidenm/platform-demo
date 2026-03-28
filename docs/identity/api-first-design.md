# Identity API-First Design

This document defines the API contract before implementation with an event-first style.
Goal: keep identity reusable across multiple apps while preserving a clean extraction path.

Machine-readable contract:
- [`openapi.yaml`](./openapi.yaml)

## 1. API Style

- Public base path for the registration slice: `/v1`
- JSON for request/response payloads
- UTC ISO-8601 timestamps
- Most write operations are asynchronous endpoints:
  - `POST /v1/register-user` -> `202 Accepted`
  - returns `command_id`
  - processing is asynchronous and emits events
- Reads are query endpoints over read models
- Auth/session/token endpoints stay synchronous for UX

## 2. Asynchronous Request Lifecycle

1. Client posts a request with optional `Idempotency-Key` and `X-Correlation-Id`.
2. API validates shape and authorization, then accepts (`202`).
3. Internal command processor handles business logic.
4. Domain events are emitted (CloudEvents-style envelope).
5. Read models are updated.
6. Client polls `GET /v1/commands/{commandId}` or consumes events.

## 3. Endpoint Groups

### 3.1 Auth (Synchronous)

- `POST /identity/v1/auth/login`
- `POST /identity/v1/auth/refresh`
- `POST /identity/v1/auth/logout`
- `POST /identity/v1/token/exchange`

### 3.2 Asynchronous Writes

- `POST /v1/register-user`
- `POST /identity/v1/commands/create-organization`
- `POST /identity/v1/commands/invite-organization-member`
- `POST /identity/v1/commands/assign-organization-role`
- `POST /identity/v1/commands/create-audience-grant`
- `POST /identity/v1/commands/revoke-audience-grant`
- `POST /identity/v1/commands/create-impersonation-grant`
- `POST /identity/v1/commands/deactivate-user`

### 3.3 Query (Read Models)

- `GET /identity/v1/me`
- `GET /v1/commands/{commandId}`
- `GET /v1/users/{userId}`
- `GET /identity/v1/organizations/{orgId}/members/{userId}`
- `GET /identity/v1/audiences/{audience}/grants`

### 3.4 Events

- `GET /identity/v1/events`
- `GET /identity/v1/events/{eventId}`
- `GET /identity/v1/events/stream` (SSE)

## 4. Context Rules

- `context_type=org` requires `org_id`
- `context_type=platform` omits `org_id`

JWT context claims expected by resource services:
- `sub`, `aud`, `context_type`, `org_id` (org context only), `scope`, `roles`, `exp`, `jti`

## 5. Event Envelope

Events use a CloudEvents-like envelope:
- `id`, `specversion`, `source`, `type`, `time`, `data`
- optional tracing/context fields:
  - `correlation_id`, `causation_id`, `actor`, `tenant`

Example:
```json
{
  "id": "evt_01J9Y8Z2A",
  "specversion": "1.0",
  "source": "platform-demo.identity",
  "type": "identity.user.registered",
  "time": "2026-03-26T21:00:00Z",
  "correlation_id": "corr_123",
  "data": {
    "user_id": "usr_01J...",
    "email": "user@example.com"
  }
}
```

## 6. Request Examples

Register user request:
```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd!",
  "display_name": "Jane User"
}
```

Create audience grant request:
```json
{
  "subject_type": "USER",
  "subject_id": "usr_01J...",
  "audience": "platform-api",
  "context_type": "org",
  "org_id": "org_01J...",
  "role": "AUD_ADMIN",
  "scopes": ["classes:read", "classes:write"]
}
```

Accepted response:
```json
{
  "command_id": "cmd_01J9Y8TQK",
  "command_type": "create_audience_grant",
  "status": "RECEIVED",
  "accepted_at": "2026-03-26T21:00:00Z",
  "correlation_id": "corr_123"
}
```

## 7. Contract-First Delivery Flow

1. Finalize OpenAPI and examples.
2. Generate server/client stubs from contract.
3. Implement handlers and command processors.
4. Add contract tests for command status and event shapes.
5. Add integration tests for auth + context isolation.

## 8. Out-of-Scope

- Domain entities (classes, leads, invoices, appointments).
- Domain workflow transitions.
- Domain-specific reporting.

These remain in app services and consume identity context plus policy decisions.
