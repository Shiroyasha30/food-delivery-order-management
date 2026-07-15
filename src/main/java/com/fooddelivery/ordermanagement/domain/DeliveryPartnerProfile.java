package com.fooddelivery.ordermanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_partner_profiles")
public class DeliveryPartnerProfile {

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(nullable = false)
    private String displayName;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    protected DeliveryPartnerProfile() {
    }

    public DeliveryPartnerProfile(String userId, String displayName, City city) {
        this.userId = userId;
        this.displayName = displayName;
        this.city = city;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }
}
