# EXE101 Backend

## Overview

This is a Spring Boot backend for a pet spa / pet service management system.

The current codebase includes authentication, user management, local file serving, and partial mappings for `shop`,
`shop_member`, and `service`.

However, the database schema is significantly more extensive than the current Java implementation. Flyway migrations
already define additional domains such as customer, pet, booking, product, inventory, invoice, payment, conversation,
vaccine, package, etc., but the corresponding Java modules are not fully implemented yet.

To quickly understand the project, keep in mind:

1. The application uses Spring Boot + Spring Security + JWT + JPA + PostgreSQL + Flyway.
2. The source of truth for the database is located at `src/main/resources/db/migration`.
3. The currently functional scope is mainly `auth` and `user`; most other modules are still scaffolding.

---

## Architecture

### High-level

- Application entry point: `src/main/java/com/exe101/ProjectEXE101Application.java`
- Config and cross-cutting concerns are located in:
  - `config`: security, Jackson, model mapper, static resources
  - `exception`: custom exceptions and global exception handler
  - `file`: local file storage and public URL generation
  - `common`: shared interfaces
- Business logic is organized by feature packages:
  - `auth`
  - `user`
  - `userCredential`
  - `shop`
  - `shopMember`
  - `service`

---

### Layering Convention

Each feature is typically structured into:

- `controller`: HTTP entrypoint
- `service`: business logic
- `repository`: Spring Data JPA repository
- `entity`: JPA entity mapped to DB
- `dto`: request/response objects
- `mapper`: convert entity <-> DTO
- `exception`: domain-specific errors

---

### Current Implemented Runtime Flow

#### Authentication

- `POST /api/auth/register`
  - create `users`
  - upload avatar locally if provided
  - create `user_credentials`
  - generate access token + refresh token
- `POST /api/auth/authenticate`
  - login using email/password
- `POST /api/auth/refreshToken`
  - rotate refresh token
- `POST /api/auth/logout`
  - revoke a single refresh token
- `POST /api/auth/logout-all`
  - revoke all refresh tokens of a user

#### User

- `GET /api/user/{id}` returns `UserDTO`

---

### Security Architecture

- The app uses stateless JWT authentication.
- `JwtAuthenticationFilterController` reads `Authorization: Bearer <token>`.
- JWT contains at least:
  - `sub`: email
  - `role`
  - `userId`
- Current public routes:
  - `/api/auth/**`
  - `/error`
  - `/files/**`
  - `/graphql`
  - `/ws/**`
  - `/ws-sockjs/**`
  - `/chat/**`
  - `/api/test/**`
- All other routes require authentication.

---

## Database Architecture

### Source of Truth

- Flyway is enabled.
- Default schema is `prod`.
- Do NOT treat `init_db.sql` in root as the main runtime migration.
- Runtime schema is built from:
  - `V1__init_db.sql`
  - `V2__add_last_login_at_to_users.sql`
  - `V3__add_col_user_credentials.sql`
  - `V4__alter_refresh_tokens.sql`

---

### Database Design Style

- PostgreSQL-first approach
- Use DB enums for many domain fields
- Use trigger `set_updated_at()` for tables with `updated_at`
- Use composite uniqueness and composite FK to ensure multi-tenant safety via `shop_id`

---

### Tables Relevant to Current Code

- `users`
- `user_credentials`
- `refresh_tokens`
- `shops`
- `shop_members`
- `services`

---

### Important Reality Check

- SQL schema is ahead of Java implementation
- Many tables exist without corresponding Java modules
- When implementing new features:
  - ALWAYS check both migration SQL and existing Java code
  - NEVER assume they are fully synchronized

---

## Coding Rules

### General

- Keep business logic in `service`, NOT in `controller`
- Controller responsibilities:
  - validate request
  - call service
  - map HTTP status if needed
  - return DTO/response
- Repository handles data access only
- Entity is for DB mapping only, NOT for API request/response
- Always use DTO for responses to clients

---

### Package & Naming

- Follow feature-based packages first, then technical layering
- Use meaningful class names:
  - `AuthenticationService`
  - `UserController`
  - `IUserRepository`
  - `UserDTO`
- Avoid generic packages like `misc` or `utils`

---

### Database & JPA

