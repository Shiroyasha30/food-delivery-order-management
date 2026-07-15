# AGENTS.md — food-delivery-order-management

Guidance for AI agents and developers working in this repository.

## Maintain this file

Keep `AGENTS.md` current as the project evolves. When you change architecture, conventions, stack choices, workflows, or environment setup, update this file in the same change (or immediately after). Do not let it drift.

## Project

Spring Boot food delivery **order management** system: multi-city restaurants, per-restaurant menus, order lifecycle, concurrent stock safety, delivery-partner assignment under contention, atomic order + stock + payment, async status fan-out, and post-delivery ratings/reviews.

Roles: **admin**, **restaurant owner**, **customer**, **delivery partner**.

See `README.md` for the product description.

## Commit messages

Keep commit messages **concise** and on a **single line** (no multi-paragraph or multi-line bodies unless the user explicitly asks otherwise).

## Stack (expected)

- Java / Spring Boot / Maven
- Layered structure: controllers, services, repositories, models/entities, configuration
- JUnit 5 for tests; MockMvc / `@SpringBootTest` as appropriate

Prefer constructor injection, Bean Validation, `@ControllerAdvice` for API errors, and SLF4J logging. Match existing patterns once code exists.

## Agent skills (project)

Use these under `.cursor/skills/`:

| Skill | When |
|-------|------|
| `development` | Before any feature: write plan, get user approval, then implement; never commit the plan |
| `validate-changes` | After code changes, before commit: run integration tests; on failure diagnose and get user verification before fixing |
| `commit-changes` | When committing: small single-line message after validation |

Implementation plans live under `.development-plans/` (gitignored).

## Development norms

- Prefer small, focused changes; match repository style once scaffolding lands.
- Do not commit secrets (`.env`, credentials, tokens).
- Only create commits when the user explicitly asks.
- Do not push unless the user asks.
- Update `README.md` when setup steps or product scope change materially.
- Update this `AGENTS.md` when agent/dev conventions change.

## Concurrent / domain constraints (product)

Agents implementing features must respect:

1. **Stock** — concurrent orders for the same menu item must not oversell limited stock.
2. **Partner assignment** — multiple partners contending for the same order must be handled safely.
3. **Order placement** — atomically reflect item stock, order state, and payment.
4. **Status updates** — fan out asynchronously to customer, restaurant, and delivery partner without blocking the calling flow.
5. **Ratings/reviews** — supported after delivery.

## Order lifecycle

`placed → accepted → preparing → out-for-delivery → delivered` (plus reject/cancel paths as implemented).
