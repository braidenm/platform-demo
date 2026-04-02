# Identity API-First Design

This document defines the API contract before implementation for the identity slice.
Goal: keep identity reusable across multiple apps while preserving a clean extraction path.

Machine-readable contract:
- [`openapi.yaml`](./openapi.yaml)

## 1. API Style

- Public base path for the registration/auth slice: `/v1`
- JSON for request/response payloads
- UTC ISO-8601 timestamps
- `register-user` is currently a synchronous create endpoint
- registration supports `Idempotency-Key`
- auth/session/token endpoints are synchronous for UX
- internal event emission is still allowed even when the external API is synchronous

## 2. Registration Lifecycle

1. Client posts a request with optional `Idempotency-Key`.
2. API validates the request and creates the user synchronously.
3. API returns `201 Created` with the created user identifier.
4. A repeated request with the same idempotency key returns the same created user response.
5. The user can be fetched from `GET /v1/users/{userId}`.
6. Internal events may still be emitted for downstream processing.

## 3. Auth Session Policy (planned next slice)

### Access token
- TTL: **15 minutes**
- Returned as `expires_in=900`

### Refresh token
- TTL: **30 days**
- Returned as `refresh_expires_in=2592000`
- Rotated on every successful refresh
- Previous refresh token becomes invalid after rotation
- Logout revokes the active refresh token/session
- Expired, revoked, or reused refresh tokens must be rejected

### Client communication
Clients should be told explicitly through API responses and docs that:
- access tokens are short-lived bearer credentials
- refresh tokens are long-lived session credentials
- refresh tokens must be replaced when `/v1/refresh` returns a new one
- clients must treat refresh token reuse failures as a signal to re-authenticate

## 4. Endpoint Groups

### 4.1 Current Implemented Endpoints
- `POST /v1/register-user`
- `GET /v1/users/{userId}`

### 4.2 Planned Next Auth Endpoints
- `POST /v1/login`
- `POST /v1/refresh`
- `POST /v1/logout`
- `GET /v1/me`

### 4.3 Deferred Future Endpoints
- machine-to-machine token flows
- delegated auth flows
- organizations and membership APIs
- authorization decision endpoints
- event stream endpoints when they add product value

## 5. Eventing Direction

The external API no longer exposes a command-status API for registration.
That does not prevent the service from emitting internal domain events or later publishing Kafka events when the platform is ready for that.

## 6. Request / Response Examples

Register user request:
```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd!",
  "display_name": "Jane User"
}
```

Register user response:
```json
{
  "user_id": "usr_01J9Y8TQK",
  "email": "user@example.com",
  "status": "ACTIVE",
  "created_at": "2026-03-26T21:00:00Z"
}
```

Planned login/refresh response:
```json
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_in": 900,
  "refresh_token": "...",
  "refresh_expires_in": 2592000
}
```

## 7. Contract-First Delivery Flow

1. Finalize OpenAPI and examples.
2. Implement handlers and persistence boundaries.
3. Keep shared concerns in the `shared` Gradle module when they are truly cross-cutting.
4. Add contract/integration tests for the implemented APIs.
5. Expand into login/refresh/logout/me next.

## 8. Out-of-Scope

- Organizations and memberships
- Audience grants
- Impersonation
- Domain entities (classes, leads, invoices, appointments)
- Domain workflow transitions

These remain deferred until the core identity/auth slice is stable.
