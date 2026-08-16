package com.rupesh.springpractice.filter;

import com.rupesh.springpractice.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SentinelRequestFilterIntegrationTest extends AbstractRedisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowInitialRequestsAndBlockOnAbuse() throws Exception {
        String testUserAgent = "MockBot/1.0";
        String testIp = "192.168.1.100";

        // Under 30 requests -> 200 OK
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/ping")
                            .header("User-Agent", testUserAgent)
                            .header("X-Forwarded-For", testIp))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Sentinel core running"));
        }

        // 31st request -> 429 Too Many Requests
        mockMvc.perform(get("/ping")
                        .header("User-Agent", testUserAgent)
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Security constraint violation"))
                .andExpect(jsonPath("$.risk_score").value(90));
    }
}