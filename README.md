# Platform: Enterprise Modular Monolith Demo

This project is a high-maturity **Modular Monolith** built with Kotlin and Spring Boot. It serves as a portfolio demonstration of enterprise-level architectural patterns—such as DDD, CQRS, and Event-Driven Design—simplified into a single, maintainable deployment unit.

The goal is to showcase how a system can be designed with strict modular boundaries, allowing for the developer experience of a monolith while maintaining a clear path toward microservices if the need arises.

---

## 🚀 Portfolio Highlights

- **Modular Monolith Architecture**: Uses a multi-module Gradle setup to enforce logical boundaries between domains (Identity, Catalog, Payments, etc.).
- **Event-Driven CQRS**: Implements Command Query Responsibility Segregation using the **Axon Framework** for internal state changes.
- **Relationship-Based Access Control (FGA)**: Features a flexible Identity service inspired by Google Zanzibar, supporting complex multi-tenancy where users can belong to multiple organizations with varying roles.
- **Domain-First Testing**: A robust testing strategy that focuses on high-level domain behaviors rather than fragile unit-level mocks.

---

## 🛠 Tech Stack

- **Language**: Kotlin 2.1+
- **Runtime**: JDK 21 (LTS)
- **Framework**: Spring Boot 3.4+
- **Command/Event Handling**: Axon Framework (PostgreSQL-backed)
- **Persistence**: 
    - **PostgreSQL**: Primary transactional store (Schema-per-module strategy).
    - **MongoDB**: Optimized read models and search capabilities.
- **Infrastructure**: Docker & GitHub Container Registry (GHCR).
- **Observability**: Spring Boot Actuator for health monitoring and metrics.
- **Testing**: H2 and Mockito for lightweight context smoke tests.

---

## 🏗 Coding Philosophy & Architecture

### Modular Monolith
Unlike a traditional "big ball of mud" monolith, this project is structured as a **Modular Monolith**. Each domain (e.g., `identity`, `catalog`) resides in its own Gradle module. This ensures that:
1. Modules cannot accidentally depend on each other's internals.
2. The system can be refactored into microservices by simply moving a module to its own repository.
3. The entire application runs in a single JVM, drastically reducing operational complexity and latency.

### Observability
Built-in **Spring Boot Actuator** endpoints are exposed (e.g., `/actuator/health`, `/actuator/info`, `/actuator/metrics`) to facilitate monitoring and operational health checks in a production or homelab environment.

### Domain-Driven Design (DDD) & CQRS
The project follows DDD principles, keeping business logic at the core of the aggregates. We use **CQRS** to separate the write model (commands that change state) from the read model (queries that fetch data), allowing each to scale and evolve independently.

### Testing Strategy: Context Smoke Tests
I prioritize **Context-Level Smoke Tests** to ensure the modular monolith is correctly wired.
- **The Approach**: We use `@SpringBootTest` to verify that the application context loads without errors. External dependencies like PostgreSQL and MongoDB are substituted with H2 or Mockito mocks during testing.
- **The Benefit**: This provides immediate feedback on configuration and wiring issues without requiring a full Docker environment or complex infrastructure setup.

---

## 🔑 Identity & Authorization (IAM)

The `identity` module is designed to be a standalone IAM provider for all my applications.
- **Multi-tenancy**: Supports "Accounts" that can own or belong to multiple "Organizations".
- **Internal FGA**: Implements a relationship-based authorization system. Instead of simple roles, it stores tuples like `(Object:Org, Relation:Admin, Subject:User)`. 
- **Extensibility**: This model easily handles "Help Desk" scenarios where a system admin can be granted temporary, scoped access to a tenant's data without global permissions.

---

## 🛠 How to Add New Services

1. **Create the Module**: Add a new directory (e.g., `shipping`) with a `build.gradle.kts` file.
2. **Register the Module**: Add `include("shipping")` to `settings.gradle.kts`.
3. **Link to App**: Add `implementation(project(":shipping"))` to `app/build.gradle.kts`.
4. **Define the Domain**: Implement your Aggregates, Commands, and Events within the new module.
5. **Database**: If needed, add a new schema to `app/src/test/resources/init-schemas.sql`.

