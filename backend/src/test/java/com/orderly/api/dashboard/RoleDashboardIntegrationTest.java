package com.orderly.api.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleDashboardIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void shouldExposeSystemOverviewForSuperAdmin() throws Exception {
                String token = authenticate("superadmin@orderly.local", "Orderly123!");

                mockMvc.perform(get("/api/v1/admin/overview")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
        }

        @Test
        void shouldExposeBusinessDashboardForTenantAdmin() throws Exception {
                String token = authenticate("admin@orderly.local", "Orderly123!");
                String businessId = createBusiness(token, "Panel Bistro");

                mockMvc.perform(get("/api/v1/businesses/{businessId}/dashboard", businessId)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Business-Id", businessId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.businessId").value(businessId));
        }

        @Test
        void shouldExposeOperatorOverview() throws Exception {
                String token = authenticate("ops@orderly.local", "Orderly123!");

                mockMvc.perform(get("/api/v1/operator/overview")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.role").value("OPERATOR"));
        }

        private String authenticate(String email, String password) throws Exception {
                String body = """
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password);

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

        private String createBusiness(String token, String name) throws Exception {
                String uniqueName = name + " " + System.nanoTime();
                String createBusinessBody = """
                                {
                                  "name": "%s",
                                  "businessType": "restaurant"
                                }
                                """.formatted(uniqueName);

                String response = mockMvc.perform(post("/api/v1/businesses")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBusinessBody))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                return objectMapper.readTree(response).get("id").asText();
        }
}
