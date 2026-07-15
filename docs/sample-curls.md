# Sample curls for API testing

Prerequisites:

```bash
mvn spring-boot:run
# BASE defaults to http://localhost:8080
export BASE=http://localhost:8080
```

Auth header on every protected call: `X-User-Id: <userId>`

Seeded users: `admin-1`, `owner-1`, `customer-1`, `partner-1`  
(Or create fresh owners/partners via admin APIs below.)

Replace path ids (`1`, `2`, …) with values returned from earlier calls.  
A scripted happy-path that captures ids is in [`scripts/smoke-api.sh`](../scripts/smoke-api.sh).

---

## Public

### Health

```bash
curl -sS "$BASE/health"
```

---

## Admin (`X-User-Id: admin-1`)

### Add city

```bash
curl -sS -X POST "$BASE/api/v1/admin/cities" \
  -H "X-User-Id: admin-1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Mumbai"}'
```

### Add restaurant owner

```bash
curl -sS -X POST "$BASE/api/v1/admin/owners" \
  -H "X-User-Id: admin-1" \
  -H "Content-Type: application/json" \
  -d '{"id":"owner-mumbai-1","displayName":"Mumbai Owner"}'
```

### Add restaurant (link existing owners)

```bash
curl -sS -X POST "$BASE/api/v1/admin/restaurants" \
  -H "X-User-Id: admin-1" \
  -H "Content-Type: application/json" \
  -d '{"cityId":1,"name":"Bombay Kitchen","ownerIds":["owner-1"]}'
```

### Add delivery partner

```bash
curl -sS -X POST "$BASE/api/v1/admin/delivery-partners" \
  -H "X-User-Id: admin-1" \
  -H "Content-Type: application/json" \
  -d '{"userId":"partner-1","displayName":"Rider One","cityId":1}'
```

---

## Owner (`X-User-Id: owner-1`)

### Add menu item

```bash
curl -sS -X POST "$BASE/api/v1/owner/restaurants/1/menu-items" \
  -H "X-User-Id: owner-1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Butter Chicken","price":320.00,"stock":20}'
```

### Modify menu item

```bash
curl -sS -X PUT "$BASE/api/v1/owner/restaurants/1/menu-items/1" \
  -H "X-User-Id: owner-1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Butter Chicken","price":299.00,"stock":18}'
```

### Delete menu item

```bash
curl -sS -X DELETE "$BASE/api/v1/owner/restaurants/1/menu-items/2" \
  -H "X-User-Id: owner-1" \
  -w "\nHTTP %{http_code}\n"
```

### List restaurant orders

```bash
curl -sS "$BASE/api/v1/owner/restaurants/1/orders" \
  -H "X-User-Id: owner-1"
```

### Accept order

```bash
curl -sS -X POST "$BASE/api/v1/owner/orders/1/accept" \
  -H "X-User-Id: owner-1"
```

### Reject order (restores stock)

```bash
curl -sS -X POST "$BASE/api/v1/owner/orders/1/reject" \
  -H "X-User-Id: owner-1"
```

### Mark preparing

```bash
curl -sS -X POST "$BASE/api/v1/owner/orders/1/preparing" \
  -H "X-User-Id: owner-1"
```

---

## Customer (`X-User-Id: customer-1`)

### Get restaurants in a city

```bash
curl -sS "$BASE/api/v1/customer/cities/1/restaurants" \
  -H "X-User-Id: customer-1"
```

### Get menu of a restaurant

```bash
curl -sS "$BASE/api/v1/customer/restaurants/1/menu" \
  -H "X-User-Id: customer-1"
```

### Get restaurants by menu item name (city-scoped)

```bash
curl -sS --get "$BASE/api/v1/customer/cities/1/restaurants/by-menu-item" \
  --data-urlencode "name=Butter Chicken" \
  -H "X-User-Id: customer-1"
```

### Place order

```bash
curl -sS -X POST "$BASE/api/v1/customer/restaurants/1/orders" \
  -H "X-User-Id: customer-1" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"menuItemId":1,"quantity":2}]}'
```

### Get order status

```bash
curl -sS "$BASE/api/v1/customer/orders/1" \
  -H "X-User-Id: customer-1"
```

### Rate item (only after DELIVERED)

```bash
curl -sS -X POST "$BASE/api/v1/customer/orders/1/ratings/items/1" \
  -H "X-User-Id: customer-1" \
  -H "Content-Type: application/json" \
  -d '{"rating":5,"review":"Excellent"}'
```

### Rate delivery (only after DELIVERED)

```bash
curl -sS -X POST "$BASE/api/v1/customer/orders/1/ratings/delivery" \
  -H "X-User-Id: customer-1" \
  -H "Content-Type: application/json" \
  -d '{"rating":4,"review":"On time"}'
```

---

## Delivery partner (`X-User-Id: partner-1`)

Partner must be registered for the city (`POST /admin/delivery-partners`).

### List available orders in city

```bash
curl -sS "$BASE/api/v1/partner/cities/1/orders/available" \
  -H "X-User-Id: partner-1"
```

### Accept / claim order

```bash
curl -sS -X POST "$BASE/api/v1/partner/orders/1/accept" \
  -H "X-User-Id: partner-1"
```

### Update status → OUT_FOR_DELIVERY

```bash
curl -sS -X POST "$BASE/api/v1/partner/orders/1/status" \
  -H "X-User-Id: partner-1" \
  -H "Content-Type: application/json" \
  -d '{"status":"OUT_FOR_DELIVERY"}'
```

### Update status → DELIVERED

```bash
curl -sS -X POST "$BASE/api/v1/partner/orders/1/status" \
  -H "X-User-Id: partner-1" \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}'
```

---

## Suggested happy-path order

1. Health  
2. Admin: city → restaurant (with `owner-1`) → delivery partner  
3. Owner: add menu item (optionally modify)  
4. Customer: restaurants → menu → by-menu-item → place order → get status  
5. Owner: list orders → accept → preparing  
6. Partner: available → accept → OUT_FOR_DELIVERY → DELIVERED  
7. Customer: rate item → rate delivery  

Use **reject** on a *separate* order if you want to exercise stock restore without breaking the deliver/rate path.  
Use **delete menu item** on a throwaway item (not the one on the live order).
