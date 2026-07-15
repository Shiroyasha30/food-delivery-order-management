package com.fooddelivery.ordermanagement.owner.dto;

import java.math.BigDecimal;

public record MenuItemResponse(Long id, Long restaurantId, String name, BigDecimal price, Integer stock) {
}
