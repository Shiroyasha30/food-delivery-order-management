package com.fooddelivery.ordermanagement.owner;

import com.fooddelivery.ordermanagement.owner.dto.MenuItemRequest;
import com.fooddelivery.ordermanagement.owner.dto.MenuItemResponse;
import com.fooddelivery.ordermanagement.owner.dto.OwnerOrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OwnerService {

    public MenuItemResponse addMenuItem(String ownerId, Long restaurantId, MenuItemRequest request) {
        throw notImplemented();
    }

    public MenuItemResponse modifyMenuItem(String ownerId, Long restaurantId, Long itemId, MenuItemRequest request) {
        throw notImplemented();
    }

    public void deleteMenuItem(String ownerId, Long restaurantId, Long itemId) {
        throw notImplemented();
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

    private static ResponseStatusException notImplemented() {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
