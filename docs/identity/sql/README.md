# Identity SQL Seeds

- `bootstrap-seed.sql`: Idempotent bootstrap seed template for:
  - super-admin user
  - personal org
  - global `SYSTEM_ADMIN` audience grant
  - frontend public OAuth client (`client_id`)

Notes:
- This script assumes the audience-first schema described in `../plan.md`.
- If your current schema differs, adjust column names before running.
- Use psql `-v` variables to inject secure values (especially `admin_password_hash`).
