package com.fooddelivery.ordermanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration coverage for auth denial, reject+stock restore, and stock contention.
 * Complements {@link OrderLifecycleFlowIT}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderEdgeCasesIT {

    private static final String ADMIN = "admin-1";
    private static final String OWNER = "owner-1";
    private static final String CUSTOMER = "customer-1";
    private static final String USER_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedApiRequiresKnownUser() throws Exception {
        mockMvc.perform(get("/api/v1/customer/cities/1/restaurants"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/customer/cities/1/restaurants")
                        .header(USER_HEADER, "unknown-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleMismatchIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cities")
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nope-%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateOwnerUser() throws Exception {
        String ownerId = "owner-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/admin/owners")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"%s","displayName":"Extra Owner"}
                                """.formatted(ownerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ownerId));
    }

    @Test
    void ownerRejectRestoresStock() throws Exception {
        Fixture fixture = createRestaurantWithItem(stock(3));

        long orderId = placeOrder(fixture.restaurantId(), fixture.menuItemId(), 2);

        mockMvc.perform(get("/api/v1/customer/restaurants/{id}/menu", fixture.restaurantId())
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fixture.menuItemId()))
                .andExpect(jsonPath("$[0].stock").value(1));

        mockMvc.perform(post("/api/v1/owner/orders/{orderId}/reject", orderId)
                        .header(USER_HEADER, OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/v1/customer/restaurants/{id}/menu", fixture.restaurantId())
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stock").value(3));
    }

    @Test
    void placeOrderFailsWhenStockInsufficient() throws Exception {
        Fixture fixture = createRestaurantWithItem(stock(1));

        mockMvc.perform(post("/api/v1/customer/restaurants/{id}/orders", fixture.restaurantId())
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items":[{"menuItemId":%d,"quantity":5}]
                                }
                                """.formatted(fixture.menuItemId())))
                .andExpect(status().isConflict());
    }

    @Test
    void ratingsRejectedBeforeDelivery() throws Exception {
        Fixture fixture = createRestaurantWithItem(stock(2));
        long orderId = placeOrder(fixture.restaurantId(), fixture.menuItemId(), 1);

        mockMvc.perform(post("/api/v1/customer/orders/{orderId}/ratings/delivery", orderId)
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":3,"review":"too early"}
                                """))
                .andExpect(status().isConflict());
    }

    private Fixture createRestaurantWithItem(int stock) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        long cityId = readId(mockMvc.perform(post("/api/v1/admin/cities")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"EdgeCity-%s"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn());

        long restaurantId = readId(mockMvc.perform(post("/api/v1/admin/restaurants")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cityId": %d,
                                  "name": "EdgeRest-%s",
                                  "ownerIds": ["%s"]
                                }
                                """.formatted(cityId, suffix, OWNER)))
                .andExpect(status().isCreated())
                .andReturn());

        long menuItemId = readId(mockMvc.perform(post("/api/v1/owner/restaurants/{id}/menu-items", restaurantId)
                        .header(USER_HEADER, OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "EdgeItem-%s",
                                  "price": 50,
                                  "stock": %d
                                }
                                """.formatted(suffix, stock)))
                .andExpect(status().isCreated())
                .andReturn());

        return new Fixture(cityId, restaurantId, menuItemId);
    }

    private long placeOrder(long restaurantId, long menuItemId, int quantity) throws Exception {
        return readId(mockMvc.perform(post("/api/v1/customer/restaurants/{id}/orders", restaurantId)
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items":[{"menuItemId":%d,"quantity":%d}]
                                }
                                """.formatted(menuItemId, quantity)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn());
    }

    private static int stock(int value) {
        return value;
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private record Fixture(long cityId, long restaurantId, long menuItemId) {
    }
}
