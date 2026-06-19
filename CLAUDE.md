# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
./gradlew build

# Run application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.ohsooo.platform.ohsooshoppingmall.<ClassName>"
```

## Tech Stack

- **Java 17**, Spring Boot 3.2.5, Gradle
- **Database**: PostgreSQL (localhost:5432, DB: `ohsoo_shoppingmall`), schema managed by **Flyway**
- 🚨 **Flyway Automation Rule**: NEVER automatically create or modify `V__*.sql` migration files when updating Entities. Always ask the user for confirmation or wait for explicit instructions.
- **Cache/Token Store**: Redis (localhost:6380)
- **Auth**: JWT (access token 30 min, refresh token 14 days in HttpOnly cookie stored in Redis) + OAuth2 (Google, Naver, Kakao)
- **Payment PG**: Toss Payments
- **API Docs**: SpringDoc OpenAPI — Swagger UI at `/swagger-ui.html`
- **ORM**: Spring Data JPA, `ddl-auto: validate` (Flyway owns schema)

## Required Environment Variables

```
JWT_SECRET, OAUTH_REDIRECT_URI
MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM
GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
NAVER_CLIENT_ID, NAVER_CLIENT_SECRET
KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET
TOSS_SECRET_KEY
```

Configure in `.env` (already gitignored).

## Architecture

### Package Layout

All application code lives under `com.ohsooo.platform.ohsooshoppingmall`. The structure is domain-driven:

```
domain/
  identity/
    auth/   — AuthIdentity entity, JWT-based login, email verification, OAuth2 callback
    user/   — User profile (User entity is separate from AuthIdentity)
  catalog/  — Item, Category, Option, ItemVariant, ItemVariantOption (join table)
  store/    — Store (owned by a User with OWNER role)
  cart/     — Cart, CartItem
  order/    — Order, OrderItem, OrderItemHistory (status audit trail)
  payment/  — Payment, PaymentEvent, Refund (PG provider abstraction)
  inventory/— Inventory entity (stock separate from ItemVariant)
  pricing/  — PricingService (calculates order totals; no entity)
global/
  exception/— BusinessException, BaseErrorCode interface, GlobalExceptionHandler
  jwt/      — JwtProvider, JwtAuthenticationFilter, RefreshTokenStore (Redis)
  oauth/    — OAuth2SuccessHandler
  response/ — ApiResponse<T>, BaseResponse<T>
  mail/     — EmailSender interface + SmtpEmailSender
  cors/, config/, redis/
```

Each domain follows the layered pattern: `controller → service → repository → entity`, with `dto/`, `mapper/`, and `exception/` alongside.

### Key Design Decisions

**Identity split**: `AuthIdentity` holds credentials (password hash, OAuth provider) while `User` holds profile data (name, address, role). They are linked but separate JPA entities.

**Error handling**: All business errors throw `BusinessException(BaseErrorCode)`. Each domain has its own `*ErrorCode` enum implementing `BaseErrorCode`. `GlobalExceptionHandler` converts these to `ApiResponse.error(code, message)`.

**Two response wrappers exist** (inconsistency in the codebase):
- `ApiResponse<T>` — `{success, data, errorCode, message}` — used in auth, payment, most controllers
- `BaseResponse<T>` — `{success, code, message, data}` — used in inventory controller

**Order creation sources**: `OrderService.createOrder` switches on `request.source`:
- `CART_ALL` — entire cart
- `CART_SELECTED` — specific `cartItemIds`
- `DIRECT` — `items` list of variant + quantity

**Payment confirm flow** (no `@Transactional` on the outer method intentionally):
1. TX1: Acquire pessimistic lock on Payment → mark `CONFIRMING` → commit → release DB connection
2. Call Toss PG API (outside any transaction — avoids holding DB connection during network I/O)
3. TX2: On success → mark `CAPTURED`, update Order to `PAID`, deduct inventory
4. TX3: On failure → mark `FAILED`

**Inventory stock safety**: `decreaseStock` uses an atomic `UPDATE … WHERE quantity >= amount` query (returns 0 rows on insufficient stock). `adjustStock` uses a pessimistic lock. Both sync `ItemVariant.status` (ACTIVE / OUT_OF_STOCK) after the change.

**Flyway migrations** live in `src/main/resources/db/migration/`.
- **Automation Rule**: Do not generate migration files automatically during iterative local development. Wait for the user to explicitly ask for a consolidated migration file.
- **Naming Convention**: New migrations must strictly follow this pattern: `V<Version>__<domain>_<action>.sql` (Note the double underscore `__`).
    - *Examples:*
        - `V10__member_create_table.sql`
        - `V11__member_add_email_column.sql`
        - `V12__diary_create_table.sql`
        - `V13__diary_add_mood_column.sql`
        - `V14__auth_create_refresh_token_table.sql`

**Mappers** are plain Spring-managed classes (`@Component`), not MapStruct.

**WebSocket** dependency (STOMP + SockJS) is declared in `build.gradle` but not yet implemented.

## Git Commit Guidelines

- **No Direct Push**: NEVER push changes directly to remote repositories.
- **Commit Message Generation**: When a task is complete, recommend a commit message to the user following this strict format. Do not execute the commit yourself unless explicitly instructed.

### Format

```
:<gitmoji_text>: <Prefix>: <Commit Message>
```

*The first letter of the Prefix must always be capitalized (e.g., Feat, Chore, Fix).*

### Gitmoji & Prefix Reference Table
| Gitmoji | Text Code | Prefix | Meaning |
| :--- | :--- | :--- | :--- |
| ✨ | `:sparkles:` | `Feat` | New features |
| 🐛 | `:bug:` | `Fix` | Bug fixes |
| ♻️ | `:recycle:` | `Refactor`| Code refactoring |
| 📝 | `:memo:` | `Docs` | Documentation changes |
| 🎨 | `:art:` | `Style` | Code style/formatting changes |
| 🔥 | `:fire:` | `Remove` | Removing code or files |
| 🚀 | `:rocket:` | `Perf` | Performance improvements |
| 🔧 | `:wrench:` | `Chore` | Build tasks, config updates, dependencies |
| 🧪 | `:test_tube:` | `Test` | Adding or updating tests |
| 🚑 | `:ambulance:` | `Hotfix` | Critical hotfixes |
| 🔀 | `:twisted_rightwards_arrows:` | `Merge` | Merging branches |

*Example Recommendation:*
`:sparkles: Feat: Add JWT token reissue endpoint`

