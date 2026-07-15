package com.fooddelivery.ordermanagement.partner.dto;

import com.fooddelivery.ordermanagement.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
