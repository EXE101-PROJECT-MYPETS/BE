# EXE101 Backend

## Overview

This repository is a Spring Boot backend for a pet spa / pet service management system.

What is already in place:

- JWT authentication with access token + refresh token flow
- PostgreSQL schema managed by Flyway
- Cloud-only avatar upload via Supabase Storage
- JPA entity mapping for most of the SQL schema
- CRUD-style DTO, mapper, repository, service, and controller layers for many main aggregates

What is not true yet:

- Not every mapped entity represents a finished business feature
- Many newly added domains are still thin CRUD scaffolding
- Several workflows still need validation, authorization, and cross-aggregate rules

Treat this README as the primary onboarding note for future agents and engineers. Read it before scanning the codebase.

## Quick Reality Check

- Main Java package: `com.exe101`
- Maven coordinates still use `artifactId=react` and `name=react`
- Runtime DB schema defaults to `prod`
- Avatar storage is Supabase-only, not local disk
- `auth` is the most real feature area
- Most other aggregate modules compile and expose CRUD endpoints, but they are not final workflow implementations
- `compile` passes
- `test` is currently broken because the test package is `com.react`, not `com.exe101`

## Fast Start For New Agents

Read in this order before changing anything substantial:

1. This `README.md`
2. `src/main/resources/application.properties`
3. `src/main/resources/db/migration/V1__init_db.sql` through `V4__alter_refresh_tokens.sql`
4. `src/main/java/com/exe101/config/SecurityConfig.java`
5. `src/main/java/com/exe101/auth/controller/AuthenticationController.java`
6. `src/main/java/com/exe101/auth/service/AuthenticationService.java`
7. The specific feature package you are about to touch

If your change touches DB-backed behavior, read the Flyway SQL before reading the entity class.

## Architecture

### Tech Stack

- Spring Boot 3.2.7
- Java 17
- Spring Web
- Spring Validation
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- `jjwt`
- Supabase Storage integration
- WebSocket support
- LangChain4j and Qdrant dependencies are present in `pom.xml`, but they are not part of the main documented runtime
  flow yet

### Source Of Truth

- Application entrypoint: `src/main/java/com/exe101/ProjectEXE101Application.java`
- Runtime config: `src/main/resources/application.properties`
- Database source of truth: `src/main/resources/db/migration`
- Default runtime schema: `prod`
- Root `init_db.sql` is reference/bootstrap material, not the primary runtime migration source

### High-Level Request Flow

Normal request flow in this repo is:

1. `controller` receives HTTP request
2. DTO validation runs via Jakarta Validation
3. `service` handles business logic
4. `repository` performs persistence access
5. `mapper` converts entity <-> DTO
6. `RestExceptionHandler` normalizes API errors

For most CRUD modules, services follow the shared generic contract in `com.exe101.common.IService<E, D, ID>`.

### Package Structure

Current top-level packages under `src/main/java/com/exe101`:

- `auth`: authentication, JWT, refresh token flow
- `booking`: booking aggregate and helper booking tables
- `common`: shared interfaces and shared integrations such as Supabase storage
- `config`: security and application configuration
- `conversation`: conversations, members, messages
- `customer`: customers
- `exception`: base exceptions, error payload, exception handlers
- `file`: upload utility
- `inventory`: inventory aggregate and movements
- `invoice`: invoices and invoice lines
- `payment`: payment intents and payment transactions
- `pet`: pets, species, breeds, health profile
- `product`: products
- `resource`: shop resources
- `servicePackage`: packages, customer packages, package ledger
- `service_shop`: service catalog for shops
- `shop`: shop aggregate placeholder
- `shopMember`: shop member placeholder
- `user`: users and basic user endpoint
- `userCredential`: login credential model
- `vaccine`: vaccine catalog and pet vaccination support

### Layering Convention

Feature packages generally follow this layout:

- `entity`: JPA mappings
- `repository`: Spring Data repositories
- `dto`: API request / response models
- `mapper`: manual mapping code
- `service`: orchestration and domain logic
- `controller`: HTTP entrypoints
- `exception`: feature-specific exceptions

If a feature already follows this structure, keep it consistent. Do not introduce a different style in a nearby package
unless there is a real architectural reason.

