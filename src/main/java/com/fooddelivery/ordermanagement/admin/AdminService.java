package com.fooddelivery.ordermanagement.admin;

import com.fooddelivery.ordermanagement.admin.dto.CityResponse;
import com.fooddelivery.ordermanagement.admin.dto.CreateCityRequest;
import com.fooddelivery.ordermanagement.admin.dto.CreateDeliveryPartnerRequest;
import com.fooddelivery.ordermanagement.admin.dto.CreateOwnerRequest;
import com.fooddelivery.ordermanagement.admin.dto.CreateRestaurantRequest;
import com.fooddelivery.ordermanagement.admin.dto.DeliveryPartnerResponse;
import com.fooddelivery.ordermanagement.admin.dto.OwnerResponse;
import com.fooddelivery.ordermanagement.admin.dto.RestaurantResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {

    public CityResponse addCity(CreateCityRequest request) {
        throw notImplemented();
    }

    public OwnerResponse addOwner(CreateOwnerRequest request) {
        throw notImplemented();
    }

    public RestaurantResponse addRestaurant(CreateRestaurantRequest request) {
        throw notImplemented();
    }

    public DeliveryPartnerResponse addDeliveryPartner(CreateDeliveryPartnerRequest request) {
        throw notImplemented();
    }

    private static ResponseStatusException notImplemented() {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
