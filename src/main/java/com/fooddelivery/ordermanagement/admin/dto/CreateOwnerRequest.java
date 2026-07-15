package com.fooddelivery.ordermanagement.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOwnerRequest(
        @NotBlank String id,
        @NotBlank String displayName
) {
}
