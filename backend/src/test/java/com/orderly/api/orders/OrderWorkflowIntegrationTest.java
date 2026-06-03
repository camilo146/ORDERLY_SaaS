package com.orderly.api.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderWorkflowIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        void shouldCreateBusinessProductAndOrderUsingTenantAwareFlow() throws Exception {
                String token = authenticate();

                String businessId = createBusiness(token, "Pizza Nova");
                String productId = createProduct(token, businessId, "Pizza Personal", 25000, "Queso y pepperoni");

                String createOrderBody = """
                                {
                                  "customerName": "Laura",
                                  "customerWhatsapp": "+573001112233",
                                  "deliveryType": "pickup",
                                                                                        "paymentMethod": "CASH",
                                  "notes": "Sin cebolla",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 2,
                                      "notes": "Extra queso"
                                    }
                                  ]
                                }
                                """.formatted(productId);

                String orderResponse = mockMvc.perform(post("/api/v1/businesses/{businessId}/orders", businessId)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Business-Id", businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createOrderBody))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.customerName").value("Laura"))
                                .andExpect(jsonPath("$.totalAmount").value(50000.0))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String orderId = objectMapper.readTree(orderResponse).get("id").asText();

                mockMvc.perform(get("/api/v1/businesses/{businessId}/orders", businessId)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Business-Id", businessId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(orderId));

                mockMvc.perform(patch("/api/v1/businesses/{businessId}/orders/{orderId}/status", businessId, orderId)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Business-Id", businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"READY\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("READY"));
        }

        @Test
        void shouldReturn429WhenPlanActiveOrderLimitIsReached() throws Exception {
                String token = authenticate();

                String businessId = createBusiness(token, "Plan Limit Test");
                String productId = createProduct(token, businessId, "Combo Limitado", 12000, "Prueba de límite");

                UUID planId = UUID.randomUUID();
                jdbcTemplate.update(
                                "INSERT INTO plans (id, name, max_active_orders, created_at) VALUES (?, ?, ?, ?)",
                                planId,
                                "TEST_LIMIT_" + planId.toString().substring(0, 8),
                                1,
                                Timestamp.from(Instant.now()));
                jdbcTemplate.update("UPDATE businesses SET plan_id = ? WHERE id = ?", planId,
                                UUID.fromString(businessId));

                mockMvc.perform(post("/api/v1/businesses/{businessId}/orders", businessId)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Business-Id", businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createOrderBody(productId, "Laura")))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/v1/businesses/{businessId}/orders", businessId)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Business-Id", businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createOrderBody(productId, "Carlos")))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.code").value("PLAN_LIMIT_EXCEEDED"));
        }

        private String authenticate() throws Exception {
                String body = """
                                {
                                  "email": "admin@orderly.local",
                                  "password": "Orderly123!"
                                }
                                """;

                String response = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode jsonNode = objectMapper.readTree(response);
                return jsonNode.get("accessToken").asText();
        }

        private String createBusiness(String token, String businessName) throws Exception {
                String uniqueName = businessName + " " + System.nanoTime();
                String createBusinessBody = """
                                                                {
                                                                        "name": "%s",
                                                                        "businessType": "restaurant"
                                                                }
                                """.formatted(uniqueName);

                String businessResponse = mockMvc.perform(post("/api/v1/businesses")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBusinessBody))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                return objectMapper.readTree(businessResponse).get("id").asText();
        }

        private String createProduct(String token, String businessId, String name, int price, String description)
                        throws Exception {
                String createProductBody = """
                                {
                                        "name": "%s",
                                        "price": %d,
                                        "description": "%s"
                                }
                                """.formatted(name, price, description);

                String productResponse = mockMvc.perform(post("/api/v1/businesses/{businessId}/products", businessId)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Business-Id", businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createProductBody))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                return objectMapper.readTree(productResponse).get("id").asText();
        }

        private String createOrderBody(String productId, String customerName) {
                return """
                                {
                                        "customerName": "%s",
                                        "customerWhatsapp": "+573001112233",
                                        "deliveryType": "pickup",
                                        "paymentMethod": "CASH",
                                        "notes": "Sin cebolla",
                                        "items": [
                                                {
                                                        "productId": "%s",
                                                        "quantity": 1,
                                                        "notes": "Extra queso"
                                                }
                                        ]
                                }
                                """.formatted(customerName, productId);
        }
}