### Module Status Matrix

| Module           | Current state | HTTP surface         | Notes                                                            |
|------------------|---------------|----------------------|------------------------------------------------------------------|
| `auth`           | Concrete      | `/api/auth/*`        | Register, login, refresh, logout, logout-all                     |
| `user`           | Partial       | `GET /api/user/{id}` | Read-only user endpoint                                          |
| `service_shop`   | CRUD scaffold | `/api/services`      | Service catalog CRUD                                             |
| `customer`       | CRUD scaffold | `/api/customers`     | Basic aggregate CRUD                                             |
| `pet`            | CRUD scaffold | `/api/pets`          | Basic aggregate CRUD                                             |
| `vaccine`        | CRUD scaffold | `/api/vaccines`      | Vaccine master data CRUD                                         |
| `product`        | CRUD scaffold | `/api/products`      | Product CRUD                                                     |
| `inventory`      | CRUD scaffold | `/api/inventories`   | Composite key aggregate                                          |
| `resource`       | CRUD scaffold | `/api/resources`     | Shop resource CRUD                                               |
| `booking`        | CRUD scaffold | `/api/bookings`      | Helper tables exist but are mostly internal                      |
| `servicePackage` | CRUD scaffold | `/api/packages`      | Customer package and ledger exist behind the aggregate           |
| `invoice`        | CRUD scaffold | `/api/invoices`      | Invoice line support exists in mapping layer                     |
| `payment`        | CRUD scaffold | `/api/payments`      | Public aggregate is payment intent, not every transaction detail |
| `conversation`   | CRUD scaffold | `/api/conversations` | Members and messages are mostly internal/helper models           |
| `shop`           | Incomplete    | route prefix only    | Controller exists, no real endpoints                             |
| `shopMember`     | Incomplete    | route prefix only    | Controller exists, no real endpoints                             |

### Security Architecture

- Authentication is stateless JWT
- `JwtAuthenticationFilterController` reads `Authorization: Bearer <token>`
- JWT currently carries at least `sub`, `role`, and `userId`
- Public routes configured in `SecurityConfig`:
  - `/api/auth/**`
  - `/error`
  - `/graphql`
  - `/ws/**`
  - `/ws-sockjs/**`
  - `/chat/**`
  - `/api/test/**`
- Everything else is authenticated
- Current CORS allows:
  - `http://localhost:5173`
  - `https://*.vercel.app`

### Storage Architecture

- Avatar upload is cloud-only
- `FileUploadUtil` delegates upload to Supabase
- Local file serving via `/files/**` is no longer part of the runtime design
- If frontend consumes avatar URLs directly, the target Supabase bucket must support public access or an equivalent
  signed URL strategy

### Database Architecture

Runtime migrations currently used by Flyway:

- `V1__init_db.sql`
- `V2__add_last_login_at_to_users.sql`
- `V3__add_col_user_credentials.sql`
- `V4__alter_refresh_tokens.sql`

Important database characteristics:

- PostgreSQL-first design
- DB enums are used heavily
- `updated_at` is managed in SQL through triggers
- Multi-tenancy is represented through `shop_id`
- Some tables use composite foreign keys such as `(shop_id, id)` to prevent cross-shop references
- Several nullable uniqueness constraints are implemented with partial indexes in SQL

### Mapping Coverage

The codebase now maps most of the main schema areas, including:

- auth and users
- shops and shop members
- customers
- pets, species, breeds, health profiles
- vaccines and pet vaccinations
- services
- products and inventories
- bookings and related helper tables
- packages and package ledgers
- invoices
- payment intents and payment transactions
- conversations, members, and messages

What that does not mean:

- It does not mean every table needs a public controller
- It does not mean every aggregate already enforces all business rules
- It does not mean SQL and Java are fully aligned in every edge case

## Coding Rules

### Layer Rules

- Keep business logic in `service`, not in `controller`
- Keep controllers thin
- Repositories should only handle persistence access
- Entities are persistence models, not API contracts
- Return DTOs to clients, not entities
- If a module already uses the generic `IService` contract, keep following it unless there is a clear reason to diverge

### Package And Naming Rules

