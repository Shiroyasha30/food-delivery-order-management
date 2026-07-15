package com.fooddelivery.ordermanagement.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DeliveryPartnerNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryPartnerNotificationListener.class);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(OrderStatusChangedEvent event) {
        if (event.deliveryPartnerId() == null) {
            return;
        }
        log.info("Notify delivery partner {}: order {} status {} -> {}",
                event.deliveryPartnerId(), event.orderId(), event.previousStatus(), event.newStatus());
    }
}
