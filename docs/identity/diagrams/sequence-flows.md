# Identity Sequence Flows

## 1) Login + App-Scoped Token

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

## 2) Token Exchange for Org/App Context Switch

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

## 3) API Authorization Check (Scope + Domain Rule)

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

## 4) Controlled Impersonation (Later Phase)

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
