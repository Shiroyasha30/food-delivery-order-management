#!/usr/bin/env bash
# End-to-end smoke curls against a running local server.
# Requires: curl, jq
# Usage: BASE=http://localhost:8080 ./scripts/smoke-api.sh

set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
ADMIN_H=(-H "X-User-Id: admin-1" -H "Content-Type: application/json")
OWNER_H=(-H "X-User-Id: owner-1" -H "Content-Type: application/json")
CUSTOMER_H=(-H "X-User-Id: customer-1" -H "Content-Type: application/json")
PARTNER_H=(-H "X-User-Id: partner-1" -H "Content-Type: application/json")

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 1
fi

echo "== health =="
curl -sS "$BASE/health" | jq .

SUFFIX="$(date +%s)"
CITY_NAME="SmokeCity-$SUFFIX"
REST_NAME="SmokeRest-$SUFFIX"
ITEM_NAME="SmokeItem-$SUFFIX"
TEMP_ITEM_NAME="TempItem-$SUFFIX"
OWNER_ID="owner-smoke-$SUFFIX"
PARTNER_ID="partner-smoke-$SUFFIX"

echo "== admin: add owner =="
curl -sS -X POST "$BASE/api/v1/admin/owners" "${ADMIN_H[@]}" \
  -d "{\"id\":\"$OWNER_ID\",\"displayName\":\"Smoke Owner\"}" | jq .

echo "== admin: add city =="
CITY_ID="$(curl -sS -X POST "$BASE/api/v1/admin/cities" "${ADMIN_H[@]}" \
  -d "{\"name\":\"$CITY_NAME\"}" | tee /dev/stderr | jq -r '.id')"
echo "CITY_ID=$CITY_ID"

echo "== admin: add restaurant =="
REST_ID="$(curl -sS -X POST "$BASE/api/v1/admin/restaurants" "${ADMIN_H[@]}" \
  -d "{\"cityId\":$CITY_ID,\"name\":\"$REST_NAME\",\"ownerIds\":[\"$OWNER_ID\",\"owner-1\"]}" \
  | tee /dev/stderr | jq -r '.id')"
echo "REST_ID=$REST_ID"

echo "== admin: add delivery partner =="
curl -sS -X POST "$BASE/api/v1/admin/delivery-partners" "${ADMIN_H[@]}" \
  -d "{\"userId\":\"$PARTNER_ID\",\"displayName\":\"Smoke Rider\",\"cityId\":$CITY_ID}" | jq .

echo "== owner: add menu item =="
ITEM_ID="$(curl -sS -X POST "$BASE/api/v1/owner/restaurants/$REST_ID/menu-items" \
  -H "X-User-Id: $OWNER_ID" -H "Content-Type: application/json" \
  -d "{\"name\":\"$ITEM_NAME\",\"price\":150.00,\"stock\":10}" \
  | tee /dev/stderr | jq -r '.id')"
echo "ITEM_ID=$ITEM_ID"

echo "== owner: modify menu item =="
curl -sS -X PUT "$BASE/api/v1/owner/restaurants/$REST_ID/menu-items/$ITEM_ID" \
  -H "X-User-Id: $OWNER_ID" -H "Content-Type: application/json" \
  -d "{\"name\":\"$ITEM_NAME\",\"price\":145.00,\"stock\":9}" | jq .

echo "== owner: add + delete throwaway menu item =="
TEMP_ID="$(curl -sS -X POST "$BASE/api/v1/owner/restaurants/$REST_ID/menu-items" \
  -H "X-User-Id: $OWNER_ID" -H "Content-Type: application/json" \
  -d "{\"name\":\"$TEMP_ITEM_NAME\",\"price\":10,\"stock\":1}" | jq -r '.id')"
curl -sS -X DELETE "$BASE/api/v1/owner/restaurants/$REST_ID/menu-items/$TEMP_ID" \
  -H "X-User-Id: $OWNER_ID" -w "delete HTTP %{http_code}\n" -o /dev/null

echo "== customer: browse =="
curl -sS "$BASE/api/v1/customer/cities/$CITY_ID/restaurants" "${CUSTOMER_H[@]:0:1}" | jq .
curl -sS "$BASE/api/v1/customer/restaurants/$REST_ID/menu" -H "X-User-Id: customer-1" | jq .
curl -sS --get "$BASE/api/v1/customer/cities/$CITY_ID/restaurants/by-menu-item" \
  --data-urlencode "name=$ITEM_NAME" -H "X-User-Id: customer-1" | jq .

echo "== customer: place order =="
ORDER_ID="$(curl -sS -X POST "$BASE/api/v1/customer/restaurants/$REST_ID/orders" "${CUSTOMER_H[@]}" \
  -d "{\"items\":[{\"menuItemId\":$ITEM_ID,\"quantity\":2}]}" \
  | tee /dev/stderr | jq -r '.id')"
echo "ORDER_ID=$ORDER_ID"

echo "== customer: get status =="
curl -sS "$BASE/api/v1/customer/orders/$ORDER_ID" -H "X-User-Id: customer-1" | jq .

echo "== owner: list / accept / preparing =="
curl -sS "$BASE/api/v1/owner/restaurants/$REST_ID/orders" -H "X-User-Id: $OWNER_ID" | jq .
curl -sS -X POST "$BASE/api/v1/owner/orders/$ORDER_ID/accept" -H "X-User-Id: $OWNER_ID" | jq .
curl -sS -X POST "$BASE/api/v1/owner/orders/$ORDER_ID/preparing" -H "X-User-Id: $OWNER_ID" | jq .

echo "== partner: available / claim / status =="
curl -sS "$BASE/api/v1/partner/cities/$CITY_ID/orders/available" -H "X-User-Id: $PARTNER_ID" | jq .
curl -sS -X POST "$BASE/api/v1/partner/orders/$ORDER_ID/accept" -H "X-User-Id: $PARTNER_ID" | jq .
curl -sS -X POST "$BASE/api/v1/partner/orders/$ORDER_ID/status" \
  -H "X-User-Id: $PARTNER_ID" -H "Content-Type: application/json" \
  -d '{"status":"OUT_FOR_DELIVERY"}' | jq .
curl -sS -X POST "$BASE/api/v1/partner/orders/$ORDER_ID/status" \
  -H "X-User-Id: $PARTNER_ID" -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}' | jq .

echo "== customer: ratings =="
curl -sS -X POST "$BASE/api/v1/customer/orders/$ORDER_ID/ratings/items/$ITEM_ID" "${CUSTOMER_H[@]}" \
  -d '{"rating":5,"review":"Great"}' | jq .
curl -sS -X POST "$BASE/api/v1/customer/orders/$ORDER_ID/ratings/delivery" "${CUSTOMER_H[@]}" \
  -d '{"rating":4,"review":"Fast"}' | jq .

echo "== reject path (separate order) =="
REJECT_ORDER_ID="$(curl -sS -X POST "$BASE/api/v1/customer/restaurants/$REST_ID/orders" "${CUSTOMER_H[@]}" \
  -d "{\"items\":[{\"menuItemId\":$ITEM_ID,\"quantity\":1}]}" | jq -r '.id')"
curl -sS -X POST "$BASE/api/v1/owner/orders/$REJECT_ORDER_ID/reject" -H "X-User-Id: $OWNER_ID" | jq .

echo "Done. CITY_ID=$CITY_ID REST_ID=$REST_ID ITEM_ID=$ITEM_ID ORDER_ID=$ORDER_ID"
