# EXE101 Backend

## Overview

This repository is a Spring Boot backend for a pet spa / pet service management system.

Current reality of the project:

- JWT authentication with access token + refresh token flow is implemented
- PostgreSQL schema is managed by Flyway
- Avatar upload is cloud-only via Supabase Storage
- Most SQL tables are already mapped to JPA entities
- Many main aggregates already have `controller`, `dto`, `mapper`, `repository`, and `service` layers
- A large part of the new domains are still CRUD scaffolding, not final business workflows

Use this README as the onboarding note for future agents and engineers before re-scanning the codebase.

## Quick Reality Check

- Main Java package: `com.exe101`
- Maven metadata still says `artifactId=react`
- Runtime schema defaults to `prod`
- Auth is the most mature feature area
- Most other domains expose CRUD routes, but still need domain rules and authorization hardening
- Service listing uses cursor-based infinite scroll on `/api/services`
- Multi-shop endpoints must require `X-Shop-Id`; never expose all shops by default
- `compile` passes
- `test` is currently broken because the test package is `com.react`, while the app package is `com.exe101`

## Multi-Shop API Rule

This system can contain many shops. Any endpoint that lists or searches shop-owned data must require a shop scope on every request.

Required rule:

- Shop-owned endpoints must require the `X-Shop-Id` request header when shop scope is needed.
- Do not make `X-Shop-Id` optional on shop-owned list/search APIs.
- Do not use `(:shopId IS NULL OR ...)` as an API behavior for shop-owned lists. Repository queries may keep null handling for internal reuse, but controllers must require `X-Shop-Id`.
- Create/update endpoints must take shop scope from `X-Shop-Id`; body `shopId`, when present, is not the source of truth.
- Global catalog endpoints can omit `X-Shop-Id` only when the entity has no `shop_id`, for example `/api/vaccines`.

Shop-owned endpoint groups currently requiring `X-Shop-Id`:

- `/api/services`
- `/api/bookings`
- `/api/orders`
- `/api/customers`
- `/api/pets`
- `/api/products`
- `/api/resources`
- `/api/inventories`
- `/api/packages`
- `/api/invoices`
- `/api/payments`
- `/api/conversations`
- `/api/reviews`
- `/api/service-categories`
- `/api/shops/staff`

## Fast Start For New Agents

Read in this order before touching the code:

1. `README.md`
2. `src/main/resources/application.properties`
3. `src/main/resources/db/migration/V1__init_db.sql` through `V6__seed_default_service_categories.sql`
4. `src/main/java/com/exe101/config/SecurityConfig.java`
5. `src/main/java/com/exe101/auth/controller/AuthenticationController.java`
6. `src/main/java/com/exe101/auth/service/AuthenticationService.java`
7. The specific feature package you plan to modify

If the task touches persistence or API behavior, read the Flyway SQL first.

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
- LangChain4j and Qdrant dependencies exist in `pom.xml`, but they are not part of the main documented runtime flow yet

### Source Of Truth

| Area                     | Source                                                            |
|--------------------------|-------------------------------------------------------------------|
| Application entrypoint   | `src/main/java/com/exe101/ProjectEXE101Application.java`          |
| Runtime config           | `src/main/resources/application.properties`                       |
| Database source of truth | `src/main/resources/db/migration`                                 |
| Default schema           | `prod`                                                            |
| Legacy bootstrap SQL     | `init_db.sql` at repo root, not the main runtime migration source |

### Package Layout

