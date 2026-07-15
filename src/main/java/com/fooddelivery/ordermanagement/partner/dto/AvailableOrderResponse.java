package com.fooddelivery.ordermanagement.partner.dto;

import com.fooddelivery.ordermanagement.domain.OrderStatus;

public record AvailableOrderResponse(Long orderId, Long restaurantId, OrderStatus status) {
}
