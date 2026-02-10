# Architectural Plan: Modular Monolith Platform

## 1. Executive Summary & Architect's Feedback

This project aims to build a robust, scalable, and flexible modular monolith using Spring Boot and Kotlin. The focus is on a high-maturity architectural stack: DDD, CQRS, and Event-Driven Design.

### Quality Feedback & Analysis
*   **Modular Monolith Choice**: This is the correct starting point. It avoids the "distributed monolith" trap while allowing for clear boundaries. We use a **multi-module Gradle setup** to enforce strict boundaries while keeping a **single Spring Boot application** as the entry point (`app` module). This provides the best of both worlds: modularity and simplicity of deployment.
*   **Multi-tenancy & Identity**: The requirement for "Account user with multiple organizations" and "Help desk roles" suggests a relationship-based access control model. **Fine-Grained Authorization (FGA)** (inspired by Google Zanzibar) is highly recommended over traditional RBAC, as it handles complex relationships (e.g., `User X is admin of Org Y`, `User Z is help-desk for Org Y's data`) much more naturally.
*   **Event-Driven & CQRS**: You mentioned Axon. While powerful for Event Sourcing (ES), it can be opinionated. If you want to keep it "free" and flexible, we should decide if we need **Full Event Sourcing** (storing events as the source of truth) or just **Event-Driven CQRS** (using events to sync read models).
*   **Sandbox Mode**: To implement this effectively, we should treat "Sandbox" as a first-class execution context. This can be achieved via a `X-Sandbox` header or a flag in the Tenant context, which determines whether the logic interacts with "Live" or "Mock/Isolated" infrastructure.

---

## 2. Proposed Module Structure (Gradle-based)

To ensure strict boundaries and easy extraction to microservices later, we use a multi-module Gradle project structure:

*   `:identity`: IAM service (Users, Orgs, Permissions, Tokens).
*   `:catalog`: Product/Service catalog.
*   `:orders-billing`: Order management and invoicing.
*   `:provisioning`: Handling the lifecycle of services sold.
*   `:payments`: Integration with payment gateways (Mocked initially).
*   `:common`: Shared kernels, DTOs, and utility logic (Imported by all).
*   `:gateway`: Entry point logic (if needed beyond standard Spring Boot routing).
*   `:app`: The executable Spring Boot application that aggregates all modules.

---

## 3. Technology Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.1 |
| **JDK Version**| 21 (LTS) |
| **Framework** | Spring Boot 3.4+ |
| **Build Tool** | Gradle 8.12 (Multi-Module) |
| **Port** | 8080 (Unified) |
| **CQRS/ES** | Axon Framework (Internal events, Postgres-backed) |
| **Primary DB** | PostgreSQL (Single instance, Schema-per-module) |
| **Read DB** | MongoDB (For specific read models/searching) |
| **Authz** | Lightweight internal FGA |
| **Deployment** | Docker (Single image) |
| **Monitoring** | Spring Boot Actuator (`/actuator`) |
| **Testing**    | H2 and Mockito (Smoke Tests) |

---

## 4. Key Architectural Patterns

### 4.1. Identity & Multi-tenancy
*   **User vs. Account**: A `User` represents a person (credentials). An `Account` or `Membership` links a `User` to an `Organization`.
*   **Internal FGA**: We will implement a `Relation` based permission system. 
    *   Example: `(Object: Org:123, Relation: Member, Subject: User:abc)`
    *   This allows the "Help Desk" use case by adding a `Relation: Support` between a system admin and a customer organization.
*   **Token Exchange**: The Identity service will support issuing tokens for different audiences/apps.

### 4.2. Event-Driven CQRS
*   **Internal**: Axon Framework will manage the Command Bus and Event Store (persisted in Postgres).
*   **Read Models**: Projectors will update PostgreSQL or MongoDB depending on the module's needs.

### 4.3. Sandbox Mode
*   Context-aware execution. If `X-Sandbox: true` is present, the persistence layer (or schema resolver) can redirect to a `sandbox` schema or mock external integrations.

### 4.4. Database Schema Strategy

