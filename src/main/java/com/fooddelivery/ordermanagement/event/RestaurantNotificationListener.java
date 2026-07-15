package com.fooddelivery.ordermanagement.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RestaurantNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(RestaurantNotificationListener.class);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Notify restaurant {}: new order {}", event.restaurantId(), event.orderId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(OrderStatusChangedEvent event) {
        log.info("Notify restaurant {}: order {} status {} -> {}",
                event.restaurantId(), event.orderId(), event.previousStatus(), event.newStatus());
    }
}
