# EXE101 Backend

## Overview

This is a Spring Boot backend for a pet spa / pet service management system.

The current project already has:

- JWT-based authentication and refresh token flow
- PostgreSQL + Flyway schema management
- cloud-only image upload via Supabase Storage
- JPA entity mapping for most of the database schema
- CRUD-style repositories, DTOs, mappers, services, and controllers for many main aggregates

The project does **not** yet have full domain workflows for every module. A large part of the newly added modules are
structural CRUD scaffolding and still need business rules, authorization checks, and workflow-specific validation.

Read this file before making changes. It is the project-level onboarding note for future agents and engineers.

## Architecture

### Tech stack

- Spring Boot 3.2.7
- Java 17
- Spring Web
- Spring Validation
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT via `jjwt`
- Supabase Storage
- WebSocket support
- LangChain4j / Qdrant dependencies are present in `pom.xml`, but they are not part of the main documented runtime flow
  yet

### Source of truth

- Database source of truth: `src/main/resources/db/migration`
- Runtime schema: `prod`
- Main application entrypoint: `src/main/java/com/exe101/ProjectEXE101Application.java`

### Package structure

Current top-level feature packages:

- `auth`
- `booking`
- `common`
- `config`
- `conversation`
- `customer`
- `exception`
- `file`
- `inventory`
- `invoice`
- `payment`
- `pet`
- `product`
- `resource`
- `servicePackage`
- `service_shop`
- `shop`
- `shopMember`
- `user`
- `userCredential`
- `vaccine`

### Layering convention

Each feature generally follows this structure:

- `controller`: HTTP entrypoints
- `service`: business logic
- `repository`: Spring Data JPA access
- `entity`: JPA mappings
- `dto`: request / response models
- `mapper`: entity <-> DTO mapping
- `exception`: domain exceptions

### Runtime shape

#### Auth flow

- `POST /api/auth/register`
  - creates `users`
  - uploads avatar to Supabase Storage if present
  - creates `user_credentials`
  - returns access token + refresh token
- `POST /api/auth/authenticate`
  - logs in with email/password
- `POST /api/auth/refreshToken`
  - rotates refresh token
- `POST /api/auth/logout`
  - revokes a refresh token
- `POST /api/auth/logout-all`
  - revokes all refresh tokens for the authenticated user

#### Main CRUD controllers currently exposed

- `/api/services`
- `/api/customers`
- `/api/pets`
- `/api/vaccines`
- `/api/products`
- `/api/resources`
- `/api/bookings`
- `/api/packages`
- `/api/invoices`
- `/api/payments`
- `/api/conversations`
- `/api/inventories`
- `/api/user`
- `/api/shop`
- `/api/shop-member`

#### Current architectural reality

- `auth` is the most concrete area with actual application flow.
- `customer`, `pet`, `booking`, `product`, `invoice`, `payment`, `conversation`, `resource`, `servicePackage`, `vaccine`
  now have structural CRUD layers.
- `shop` and `shopMember` still exist but are not fully implemented at the same quality level.
- Many modules compile and expose CRUD routes, but they are still thin and should not be mistaken for finished domain
  behavior.

### Security architecture

- Stateless JWT authentication
- `JwtAuthenticationFilterController` reads `Authorization: Bearer <token>`
- JWT contains at least:
  - `sub`: email
  - `role`
  - `userId`
- Current public routes:
  - `/api/auth/**`
  - `/error`
  - `/graphql`
  - `/ws/**`
  - `/ws-sockjs/**`
  - `/chat/**`
  - `/api/test/**`
- Everything else requires authentication

### Storage architecture

- Image upload is cloud-only
- Supabase Storage is used for public avatar upload
- Local file serving via `/files/**` is no longer part of the runtime design

### Database architecture

- Flyway is enabled
- Default schema is `prod`
- Runtime migrations:
  - `V1__init_db.sql`
  - `V2__add_last_login_at_to_users.sql`
  - `V3__add_col_user_credentials.sql`
  - `V4__alter_refresh_tokens.sql`
- Root `init_db.sql` is not the main runtime migration source

### Database design style

- PostgreSQL-first design
- DB enums for many domain states
- trigger-based `updated_at` handling in SQL
- composite uniqueness and composite FKs for `shop_id`-based multi-tenancy

### Mapping status

- Most tables from Flyway are now mapped as JPA entities
- Many primary aggregates also have repository + DTO + mapper + service + controller
- The main remaining gap is business logic depth, not entity coverage

### Architecture warning

Do not assume that "entity exists" means "feature is done".

Before extending a module, check:

- migration SQL
- entity mapping
- DTO coverage
- mapper coverage
- controller route
- authorization expectations
- multi-tenant implications

## Coding Rules

### General

- Keep business logic in `service`, not in `controller`
- Controllers should:
  - validate request
  - call service
  - return DTO / response
  - map status only when needed
- Repositories are for persistence access only
- Entities are persistence models, not public API contracts
- Prefer DTOs for request and response payloads

### Package and naming

- Follow feature-first packaging
- Use explicit names:
  - `CustomerService`
  - `BookingController`
  - `IInvoiceRepository`
  - `PaymentIntentDTO`
- Avoid generic packages like `misc`, `commonUtils`, `helpers`, etc. unless they are truly cross-cutting

### Database and JPA

- Every schema change must go through a new Flyway migration
- Do not manually change production schema without migration
- Prefer `EnumType.STRING`
- If a table contains `shop_id`, think about multi-tenancy before writing queries or APIs
- When a migration changes structure, update:
  - entity
  - repository
  - DTO
  - mapper
  - service
  - controller if contract changed

### Error handling

