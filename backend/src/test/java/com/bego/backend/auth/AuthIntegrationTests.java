package com.bego.backend.auth;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerMeRefreshAndLogout() throws Exception {
        String email = "phase2-" + UUID.randomUUID() + "@example.com";
        String password = "password123";

        String registerBody = """
                {
                  "email": "%s",
                  "password": "%s",
                  "displayName": "Phase Two"
                }
                """.formatted(email, password);

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = JsonPath.read(registerResponse, "$.accessToken");
        String refreshToken = JsonPath.read(registerResponse, "$.refreshToken");

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        String refreshBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshedAccessToken = JsonPath.read(refreshResponse, "$.accessToken");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + refreshedAccessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + refreshedAccessToken))
                .andExpect(status().isUnauthorized());
    }
}
