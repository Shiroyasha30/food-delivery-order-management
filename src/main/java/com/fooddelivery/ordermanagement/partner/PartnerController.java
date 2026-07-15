package com.fooddelivery.ordermanagement.partner;

import com.fooddelivery.ordermanagement.customer.dto.OrderResponse;
import com.fooddelivery.ordermanagement.partner.dto.AvailableOrderResponse;
import com.fooddelivery.ordermanagement.partner.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partner")
@PreAuthorize("hasAuthority('DELIVERY_PARTNER')")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping("/cities/{cityId}/orders/available")
    public List<AvailableOrderResponse> listAvailable(
            Authentication authentication,
            @PathVariable Long cityId
    ) {
        return partnerService.listAvailableOrders(authentication.getName(), cityId);
    }

    @PostMapping("/orders/{orderId}/accept")
    public OrderResponse acceptOrder(Authentication authentication, @PathVariable Long orderId) {
        return partnerService.acceptOrder(authentication.getName(), orderId);
    }

    @PostMapping("/orders/{orderId}/status")
    public OrderResponse updateStatus(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return partnerService.updateOrderStatus(authentication.getName(), orderId, request.status());
    }
}
