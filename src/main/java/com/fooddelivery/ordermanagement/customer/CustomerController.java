package com.fooddelivery.ordermanagement.customer;

import com.fooddelivery.ordermanagement.customer.dto.CustomerMenuItemResponse;
import com.fooddelivery.ordermanagement.customer.dto.OrderResponse;
import com.fooddelivery.ordermanagement.customer.dto.PlaceOrderRequest;
import com.fooddelivery.ordermanagement.customer.dto.RateDeliveryRequest;
import com.fooddelivery.ordermanagement.customer.dto.RateItemRequest;
import com.fooddelivery.ordermanagement.customer.dto.RatingResponse;
import com.fooddelivery.ordermanagement.customer.dto.RestaurantSummaryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer")
@PreAuthorize("hasAuthority('CUSTOMER')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/cities/{cityId}/restaurants")
    public List<RestaurantSummaryResponse> getRestaurantsInCity(@PathVariable Long cityId) {
        return customerService.getRestaurantsInCity(cityId);
    }

    @GetMapping("/restaurants/{restaurantId}/menu")
    public List<CustomerMenuItemResponse> getMenu(@PathVariable Long restaurantId) {
        return customerService.getMenu(restaurantId);
    }

    @GetMapping("/cities/{cityId}/restaurants/by-menu-item")
    public List<RestaurantSummaryResponse> getRestaurantsByMenuItemName(
            @PathVariable Long cityId,
            @RequestParam @NotBlank String name
    ) {
        return customerService.findRestaurantsByMenuItemName(cityId, name);
    }

    @PostMapping("/restaurants/{restaurantId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(
            Authentication authentication,
            @PathVariable Long restaurantId,
            @Valid @RequestBody PlaceOrderRequest request
    ) {
        return customerService.placeOrder(authentication.getName(), restaurantId, request);
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrderStatus(Authentication authentication, @PathVariable Long orderId) {
        return customerService.getOrderStatus(authentication.getName(), orderId);
    }

    @PostMapping("/orders/{orderId}/ratings/items/{itemId}")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse rateItem(
            Authentication authentication,
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody RateItemRequest request
    ) {
        return customerService.rateItem(authentication.getName(), orderId, itemId, request);
    }

    @PostMapping("/orders/{orderId}/ratings/delivery")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse rateDelivery(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody RateDeliveryRequest request
    ) {
        return customerService.rateDelivery(authentication.getName(), orderId, request);
    }
}