We currently use a **Schema-per-Module** strategy in PostgreSQL. 

*   **Pros**: 
    *   Strict logical separation. 
    *   Easier extraction to microservices (each module has its own tablespace).
    *   Avoids naming collisions.
*   **Cons**:
    *   Slightly more complex setup (requires `currentSchema` or `search_path`).
    *   Cross-module joins are more explicit.

**Alternative: Single Schema**
If you prefer a simpler model, you can change `DB_SCHEMAS` to just `public` or `platform`. All modules will then share the same namespace. This is easier for small teams but requires more discipline in naming tables (e.g., prefixing tables with the module name like `identity_users`).

---

## 5. Implementation Roadmap

1.  **Consolidation**: Moving from multi-module Gradle to a single Spring Boot application.
2.  **Infrastructure**: Local `docker-compose.yml` for Postgres and Mongo (for dev/testing).
3.  **Schema Management**: Tables will be managed via JPA/Hibernate for now (until Flyway is integrated).
4.  **Identity Module**:
    *   User registration and authentication (JWT).
    *   Organization management.
    *   Lightweight FGA implementation.

---

## 6. Infrastructure & CI/CD Strategy

### 6.1. Deployment Architecture
The infrastructure is managed in a separate **Infrastructure Repository**. This project focuses on building the application containers and triggering the infrastructure deployment.

*   **Shared Network**: Uses `shared-net` for communication.
*   **Databases**: PostgreSQL and MongoDB are managed externally by the infra repo.
*   **Secrets**: All secrets (DB credentials, tokens) are injected as environment variables by the host system/Docker Compose in the infra repo.

### 6.2. CI/CD Workflow (GitHub Actions)
The `.github/workflows/docker-publish.yml` handles the following:
1.  **Build**: Compiles the project using JDK 21 and Gradle.
2.  **Containerize**: Builds a single Docker image for the platform.
3.  **Publish**: Pushes image to **GitHub Container Registry (GHCR)**.
4.  **Trigger**: Sends a `repository_dispatch` event to the Infrastructure Repository.

### 6.3. Infrastructure Repo Integration (How-To)
To connect this project to your infrastructure repo, follow these steps.

#### A. The "Dual-Trigger" Deployment Model
Your infrastructure setup uses two triggers for deployment:
1.  **Infra Repo Trigger (Manual/Git Push)**: When you change the infrastructure configuration (e.g., `compose.yml`, secrets, network), your `deploy` script ensures everything is in sync.
2.  **Platform Repo Trigger (Automated)**: When you push new code to the `platform` repo, it builds a new image and notifies the infra repo to pull and restart just that specific service.

#### B. Preparation Checklist (Do this first)
Before triggering the first automated deployment, ensure the following are ready on your server and in GitHub:

1.  **Shared Network**: Ensure `shared-net` exists on your server:
    ```bash
    docker network inspect shared-net >/dev/null 2>&1 || docker network create shared-net
    ```
2.  **Log Directories**: Your `deploy` script handles this, but for the first automated run, ensure `/data/logs/platform` exists or update the script first.
3.  **GitHub Secret (Platform Repo)**: Add `INFRA_REPO_TOKEN` (a PAT with `repo` scope) to your **platform** repository secrets.
4.  **GHCR Access**: 
    *   If the package is **Public**: No extra setup needed.
    *   If the package is **Private**: You must ensure your self-hosted runner is logged into GHCR (`docker login ghcr.io`) or your deployment action includes a login step using a PAT with `read:packages` scope.

#### C. Infra Repo Directory Structure
Create a new stack directory in your infrastructure repo:
```bash
mkdir -p stacks/event-platform
```

#### D. Docker Compose (`stacks/event-platform/compose.yml`)
Create the compose file. 
**Ports Note**: Since you are using Caddy on the `shared-net`, you **do not need to expose ports** to the host. Caddy will route traffic to `http://platform:8080` and `http://platform-frontend:80`.

