package com.fooddelivery.ordermanagement.customer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty List<@Valid OrderLineRequest> items
) {
    public record OrderLineRequest(
            @NotNull Long menuItemId,
            @NotNull @Min(1) Integer quantity
    ) {
    }
}
