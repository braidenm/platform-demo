\set ON_ERROR_STOP on

-- Identity bootstrap seed (template)
-- Usage example:
-- psql "$DATABASE_URL" \
--   -v admin_email='you@example.com' \
--   -v admin_password_hash='$2a$10$replace_with_real_hash' \
--   -v admin_display_name='Braiden Miller' \
--   -v personal_org_slug='braiden-personal' \
--   -v personal_org_name='Braiden Personal' \
--   -v audience_key='platform-api' \
--   -v audience_name='Platform API' \
--   -v frontend_client_id='platform-web' \
--   -v frontend_redirect_uri='http://localhost:3000/callback' \
--   -f docs/identity/sql/bootstrap-seed.sql

-- Defaults (used when -v values are not provided)
\if :{?admin_email}
\else
\set admin_email 'admin@example.com'
\endif

\if :{?admin_password_hash}
\else
\set admin_password_hash '$2a$10$replace_with_real_hash'
\endif

\if :{?admin_display_name}
\else
\set admin_display_name 'Platform Super Admin'
\endif

\if :{?personal_org_slug}
\else
\set personal_org_slug 'personal-org'
\endif

\if :{?personal_org_name}
\else
\set personal_org_name 'Personal Org'
\endif

\if :{?audience_key}
\else
\set audience_key 'platform-api'
\endif

\if :{?audience_name}
\else
\set audience_name 'Platform API'
\endif

\if :{?frontend_client_id}
\else
\set frontend_client_id 'platform-web'
\endif

\if :{?frontend_redirect_uri}
\else
\set frontend_redirect_uri 'http://localhost:3000/callback'
\endif

BEGIN;

-- Fixed bootstrap IDs for idempotent seeding
-- You can replace these with your own UUID strategy.
WITH const AS (
  SELECT
    '00000000-0000-0000-0000-000000000001'::uuid AS super_admin_user_id,
    '00000000-0000-0000-0000-000000000002'::uuid AS personal_org_id,
    '00000000-0000-0000-0000-000000000003'::uuid AS personal_org_membership_id,
    '00000000-0000-0000-0000-000000000004'::uuid AS audience_id,
    '00000000-0000-0000-0000-000000000005'::uuid AS system_admin_grant_id,
    '00000000-0000-0000-0000-000000000006'::uuid AS personal_aud_grant_id,
    '00000000-0000-0000-0000-000000000007'::uuid AS frontend_client_row_id
)
INSERT INTO users (id, email, password_hash, status, email_verified, created_at, updated_at)
SELECT
  c.super_admin_user_id,
  :'admin_email',
  :'admin_password_hash',
  'ACTIVE',
  true,
  now(),
  now()
FROM const c
ON CONFLICT (email) DO UPDATE
SET
  password_hash = EXCLUDED.password_hash,
  status = EXCLUDED.status,
  email_verified = EXCLUDED.email_verified,
  updated_at = now();

WITH const AS (
  SELECT
    '00000000-0000-0000-0000-000000000001'::uuid AS super_admin_user_id,
    '00000000-0000-0000-0000-000000000002'::uuid AS personal_org_id
)
INSERT INTO organizations (id, slug, name, status, created_by)
SELECT
  c.personal_org_id,
  :'personal_org_slug',
  :'personal_org_name',
  'ACTIVE',
  c.super_admin_user_id
FROM const c
ON CONFLICT (slug) DO UPDATE
SET
  name = EXCLUDED.name,
  status = EXCLUDED.status;

WITH const AS (
  SELECT
    '00000000-0000-0000-0000-000000000001'::uuid AS super_admin_user_id,
    '00000000-0000-0000-0000-000000000002'::uuid AS personal_org_id,
    '00000000-0000-0000-0000-000000000003'::uuid AS personal_org_membership_id
)
INSERT INTO org_memberships (id, user_id, organization_id, role, status)
SELECT
  c.personal_org_membership_id,
  c.super_admin_user_id,
  c.personal_org_id,
  'ORG_OWNER',
  'ACTIVE'
FROM const c
ON CONFLICT (user_id, organization_id) DO UPDATE
SET
  role = EXCLUDED.role,
  status = EXCLUDED.status;

WITH const AS (
  SELECT
    '00000000-0000-0000-0000-000000000004'::uuid AS audience_id
)
INSERT INTO audiences (id, key, name, status)
SELECT
  c.audience_id,
  :'audience_key',
  :'audience_name',
  'ACTIVE'
FROM const c
ON CONFLICT (key) DO UPDATE
SET
  name = EXCLUDED.name,
  status = EXCLUDED.status;

-- Global SYSTEM_ADMIN grant (no org context required)
WITH const AS (
  SELECT
    '00000000-0000-0000-0000-000000000001'::uuid AS super_admin_user_id,
    '00000000-0000-0000-0000-000000000005'::uuid AS system_admin_grant_id
)
INSERT INTO audience_grants (id, subject_type, subject_id, audience, org_id, role, scopes, status)
SELECT
  c.system_admin_grant_id,
  'USER',
  c.super_admin_user_id,
  :'audience_key',
  NULL,
  'SYSTEM_ADMIN',
  ARRAY['*:*']::text[],
  'ACTIVE'
FROM const c
ON CONFLICT (subject_type, subject_id, audience, org_id, role) DO NOTHING;

-- Optional day-to-day audience grant in personal org context
WITH const AS (
  SELECT
    '00000000-0000-0000-0000-000000000001'::uuid AS super_admin_user_id,
    '00000000-0000-0000-0000-000000000002'::uuid AS personal_org_id,
    '00000000-0000-0000-0000-000000000006'::uuid AS personal_aud_grant_id
)
INSERT INTO audience_grants (id, subject_type, subject_id, audience, org_id, role, scopes, status)
SELECT
  c.personal_aud_grant_id,
  'USER',
  c.super_admin_user_id,
  :'audience_key',
  c.personal_org_id,
  'AUD_ADMIN',
  ARRAY['*:*']::text[],
  'ACTIVE'
FROM const c
ON CONFLICT (subject_type, subject_id, audience, org_id, role) DO NOTHING;

-- Frontend public client (PKCE; no client secret)
WITH const AS (
  SELECT
    '00000000-0000-0000-0000-000000000007'::uuid AS frontend_client_row_id
)
INSERT INTO oauth_clients (
  id,
  client_id,
  client_secret_hash,
  client_type,
  allowed_audiences,
  allowed_scopes,
  redirect_uris,
  status
)
SELECT
  c.frontend_client_row_id,
  :'frontend_client_id',
  NULL,
  'PUBLIC',
  ARRAY[:'audience_key']::text[],
  ARRAY['openid', 'profile', 'email']::text[],
  ARRAY[:'frontend_redirect_uri']::text[],
  'ACTIVE'
FROM const c
ON CONFLICT (client_id) DO UPDATE
SET
  allowed_audiences = EXCLUDED.allowed_audiences,
  allowed_scopes = EXCLUDED.allowed_scopes,
  redirect_uris = EXCLUDED.redirect_uris,
  status = EXCLUDED.status;

COMMIT;
