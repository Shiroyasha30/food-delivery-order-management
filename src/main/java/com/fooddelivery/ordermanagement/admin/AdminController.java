package com.fooddelivery.ordermanagement.admin;

import com.fooddelivery.ordermanagement.admin.dto.CityResponse;
import com.fooddelivery.ordermanagement.admin.dto.CreateCityRequest;
import com.fooddelivery.ordermanagement.admin.dto.CreateDeliveryPartnerRequest;
import com.fooddelivery.ordermanagement.admin.dto.CreateOwnerRequest;
import com.fooddelivery.ordermanagement.admin.dto.CreateRestaurantRequest;
import com.fooddelivery.ordermanagement.admin.dto.DeliveryPartnerResponse;
import com.fooddelivery.ordermanagement.admin.dto.OwnerResponse;
import com.fooddelivery.ordermanagement.admin.dto.RestaurantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse addCity(@Valid @RequestBody CreateCityRequest request) {
        return adminService.addCity(request);
    }

    @PostMapping("/owners")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse addOwner(@Valid @RequestBody CreateOwnerRequest request) {
        return adminService.addOwner(request);
    }

    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse addRestaurant(@Valid @RequestBody CreateRestaurantRequest request) {
        return adminService.addRestaurant(request);
    }

    @PostMapping("/delivery-partners")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryPartnerResponse addDeliveryPartner(@Valid @RequestBody CreateDeliveryPartnerRequest request) {
        return adminService.addDeliveryPartner(request);
    }
}
