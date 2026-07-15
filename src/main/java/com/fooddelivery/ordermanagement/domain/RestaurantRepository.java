package com.fooddelivery.ordermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByCityId(Long cityId);

    @Query("select r from Restaurant r join fetch r.owners where r.id = :id")
    Optional<Restaurant> findByIdWithOwners(@Param("id") Long id);

    @Query("""
            select distinct r from Restaurant r
            join r.owners o
            where r.id = :restaurantId and o.id = :ownerId
            """)
    Optional<Restaurant> findByIdAndOwnerId(@Param("restaurantId") Long restaurantId, @Param("ownerId") String ownerId);
}
