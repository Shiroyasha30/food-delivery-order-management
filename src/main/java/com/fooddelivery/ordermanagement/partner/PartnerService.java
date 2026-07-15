package com.fooddelivery.ordermanagement.partner;

import com.fooddelivery.ordermanagement.customer.dto.OrderResponse;
import com.fooddelivery.ordermanagement.domain.OrderStatus;
import com.fooddelivery.ordermanagement.partner.dto.AvailableOrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PartnerService {

    public List<AvailableOrderResponse> listAvailableOrders(String partnerUserId, Long cityId) {
        throw notImplemented();
    }

    public OrderResponse acceptOrder(String partnerUserId, Long orderId) {
        throw notImplemented();
    }

    public OrderResponse updateOrderStatus(String partnerUserId, Long orderId, OrderStatus status) {
        throw notImplemented();
    }

    private static ResponseStatusException notImplemented() {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
