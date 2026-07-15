package com.fooddelivery.ordermanagement.owner.dto;

import com.fooddelivery.ordermanagement.domain.OrderStatus;

import java.util.List;

public record OwnerOrderResponse(
        Long id,
        String customerId,
        OrderStatus status,
        List<OwnerOrderLineResponse> lines
) {
}