```yaml
services:
  platform:
    image: ${IMAGE:-ghcr.io/YOUR_GITHUB_USERNAME/platform}:${TAG:-latest}
    container_name: platform
    restart: unless-stopped
    environment:
      - DB_HOST=postgres
      - POSTGRES_DB=${POSTGRES_DB:-platform}
      - POSTGRES_USER=${POSTGRES_USER}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
      - MONGO_HOST=mongodb
      - MONGO_ROOT_USER=${MONGO_ROOT_USER}
      - MONGO_ROOT_PASSWORD=${MONGO_ROOT_PASSWORD}
    networks:
      - shared-net

  platform-frontend:
    image: ${FRONTEND_IMAGE:-ghcr.io/YOUR_GITHUB_USERNAME/platform-frontend}:${FRONTEND_TAG:-latest}
    container_name: platform-frontend
    restart: unless-stopped
    networks:
      - shared-net

networks:
  shared-net:
    external: true
```

#### E. Update `deploy` script in Infra Repo
Yes, you **must** update the `deploy` script. This ensures that when you run a full system update or change infra-level configs, the platform service is included in the idempotency checks and lifecycle management.

*   **Add log directory**:
    ```bash
    for dir in /data/logs/caddy /data/logs/postgres /data/logs/mongodb /data/logs/platform /data/logs/platform-frontend; do
        ensure_dir "$dir"
    done
    ```
*   **Add deployment call**:
    ```bash
    echo "==> Deploy Platform"
    deploy_stack "event-platform" "$ROOT/stacks/event-platform"
    ```

#### F. Infra Repo Workflow (`.github/workflows/deploy-service.yml`)
This action listens for dispatch events to update either the backend or the frontend.
    
```yaml
on:
  repository_dispatch:
    types: [platform_updated, frontend_updated]

jobs:
  deploy:
    runs-on: [self-hosted, Linux, X64, homelab]
    steps:
      - uses: actions/checkout@v4

      - name: Deploy Backend
        if: github.event.action == 'platform_updated'
        env:
          IMAGE: ${{ github.event.client_payload.image }}
          TAG: ${{ github.event.client_payload.tag }}
        run: |
          cd stacks/event-platform
          IMAGE="$IMAGE" TAG="$TAG" docker compose pull platform
          IMAGE="$IMAGE" TAG="$TAG" docker compose up -d platform

      - name: Deploy Frontend
        if: github.event.action == 'frontend_updated'
        env:
          IMAGE: ${{ github.event.client_payload.image }}
          TAG: ${{ github.event.client_payload.tag }}
        run: |
          cd stacks/event-platform
          FRONTEND_IMAGE="$IMAGE" FRONTEND_TAG="$TAG" docker compose pull platform-frontend
          FRONTEND_IMAGE="$IMAGE" FRONTEND_TAG="$TAG" docker compose up -d platform-frontend
```

#### G. Caddy & Subdomain Configuration (`Caddyfile`)

To host multiple sites and subdomains (like your Personal Site and `platform-demo.braidenmiller.com`) on the same Caddy server, update your `Caddyfile` like this:

```caddy
# Global Options
{
  log {
    output file /var/log/caddy/access.log {
      roll_keep_for 168h
    }
  }
}

# 1. Platform Demo Project
platform-demo.braidenmiller.com {
    # Backend API Proxy
    # Requests starting with /api/ are forwarded to the platform container.
    # The full path (including /api/...) is sent to Spring Boot.
    handle /api/* {
        reverse_proxy platform:8080
    }

    # Frontend Project
    # Routes to the platform-frontend container.
    handle {
        reverse_proxy platform-frontend:80
    }
}

# 2. Main Personal Site
braidenmiller.com, www.braidenmiller.com {
    root * /srv/site/PersonalSite
    file_server
    try_files {path} /index.html
}
```

#### H. Path Routing Explained (The "/api" logic)

You asked if you need to configure anything in the API gateway or if paths just get "attached". 