---

## 🤖 CI/CD & GitHub Actions

### External Database Configuration

If you are deploying this application to a server with existing PostgreSQL and MongoDB instances, you can configure the connection using the following environment variables:

#### PostgreSQL
- `DB_HOST`: Hostname of your Postgres server (Default: `localhost`).
- `DB_PORT`: Port of your Postgres server (Default: `5433`).
- `POSTGRES_DB`: Database name (Default: `platformdemo`).
- `POSTGRES_USER`: Database username (Default: `platformdemo`).
- `POSTGRES_PASSWORD`: Database password.
- `DB_SCHEMAS`: Comma-separated list of schemas to initialize (Default: `identity,public`).
- `SPRING_JPA_HIBERNATE_DDL_AUTO`: Hibernate DDL mode (e.g., `none`, `validate`, `update`). Use `validate` or `none` for production.

#### MongoDB
- `SPRING_DATA_MONGODB_URI`: Full MongoDB connection string (e.g., `mongodb://user:pass@host:27017/db?authSource=admin`).
- *Alternatively, use individual variables:*
    - `MONGO_HOST`: Hostname (Default: `localhost`).
    - `MONGO_PORT`: Port (Default: `27017`).
    - `MONGO_DB`: Database name (Default: `platformdemo`).
    - `MONGO_ROOT_USER`: Username (Default: `platformdemo`).
    - `MONGO_ROOT_PASSWORD`: Password.
    - `MONGO_AUTH_SOURCE`: Authentication database (Default: `admin`).

The project uses a fully automated pipeline via GitHub Actions (`.github/workflows/docker-publish.yml`):
1. **Build & Test**: Runs the full Gradle build and Testcontainers-backed integration tests.
2. **Containerize**: Builds a multi-stage Docker image using JDK 21.
3. **Publish**: Pushes the image to **GitHub Container Registry (GHCR)**.
4. **Auto-Deploy**: Sends a `repository_dispatch` to my **Infrastructure Repository**, which triggers a self-hosted runner to pull and restart the service on my homelab server.

---

## 🏃 Getting Started

### Local Development

1.  **Prerequisites**: JDK 21 and Docker.
2.  **Run Everything**: The easiest way to start the entire platform (App + Databases) is using Docker Compose:
    ```bash
    docker compose up --build -d
    ```
    *Note: The `--build` flag ensures the application JAR is rebuilt inside the container.*

3.  **Manual Development (IDE)**: If you want to run the application from your IDE:
    - Start only the databases: `docker compose up postgres mongodb -d`
    - Run the app: `./gradlew :services:app:bootRun`
    - *Note: PostgreSQL is mapped to port **5433** locally to avoid conflicts with existing Postgres installations.*

4.  **Access**:
    - App: `http://localhost:8080/api/...`
    - Actuator: `http://localhost:8080/actuator/health`
    - Test Endpoints: `http://localhost:8080/api/test-deployment/postgres`

5.  **Smoke Tests**: Run `./gradlew test` to execute the context smoke tests. These tests use an in-memory H2 database and do not require Docker.

### Documentation
Detailed architectural plans, database schema strategies, and homelab deployment guides are available in [docs/plan.md](docs/plan.md).

---

## 🔮 Future Enhancements

- [ ] **Advanced FGA**: Transition from the internal JPA-based FGA to a dedicated **OpenFGA** instance.
- [ ] **Sandbox Mode**: Full implementation of the `X-Sandbox` header to allow users to test APIs without affecting live data.
- [ ] **Search Service**: A dedicated search module leveraging **ElasticSearch** or **OpenSearch**.
- [ ] **Frontend Demo**: A React/Next.js dashboard to visualize the multi-tenant organization management.
- [ ] **API Gateway**: Integration of Spring Cloud Gateway for advanced rate limiting and request transformation.