- Every schema change MUST go through Flyway migration
- NEVER manually modify production DB without migration
- Prefer `EnumType.STRING`, NOT ordinal
- If a table has `shop_id`, ALWAYS consider multi-tenancy
- When migration changes schema:
  - update entity, repository, DTO accordingly

---

### Error Handling

- Domain errors should use custom exceptions (extend `AppException`)
- Global API errors handled by `RestExceptionHandler`
- Validation errors return `412 PRECONDITION_FAILED`

---

### Mapping & DTOs

- Mapping is mostly manual (e.g., `UserMapper`)
- When adding fields to entity:
  - update DTO and mapper together

---

### Scope Discipline

- Do NOT blindly implement based on SQL table names
- Before coding:
  - check if endpoint, service, mapper, validation, auth rules exist
- If module is scaffold:
  - write TODO or assumption instead of incorrect implementation

---

## API Rules

### Base Conventions

- Base path: `/api`
- Auth: Bearer token in `Authorization` header
- Public files served via `/files/**`
- Success response: usually returns DTO/object directly
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
  - request validation sai
  - `ValidateException`
- `404 NOT_FOUND`
  - `NotFoundException`
- `403 FORBIDDEN`
  - `PermissionNotAllowedException`
- `400 BAD_REQUEST`
  - cac `AppException` con lai
- `500 INTERNAL_SERVER_ERROR`
  - `NullPointerException` va loi khong du kien

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
- Tra ve:
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

### User endpoint

#### `GET /api/user/{id}`

- Yeu cau authenticated
- Tra `UserDTO`

### API stability note

- Only auth and user are considered stable
  shop, shopMember, service are not fully implemented
- Do NOT assume endpoint exists based on package name

## Build/Test

### Prerequisites

- Java 17
- Maven Wrapper (`mvnw.cmd`)
- PostgreSQL accessible
- Required environment variables:
  - `DB_URL`
  - `DB_USER`
  - `DB_PASS`
  - `DB_SCHEMA`
  - `JWT_SECRET`
  - `UPLOAD_DIR`
  - `PUBLIC_BASE_URL`
  - `SUPABASE_URL`
  - `SUPABASE_SERVICE_KEY`
  - `SUPABASE_BUCKET`

NEVER commit real secrets

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

- `compile` is passing.
- `test` is failing at bootstrap, not due to business logic.
- Current cause:
  - `src/test/java/com/react/ProjectEXE101ApplicationTests.java` is in package `com.react`
  - the main application is in package `com.exe101`
  - therefore `@SpringBootTest` cannot find `@SpringBootConfiguration`

### Test reality

- Test coverage is currently very low.
- The only test is a context load test and it is failing due to incorrect package structure.
- Before adding major features, at minimum:
  - add service tests
  - add controller integration tests
  - add repository tests if there are custom queries

## Do & Don't

### Do

- Read migrations before modifying entity/repository.
- Keep this `README.md` updated when there are important architecture/API changes.
- Keep controllers thin and services rich.
- Add new migrations instead of modifying released ones.
- Use DTOs properly for request/response.
- Verify authentication rules before exposing new endpoints.
- Check whether routes are public or protected in `SecurityConfig`.
- Be mindful of `shop_id` when working with multi-tenant features.

### Don’t

- Don’t manually modify production schema without adding migration.
- Don’t expose entities directly to the client.
- Don’t assume SQL and Java are fully synchronized.
- Don’t add endpoints to scaffold modules without fixing repository/service types to match the correct domain.
- Don’t hardcode secrets, tokens, DB passwords, or Supabase keys in code/docs.
- Don’t silently change existing route mappings; update README and frontend contract if changed.
- Don’t skip request validation for auth or public endpoints.

## Known Gaps and Mismatches

- `CredentialProvider` in Java is `LOCAL, GOOGLE, FACEBOOK`, but DB enum in migration is `LOCAL, GOOGLE, APPLE`.
- `ServiceController` is currently mapped to `/api/shop`, causing namespace conflict with `ShopController`.
- `IServiceRepository` and `ServiceService` are incorrectly using `Shop/ShopDTO` instead of `Service/ServiceDTO`.
- `logout-all` is casting principal to `User`, while the security flow is using `UserPrincipal`.
- Many tables in SQL do not yet have corresponding Java modules.

README nay nen duoc xem la onboarding note cho agent/engineer moi. Neu co thay doi quan trong ve schema, auth flow,
route, hoac package structure, cap nhat file nay trong cung PR.
