package com.fooddelivery.ordermanagement.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCityRequest(@NotBlank String name) {
}
