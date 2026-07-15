package com.fooddelivery.ordermanagement.event;

import com.fooddelivery.ordermanagement.domain.OrderStatus;

public record OrderStatusChangedEvent(
        Long orderId,
        String customerId,
        Long restaurantId,
        String deliveryPartnerId,
        OrderStatus previousStatus,
        OrderStatus newStatus
) {
}
