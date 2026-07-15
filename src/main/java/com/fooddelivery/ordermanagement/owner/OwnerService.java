package com.fooddelivery.ordermanagement.owner;

import com.fooddelivery.ordermanagement.domain.MenuItem;
import com.fooddelivery.ordermanagement.domain.MenuItemRepository;
import com.fooddelivery.ordermanagement.domain.Restaurant;
import com.fooddelivery.ordermanagement.domain.RestaurantRepository;
import com.fooddelivery.ordermanagement.owner.dto.MenuItemRequest;
import com.fooddelivery.ordermanagement.owner.dto.MenuItemResponse;
import com.fooddelivery.ordermanagement.owner.dto.OwnerOrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OwnerService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public OwnerService(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional
    public MenuItemResponse addMenuItem(String ownerId, Long restaurantId, MenuItemRequest request) {
        Restaurant restaurant = requireOwnedRestaurant(ownerId, restaurantId);
        MenuItem item = menuItemRepository.save(new MenuItem(
                restaurant,
                request.name().trim(),
                request.price(),
                request.stock()
        ));
        return toResponse(item);
    }

    @Transactional
    public MenuItemResponse modifyMenuItem(String ownerId, Long restaurantId, Long itemId, MenuItemRequest request) {
        requireOwnedRestaurant(ownerId, restaurantId);
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        item.setName(request.name().trim());
        item.setPrice(request.price());
        item.setStock(request.stock());
        return toResponse(item);
    }

    @Transactional
    public void deleteMenuItem(String ownerId, Long restaurantId, Long itemId) {
        requireOwnedRestaurant(ownerId, restaurantId);
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        menuItemRepository.delete(item);
    }

    public List<OwnerOrderResponse> listOrders(String ownerId, Long restaurantId) {
        throw notImplemented();
    }

    public OwnerOrderResponse acceptOrder(String ownerId, Long orderId) {
        throw notImplemented();
    }

    public OwnerOrderResponse rejectOrder(String ownerId, Long orderId) {
        throw notImplemented();
    }

    public OwnerOrderResponse markPreparing(String ownerId, Long orderId) {
        throw notImplemented();
    }

    private Restaurant requireOwnedRestaurant(String ownerId, Long restaurantId) {
        return restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not an owner of this restaurant"));
    }

    private static MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getRestaurant().getId(),
                item.getName(),
                item.getPrice(),
                item.getStock()
        );
    }

    private static ResponseStatusException notImplemented() {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
