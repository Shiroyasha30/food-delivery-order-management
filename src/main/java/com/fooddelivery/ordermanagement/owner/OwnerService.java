package com.fooddelivery.ordermanagement.owner;

import com.fooddelivery.ordermanagement.domain.MenuItemRepository;
import com.fooddelivery.ordermanagement.domain.OrderEntity;
import com.fooddelivery.ordermanagement.domain.OrderLine;
import com.fooddelivery.ordermanagement.domain.OrderRepository;
import com.fooddelivery.ordermanagement.domain.OrderStatus;
import com.fooddelivery.ordermanagement.domain.Restaurant;
import com.fooddelivery.ordermanagement.domain.RestaurantRepository;
import com.fooddelivery.ordermanagement.event.OrderStatusChangedEvent;
import com.fooddelivery.ordermanagement.owner.dto.MenuItemRequest;
import com.fooddelivery.ordermanagement.owner.dto.MenuItemResponse;
import com.fooddelivery.ordermanagement.owner.dto.OwnerOrderLineResponse;
import com.fooddelivery.ordermanagement.owner.dto.OwnerOrderResponse;
import com.fooddelivery.ordermanagement.domain.MenuItem;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OwnerService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OwnerService(
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            OrderRepository orderRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
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
        return toMenuResponse(item);
    }

    @Transactional
    public MenuItemResponse modifyMenuItem(String ownerId, Long restaurantId, Long itemId, MenuItemRequest request) {
        requireOwnedRestaurant(ownerId, restaurantId);
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        item.setName(request.name().trim());
        item.setPrice(request.price());
        item.setStock(request.stock());
        return toMenuResponse(item);
    }

    @Transactional
    public void deleteMenuItem(String ownerId, Long restaurantId, Long itemId) {
        requireOwnedRestaurant(ownerId, restaurantId);
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        menuItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public List<OwnerOrderResponse> listOrders(String ownerId, Long restaurantId) {
        requireOwnedRestaurant(ownerId, restaurantId);
        return orderRepository.findByRestaurantIdOrderByIdDesc(restaurantId).stream()
                .map(this::toOwnerOrder)
                .toList();
    }

    @Transactional
    public OwnerOrderResponse acceptOrder(String ownerId, Long orderId) {
        return transitionOwnedOrder(ownerId, orderId, OrderStatus.PLACED, OrderStatus.ACCEPTED);
    }

    @Transactional
    public OwnerOrderResponse rejectOrder(String ownerId, Long orderId) {
        OrderEntity order = requireOwnedOrder(ownerId, orderId);
        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be rejected from status " + order.getStatus());
        }
        OrderStatus previous = order.getStatus();
        for (OrderLine line : order.getLines()) {
            MenuItem item = line.getMenuItem();
            item.setStock(item.getStock() + line.getQuantity());
        }
        order.setStatus(OrderStatus.REJECTED);
        publishStatusChange(order, previous, OrderStatus.REJECTED);
        return toOwnerOrder(order);
    }

    @Transactional
    public OwnerOrderResponse markPreparing(String ownerId, Long orderId) {
        return transitionOwnedOrder(ownerId, orderId, OrderStatus.ACCEPTED, OrderStatus.PREPARING);
    }

    private OwnerOrderResponse transitionOwnedOrder(
            String ownerId,
            Long orderId,
            OrderStatus expected,
            OrderStatus next
    ) {
        OrderEntity order = requireOwnedOrder(ownerId, orderId);
        if (order.getStatus() != expected) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Expected status " + expected + " but was " + order.getStatus()
            );
        }
        order.setStatus(next);
        publishStatusChange(order, expected, next);
        return toOwnerOrder(order);
    }

    private void publishStatusChange(OrderEntity order, OrderStatus previous, OrderStatus next) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurant().getId(),
                order.getDeliveryPartnerId(),
                previous,
                next
        ));
    }

    private OrderEntity requireOwnedOrder(String ownerId, Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        requireOwnedRestaurant(ownerId, order.getRestaurant().getId());
        return order;
    }

    private Restaurant requireOwnedRestaurant(String ownerId, Long restaurantId) {
        return restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not an owner of this restaurant"));
    }

    private static MenuItemResponse toMenuResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getRestaurant().getId(),
                item.getName(),
                item.getPrice(),
                item.getStock()
        );
    }

    private OwnerOrderResponse toOwnerOrder(OrderEntity order) {
        List<OwnerOrderLineResponse> lines = order.getLines().stream()
                .map(line -> new OwnerOrderLineResponse(
                        line.getMenuItem().getId(),
                        line.getMenuItemName(),
                        line.getQuantity(),
                        line.getUnitPrice()
                ))
                .toList();
        return new OwnerOrderResponse(order.getId(), order.getCustomerId(), order.getStatus(), lines);
    }
}
