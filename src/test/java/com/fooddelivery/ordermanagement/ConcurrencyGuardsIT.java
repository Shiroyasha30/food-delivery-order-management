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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration coverage for concurrent stock and partner-claim safety.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConcurrencyGuardsIT {

    private static final String ADMIN = "admin-1";
    private static final String OWNER = "owner-1";
    private static final String CUSTOMER = "customer-1";
    private static final String USER_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void concurrentOrdersCannotOversellLimitedStock() throws Exception {
        Fixture fixture = createRestaurantWithItem(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tasks = List.of(
                    () -> placeOrderStatus(fixture.restaurantId(), fixture.menuItemId(), 1),
                    () -> placeOrderStatus(fixture.restaurantId(), fixture.menuItemId(), 1)
            );
            List<Future<Integer>> futures = pool.invokeAll(tasks);
            pool.shutdown();
            assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(statuses).containsExactlyInAnyOrder(201, 409);

            mockMvc.perform(get("/api/v1/customer/restaurants/{id}/menu", fixture.restaurantId())
                            .header(USER_HEADER, CUSTOMER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].stock").value(0));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentPartnersCannotBothClaimSameOrder() throws Exception {
        Fixture fixture = createRestaurantWithItem(2);
        String partnerA = createPartnerForCity(fixture.cityId());
        String partnerB = createPartnerForCity(fixture.cityId());

        long orderId = placeOrder(fixture.restaurantId(), fixture.menuItemId(), 1);

        mockMvc.perform(post("/api/v1/owner/orders/{id}/accept", orderId)
                        .header(USER_HEADER, OWNER))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/owner/orders/{id}/preparing", orderId)
                        .header(USER_HEADER, OWNER))
                .andExpect(status().isOk());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tasks = List.of(
                    () -> claimStatus(orderId, partnerA),
                    () -> claimStatus(orderId, partnerB)
            );
            List<Future<Integer>> futures = pool.invokeAll(tasks);
            pool.shutdown();
            assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(statuses).containsExactlyInAnyOrder(200, 409);

            MvcResult order = mockMvc.perform(get("/api/v1/customer/orders/{id}", orderId)
                            .header(USER_HEADER, CUSTOMER))
                    .andExpect(status().isOk())
                    .andReturn();
            String assigned = objectMapper.readTree(order.getResponse().getContentAsString())
                    .get("deliveryPartnerId")
                    .asText();
            assertThat(assigned).isIn(partnerA, partnerB);
        } finally {
            pool.shutdownNow();
        }
    }

    private String createPartnerForCity(long cityId) throws Exception {
        String partnerId = "partner-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/admin/delivery-partners")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "displayName": "Concurrent Partner",
                                  "cityId": %d
                                }
                                """.formatted(partnerId, cityId)))
                .andExpect(status().isCreated());
        return partnerId;
    }

    private int placeOrderStatus(long restaurantId, long menuItemId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/customer/restaurants/{id}/orders", restaurantId)
                        .header(USER_HEADER, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items":[{"menuItemId":%d,"quantity":%d}]
                                }
                                """.formatted(menuItemId, quantity)))
                .andReturn();
        return result.getResponse().getStatus();
    }

    private int claimStatus(long orderId, String partnerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/partner/orders/{id}/accept", orderId)
                        .header(USER_HEADER, partnerId))
                .andReturn();
        return result.getResponse().getStatus();
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
                .andReturn());
    }

    private Fixture createRestaurantWithItem(int stock) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        long cityId = readId(mockMvc.perform(post("/api/v1/admin/cities")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ConcCity-%s"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn());

        long restaurantId = readId(mockMvc.perform(post("/api/v1/admin/restaurants")
                        .header(USER_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cityId": %d,
                                  "name": "ConcRest-%s",
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
                                  "name": "ConcItem-%s",
                                  "price": 25,
                                  "stock": %d
                                }
                                """.formatted(suffix, stock)))
                .andExpect(status().isCreated())
                .andReturn());

        return new Fixture(cityId, restaurantId, menuItemId);
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private record Fixture(long cityId, long restaurantId, long menuItemId) {
    }
}