```text
com.exe101/
|-- auth/              # Authentication, JWT, refresh token flow
|-- booking/           # Booking aggregate and helper booking tables
|-- common/            # Shared interfaces and integrations
|-- config/            # Security and application configuration
|-- conversation/      # Conversations, members, messages
|-- customer/          # Customers
|-- exception/         # Base exceptions, error payload, handlers
|-- file/              # Upload utility
|-- inventory/         # Inventory aggregate and movements
|-- invoice/           # Invoices and invoice lines
|-- payment/           # Payment intents and payment transactions
|-- pet/               # Pets, species, breeds, health profile
|-- product/           # Products
|-- review/            # Product reviews
|-- resource/          # Shop resources
|-- servicePackage/    # Packages, customer packages, package ledger
|-- service_shop/      # Service catalog and service categories for shops
|-- shop/              # Shop aggregate placeholder
|-- shopMember/        # Shop member placeholder
|-- user/              # Users and basic user endpoint
|-- userCredential/    # Login credential model
`-- vaccine/           # Vaccine catalog and pet vaccination support
```

### Feature Package Shape

Most feature packages follow this internal structure:

```text
feature/
|-- controller/        # HTTP endpoints
|-- dto/               # Request / response payloads
|-- entity/            # JPA mappings
|-- exception/         # Feature-specific exceptions
|-- mapper/            # Manual entity <-> DTO mapping
|-- repository/        # Spring Data repositories
`-- service/           # Business logic
```

Shared CRUD services commonly implement `com.exe101.common.IService<E, D, ID>`.

### High-Level Request Flow

```text
HTTP request
  -> controller
  -> DTO validation
  -> service
  -> repository
  -> mapper
  -> HTTP response

Errors
  -> RestExceptionHandler
  -> normalized ErrorPayload
```

### Module Status Matrix

| Module           | Status        | Base path            | Notes                                                                                           |
|------------------|---------------|----------------------|-------------------------------------------------------------------------------------------------|
| `auth`           | Concrete      | `/api/auth`          | Customer register, shop-owner register, customer login, shop login, refresh, logout, logout-all |
| `user`           | Partial       | `/api/user`          | Only `GET /api/user/{id}` is exposed                                                            |
| `service_shop`   | CRUD scaffold | `/api/services`, `/api/service-categories` | Service catalog CRUD, cursor scroll, and shop-scoped service category CRUD                      |
| `customer`       | CRUD scaffold | `/api/customers`     | Customer aggregate CRUD                                                                         |
| `pet`            | CRUD scaffold | `/api/pets`          | Pet aggregate CRUD                                                                              |
| `vaccine`        | CRUD scaffold | `/api/vaccines`      | Vaccine master CRUD                                                                             |
| `product`        | CRUD scaffold | `/api/products`      | Product CRUD                                                                                    |
| `inventory`      | CRUD scaffold | `/api/inventories`   | Composite-key inventory CRUD                                                                    |
| `resource`       | CRUD scaffold | `/api/resources`     | Shop resource CRUD                                                                              |
| `booking`        | CRUD scaffold | `/api/bookings`      | Service appointment CRUD plus cursor scroll                                                     |
| `order`          | CRUD scaffold | `/api/orders`        | Online product order CRUD plus cursor scroll                                                    |
| `servicePackage` | CRUD scaffold | `/api/packages`      | Package CRUD                                                                                    |
| `invoice`        | CRUD scaffold | `/api/invoices`      | Invoice CRUD                                                                                    |
| `payment`        | CRUD scaffold | `/api/payments`      | Payment intent CRUD                                                                             |
| `conversation`   | CRUD scaffold | `/api/conversations` | Conversation CRUD                                                                               |
| `review`         | CRUD scaffold | `/api/reviews`       | Product review CRUD (shop-scoped)                                                               |
| `shop`           | Incomplete    | `/api/shop`          | Prefix exists, no real handler methods                                                          |
| `shopMember`     | Incomplete    | `/api/shop-member`   | Prefix exists, no real handler methods                                                          |

### Security Architecture

| Concern           | Current behavior                                                                            |
|-------------------|---------------------------------------------------------------------------------------------|
| Auth model        | Stateless JWT                                                                               |
| JWT filter        | `JwtAuthenticationFilterController`                                                         |
| JWT claims in use | `sub`, `role`, `userId`                                                                     |
| Public routes     | `/api/auth/**`, `/api/public/**`, `/error`, `/graphql`, `/ws/**`, `/ws-sockjs/**`, `/chat/**`, `/api/test/**` |
| Protected routes  | Everything else                                                                             |
| CORS origins      | `http://localhost:3000`, `http://localhost:5173`, `https://*.vercel.app`                    |

