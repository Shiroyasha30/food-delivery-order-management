package com.fooddelivery.ordermanagement.customer;

import com.fooddelivery.ordermanagement.customer.dto.CustomerMenuItemResponse;
import com.fooddelivery.ordermanagement.customer.dto.OrderLineResponse;
import com.fooddelivery.ordermanagement.customer.dto.OrderResponse;
import com.fooddelivery.ordermanagement.customer.dto.PlaceOrderRequest;
import com.fooddelivery.ordermanagement.customer.dto.RateDeliveryRequest;
import com.fooddelivery.ordermanagement.customer.dto.RateItemRequest;
import com.fooddelivery.ordermanagement.customer.dto.RatingResponse;
import com.fooddelivery.ordermanagement.customer.dto.RestaurantSummaryResponse;
import com.fooddelivery.ordermanagement.domain.DeliveryRating;
import com.fooddelivery.ordermanagement.domain.DeliveryRatingRepository;
import com.fooddelivery.ordermanagement.domain.ItemRating;
import com.fooddelivery.ordermanagement.domain.ItemRatingRepository;
import com.fooddelivery.ordermanagement.domain.MenuItem;
import com.fooddelivery.ordermanagement.domain.MenuItemRepository;
import com.fooddelivery.ordermanagement.domain.OrderEntity;
import com.fooddelivery.ordermanagement.domain.OrderLine;
import com.fooddelivery.ordermanagement.domain.OrderRepository;
import com.fooddelivery.ordermanagement.domain.OrderStatus;
import com.fooddelivery.ordermanagement.domain.PaymentStatus;
import com.fooddelivery.ordermanagement.domain.Restaurant;
import com.fooddelivery.ordermanagement.domain.RestaurantRepository;
import com.fooddelivery.ordermanagement.event.OrderPlacedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final ItemRatingRepository itemRatingRepository;
    private final DeliveryRatingRepository deliveryRatingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CustomerService(
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            OrderRepository orderRepository,
            ItemRatingRepository itemRatingRepository,
            DeliveryRatingRepository deliveryRatingRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.itemRatingRepository = itemRatingRepository;
        this.deliveryRatingRepository = deliveryRatingRepository;
        this.eventPublisher = eventPublisher;
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

    @Transactional
    public OrderResponse placeOrder(String customerId, Long restaurantId, PlaceOrderRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        List<PreparedLine> prepared = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (PlaceOrderRequest.OrderLineRequest lineReq : request.items()) {
            MenuItem item = menuItemRepository.findByIdAndRestaurantId(lineReq.menuItemId(), restaurantId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Menu item not found for restaurant: " + lineReq.menuItemId()
                    ));
            int updated = menuItemRepository.decrementStockIfAvailable(item.getId(), lineReq.quantity());
            if (updated != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for item: " + item.getName()
                );
            }
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(lineReq.quantity()));
            total = total.add(lineTotal);
            prepared.add(new PreparedLine(item, lineReq.quantity(), item.getPrice()));
        }

        // Stub payment: always success; persisted atomically with order + stock.
        OrderEntity order = new OrderEntity(
                customerId,
                restaurant,
                OrderStatus.PLACED,
                PaymentStatus.SUCCESS,
                total
        );
        for (PreparedLine line : prepared) {
            order.addLine(new OrderLine(line.item(), line.item().getName(), line.quantity(), line.unitPrice()));
        }
        order = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPlacedEvent(order.getId(), customerId, restaurant.getId()));
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderStatus(String customerId, Long orderId) {
        OrderEntity order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return toOrderResponse(order);
    }

    @Transactional
    public RatingResponse rateItem(String customerId, Long orderId, Long itemId, RateItemRequest request) {
        OrderEntity order = requireDeliveredOrder(customerId, orderId);
        boolean inOrder = order.getLines().stream().anyMatch(line -> line.getMenuItem().getId().equals(itemId));
        if (!inOrder) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item not part of this order");
        }
        if (itemRatingRepository.existsByOrderIdAndMenuItemIdAndCustomerId(orderId, itemId, customerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item already rated for this order");
        }
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        ItemRating rating = itemRatingRepository.save(new ItemRating(
                order,
                item,
                customerId,
                request.rating(),
                request.review()
        ));
        return new RatingResponse(rating.getId(), rating.getRating(), rating.getReview());
    }

    @Transactional
    public RatingResponse rateDelivery(String customerId, Long orderId, RateDeliveryRequest request) {
        OrderEntity order = requireDeliveredOrder(customerId, orderId);
        if (deliveryRatingRepository.existsByOrderIdAndCustomerId(orderId, customerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delivery already rated for this order");
        }
        DeliveryRating rating = deliveryRatingRepository.save(new DeliveryRating(
                order,
                customerId,
                order.getDeliveryPartnerId(),
                request.rating(),
                request.review()
        ));
        return new RatingResponse(rating.getId(), rating.getRating(), rating.getReview());
    }

    private OrderEntity requireDeliveredOrder(String customerId, Long orderId) {
        OrderEntity order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ratings allowed only after delivery");
        }
        return order;
    }

    public static OrderResponse toOrderResponse(OrderEntity order) {
        List<OrderLineResponse> lines = order.getLines().stream()
                .map(line -> new OrderLineResponse(
                        line.getMenuItem().getId(),
                        line.getMenuItemName(),
                        line.getQuantity(),
                        line.getUnitPrice()
                ))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getRestaurant().getId(),
                order.getStatus(),
                order.getPaymentStatus().name(),
                order.getDeliveryPartnerId(),
                lines,
                order.getTotalAmount()
        );
    }

    private RestaurantSummaryResponse toSummary(Restaurant restaurant) {
        return new RestaurantSummaryResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCity().getId()
        );
    }


    private record PreparedLine(MenuItem item, int quantity, BigDecimal unitPrice) {
    }
}
