package com.fooddelivery.ordermanagement.admin;

import com.fooddelivery.ordermanagement.admin.dto.CityResponse;
import com.fooddelivery.ordermanagement.admin.dto.CreateCityRequest;
import com.fooddelivery.ordermanagement.admin.dto.CreateDeliveryPartnerRequest;
import com.fooddelivery.ordermanagement.admin.dto.CreateOwnerRequest;
import com.fooddelivery.ordermanagement.admin.dto.CreateRestaurantRequest;
import com.fooddelivery.ordermanagement.admin.dto.DeliveryPartnerResponse;
import com.fooddelivery.ordermanagement.admin.dto.OwnerResponse;
import com.fooddelivery.ordermanagement.admin.dto.RestaurantResponse;
import com.fooddelivery.ordermanagement.domain.City;
import com.fooddelivery.ordermanagement.domain.CityRepository;
import com.fooddelivery.ordermanagement.domain.DeliveryPartnerProfile;
import com.fooddelivery.ordermanagement.domain.DeliveryPartnerProfileRepository;
import com.fooddelivery.ordermanagement.domain.Restaurant;
import com.fooddelivery.ordermanagement.domain.RestaurantRepository;
import com.fooddelivery.ordermanagement.user.Role;
import com.fooddelivery.ordermanagement.user.RoleRepository;
import com.fooddelivery.ordermanagement.user.User;
import com.fooddelivery.ordermanagement.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryPartnerProfileRepository deliveryPartnerProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminService(
            CityRepository cityRepository,
            RestaurantRepository restaurantRepository,
            DeliveryPartnerProfileRepository deliveryPartnerProfileRepository,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.cityRepository = cityRepository;
        this.restaurantRepository = restaurantRepository;
        this.deliveryPartnerProfileRepository = deliveryPartnerProfileRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public CityResponse addCity(CreateCityRequest request) {
        cityRepository.findByNameIgnoreCase(request.name().trim()).ifPresent(c -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "City already exists");
        });
        City city = cityRepository.save(new City(request.name().trim()));
        return new CityResponse(city.getId(), city.getName());
    }

    @Transactional
    public OwnerResponse addOwner(CreateOwnerRequest request) {
        if (userRepository.existsById(request.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }
        Role ownerRole = roleRepository.findByName("RESTAURANT_OWNER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role missing"));
        User user = new User(request.id().trim(), request.displayName().trim());
        user.setRoles(Set.of(ownerRole));
        userRepository.save(user);
        return new OwnerResponse(user.getId(), user.getDisplayName());
    }

    @Transactional
    public RestaurantResponse addRestaurant(CreateRestaurantRequest request) {
        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found"));

        Set<User> owners = new HashSet<>();
        for (String ownerId : request.ownerIds()) {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner not found: " + ownerId));
            boolean isOwner = owner.getRoles().stream().anyMatch(r -> "RESTAURANT_OWNER".equals(r.getName()));
            if (!isOwner) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a restaurant owner: " + ownerId);
            }
            owners.add(owner);
        }

        Restaurant restaurant = new Restaurant(request.name().trim(), city);
        restaurant.setOwners(owners);
        restaurant = restaurantRepository.save(restaurant);

        return new RestaurantResponse(
                restaurant.getId(),
                city.getId(),
                restaurant.getName(),
                owners.stream().map(User::getId).collect(Collectors.toSet())
        );
    }

    @Transactional
    public DeliveryPartnerResponse addDeliveryPartner(CreateDeliveryPartnerRequest request) {
        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found"));

        if (deliveryPartnerProfileRepository.existsById(request.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delivery partner already exists");
        }

        Role partnerRole = roleRepository.findByName("DELIVERY_PARTNER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role missing"));

        User user = userRepository.findById(request.userId()).orElseGet(() -> {
            User created = new User(request.userId().trim(), request.displayName().trim());
            created.setRoles(new HashSet<>(Set.of(partnerRole)));
            return userRepository.save(created);
        });

        if (user.getRoles().stream().noneMatch(r -> "DELIVERY_PARTNER".equals(r.getName()))) {
            user.getRoles().add(partnerRole);
            userRepository.save(user);
        }
        user.setDisplayName(request.displayName().trim());

        DeliveryPartnerProfile profile = deliveryPartnerProfileRepository.save(
                new DeliveryPartnerProfile(user.getId(), request.displayName().trim(), city)
        );

        return new DeliveryPartnerResponse(profile.getUserId(), profile.getDisplayName(), city.getId());
    }
}