### Storage Architecture

| Concern            | Current behavior                                                |
|--------------------|-----------------------------------------------------------------|
| Avatar storage     | Supabase Storage                                                |
| Local file serving | Removed from runtime design                                     |
| Upload helper      | `FileUploadUtil` delegates to Supabase                          |
| Operational note   | Bucket/access strategy must support returned client-facing URLs |

### Database Architecture

Flyway migrations currently active:

| Version | File                                      | Purpose                                                   |
|---------|-------------------------------------------|-----------------------------------------------------------|
| `V1`    | `V1__init_db.sql`                         | Main schema bootstrap                                     |
| `V2`    | `V2__add_last_login_at_to_users.sql`      | Add `users.last_login_at`                                 |
| `V3`    | `V3__add_col_user_credentials.sql`        | Extend credential data and add `users.role`               |
| `V4`    | `V4__alter_refresh_tokens.sql`            | Align refresh token column names with code                |
| `V5`    | `V5__add_service_categories.sql`          | Add `service_categories`, `services.category_id`, indexes |
| `V6`    | `V6__seed_default_service_categories.sql` | Seed default service categories for every existing shop   |
| `V7`    | `V7__seed_service_management_demo_data.sql` | Seed service catalog demo data                            |
| `V8`    | `V8__seed_booking_demo_data.sql`          | Seed service appointment demo data                        |
| `V9`    | `V9__create_orders.sql`                   | Add online order tables and order enums                   |
| `V10`   | `V10__link_invoices_to_orders.sql`        | Allow invoices to reference orders                        |
| `V11`   | `V11__seed_order_demo_data.sql`           | Move demo booking products out of bookings and seed orders |

Important SQL design traits:

- PostgreSQL-first schema
- DB enums are used heavily
- `updated_at` is maintained in SQL via triggers
- Multi-tenancy is centered around `shop_id`
- Several relations use composite keys such as `(shop_id, id)`
- Some uniqueness rules are implemented through partial indexes

### Mapping Coverage

The codebase now maps most of the main SQL areas:

- auth and users
- shops and shop members
- customers
- pets, species, breeds, health profiles
- vaccines and pet vaccinations
- services and service categories
- products and inventories
- bookings and helper booking tables
- orders and helper order tables
- packages and package ledgers
- invoices
- payment intents and payment transactions
- conversations, members, and messages

Do not confuse "mapped" with "finished":

- mapped does not mean public API exists
- mapped does not mean domain rules are complete
- compile success does not prove SQL and Java are perfectly aligned

## Coding Rules

### Layer Rules

- Keep business logic in `service`, not in `controller`
- Keep controllers thin
- Repositories are for persistence access only
- Entities are persistence models, not API contracts
- Return DTOs to clients, not entities
- If a package already follows the generic CRUD style, keep it consistent

### Naming Rules

Use the existing naming style consistently:

| Layer      | Example               |
|------------|-----------------------|
| Controller | `CustomerController`  |
| Service    | `CustomerService`     |
| Repository | `ICustomerRepository` |
| DTO        | `CustomerDTO`         |
| Mapper     | `CustomerMapper`      |
| Exception  | `CustomerNotFound`    |

Do not casually rename existing package names like `service_shop` or `servicePackage` in unrelated work.

### DTO And Mapper Rules

- Update DTO and mapper in the same change when an exposed field changes
- Prefer explicit mapping over hidden magic
- For helper or join tables, keep payloads scalar-first unless nested objects are clearly needed
- Do not expose entity classes directly in controller responses

### Database And JPA Rules

- Every schema change must be a new Flyway migration
- Do not rewrite released migrations unless the branch is still private and disposable
- Do not manually change production schema
- Prefer `EnumType.STRING`
- Check SQL enum names, nullability, defaults, and composite keys before changing entities
- If a table includes `shop_id`, think about tenancy scope before writing repository methods

When schema changes, update all impacted layers in the same work:

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
- API errors are normalized by `exception/rest/RestExceptionHandler`
- Validation failures currently return `412 PRECONDITION_FAILED`

