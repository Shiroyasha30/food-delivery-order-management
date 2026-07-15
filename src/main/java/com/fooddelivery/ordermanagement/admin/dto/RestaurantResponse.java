package com.fooddelivery.ordermanagement.admin.dto;

import java.util.Set;

public record RestaurantResponse(Long id, Long cityId, String name, Set<String> ownerIds) {
}
