package com.fooddelivery.ordermanagement.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        Role admin = ensureRole("ADMIN");
        Role restaurantOwner = ensureRole("RESTAURANT_OWNER");
        Role customer = ensureRole("CUSTOMER");
        Role deliveryPartner = ensureRole("DELIVERY_PARTNER");

        ensureUser("admin-1", "Admin User", Set.of(admin));
        ensureUser("owner-1", "Restaurant Owner", Set.of(restaurantOwner));
        ensureUser("customer-1", "Customer User", Set.of(customer));
        ensureUser("partner-1", "Delivery Partner", Set.of(deliveryPartner));

        log.info("Seeded roles and sample users (idempotent)");
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name).orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private void ensureUser(String id, String displayName, Set<Role> roles) {
        if (userRepository.existsById(id)) {
            return;
        }
        User user = new User(id, displayName);
        user.setRoles(roles);
        userRepository.save(user);
    }
}
