package com.rupesh.springpractice.detection;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo")
class ConsoleIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void remoteConsoleRequiresAuthenticationAndMutationRequiresHeader() throws Exception {
        mvc.perform(get("/api/console/overview").with(r->{r.setRemoteAddr("203.0.113.8");return r;})).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/console/replay").contentType("application/json").content("{\"scenario\":\"human\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/console/replay").header("X-Sentinel-Console","sentinel").contentType("application/json").content("{\"scenario\":\"bad\"}")).andExpect(status().isBadRequest());
    }
    @Test void forwardedHeaderRotationDoesNotBypassSourceBudget() throws Exception {
        for(int i=0;i<120;i++) mvc.perform(get("/api/protected/catalog").header("User-Agent","unique-"+i).header("X-Forwarded-For","192.0.2."+i)
            .with(r->{r.setRemoteAddr("198.51.100.19");return r;})).andExpect(status().isOk());
        mvc.perform(get("/api/protected/catalog").header("User-Agent","unique-last").header("X-Forwarded-For","8.8.8.8")
            .with(r->{r.setRemoteAddr("198.51.100.19");return r;})).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }
    @Test void consoleTrafficIsExcludedAndReplayProducesEvidence() throws Exception {
        mvc.perform(get("/api/console/overview")).andExpect(status().isOk()).andExpect(jsonPath("$.storage").value("LOCAL_DEMO"));
        mvc.perform(post("/api/console/replay").header("X-Sentinel-Console","sentinel").contentType("application/json").content("{\"scenario\":\"scraper\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.firstDetection").value(31));
    }
}
