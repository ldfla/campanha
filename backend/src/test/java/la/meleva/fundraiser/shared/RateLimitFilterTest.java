package la.meleva.fundraiser.shared;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitFilterTest {

    @RestController
    static class Dummy {
        @PostMapping("/auth/login")
        String login() {
            return "ok";
        }
    }

    @Test
    void blocksAfterTenRequestsInTheWindow() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Dummy()).addFilters(new RateLimitFilter()).build();
        for (int request = 0; request < 10; request++) {
            mvc.perform(post("/auth/login").servletPath("/auth/login")).andExpect(status().isOk());
        }
        mvc.perform(post("/auth/login").servletPath("/auth/login")).andExpect(status().isTooManyRequests());
    }
}
