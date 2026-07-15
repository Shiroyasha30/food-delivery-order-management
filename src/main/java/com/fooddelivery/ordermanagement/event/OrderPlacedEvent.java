package com.fooddelivery.ordermanagement.event;

public record OrderPlacedEvent(
        Long orderId,
        String customerId,
        Long restaurantId
) {
}
