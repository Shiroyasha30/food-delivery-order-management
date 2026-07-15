package com.fooddelivery.ordermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRatingRepository extends JpaRepository<DeliveryRating, Long> {
    boolean existsByOrderIdAndCustomerId(Long orderId, String customerId);
}
