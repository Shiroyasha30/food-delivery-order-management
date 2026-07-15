# Food Delivery Order Management

## Basic Project Description

A food delivery order management system at scale with multiple restaurants across cities, menu management per restaurant, customer order placement, the order lifecycle (placed → accepted → preparing → out-for-delivery → delivered), and delivery-partner assignment. Concurrent orders for the same menu item should not oversell limited stock, and partner assignment should handle multiple partners contending for the same order. Order placement must atomically reflect item stock, order state, and payment. Status updates should fan out asynchronously to customer, restaurant, and delivery partner without blocking the calling flow. Ratings and reviews after delivery should be supported.

### Roles

- **Admin** — manage cities, restaurants, and delivery partners
- **Restaurant owner** — manage menu and accept or reject orders
- **Customer** — browse, order, track, rate
- **Delivery partner** — accept assignments and update status