1.  **Caddy Behavior**: The `handle /api/*` block tells Caddy to catch any request starting with `/api/`. When using `reverse_proxy`, Caddy **does not strip** the `/api/` prefix by default. It sends the exact URI it received to the backend.
2.  **Spring Boot Logic**: Since we consolidated the platform into a single application, each module's controllers already include the `/api/` prefix in their `@RequestMapping` (e.g., `@RequestMapping("/api/identity")`).
3.  **Result**: 
    *   Request: `GET platform-demo.braidenmiller.com/api/identity/users/register`
    *   Caddy sends to platform: `GET /api/identity/users/register`
    *   Spring Boot matches: `TestController` (`/api/identity`) -> `register` (`/users/register`)
    *   **No extra gateway configuration is needed.** The controllers handle their own routing prefixes.

#### I. Cloudflare Tunnel & DNS Setup

To make `platform-demo.braidenmiller.com` accessible:

1.  **DNS/Tunnel**: Point the subdomain `platform-demo` in Cloudflare to your server's tunnel.
2.  **Tunnel Ingress**: In your `cloudflared` configuration (in the infra repo), ensure it routes the hostname to your Caddy container:
    ```yaml
    ingress:
      - hostname: platform-demo.braidenmiller.com
        service: http://caddy:80 # Or wherever Caddy is listening
      - hostname: braidenmiller.com
        service: http://caddy:80
      - service: http_status:404
    ```
3.  **Caddy handles the rest**: Caddy will differentiate between `platform-demo.braidenmiller.com` and your main site using the host headers, applying the correct `handle` blocks.

### 6.4. Testing
*   **Smoke Testing**: The system uses a simplified smoke testing strategy.
    *   **Context Verification**: We use `@SpringBootTest` to ensure all modules are correctly wired and the application context starts successfully.
    *   **Lightweight Infrastructure**: External dependencies are swapped for H2 (database) and Mockito mocks (MongoDB) during tests.
    *   **No Docker Required**: Tests run entirely in-memory or with mocks, eliminating the need for a Docker daemon during the build process.

#### J. CORS & Same-Origin
Because both the frontend and the backend APIs are served under the same domain (`platform-demo.braidenmiller.com`) via Caddy, they are considered **Same-Origin**. This means you generally won't encounter CORS issues. 

However, if you ever move the frontend to a different subdomain or domain, you will need to configure CORS in `SecurityConfig.kt`.

---

### 6.5. GitHub Container Registry (GHCR) Guide

This project is configured to use GHCR for hosting Docker images.

#### A. Automated Build & Push (Recommended)
The GitHub Actions workflow (`.github/workflows/docker-publish.yml`) handles this automatically:
1.  **On Every Push to `main`**: A new image is built, tagged with `:latest` and the commit SHA, and pushed to GHCR. It then triggers the infra repo, passing the full image name and the specific commit SHA as the tag.
2.  **On Pull Requests**: The project is built to ensure it compiles, but no image is pushed.

**Requirements**:
*   Ensure that GitHub Actions have "Read and write permissions" in **Settings > Actions > General > Workflow permissions**.

#### B. Manual Build & Push
If you need to push an image manually from your local machine:

1.  **Generate a Personal Access Token (PAT)**:
    *   Go to **GitHub Settings > Developer settings > Personal access tokens > Tokens (classic)**.
    *   Generate a new token with at least the `write:packages` scope.
2.  **Authenticate**:
    ```bash
    export CR_PAT=YOUR_TOKEN
    echo $CR_PAT | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
    ```
3.  **Build and Tag**:
    ```bash
    docker build -t ghcr.io/YOUR_GITHUB_USERNAME/platform:latest .
    ```
4.  **Push**:
    ```bash
    docker push ghcr.io/YOUR_GITHUB_USERNAME/platform:latest
    ```

#### C. Image Visibility & Permissions
By default, images pushed to GHCR are often **private**. 
1.  **To make it Public**: Go to your GitHub Profile > Packages > platform > Package Settings and change visibility to Public.
2.  **To keep it Private**: You must ensure your homelab/infrastructure repository has a `DOCKER_CONFIG` or uses a `docker login` with a PAT that has `read:packages` scope to pull the image.

*Note: Replace `YOUR_GITHUB_USERNAME` with your actual GitHub username (in lowercase).*
