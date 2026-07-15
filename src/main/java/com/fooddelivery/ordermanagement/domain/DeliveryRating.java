package com.fooddelivery.ordermanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "delivery_ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "customer_id"})
)
public class DeliveryRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(nullable = false, length = 64)
    private String customerId;

    @Column(length = 64)
    private String deliveryPartnerId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String review;

    protected DeliveryRating() {
    }

    public DeliveryRating(
            OrderEntity order,
            String customerId,
            String deliveryPartnerId,
            Integer rating,
            String review
    ) {
        this.order = order;
        this.customerId = customerId;
        this.deliveryPartnerId = deliveryPartnerId;
        this.rating = rating;
        this.review = review;
    }

    public Long getId() {
        return id;
    }

    public Integer getRating() {
        return rating;
    }

    public String getReview() {
        return review;
    }
}
