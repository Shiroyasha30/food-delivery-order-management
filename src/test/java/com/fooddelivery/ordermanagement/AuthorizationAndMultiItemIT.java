package com.fooddelivery.ordermanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.ordermanagement.user.Role;
import com.fooddelivery.ordermanagement.user.RoleRepository;
import com.fooddelivery.ordermanagement.user.User;
import com.fooddelivery.ordermanagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization isolation and multi-item order placement coverage.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationAndMultiItemIT {

    private static final String ADMIN = "admin-1";
    private static final String OWNER = "owner-1";
    private static final String CUSTOMER = "customer-1";
    private static final String USER_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void customerCannotReadAnotherCustomersOrder() throws Exception {
        String otherCustomer = ensureCustomer("customer-" + UUID.randomUUID().toString().substring(0, 8));
        Fixture fixture = createRestaurantWithTwoItems(OWNER, 2, 2);
        long orderId = placeSingleItemOrder(fixture.restaurantId(), fixture.itemAId(), 1);

        mockMvc.perform(get("/api/v1/customer/orders/{id}", orderId)
                        .header(USER_HEADER, otherCustomer))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/customer/orders/{id}", orderId)
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void ownerOfDifferentRestaurantCannotManageOrders() throws Exception {
        String otherOwner = "owner-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/admin/owners")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"%s","displayName":"Other Owner"}
                                """.formatted(otherOwner)))
                .andExpect(status().isCreated());

        Fixture fixture = createRestaurantWithTwoItems(OWNER, 5, 5);
        long orderId = placeSingleItemOrder(fixture.restaurantId(), fixture.itemAId(), 1);

        mockMvc.perform(post("/api/v1/owner/orders/{id}/accept", orderId)
                        .header(USER_HEADER, otherOwner))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/owner/restaurants/{id}/orders", fixture.restaurantId())
                        .header(USER_HEADER, otherOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanPlaceMultiItemOrderAtomically() throws Exception {
        Fixture fixture = createRestaurantWithTwoItems(OWNER, 3, 4);

        mockMvc.perform(post("/api/v1/customer/restaurants/{id}/orders", fixture.restaurantId())
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items":[
                                    {"menuItemId":%d,"quantity":2},
                                    {"menuItemId":%d,"quantity":1}
                                  ]
                                }
                                """.formatted(fixture.itemAId(), fixture.itemBId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(40.0));

        mockMvc.perform(get("/api/v1/customer/restaurants/{id}/menu", fixture.restaurantId())
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + fixture.itemAId() + ")].stock").value(contains(1)))
                .andExpect(jsonPath("$[?(@.id==" + fixture.itemBId() + ")].stock").value(contains(3)));
    }

    @Test
    void multiItemOrderRollsBackWhenSecondItemLacksStock() throws Exception {
        Fixture fixture = createRestaurantWithTwoItems(OWNER, 5, 0);

        mockMvc.perform(post("/api/v1/customer/restaurants/{id}/orders", fixture.restaurantId())
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items":[
                                    {"menuItemId":%d,"quantity":1},
                                    {"menuItemId":%d,"quantity":1}
                                  ]
                                }
                                """.formatted(fixture.itemAId(), fixture.itemBId())))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/customer/restaurants/{id}/menu", fixture.restaurantId())
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + fixture.itemAId() + ")].stock").value(contains(5)))
                .andExpect(jsonPath("$[?(@.id==" + fixture.itemBId() + ")].stock").value(contains(0)));
    }

    @Test
    void menuItemNameSearchIsCaseInsensitive() throws Exception {
        Fixture fixture = createRestaurantWithTwoItems(OWNER, 1, 1);

        mockMvc.perform(get("/api/v1/customer/cities/{cityId}/restaurants/by-menu-item", fixture.cityId())
                        .param("name", fixture.itemAName().toUpperCase())
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fixture.restaurantId()));
    }

    private String ensureCustomer(String userId) {
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role missing"));
        User user = new User(userId, "Other Customer");
        user.setRoles(new HashSet<>(Set.of(customerRole)));
        userRepository.save(user);
        return userId;
    }

    private long placeSingleItemOrder(long restaurantId, long menuItemId, int qty) throws Exception {
        return readId(mockMvc.perform(post("/api/v1/customer/restaurants/{id}/orders", restaurantId)
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"menuItemId":%d,"quantity":%d}]}
                                """.formatted(menuItemId, qty)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private Fixture createRestaurantWithTwoItems(String ownerId, int stockA, int stockB) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String itemAName = "Alpha-" + suffix;
        String itemBName = "Beta-" + suffix;

        long cityId = readId(mockMvc.perform(post("/api/v1/admin/cities")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"AuthCity-%s"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn());

        long restaurantId = readId(mockMvc.perform(post("/api/v1/admin/restaurants")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cityId": %d,
                                  "name": "AuthRest-%s",
                                  "ownerIds": ["%s"]
                                }
                                """.formatted(cityId, suffix, ownerId)))
                .andExpect(status().isCreated())
                .andReturn());

        long itemAId = readId(mockMvc.perform(post("/api/v1/owner/restaurants/{id}/menu-items", restaurantId)
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","price":10.00,"stock":%d}
                                """.formatted(itemAName, stockA)))
                .andExpect(status().isCreated())
                .andReturn());

        long itemBId = readId(mockMvc.perform(post("/api/v1/owner/restaurants/{id}/menu-items", restaurantId)
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","price":20.00,"stock":%d}
                                """.formatted(itemBName, stockB)))
                .andExpect(status().isCreated())
                .andReturn());

        return new Fixture(cityId, restaurantId, itemAId, itemBId, itemAName, itemBName);
    }

    private long readId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private record Fixture(
            long cityId,
            long restaurantId,
            long itemAId,
            long itemBId,
            String itemAName,
            String itemBName
    ) {
    }
}
