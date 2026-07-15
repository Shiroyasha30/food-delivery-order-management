package com.fooddelivery.ordermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByRestaurantIdOrderByIdDesc(Long restaurantId);

    Optional<OrderEntity> findByIdAndCustomerId(Long id, String customerId);

    @Query("""
            select o from OrderEntity o
            join o.restaurant r
            where r.city.id = :cityId
              and o.status = com.fooddelivery.ordermanagement.domain.OrderStatus.PREPARING
              and o.deliveryPartnerId is null
            order by o.id asc
            """)
    List<OrderEntity> findAvailableForPartnerInCity(@Param("cityId") Long cityId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OrderEntity o
            set o.deliveryPartnerId = :partnerId
            where o.id = :orderId
              and o.deliveryPartnerId is null
              and o.status = com.fooddelivery.ordermanagement.domain.OrderStatus.PREPARING
            """)
    int claimOrder(@Param("orderId") Long orderId, @Param("partnerId") String partnerId);
}
