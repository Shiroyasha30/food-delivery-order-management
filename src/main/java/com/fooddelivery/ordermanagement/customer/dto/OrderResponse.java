package com.fooddelivery.ordermanagement.customer.dto;

import com.fooddelivery.ordermanagement.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long id,
        Long restaurantId,
        OrderStatus status,
        String paymentStatus,
        String deliveryPartnerId,
        List<OrderLineResponse> lines,
        BigDecimal totalAmount
) {
}