### Security Rules

- Assume every non-public route requires JWT unless `SecurityConfig` says otherwise
- Re-check whitelist behavior before exposing new endpoints
- Do not postpone authorization design for later if the endpoint is being added now
- Be explicit about ownership and `shop_id` scoping

### Scope Discipline

- CRUD scaffolding is not finished workflow logic
- Not every helper table needs a public controller
- Before coding, decide whether the task is structural mapping, CRUD completion, business-rule implementation,
  validation, authorization, or orchestration

## API Rules

### Base Conventions

| Rule                 | Current behavior                               |
|----------------------|------------------------------------------------|
| Base path            | `/api`                                         |
| Auth header          | `Authorization: Bearer <token>`                |
| Success payloads     | Usually DTOs returned directly                 |
| Error payload        | `code`, `message`, `data`                      |
| Create responses     | Many controllers still return `200`, not `201` |
| Inventory identifier | Composite key from `X-Shop-Id` header + `{productId}` |

Error payload shape:

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message",
  "data": {}
}
```

### Auth API

| Method | Endpoint                        | Description                                                                                                                                              |
|--------|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `POST` | `/api/auth/register/email-verification/send-code`   | Send a 6-digit email verification code before first-time registration; request only needs `email`                                         |
| `POST` | `/api/auth/register/email-verification/verify-code` | Verify the first-time registration email code; request needs `email` and `code`                                            |
| `POST` | `/api/auth/register`            | Register with `multipart/form-data`, create user and credential, optionally upload avatar to Supabase, return tokens plus user DTO |
| `POST` | `/api/auth/shop-owner/register` | Register a shop owner with `multipart/form-data`, create `users`, `user_credentials`, `shops`, `shop_members`, then return tokens plus user and shop DTO |
| `POST` | `/api/auth/customer/login`      | Customer login with email/password JSON payload                                                                                                          |
| `POST` | `/api/auth/shop/login`          | Shop login with email/password JSON payload; only `SHOP` accounts with active `shop_members` are allowed; response includes `shops` and `currentShopId` |
| `POST` | `/api/auth/refreshToken`        | Rotate refresh token using `{ "refreshToken": "token" }`; shop users receive refreshed `shops` and `currentShopId`                                      |
| `POST` | `/api/auth/logout`              | Revoke one refresh token using `{ "refreshToken": "token" }`                                                                                             |
| `POST` | `/api/auth/logout-all`          | Revoke all refresh tokens for the authenticated user; current implementation still has a principal-casting bug                                           |

Auth payload examples:

First-time registration email verification:

`POST /api/auth/register/email-verification/send-code`

```json
{
  "email": "user@example.com"
}
```

`POST /api/auth/register/email-verification/verify-code`

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

The registration email verification endpoints are standalone public APIs for frontend-led registration flows. Current customer/shop-owner register endpoints do not call the email service automatically.

`POST /api/auth/customer/login`

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

`POST /api/auth/shop/login` response shape:

```json
{
  "accessToken": "jwt",
  "role": "SHOP",
  "refreshToken": "refresh-token",
  "user": {
    "id": 1,
    "email": "owner@example.com",
    "fullName": "Shop Owner",
    "role": "SHOP"
  },
  "shops": [
    {
      "id": 1,
      "name": "Pet Spa A",
      "addressText": "123 Street",
      "shopStatus": "ACTIVE",
      "memberRole": "OWNER",
      "memberStatus": "ACTIVE"
    }
  ],
  "currentShopId": 1
}
```

Frontend must send `currentShopId` or a selected item from `shops` as the `X-Shop-Id` header when calling shop-owned endpoints.

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

### Controller Overview

| Module           | Base path            | Methods               | Status        | Description                                                                         |
|------------------|----------------------|-----------------------|---------------|-------------------------------------------------------------------------------------|
| `auth`           | `/api/auth`          | `POST`                | Concrete      | Register email verification, customer register, shop-owner register, customer login, shop login, refresh, logout |
| `user`           | `/api/user`          | `GET`                 | Partial       | Read-only user lookup                                                               |
| `service_shop`   | `/api/services`      | `GET/POST/PUT/DELETE` | CRUD scaffold | Service catalog CRUD plus cursor scroll                                             |
| `service_category` | `/api/service-categories` | `GET/POST/PUT/DELETE` | CRUD scaffold | Shop-scoped service category CRUD with active filter                                |
| `customer`       | `/api/customers`     | `GET/POST/PUT/DELETE` | CRUD scaffold | Customer CRUD                                                                       |
| `pet`            | `/api/pets`          | `GET/POST/PUT/DELETE` | CRUD scaffold | Pet CRUD                                                                            |
| `vaccine`        | `/api/vaccines`      | `GET/POST/PUT/DELETE` | CRUD scaffold | Vaccine CRUD                                                                        |
| `product`        | `/api/products`      | `GET/POST/PUT/DELETE` | CRUD scaffold | Product CRUD with dynamic `reviewAvg` and `totalReviews` from reviews             |
| `inventory`      | `/api/inventories`   | `GET/POST/PUT/DELETE` | CRUD scaffold | Composite-key inventory CRUD                                                        |
| `resource`       | `/api/resources`     | `GET/POST/PUT/DELETE` | CRUD scaffold | Resource CRUD                                                                       |
| `booking`        | `/api/bookings`      | `GET/POST/PUT/DELETE` | CRUD scaffold | Service appointment CRUD plus cursor scroll                                         |
| `order`          | `/api/orders`        | `GET/POST/PUT/DELETE` | CRUD scaffold | Online product order CRUD plus cursor scroll                                        |
| `servicePackage` | `/api/packages`      | `GET/POST/PUT/DELETE` | CRUD scaffold | Package CRUD                                                                        |
| `invoice`        | `/api/invoices`      | `GET/POST/PUT/DELETE` | CRUD scaffold | Invoice CRUD                                                                        |
| `payment`        | `/api/payments`      | `GET/POST/PUT/DELETE` | CRUD scaffold | Payment CRUD                                                                        |
| `conversation`   | `/api/conversations` | `GET/POST/PUT/DELETE` | CRUD scaffold | Conversation CRUD                                                                   |
| `shop`           | `/api/shop`          | route prefix only     | Incomplete    | No real handler methods yet                                                         |
| `shopMember`     | `/api/shop-member`   | route prefix only     | Incomplete    | No real handler methods yet                                                         |

### Standard CRUD Pattern

Applies to these route groups:

- `/api/services`
- `/api/service-categories`
- `/api/customers`
- `/api/pets`
- `/api/vaccines`
- `/api/products`
- `/api/resources`
- `/api/bookings`
- `/api/orders`
- `/api/packages`
- `/api/invoices`
- `/api/payments`
- `/api/conversations`

| Method   | Endpoint pattern       | Description          |
|----------|------------------------|----------------------|
| `GET`    | `/api/<resource>`      | List records         |
| `GET`    | `/api/<resource>/{id}` | Get one record by id |
| `POST`   | `/api/<resource>`      | Create one record    |
| `PUT`    | `/api/<resource>/{id}` | Update one record    |
| `DELETE` | `/api/<resource>/{id}` | Delete one record    |

### Special Route Shapes

| Module             | Method   | Endpoint                                             | Description                                          |
|--------------------|----------|------------------------------------------------------|------------------------------------------------------|
| `customer`         | `GET`    | `/api/customers?shopId=1`                            | List customers for one shop                          |
| `pet`              | `GET`    | `/api/pets?shopId=1`                                 | List pets for one shop                               |
| `product`          | `GET`    | `/api/products?shopId=1`                             | List products for one shop; response includes `reviewAvg` and `totalReviews` |
| `product`          | `GET`    | `/api/public/products/mobile?size=20&cursor=<nextCursor>` | Public mobile infinite scroll, sorted by rating descending, max 20 records per request; accepts `X-Shop-Id` header or `shopId` query |
| `product`          | `GET`    | `/api/public/products?shopId=1&size=20` | Public product list by shop for product detail related sections |
| `product`          | `GET`    | `/api/public/products/mobile/{productId}` | Public product detail for mobile |
| `product`          | `GET`    | `/api/public/products/{productId}/reviews` | Public review list by product |
| `product`          | `GET`    | `/api/public/products/reviews?productId=1` | Public review list by query |
| `product`          | `GET`    | `/api/public/products/{productId}/related?size=10` | Public related products in same shop |
| `shop`             | `GET`    | `/api/public/shops/{shopId}` | Public shop info |
| `shop`             | `GET`    | `/api/public/shop/{shopId}` | Public shop info alias |
| `resource`         | `GET`    | `/api/resources?shopId=1`                            | List shop resources for one shop                     |
| `package`          | `GET`    | `/api/packages?shopId=1`                             | List service packages for one shop                   |
| `invoice`          | `GET`    | `/api/invoices?shopId=1`                             | List invoices for one shop                           |
| `payment`          | `GET`    | `/api/payments?shopId=1`                             | List payment intents for one shop                    |
| `conversation`     | `GET`    | `/api/conversations?shopId=1`                        | List conversations for one shop                      |
| `inventory`        | `GET`    | `/api/inventories?shopId=1`                          | List inventory rows for one shop                     |
| `inventory`        | `GET`    | `/api/inventories/{shopId}/{productId}`              | Get one composite-key row                            |
| `inventory`        | `POST`   | `/api/inventories`                                   | Create inventory row                                 |
| `inventory`        | `PUT`    | `/api/inventories/{shopId}/{productId}`              | Update inventory row                                 |
| `inventory`        | `DELETE` | `/api/inventories/{shopId}/{productId}`              | Delete inventory row                                 |
| `service_shop`     | `GET`    | `/api/services?shopId=1&size=10&cursor=<lastId>`      | Cursor-based service list for infinite scroll; required `shopId`; optional `search`, `categoryId`, and `active` filters |
| `shop`             | `GET`    | `/api/shops/{shopId}/staff`                           | List active staff members of a shop |
| `booking`          | `GET`    | `/api/bookings?shopId=1&size=10&cursor=<nextCursor>`  | Cursor-based booking list for infinite scroll; required `shopId`; optional `customerId`, `customerName`, `status`, and `source` filters |
| `order`            | `GET`    | `/api/orders?shopId=1&size=10&cursor=<lastId>`        | Cursor-based online order list for infinite scroll; required `shopId`; optional `customerId`, `status`, and `source` filters |
| `service_category` | `GET`    | `/api/shops/{shopId}/service-categories?active=true` | List service categories by shop and optional active flag |
| `service_category` | `GET`    | `/api/shops/{shopId}/service-categories/{id}`        | Get one category inside a shop                       |
| `service_category` | `POST`   | `/api/shops/{shopId}/service-categories`             | Create category; requires active owner/manager membership |
| `service_category` | `PUT`    | `/api/shops/{shopId}/service-categories/{id}`        | Update category; requires active owner/manager membership |
| `service_category` | `DELETE` | `/api/shops/{shopId}/service-categories/{id}`        | Soft-disable category by setting `active=false`      |
| `user`             | `GET`    | `/api/user/{id}`                                     | Get user DTO by id                                   |
| `shop`             | none     | `/api/shop`                                          | Prefix exists only                                   |
| `shopMember`       | none     | `/api/shop-member`                                   | Prefix exists only                                   |

### Service Infinite Scroll

Use `GET /api/services` for frontend infinite scroll:

```text
GET /api/services?shopId=1&size=10
GET /api/services?shopId=1&size=10&cursor=<nextCursor>
```

Required query params:

- `shopId`: limits services to one shop

Optional filters:

- `search`: trims and matches service name case-insensitively
- `categoryId`: limits services to one category
- `active`: pass `true` or `false`; omit it to include both active and inactive services

Response shape:

```json
{
  "content": [
    {
      "id": 4,
      "shopId": 1,
      "name": "hieuuu123123",
      "durationMin": 30,
      "basePrice": 122222,
      "categoryId": 8,
      "active": false
    }
  ],
  "size": 10,
  "nextCursor": 4,
  "hasNext": true
}
```

Frontend should append `content`, then call the next request with `cursor=nextCursor` only while `hasNext=true`. Reset `cursor` to empty when `search`, `shopId`, `categoryId`, or `active` changes.

### Booking Infinite Scroll

Use `GET /api/bookings` for frontend infinite scroll:

```text
GET /api/bookings?shopId=1&size=10
GET /api/bookings?shopId=1&size=10&cursor=<nextCursor>
```

Required query params:

- `shopId`: limits bookings to one shop

Optional filters:

- `customerId`: limits bookings to one customer
- `customerName`: trims and matches customer full name case-insensitively
- `status`: one of `DRAFT`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `REJECTED`, `CANCELLED`
- `source`: one of `STAFF`, `CUSTOMER`, `SYSTEM`

Default sort groups by status first: `DRAFT`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `REJECTED`, `CANCELLED`. Active statuses sort by appointment time ascending inside each status; closed statuses sort by appointment time descending inside each status.

Response shape:

```json
{
  "content": [
    {
      "id": 12,
      "bookingCode": "BKG-012",
      "shopId": 1,
      "customerId": 3,
      "customerName": "Do Thi Thanh",
      "customerPhone": "096778899",
      "items": [
        {
          "itemType": "SERVICE",
          "refId": 5,
          "name": "Tam co ban",
          "quantity": 1,
          "unitPrice": 120000,
          "amount": 120000
        }
      ],
      "totalAmount": 387000,
      "status": "CONFIRMED",
      "statusLabel": "Đã xác nhận",
      "source": "STAFF",
      "assigneeId": 1,
      "assigneeName": "Nguyen Van A",
      "time": "2026-04-20T09:00:00+07:00",
      "createdAt": "2026-04-20T08:30:00+07:00"
    }
  ],
  "size": 10,
  "nextCursor": 1,
  "hasNext": true
}
```

Frontend should append `content`, then call the next request with `cursor=nextCursor` only while `hasNext=true`. Reset `cursor` to empty when `shopId`, `customerId`, `customerName`, `status`, or `source` changes.
For bookings, `nextCursor` is an opaque page cursor returned by the API; do not treat it as a booking id.

Update only booking status:

```http
PUT /api/bookings/{id}/status
Content-Type: application/json

