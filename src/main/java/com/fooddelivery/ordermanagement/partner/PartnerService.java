package com.fooddelivery.ordermanagement.partner;

import com.fooddelivery.ordermanagement.customer.CustomerService;
import com.fooddelivery.ordermanagement.customer.dto.OrderResponse;
import com.fooddelivery.ordermanagement.domain.DeliveryPartnerProfileRepository;
import com.fooddelivery.ordermanagement.domain.OrderEntity;
import com.fooddelivery.ordermanagement.domain.OrderRepository;
import com.fooddelivery.ordermanagement.domain.OrderStatus;
import com.fooddelivery.ordermanagement.event.OrderStatusChangedEvent;
import com.fooddelivery.ordermanagement.partner.dto.AvailableOrderResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class PartnerService {

    private static final Set<OrderStatus> PARTNER_STATUSES = EnumSet.of(
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final DeliveryPartnerProfileRepository deliveryPartnerProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PartnerService(
            OrderRepository orderRepository,
            DeliveryPartnerProfileRepository deliveryPartnerProfileRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.deliveryPartnerProfileRepository = deliveryPartnerProfileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<AvailableOrderResponse> listAvailableOrders(String partnerUserId, Long cityId) {
        requirePartnerInCity(partnerUserId, cityId);
        return orderRepository.findAvailableForPartnerInCity(cityId).stream()
                .map(o -> new AvailableOrderResponse(o.getId(), o.getRestaurant().getId(), o.getStatus()))
                .toList();
    }

    @Transactional
    public OrderResponse acceptOrder(String partnerUserId, Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        requirePartnerInCity(partnerUserId, order.getRestaurant().getCity().getId());

        int claimed = orderRepository.claimOrder(orderId, partnerUserId);
        if (claimed != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already assigned or not available");
        }

        orderRepository.flush();
        OrderEntity claimedOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // Claiming does not change lifecycle status; remains PREPARING until partner updates.
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                claimedOrder.getId(),
                claimedOrder.getCustomerId(),
                claimedOrder.getRestaurant().getId(),
                claimedOrder.getDeliveryPartnerId(),
                claimedOrder.getStatus(),
                claimedOrder.getStatus()
        ));

        return CustomerService.toOrderResponse(claimedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String partnerUserId, Long orderId, OrderStatus status) {
        if (!PARTNER_STATUSES.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Partner may only set OUT_FOR_DELIVERY or DELIVERED"
            );
        }

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!partnerUserId.equals(order.getDeliveryPartnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order not assigned to this partner");
        }

        OrderStatus previous = order.getStatus();
        if (status == OrderStatus.OUT_FOR_DELIVERY && previous != OrderStatus.PREPARING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "OUT_FOR_DELIVERY requires PREPARING");
        }
        if (status == OrderStatus.DELIVERED && previous != OrderStatus.OUT_FOR_DELIVERY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DELIVERED requires OUT_FOR_DELIVERY");
        }

        order.setStatus(status);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurant().getId(),
                order.getDeliveryPartnerId(),
                previous,
                status
        ));
        return CustomerService.toOrderResponse(order);
    }

    private void requirePartnerInCity(String partnerUserId, Long cityId) {
        var profile = deliveryPartnerProfileRepository.findById(partnerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a delivery partner"));
        if (!profile.getCity().getId().equals(cityId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Partner not registered for this city");
        }
    }
}
