# Identity Event Modeling

This diagram shows the event-driven flow for the identity service from command to event to projection/read model.

## Event Flow

```mermaid
flowchart LR
  subgraph Actors
    U[User]
    A[App Service]
    SA[System Admin]
  end

  subgraph Commands
    C1[RegisterUser]
    C2[CreateOrganization]
    C3[GrantOrgRole]
    C4[GrantAppAccess]
    C5[LoginWithPassword]
    C6[RefreshSession]
    C7[ExchangeToken]
    C8[DeactivateUser]
    C9[GrantImpersonation]
  end

  subgraph DomainEvents
    E1[UserRegistered]
    E2[OrganizationCreated]
    E3[OrgRoleGranted]
    E4[AudienceGrantAssigned]
    E5[UserAuthenticated]
    E6[RefreshTokenRotated]
    E7[AccessTokenIssued]
    E8[UserDeactivated]
    E9[ImpersonationGranted]
    E10[AuthorizationEvaluated]
  end

  subgraph Projections_ReadModels
    R1[User Profile Read Model]
    R2[Membership Read Model]
    R3[Audience Grants Read Model]
    R4[Session and Token Read Model]
    R5[Audit Timeline]
    R6[Policy Decision Cache]
  end

  U --> C1
  U --> C5
  U --> C7
  A --> C7
  SA --> C3
  SA --> C4
  SA --> C8
  SA --> C9
  U --> C2
  U --> C6

  C1 --> E1 --> R1
  C2 --> E2 --> R2
  C3 --> E3 --> R2
  C4 --> E4 --> R3
  C5 --> E5 --> R4
  C6 --> E6 --> R4
  C7 --> E7 --> R4
  C8 --> E8 --> R1
  C9 --> E9 --> R5

  E1 --> R5
  E2 --> R5
  E3 --> R5
  E4 --> R5
  E5 --> R5
  E6 --> R5
  E7 --> R5
  E8 --> R5
  E10 --> R6
```

## Aggregate and Event Ownership

- `UserAggregate`
  - `UserRegistered`, `UserAuthenticated`, `UserDeactivated`
- `OrganizationAggregate`
  - `OrganizationCreated`, `OrgRoleGranted`
- `AccessAggregate`
  - `AudienceGrantAssigned`, `AccessTokenIssued`, `RefreshTokenRotated`
- `PolicyAggregate` (or Policy service boundary)
  - `AuthorizationEvaluated`, `ImpersonationGranted`

## Notes

- AuthN-owned flows: `RegisterUser`, `LoginWithPassword`, `RefreshSession`, `ExchangeToken`.
- AuthZ/FGA-owned flows: `GrantOrgRole`, `GrantAppAccess`, `GrantImpersonation`, authorization decisions.
- In monolith mode these boundaries run in one process; after extraction, AuthZ can move behind policy APIs without changing domain call contracts.
- Keep write model append-only and project into read models for app queries.
- Audit events should be emitted for every command that changes auth context.
- FGA/ReBAC checks should produce decision events for traceability.