{ "status": "CONFIRMED" }
```

The endpoint also accepts `PUT /api/bookings/{id}/status?status=CONFIRMED`.

### Order Infinite Scroll

Use `GET /api/orders` for frontend online order infinite scroll:

```text
GET /api/orders?shopId=1&size=10
GET /api/orders?shopId=1&size=10&cursor=<nextCursor>
```

Required query params:

- `shopId`: limits orders to one shop

Optional filters:

- `customerId`: limits orders to one customer
- `status`: one of `PENDING`, `CONFIRMED`, `PACKING`, `SHIPPING`, `COMPLETED`, `CANCELLED`
- `source`: one of `ONLINE`, `STAFF`

Response shape:

```json
{
  "content": [
    {
      "id": 8,
      "orderCode": "ORD-008",
      "shopId": 1,
      "customerId": 3,
      "customerName": "Do Thi Thanh",
      "customerPhone": "096778899",
      "receiverName": "Do Thi Thanh",
      "receiverPhone": "096778899",
      "shippingAddress": "120 Vo Van Tan, Quan 3",
      "items": [
        {
          "id": 21,
          "shopId": 1,
          "orderId": 8,
          "productId": 4,
          "productName": "Hat dinh duong premium",
          "qty": 3,
          "unitPrice": 129000,
          "amount": 387000,
          "createdAt": "2026-04-20T08:30:00+07:00"
        }
      ],
      "totalAmount": 382000,
      "status": "COMPLETED",
      "statusLabel": "Hoan thanh",
      "source": "ONLINE",
      "createdAt": "2026-04-20T08:30:00+07:00"
    }
  ],
  "size": 10,
  "nextCursor": 8,
  "hasNext": true
}
```

Frontend should append `content`, then call the next request with `cursor=nextCursor` only while `hasNext=true`. Reset `cursor` to empty when `shopId`, `customerId`, `status`, or `source` changes.

### Status Codes

| Status                      | Meaning                                              |
|-----------------------------|------------------------------------------------------|
| `200 OK`                    | Standard successful read and update                  |
| `204 NO_CONTENT`            | Delete success in controllers that return empty body |
| `400 BAD_REQUEST`           | Generic `AppException`                               |
| `403 FORBIDDEN`             | `PermissionNotAllowedException`                      |
| `404 NOT_FOUND`             | `NotFoundException`                                  |
| `412 PRECONDITION_FAILED`   | Validation errors and `ValidateException`            |
| `500 INTERNAL_SERVER_ERROR` | Unhandled runtime errors                             |

### API Stability Notes

- `auth` is the most stable area
- Most new aggregate controllers are intentionally thin
- Advanced domain invariants are not guaranteed yet
- Do not silently change route contracts
- If a route contract changes, update README and notify frontend consumers in the same PR
- Frontend must send the selected/current `shopId` to every shop-owned list endpoint. Do not rely on backend defaults for shop scope.

## Build/Test

### Prerequisites

| Variable               | Required | Description                   |
|------------------------|----------|-------------------------------|
| `DB_URL`               | Yes      | PostgreSQL JDBC URL           |
| `DB_USER`              | Yes      | Database user                 |
| `DB_PASS`              | Yes      | Database password             |
| `JWT_SECRET`           | Yes      | JWT signing secret            |
| `SUPABASE_URL`         | Yes      | Supabase project URL          |
| `SUPABASE_SERVICE_KEY` | Yes      | Supabase service key          |
| `SUPABASE_BUCKET`      | Yes      | Bucket used for avatar upload |
| `DB_SCHEMA`            | Optional | Defaults to `prod`            |
| `PORT`                 | Optional | Defaults to `8080`            |

Other prerequisites:

- Java 17
- Maven Wrapper via `mvnw.cmd`
- Reachable PostgreSQL instance

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

| Check     | Status  | Notes                                        |
|-----------|---------|----------------------------------------------|
| `compile` | Passing | Project compiles with current mapped domains |
| `test`    | Failing | Spring bootstrap test package mismatch       |

Current `test` failure cause:

- `src/test/java/com/react/ProjectEXE101ApplicationTests.java` is in package `com.react`
- Main application package is `com.exe101`
- `@SpringBootTest` cannot locate `@SpringBootConfiguration`

### Test Reality

- Test coverage is still very low
- The only existing test is a context-load test and it is currently broken
- Before building real workflows, add service tests, controller integration tests, and repository tests where custom
  queries or composite keys matter

## Do & Don't

### Do

- Read Flyway migrations before changing entities or repositories
- Keep controllers thin and services explicit
- Keep DTOs and mappers aligned with exposed fields
- Add new migrations instead of mutating released migrations
- Re-check security rules before exposing new endpoints
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
- Do not treat `shop` or `shopMember` as complete modules without checking implementation first

## Known Gaps And Mismatches

| Area                 | Current mismatch                                                                       |
|----------------------|----------------------------------------------------------------------------------------|
| `CredentialProvider` | Java uses `LOCAL, GOOGLE, FACEBOOK`, while DB migration defines `LOCAL, GOOGLE, APPLE` |
| `logout-all`         | Controller still casts principal to `User`, while security uses `UserPrincipal`        |
| `shop`               | Controller prefix exists, but no real handlers                                         |
| `shopMember`         | Controller prefix exists, but no real handlers                                         |
| Aggregate services   | Many are still thin CRUD wrappers                                                      |
| Helper tables        | Several are mapped intentionally without public controllers                            |
| Maven metadata       | Still says `react`, while real application package is `com.exe101`                     |
| FE service tenant    | Frontend service module currently hard-codes `SHOP_ID = 1`                             |

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
