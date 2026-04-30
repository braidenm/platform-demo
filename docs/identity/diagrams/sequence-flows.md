# Identity Sequence Flows

## 1) Login + Access + Refresh + Session Revocation

```mermaid
sequenceDiagram
  participant U as User
  participant APP as App Frontend/API
  participant IDN as Identity Service (AuthN)
  participant SESS as Session Store
  participant API as Resource API (Spring Resource Server)

  U->>APP: Submit email/password
  APP->>IDN: POST /identity/v1/login
  IDN->>SESS: Create session + persist ACTIVE refresh token hash
  IDN-->>APP: access_token (short-lived JWT) + refresh_token
  APP-->>U: Session established

  U->>APP: Call protected endpoint
  APP->>API: Bearer access_token
  API->>API: Validate JWT (signature, iss, aud, exp)
  API->>API: @PreAuthorize(scope/claim checks)
  API-->>APP: 200 OK

  APP->>IDN: GET /identity/v1/session (Bearer access_token)
  IDN->>SESS: Resolve sid claim to current session state
  IDN-->>APP: session_id + status + expiry context

  U->>APP: Access token expired
  APP->>IDN: POST /identity/v1/refresh (refresh_token)
  IDN->>SESS: Verify hash ACTIVE + not expired/revoked
  IDN->>SESS: Mark old token ROTATED, issue new ACTIVE token
  IDN-->>APP: new access_token + new refresh_token

  APP->>IDN: POST /identity/v1/refresh (rotated token reused)
  IDN->>SESS: Detect reuse and revoke session/token family
  IDN-->>APP: 401 Unauthorized

  U->>APP: Logout
  APP->>IDN: POST /identity/v1/logout (latest refresh token)
  IDN->>SESS: Revoke session + token family
  IDN-->>APP: 204 No Content
```

## 2) Login + App-Scoped Token

```mermaid
sequenceDiagram
  participant U as User
  participant APP as App Frontend/API
  participant ID as Identity Service (AuthN)
  participant DB as Identity Store

  U->>APP: Submit credentials + initial context (audience, context_type, org_id?, env)
  APP->>ID: POST /identity/v1/auth/login
  ID->>DB: Validate user + password hash
  DB-->>ID: User + memberships + grants
  ID-->>APP: access_token(aud, context_type, org_id?, env) + refresh_token
  APP-->>U: Session established
```

## 3) Token Exchange for Org/App Context Switch

```mermaid
sequenceDiagram
  participant U as User
  participant APP as App Service
  participant ID as Identity Service (AuthN)
  participant POL as Authorization Policy (AuthZ/FGA)

  U->>APP: Switch context (audience, context_type, org_id?, env)
  APP->>ID: POST /identity/v1/token/exchange
  Note over ID,POL: Monolith now = in-process AuthorizationPort check<br/>Later = HTTP adapter to /identity/v1/authorize/check
  ID->>POL: AuthorizationPort.check(subject, action, resource, aud, org_id)
  POL-->>ID: allow/deny (+ decision_id)
  alt allow
    ID-->>APP: exchanged access token (short TTL)
    APP-->>U: New active context
  else deny
    ID-->>APP: 403 Forbidden
    APP-->>U: Context switch denied
  end
```

## 4) API Authorization Check (Scope + Domain Rule)

```mermaid
sequenceDiagram
  participant U as User
  participant APP as Resource API
  participant POL as Authorization Policy (AuthZ/FGA)
  participant DOM as Domain Service

  U->>APP: GET /api/v1/organizations/{orgId}/resource/{id} with Bearer token
  APP->>APP: Validate JWT (iss, aud, exp, signature)
  APP->>APP: PreAuthorize(scope/role/org checks)
  APP->>POL: AuthorizationPort.check(subject, action, resource, aud, org_id)
  POL-->>APP: allow/deny (+ decision_id)
  alt allow
    APP->>DOM: Domain-level resource rule check
    DOM-->>APP: allow
    APP-->>U: 200 OK + data
  else deny
    APP-->>U: 403 Forbidden
  end
```

## 5) Controlled Impersonation (Later Phase)

```mermaid
sequenceDiagram
  participant SA as System Admin
  participant APP as Support Console
  participant ID as Identity Service (AuthN)
  participant POL as Authorization Policy (AuthZ/FGA)
  participant AUD as Audit Store

  SA->>APP: Request "view as user" with reason
  APP->>ID: POST /identity/v1/token/exchange (as_user_id, reason)
  ID->>POL: Validate impersonation grant + scope + TTL policy
  POL-->>ID: allow/deny
  alt allow
    ID->>AUD: Write impersonation event
    ID-->>APP: impersonation token (act=admin, sub=target, short TTL)
    APP-->>SA: Scoped support session
  else deny
    ID-->>APP: 403 Forbidden
    APP-->>SA: Impersonation denied
  end
```
