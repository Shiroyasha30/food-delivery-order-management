package com.fooddelivery.ordermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRatingRepository extends JpaRepository<ItemRating, Long> {
    boolean existsByOrderIdAndMenuItemIdAndCustomerId(Long orderId, Long menuItemId, String customerId);
}
