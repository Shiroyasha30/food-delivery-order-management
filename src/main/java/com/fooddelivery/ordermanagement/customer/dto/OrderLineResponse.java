package com.fooddelivery.ordermanagement.customer.dto;

import java.math.BigDecimal;

public record OrderLineResponse(Long menuItemId, String menuItemName, int quantity, BigDecimal unitPrice) {
}
