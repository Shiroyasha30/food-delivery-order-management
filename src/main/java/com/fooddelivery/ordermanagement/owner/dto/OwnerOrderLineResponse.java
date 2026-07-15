package com.fooddelivery.ordermanagement.owner.dto;

import java.math.BigDecimal;

public record OwnerOrderLineResponse(Long menuItemId, String menuItemName, int quantity, BigDecimal unitPrice) {
}
