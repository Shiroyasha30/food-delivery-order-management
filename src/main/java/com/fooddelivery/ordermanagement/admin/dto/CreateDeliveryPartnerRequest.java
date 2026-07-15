package com.fooddelivery.ordermanagement.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeliveryPartnerRequest(
        @NotBlank String userId,
        @NotBlank String displayName,
        @NotNull Long cityId
) {
}