- Keep the current feature-first package layout
- Follow existing naming patterns:
  - `CustomerController`
  - `CustomerService`
  - `ICustomerRepository`
  - `CustomerDTO`
  - `CustomerMapper`
  - `CustomerNotFound`
- Preserve existing package names such as `service_shop` and `servicePackage`; do not rename them casually in unrelated
  PRs

### DTO And Mapper Rules

- Use DTOs consistently for request and response payloads
- Update mapper and DTO in the same change when an exposed field changes
- Prefer explicit manual mapping over hidden magic
- For helper and join tables, keep DTOs scalar-first unless nested payloads are clearly needed

### Database And JPA Rules

- Every schema change must be a new Flyway migration
- Never rewrite released migrations unless the branch has not been shared yet
- Do not change production schema manually
- Prefer `EnumType.STRING`
- Re-check SQL defaults, enum names, and nullability before changing entities
- If a table contains `shop_id`, think about multi-tenancy before writing repository methods or APIs
- When a migration changes structure, update all affected layers in the same work:
  - entity
  - repository
  - DTO
  - mapper
  - service
  - controller
  - README

### Error Handling Rules

- Feature-specific exceptions should extend `AppException`
- "Not found" exceptions should also implement `NotFoundException`
- API error normalization is centralized in `exception/rest/RestExceptionHandler`
- Validation failures currently return `412 PRECONDITION_FAILED`

### Security Rules

- Assume every non-public route requires JWT unless `SecurityConfig` says otherwise
- Re-check whitelist rules before exposing a new endpoint
- Do not add an endpoint and assume authorization later
- Be explicit about ownership and `shop_id` scoping for all multi-tenant data access

### Scope Discipline

- Do not confuse CRUD scaffolding with finished business behavior
- Do not assume a mapped helper table needs a public controller
- Before coding, decide whether the task is:
  - structural mapping
  - CRUD completion
  - business rule implementation
  - authorization work
  - validation work
  - cross-aggregate orchestration

## API Rules

### Base Conventions

