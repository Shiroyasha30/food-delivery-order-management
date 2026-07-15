package com.fooddelivery.ordermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByRestaurantId(Long restaurantId);

    Optional<MenuItem> findByIdAndRestaurantId(Long id, Long restaurantId);

    @Query("""
            select distinct m.restaurant from MenuItem m
            where m.restaurant.city.id = :cityId
              and lower(m.name) = lower(:name)
            """)
    List<Restaurant> findRestaurantsByCityIdAndItemName(
            @Param("cityId") Long cityId,
            @Param("name") String name
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MenuItem m
            set m.stock = m.stock - :qty
            where m.id = :id and m.stock >= :qty
            """)
    int decrementStockIfAvailable(@Param("id") Long id, @Param("qty") int qty);
}
