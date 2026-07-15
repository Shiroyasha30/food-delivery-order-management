# Food Delivery Order Management

## Basic Project Description

A food delivery order management system at scale with multiple restaurants across cities, menu management per restaurant, customer order placement, the order lifecycle (placed → accepted → preparing → out-for-delivery → delivered), and delivery-partner assignment. Concurrent orders for the same menu item should not oversell limited stock, and partner assignment should handle multiple partners contending for the same order. Order placement must atomically reflect item stock, order state, and payment. Status updates should fan out asynchronously to customer, restaurant, and delivery partner without blocking the calling flow. Ratings and reviews after delivery should be supported.

### Roles

- **Admin** — manage cities, restaurants, and delivery partners
- **Restaurant owner** — manage menu and accept or reject orders
- **Customer** — browse, order, track, rate
- **Delivery partner** — accept assignments and update status

## Stack

- Java 17 / Spring Boot 3.3 / Maven
- Spring Security (header auth), Spring Data JPA
- SQLite (`./data/food-delivery.db`)
- Async fan-out via Spring `ApplicationEvent` + `@Async` listeners

## Run locally

```bash
mvn spring-boot:run
```

- App: `http://localhost:8080`
- Health (public): `GET /health`
- DB file created under `data/` on startup

### Auth

Send a known user id on every protected request:

```http
X-User-Id: admin-1
```

Seeded users (from `DataSeeder`):

| User id | Role |
|---------|------|
| `admin-1` | ADMIN |
| `owner-1` | RESTAURANT_OWNER |
| `customer-1` | CUSTOMER |
| `partner-1` | DELIVERY_PARTNER |

Admin can also create owners and delivery partners via API.

## Order lifecycle

```
PLACED → ACCEPTED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
                 ↘ REJECTED
```

- Owner: accept / reject / preparing  
- Partner (after claim on `PREPARING`): `OUT_FOR_DELIVERY` → `DELIVERED`  
- Ratings allowed only after `DELIVERED`

Status changes publish `OrderPlacedEvent` / `OrderStatusChangedEvent` after commit; async listeners log notifications for customer, restaurant, and delivery partner.

## API reference (`/api/v1`)

### Admin (`ADMIN`)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/admin/cities` | Add city `{"name"}` |
| POST | `/admin/owners` | Add restaurant owner user `{"id","displayName"}` |
| POST | `/admin/restaurants` | Add restaurant `{"cityId","name","ownerIds":[]}` (owners must exist; M2M) |
| POST | `/admin/delivery-partners` | Add partner `{"userId","displayName","cityId"}` |

### Owner (`RESTAURANT_OWNER`)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/owner/restaurants/{restaurantId}/menu-items` | Add item `{"name","price","stock"}` |
| PUT | `/owner/restaurants/{restaurantId}/menu-items/{itemId}` | Modify item |
| DELETE | `/owner/restaurants/{restaurantId}/menu-items/{itemId}` | Delete item |
| GET | `/owner/restaurants/{restaurantId}/orders` | List restaurant orders |
| POST | `/owner/orders/{orderId}/accept` | `PLACED` → `ACCEPTED` |
| POST | `/owner/orders/{orderId}/reject` | Reject + restore stock |
| POST | `/owner/orders/{orderId}/preparing` | `ACCEPTED` → `PREPARING` |

### Customer (`CUSTOMER`)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/customer/cities/{cityId}/restaurants` | Restaurants in city |
| GET | `/customer/restaurants/{restaurantId}/menu` | Menu |
| GET | `/customer/cities/{cityId}/restaurants/by-menu-item?name=` | Restaurants by menu item name |
| POST | `/customer/restaurants/{restaurantId}/orders` | Place order `{"items":[{"menuItemId","quantity"}]}` |
| GET | `/customer/orders/{orderId}` | Order status |
| POST | `/customer/orders/{orderId}/ratings/items/{itemId}` | Rate item `{"rating","review"}` |
| POST | `/customer/orders/{orderId}/ratings/delivery` | Rate delivery |

### Delivery partner (`DELIVERY_PARTNER`)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/partner/cities/{cityId}/orders/available` | Unassigned `PREPARING` orders |
| POST | `/partner/orders/{orderId}/accept` | Claim order (contention-safe) |
| POST | `/partner/orders/{orderId}/status` | `{"status":"OUT_FOR_DELIVERY"|"DELIVERED"}` |

## Example flow

```bash
# 1) City + restaurant (owner-1 already seeded)
curl -s -X POST http://localhost:8080/api/v1/admin/cities \
  -H "X-User-Id: admin-1" -H "Content-Type: application/json" \
  -d '{"name":"Pune"}'

curl -s -X POST http://localhost:8080/api/v1/admin/restaurants \
  -H "X-User-Id: admin-1" -H "Content-Type: application/json" \
  -d '{"cityId":1,"name":"Spice Hub","ownerIds":["owner-1"]}'

# 2) Menu
curl -s -X POST http://localhost:8080/api/v1/owner/restaurants/1/menu-items \
  -H "X-User-Id: owner-1" -H "Content-Type: application/json" \
  -d '{"name":"Paneer Wrap","price":149.00,"stock":10}'

# 3) Partner for city
curl -s -X POST http://localhost:8080/api/v1/admin/delivery-partners \
  -H "X-User-Id: admin-1" -H "Content-Type: application/json" \
  -d '{"userId":"partner-1","displayName":"Rider One","cityId":1}'

# 4) Place order
curl -s -X POST http://localhost:8080/api/v1/customer/restaurants/1/orders \
  -H "X-User-Id: customer-1" -H "Content-Type: application/json" \
  -d '{"items":[{"menuItemId":1,"quantity":2}]}'
```

Then owner accept → preparing; partner accept → out-for-delivery → delivered; customer rate.

## Tests

```bash
mvn verify
```

Integration suites:

| Class | Focus |
|-------|--------|
| `OrderLifecycleFlowIT` | Full happy-path lifecycle |
| `OrderEdgeCasesIT` | Auth denials, reject/stock, ratings gates |
| `ConcurrencyGuardsIT` | Stock oversell + partner claim races |
| `AuthorizationAndMultiItemIT` | Isolation, multi-item order, case-insensitive search |
| `DummyIT` | Context-load smoke |

## Repo

https://github.com/Shiroyasha30/food-delivery-order-management
