package com.fooddelivery.ordermanagement.customer.dto;

import java.math.BigDecimal;

public record CustomerMenuItemResponse(Long id, String name, BigDecimal price, Integer stock) {
}
