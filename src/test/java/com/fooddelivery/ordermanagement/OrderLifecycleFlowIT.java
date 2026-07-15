package com.fooddelivery.ordermanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration coverage of the happy-path order lifecycle.
 * Extend this class as new domain behavior is added.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderLifecycleFlowIT {

    private static final String ADMIN = "admin-1";
    private static final String OWNER = "owner-1";
    private static final String CUSTOMER = "customer-1";
    private static final String PARTNER = "partner-1";
    private static final String USER_HEADER = "X-User-Id";

    private static final String SUFFIX = UUID.randomUUID().toString().substring(0, 8);
    private static final String CITY_NAME = "FlowCity-" + SUFFIX;
    private static final String RESTAURANT_NAME = "FlowRestaurant-" + SUFFIX;
    private static final String MENU_ITEM_NAME = "FlowBurger-" + SUFFIX;

    private static Long cityId;
    private static Long restaurantId;
    private static Long menuItemId;
    private static Long orderId;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(2)
    void adminAddsCity() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/cities")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(CITY_NAME)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(CITY_NAME))
                .andReturn();

        cityId = readTree(result).get("id").asLong();
        assertThat(cityId).isPositive();
    }

    @Test
    @Order(3)
    void adminAddsRestaurantLinkedToOwner() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/restaurants")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cityId": %d,
                                  "name": "%s",
                                  "ownerIds": ["%s"]
                                }
                                """.formatted(cityId, RESTAURANT_NAME, OWNER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(RESTAURANT_NAME))
                .andExpect(jsonPath("$.ownerIds[0]").value(OWNER))
                .andReturn();

        restaurantId = readTree(result).get("id").asLong();
    }

    @Test
    @Order(4)
    void adminAddsDeliveryPartnerForCity() throws Exception {
        mockMvc.perform(post("/api/v1/admin/delivery-partners")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "displayName": "Flow Partner",
                                  "cityId": %d
                                }
                                """.formatted(PARTNER, cityId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(PARTNER))
                .andExpect(jsonPath("$.cityId").value(cityId));
    }

    @Test
    @Order(5)
    void ownerManagesMenuItem() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/owner/restaurants/{restaurantId}/menu-items", restaurantId)
                        .header(USER_HEADER, OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "price": 199.50,
                                  "stock": 5
                                }
                                """.formatted(MENU_ITEM_NAME)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(MENU_ITEM_NAME))
                .andExpect(jsonPath("$.stock").value(5))
                .andReturn();

        menuItemId = readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/v1/owner/restaurants/{restaurantId}/menu-items/{itemId}", restaurantId, menuItemId)
                        .header(USER_HEADER, OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "price": 189.00,
                                  "stock": 4
                                }
                                """.formatted(MENU_ITEM_NAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(189.00))
                .andExpect(jsonPath("$.stock").value(4));
    }

    @Test
    @Order(6)
    void customerBrowsesRestaurantAndMenu() throws Exception {
        mockMvc.perform(get("/api/v1/customer/cities/{cityId}/restaurants", cityId)
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(restaurantId))
                .andExpect(jsonPath("$[0].name").value(RESTAURANT_NAME));

        mockMvc.perform(get("/api/v1/customer/restaurants/{restaurantId}/menu", restaurantId)
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(menuItemId))
                .andExpect(jsonPath("$[0].name").value(MENU_ITEM_NAME));

        mockMvc.perform(get("/api/v1/customer/cities/{cityId}/restaurants/by-menu-item", cityId)
                        .param("name", MENU_ITEM_NAME)
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(restaurantId));
    }

    @Test
    @Order(7)
    void customerPlacesOrderAndTracksStatus() throws Exception {
        MvcResult placed = mockMvc.perform(post("/api/v1/customer/restaurants/{restaurantId}/orders", restaurantId)
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"menuItemId": %d, "quantity": 2}
                                  ]
                                }
                                """.formatted(menuItemId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andReturn();

        orderId = readTree(placed).get("id").asLong();

        mockMvc.perform(get("/api/v1/customer/orders/{orderId}", orderId)
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"));

        mockMvc.perform(get("/api/v1/customer/restaurants/{restaurantId}/menu", restaurantId)
                        .header(USER_HEADER, CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stock").value(2));
    }

    @Test
    @Order(8)
    void ownerAcceptsAndMarksPreparing() throws Exception {
        mockMvc.perform(get("/api/v1/owner/restaurants/{restaurantId}/orders", restaurantId)
                        .header(USER_HEADER, OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId))
                .andExpect(jsonPath("$[0].status").value("PLACED"));

        mockMvc.perform(post("/api/v1/owner/orders/{orderId}/accept", orderId)
                        .header(USER_HEADER, OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/v1/owner/orders/{orderId}/preparing", orderId)
                        .header(USER_HEADER, OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    @Order(9)
    void partnerClaimsAndDeliversOrder() throws Exception {
        mockMvc.perform(get("/api/v1/partner/cities/{cityId}/orders/available", cityId)
                        .header(USER_HEADER, PARTNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(orderId));

        mockMvc.perform(post("/api/v1/partner/orders/{orderId}/accept", orderId)
                        .header(USER_HEADER, PARTNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryPartnerId").value(PARTNER))
                .andExpect(jsonPath("$.status").value("PREPARING"));

        mockMvc.perform(post("/api/v1/partner/orders/{orderId}/accept", orderId)
                        .header(USER_HEADER, PARTNER))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/partner/orders/{orderId}/status", orderId)
                        .header(USER_HEADER, PARTNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"OUT_FOR_DELIVERY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"));

        mockMvc.perform(post("/api/v1/partner/orders/{orderId}/status", orderId)
                        .header(USER_HEADER, PARTNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @Order(10)
    void customerRatesItemAndDeliveryAfterDelivered() throws Exception {
        mockMvc.perform(post("/api/v1/customer/orders/{orderId}/ratings/items/{itemId}", orderId, menuItemId)
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"review":"Great burger"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));

        mockMvc.perform(post("/api/v1/customer/orders/{orderId}/ratings/delivery", orderId)
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":4,"review":"On time"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    @Order(11)
    void ownerCanDeleteMenuItem() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/owner/restaurants/{restaurantId}/menu-items", restaurantId)
                        .header(USER_HEADER, OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Temp-%s",
                                  "price": 10,
                                  "stock": 1
                                }
                                """.formatted(SUFFIX)))
                .andExpect(status().isCreated())
                .andReturn();

        long tempId = readTree(created).get("id").asLong();

        mockMvc.perform(delete("/api/v1/owner/restaurants/{restaurantId}/menu-items/{itemId}",
                        restaurantId, tempId)
                        .header(USER_HEADER, OWNER))
                .andExpect(status().isNoContent());
    }

    private JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
