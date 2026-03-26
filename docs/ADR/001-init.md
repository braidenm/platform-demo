# ADR 001: Identity Service Foundation

- Status: Accepted
- Date: 2026-03-26
- Deciders: Platform Demo maintainers

## Summary

Adopt a single identity foundation in the modular monolith with clear internal boundaries between AuthN and AuthZ/FGA.  
Use an audience-first model (`aud`) for API boundaries, keep tenant context with `org_id`, and evolve from RBAC to FGA/ReBAC through stable policy contracts.

## Problem Statement

The platform needs one reusable identity foundation that supports multiple product modules, multi-org users, and future microservice extraction.  
Without a clear baseline, auth logic can fragment across apps, make authorization inconsistent, and create migration risk when modules are split out later.

## Decision Drivers

- central login and authorization across multiple product audiences
- multi-organization access with clear tenant isolation
- support for global platform administration (`SYSTEM_ADMIN`)
- compatibility with future standalone Role/FGA policy service extraction
- preference for open standards and free/open-source components

## Options Considered

1. Single identity foundation in modular monolith, with explicit AuthN/AuthZ boundaries.
2. Separate auth service per app from day one.
3. Minimal RBAC-only model with no FGA extraction path.

## Decision

Choose option 1.

Key points:
- Keep AuthN and AuthZ in one deployable module now, but separated by interfaces/packages.
- Treat AuthZ as an internal Role/FGA policy boundary now, extractable later.
- Use `aud` for protected API boundaries, `org_id` for tenant context, and `scope` for action permissions.
- Keep identity/auth in the same repository while requirements are still evolving.
- Keep centralized policy checks behind stable contracts:
  - internal `PermissionService`
  - `/identity/v1/authorize/check`
  - `/identity/v1/authorize/batch-check`
- Model `SYSTEM_ADMIN` as a global grant; keep personal org membership for day-to-day tenant work.
- In modular monolith mode, domain modules call identity via in-process interfaces/beans, not internal HTTP.

### Repository Boundary Detail

- Start with one repository while identity and policy contracts are still changing.
- Keep `identity` contracts stable and consumed by other modules as interfaces.
- Do not create internal network/API hops between Gradle modules in the same runtime.
- Extract to a separate repo/service only after:
  - policy/auth contracts are stable
  - integration tests cover contract behavior
  - identity needs independent deploy cadence or ownership
- After extraction, modules keep the same contract and switch adapter implementation (in-process -> HTTP client).

## Examples

- User logs in once, then exchanges tokens by audience and org context:
  - `aud=platform-api`, `org_id=org_a`
  - `aud=crm-api`, `org_id=org_b`
- Same user has different privileges across audiences/orgs:
  - `aud_admin` in one audience
  - `aud_member` in another
- Global operation:
  - `SYSTEM_ADMIN` token for platform-level admin endpoint
- Tenant operation:
  - org-scoped token for `/organizations/{orgId}/...` endpoint
- Monolith module-to-module authorization:
  - `orders` module calls injected `PermissionService.check(...)` directly
  - no network hop between Gradle modules inside the same runtime
- After extraction:
  - same check contract is implemented by an HTTP adapter calling `/identity/v1/authorize/check`

## Consequences

Positive:
- consistent auth model across all modules
- app-agnostic identity foundation for new product prototypes
- explicit migration path from monolith boundary to standalone Role/FGA service
- better auditability and policy traceability

Tradeoffs:
- more upfront design than basic RBAC-only setup
- requires strict enforcement of `aud` and `org_id` checks
- extraction still requires operational work (deploy, observability, reliability)
- module boundaries must be enforced to avoid accidental direct DB coupling

## Implementation Plan

1. Bootstrap: seed initial `SYSTEM_ADMIN`, signing keys, and frontend public client.
2. MVP: email/password auth, org memberships, audience-scoped token issuance.
3. Add audience grants and `client_credentials` support.
4. Add ReBAC/FGA relation checks and centralized authorization endpoints.
5. Add advanced operations: controlled impersonation, social/enterprise federation, hardening.

## References

- `docs/plan.md`
- `docs/identity/plan.md`
- `docs/identity/openapi.yaml`