- Domain exceptions should extend `AppException`
- "Not found" exceptions should implement `NotFoundException`
- Global API errors are normalized by `RestExceptionHandler`
- Validation errors currently return `412 PRECONDITION_FAILED`

### Mapping and DTOs

- Mappers are mostly manual
- When a field is added to an entity that is exposed to client code, update DTO and mapper in the same change
- For helper tables and join tables, prefer scalar-first DTOs unless nested objects are clearly required

### Scope discipline

- Do not assume a domain is complete just because the package exists
- Do not assume CRUD scaffolding equals real workflow support
- Before coding, identify whether the target area needs:
  - structural mapping
  - business rules
  - validation
  - authorization
  - orchestration across multiple aggregates

## API Rules

### Base conventions

- Base path: `/api`
- Auth: Bearer token in `Authorization`
- Uploaded image URLs should be public Supabase Storage URLs
- Successful responses usually return DTOs directly
- Error response format:

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message",
  "data": {}
}
```

### Validation and status codes

- `412 PRECONDITION_FAILED`
  - request validation errors
  - `ValidateException`
- `404 NOT_FOUND`
  - domain not found exceptions
- `403 FORBIDDEN`
  - `PermissionNotAllowedException`
- `400 BAD_REQUEST`
  - other `AppException` cases
- `500 INTERNAL_SERVER_ERROR`
  - unhandled runtime errors

### Auth endpoints

#### `POST /api/auth/register`

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

#### `POST /api/auth/authenticate`

- Content-Type: `application/json`

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

#### `POST /api/auth/refreshToken`

```json
{
  "refreshToken": "token"
}
```

#### `POST /api/auth/logout`

```json
{
  "refreshToken": "token"
}
```

### CRUD controllers currently exposed

- `GET/POST/PUT/DELETE /api/services`
- `GET/POST/PUT/DELETE /api/customers`
- `GET/POST/PUT/DELETE /api/pets`
- `GET/POST/PUT/DELETE /api/vaccines`
- `GET/POST/PUT/DELETE /api/products`
- `GET/POST/PUT/DELETE /api/resources`
- `GET/POST/PUT/DELETE /api/bookings`
- `GET/POST/PUT/DELETE /api/packages`
- `GET/POST/PUT/DELETE /api/invoices`
- `GET/POST/PUT/DELETE /api/payments`
- `GET/POST/PUT/DELETE /api/conversations`
- `GET/POST/PUT/DELETE /api/inventories/{shopId}/{productId}` for composite-key inventory rows

### API stability note

- `auth` is the most concrete feature area
- Aggregate controllers now exist for many modules, but they are still thin
- `shop` and `shopMember` are still incomplete
- Do not assume advanced domain invariants are already enforced

## Build/Test

### Prerequisites

- Java 17
- Maven Wrapper (`mvnw.cmd`)
- PostgreSQL reachable from the app
- Required environment variables:
  - `DB_URL`
  - `DB_USER`
  - `DB_PASS`
  - `DB_SCHEMA`
  - `JWT_SECRET`
  - `SUPABASE_URL`
  - `SUPABASE_SERVICE_KEY`
  - `SUPABASE_BUCKET`

Never commit real secrets to source control, logs, docs, or fixtures.

### Local commands

Compile:

```powershell
.\mvnw.cmd -q -DskipTests compile
```

Run app:

```powershell
.\mvnw.cmd spring-boot:run
```

Run tests:

```powershell
.\mvnw.cmd test
```

### Current build/test status

- `compile` is passing
- `test` is still failing at bootstrap, not because of domain code
- Current cause:
  - `src/test/java/com/react/ProjectEXE101ApplicationTests.java` is in package `com.react`
  - the main application is in package `com.exe101`
  - `@SpringBootTest` therefore cannot find `@SpringBootConfiguration`

### Test reality

- Test coverage is still very low
- The only existing test is a context-load test and it is currently broken due to package mismatch
- Before building real workflows, add:
  - service tests
  - controller integration tests
  - repository tests where custom queries appear

## Do & Don't

### Do

- Read Flyway migrations before changing entity/repository code
- Keep this README updated when architecture or API shape changes
- Keep controllers thin and services explicit
- Add new migrations instead of rewriting released migrations
- Use DTOs consistently
- Re-check security rules before exposing new endpoints
- Be explicit about `shop_id` and multi-tenancy
- Treat CRUD scaffolding as a starting point, not as finished domain logic

### Don't

- Don't manually change production schema without migration
- Don't expose entities directly to clients
- Don't assume SQL and Java are fully aligned just because code compiles
- Don't assume every mapped table needs a public controller
- Don't hardcode secrets, DB passwords, tokens, or Supabase keys
- Don't silently change route contracts without updating README and frontend consumers
- Don't skip validation and authorization planning for new business flows

## Known Gaps and Mismatches

- `CredentialProvider` in Java is `LOCAL, GOOGLE, FACEBOOK`, while DB migration defines `LOCAL, GOOGLE, APPLE`
- `AuthenticationController.logout-all` still casts principal to `User`, while the security flow is using
  `UserPrincipal`
- `shop` and `shopMember` still have incomplete service/controller implementation
- Newly added aggregate services are mostly thin CRUD wrappers and still need domain-specific rules
- Several helper tables are mapped but do not yet have dedicated service/controller layers because they are not primary
  aggregates

## Documentation Rule

- Always update this `README.md` when:
  - adding a new module
  - changing an API endpoint
  - modifying database schema
  - updating authentication flow
  - changing package structure
- If a change affects architecture, API, or database:
  - updating `README.md` is required in the same PR
- Do not merge code that changes system behavior without updating `README.md`

This README should be treated as the working onboarding note for future agents and engineers. If schema shape, security
flow, package structure, or route contracts change, update this file in the same PR.
