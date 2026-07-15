package com.fooddelivery.ordermanagement.customer;

import com.fooddelivery.ordermanagement.customer.dto.CustomerMenuItemResponse;
import com.fooddelivery.ordermanagement.customer.dto.OrderResponse;
import com.fooddelivery.ordermanagement.customer.dto.PlaceOrderRequest;
import com.fooddelivery.ordermanagement.customer.dto.RateDeliveryRequest;
import com.fooddelivery.ordermanagement.customer.dto.RateItemRequest;
import com.fooddelivery.ordermanagement.customer.dto.RatingResponse;
import com.fooddelivery.ordermanagement.customer.dto.RestaurantSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerService {

    public List<RestaurantSummaryResponse> getRestaurantsInCity(Long cityId) {
        throw notImplemented();
    }

    public List<CustomerMenuItemResponse> getMenu(Long restaurantId) {
        throw notImplemented();
    }

    public List<RestaurantSummaryResponse> findRestaurantsByMenuItemName(Long cityId, String menuItemName) {
        throw notImplemented();
    }

    public OrderResponse placeOrder(String customerId, Long restaurantId, PlaceOrderRequest request) {
        throw notImplemented();
    }

    public OrderResponse getOrderStatus(String customerId, Long orderId) {
        throw notImplemented();
    }

    public RatingResponse rateItem(String customerId, Long orderId, Long itemId, RateItemRequest request) {
        throw notImplemented();
    }

    public RatingResponse rateDelivery(String customerId, Long orderId, RateDeliveryRequest request) {
        throw notImplemented();
    }

    private static ResponseStatusException notImplemented() {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
