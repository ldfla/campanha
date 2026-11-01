package la.meleva.fundraiser.identity;

import jakarta.servlet.http.Cookie;
import la.meleva.fundraiser.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RbacTest {

    @Autowired
    private MockMvc mockMvc;

    private Cookie registerAndGetSessionCookie(String phone) throws Exception {
        return AuthTestSupport.registerAndSignIn(mockMvc, "Test", "User", phone);
    }

    @Test
    void participantCannotAccessAdminEndpoints() throws Exception {
        Cookie session = registerAndGetSessionCookie("+5511777770001");

        mockMvc.perform(get("/_cx/anything").cookie(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedCanAccessPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedUserCanAccessOwnProfile() throws Exception {
        Cookie session = registerAndGetSessionCookie("+5511777770002");

        mockMvc.perform(get("/auth/me").cookie(session))
                .andExpect(status().isOk());
    }

    @Test
    void logoutClearsAuthentication() throws Exception {
        Cookie session = registerAndGetSessionCookie("+5511777770003");

        mockMvc.perform(post("/auth/logout").cookie(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }
}
