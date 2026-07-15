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
        name = "item_ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "menu_item_id", "customer_id"})
)
public class ItemRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false, length = 64)
    private String customerId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String review;

    protected ItemRating() {
    }

    public ItemRating(OrderEntity order, MenuItem menuItem, String customerId, Integer rating, String review) {
        this.order = order;
        this.menuItem = menuItem;
        this.customerId = customerId;
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
