package com.fooddelivery.ordermanagement.owner;

import com.fooddelivery.ordermanagement.owner.dto.MenuItemRequest;
import com.fooddelivery.ordermanagement.owner.dto.MenuItemResponse;
import com.fooddelivery.ordermanagement.owner.dto.OwnerOrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner")
@PreAuthorize("hasAuthority('RESTAURANT_OWNER')")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse addMenuItem(
            Authentication authentication,
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuItemRequest request
    ) {
        return ownerService.addMenuItem(authentication.getName(), restaurantId, request);
    }

    @PutMapping("/restaurants/{restaurantId}/menu-items/{itemId}")
    public MenuItemResponse modifyMenuItem(
            Authentication authentication,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @Valid @RequestBody MenuItemRequest request
    ) {
        return ownerService.modifyMenuItem(authentication.getName(), restaurantId, itemId, request);
    }

    @DeleteMapping("/restaurants/{restaurantId}/menu-items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMenuItem(
            Authentication authentication,
            @PathVariable Long restaurantId,
            @PathVariable Long itemId
    ) {
        ownerService.deleteMenuItem(authentication.getName(), restaurantId, itemId);
    }

    @GetMapping("/restaurants/{restaurantId}/orders")
    public List<OwnerOrderResponse> listOrders(
            Authentication authentication,
            @PathVariable Long restaurantId
    ) {
        return ownerService.listOrders(authentication.getName(), restaurantId);
    }

    @PostMapping("/orders/{orderId}/accept")
    public OwnerOrderResponse acceptOrder(Authentication authentication, @PathVariable Long orderId) {
        return ownerService.acceptOrder(authentication.getName(), orderId);
    }

    @PostMapping("/orders/{orderId}/reject")
    public OwnerOrderResponse rejectOrder(Authentication authentication, @PathVariable Long orderId) {
        return ownerService.rejectOrder(authentication.getName(), orderId);
    }

    @PostMapping("/orders/{orderId}/preparing")
    public OwnerOrderResponse markPreparing(Authentication authentication, @PathVariable Long orderId) {
        return ownerService.markPreparing(authentication.getName(), orderId);
    }
}
