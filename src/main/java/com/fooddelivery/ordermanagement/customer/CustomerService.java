package com.fooddelivery.ordermanagement.customer;

import com.fooddelivery.ordermanagement.customer.dto.CustomerMenuItemResponse;
import com.fooddelivery.ordermanagement.customer.dto.OrderResponse;
import com.fooddelivery.ordermanagement.customer.dto.PlaceOrderRequest;
import com.fooddelivery.ordermanagement.customer.dto.RateDeliveryRequest;
import com.fooddelivery.ordermanagement.customer.dto.RateItemRequest;
import com.fooddelivery.ordermanagement.customer.dto.RatingResponse;
import com.fooddelivery.ordermanagement.customer.dto.RestaurantSummaryResponse;
import com.fooddelivery.ordermanagement.domain.MenuItemRepository;
import com.fooddelivery.ordermanagement.domain.Restaurant;
import com.fooddelivery.ordermanagement.domain.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public CustomerService(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional(readOnly = true)
    public List<RestaurantSummaryResponse> getRestaurantsInCity(Long cityId) {
        return restaurantRepository.findByCityId(cityId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerMenuItemResponse> getMenu(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        return menuItemRepository.findByRestaurantId(restaurantId).stream()
                .map(item -> new CustomerMenuItemResponse(
                        item.getId(),
                        item.getName(),
                        item.getPrice(),
                        item.getStock()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantSummaryResponse> findRestaurantsByMenuItemName(Long cityId, String menuItemName) {
        return menuItemRepository.findRestaurantsByCityIdAndItemName(cityId, menuItemName.trim()).stream()
                .map(this::toSummary)
                .toList();
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

    private RestaurantSummaryResponse toSummary(Restaurant restaurant) {
        return new RestaurantSummaryResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCity().getId()
        );
    }

    private static ResponseStatusException notImplemented() {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