- Base path is `/api`
- Auth is Bearer token in `Authorization`
- Successful responses usually return DTOs directly
- Error responses use the normalized payload shape:

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message",
  "data": {}
}
```

- Most aggregate controllers follow classic CRUD style:
  - `GET /resource`
  - `GET /resource/{id}`
  - `POST /resource`
  - `PUT /resource/{id}`
  - `DELETE /resource/{id}`
- Inventory is the main composite-key exception:
  - `GET /api/inventories/{shopId}/{productId}`
  - `PUT /api/inventories/{shopId}/{productId}`
  - `DELETE /api/inventories/{shopId}/{productId}`

### Auth Endpoints

`POST /api/auth/register`

- Content-Type: `multipart/form-data`
- Fields:
  - `email`
  - `password`
  - `fullName`
  - `phone`
  - `address` optional
  - `age` optional
  - `avatarUrlPreview` optional file
- Returns:
  - `accessToken`
  - `role`
  - `refreshToken`
  - `user`

`POST /api/auth/authenticate`

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

`POST /api/auth/refreshToken`

```json
{
  "refreshToken": "token"
}
```

`POST /api/auth/logout`

```json
{
  "refreshToken": "token"
}
```

`POST /api/auth/logout-all`

- No request body
- Requires authentication
- Current implementation has a principal-casting bug; see "Known Gaps and Mismatches"

### Controllers Currently Exposed

Concrete route prefixes currently present in code:

- `/api/auth`
- `/api/user`
- `/api/services`
- `/api/customers`
- `/api/pets`
- `/api/vaccines`
- `/api/products`
- `/api/inventories`
- `/api/resources`
- `/api/bookings`
- `/api/packages`
- `/api/invoices`
- `/api/payments`
- `/api/conversations`
- `/api/shop`
- `/api/shop-member`

Route behavior today:

- `/api/services`, `/api/customers`, `/api/pets`, `/api/vaccines`, `/api/products`, `/api/resources`, `/api/bookings`,
  `/api/packages`, `/api/invoices`, `/api/payments`, and `/api/conversations` expose `GET/POST/PUT/DELETE`
- `/api/inventories` exposes CRUD using `{shopId}/{productId}` as the identifier
- `/api/user` currently exposes only `GET /api/user/{id}`
- `/api/shop` and `/api/shop-member` only expose route prefixes right now; do not assume usable CRUD behavior

### Status Codes

- `200 OK`: standard successful read, update, logout
- `201` is not consistently used by current controllers; many creates still return `200`
- `400 BAD_REQUEST`: generic `AppException`
- `403 FORBIDDEN`: `PermissionNotAllowedException`
- `404 NOT_FOUND`: `NotFoundException`
- `412 PRECONDITION_FAILED`: validation errors and `ValidateException`
- `500 INTERNAL_SERVER_ERROR`: unhandled runtime errors

### API Stability Notes

- `auth` is the most stable area
- Most new aggregate controllers are intentionally thin
- Advanced domain invariants are not guaranteed yet
- Do not change route contracts silently
- If an endpoint changes shape, update README and notify frontend consumers in the same PR

## Build/Test

### Prerequisites

- Java 17
- Maven Wrapper via `mvnw.cmd`
- PostgreSQL reachable from the app
- Required environment variables:
  - `DB_URL`
  - `DB_USER`
  - `DB_PASS`
  - `JWT_SECRET`
  - `SUPABASE_URL`
  - `SUPABASE_SERVICE_KEY`
  - `SUPABASE_BUCKET`

Optional environment variables:

- `DB_SCHEMA`
  - defaults to `prod`
- `PORT`
  - defaults to `8080`

Never commit real secrets to source control, logs, docs, or fixtures.

### Local Commands

Compile:

```powershell
.\mvnw.cmd -q -DskipTests compile
```

Run the app:

```powershell
.\mvnw.cmd spring-boot:run
```

Run tests:

```powershell
.\mvnw.cmd test
```

### Current Build/Test Status

- `compile` is passing
- `test` is failing during Spring bootstrap, not because of the newly mapped domain code
- Current failure cause:
  - `src/test/java/com/react/ProjectEXE101ApplicationTests.java` uses package `com.react`
  - the application package is `com.exe101`
  - `@SpringBootTest` therefore cannot locate `@SpringBootConfiguration`

### Test Reality

- Test coverage is still very low
- The only existing test is a context-load test and it is currently broken
- Before implementing real workflows, add:
  - service tests
  - controller integration tests
  - repository tests where custom queries or composite keys matter

## Do & Don't

### Do

- Read Flyway migrations before changing entities or repositories
- Keep controllers thin and services explicit
- Keep DTOs and mappers aligned with exposed entity fields
- Add new Flyway migrations instead of mutating released migrations
- Re-check security rules before exposing a new endpoint
- Be explicit about `shop_id` and multi-tenant scope
- Update README in the same PR when architecture, API, auth flow, schema, or package structure changes
- Treat CRUD scaffolding as a starting point, not proof that a domain is finished

### Don't

- Do not expose entities directly to clients
- Do not change production schema manually
- Do not assume compile success means SQL and Java are fully aligned
- Do not assume every mapped table deserves a public controller
- Do not hardcode DB credentials, JWT secrets, refresh tokens, or Supabase keys
- Do not silently change route contracts without updating README and frontend consumers
- Do not skip validation and authorization planning for new business flows
- Do not treat `shop` or `shopMember` as complete modules without checking their actual implementation first

## Known Gaps And Mismatches

- `CredentialProvider` in Java is `LOCAL, GOOGLE, FACEBOOK`, while the DB migration defines `LOCAL, GOOGLE, APPLE`
- `AuthenticationController.logout-all` still casts the authenticated principal to `User`, while the security flow uses
  `UserPrincipal`
- `shop` and `shopMember` are still incomplete and currently expose only route prefixes
- Many newly added aggregate services are still thin CRUD wrappers
- Several helper tables are mapped but intentionally do not have dedicated public controllers
- Maven metadata still says `react`, while the real application package is `com.exe101`

## Documentation Rule

Always update this `README.md` when:

- adding a new module
- changing an API endpoint
- modifying database schema
- updating authentication flow
- changing package structure
- changing storage behavior
- changing security whitelist or auth requirements

If a change affects architecture, API, auth, or database behavior, updating `README.md` is required in the same PR.

Do not merge code that changes system behavior without updating this file.
