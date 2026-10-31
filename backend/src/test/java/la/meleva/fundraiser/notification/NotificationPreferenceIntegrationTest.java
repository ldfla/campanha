package la.meleva.fundraiser.notification;

import java.util.List;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationPreferenceIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired NotificationPreferenceService service;

    @Test
    void essentialIsAlwaysAllowedAndMarketingRespectsOptOut() {
        assertThat(NotificationPreferenceService.mayDispatch(NotificationTemplate.ACCOUNT_REGISTERED, true)).isTrue();
        assertThat(NotificationPreferenceService.mayDispatch(NotificationTemplate.RESULT_AVAILABLE, true)).isTrue();
        assertThat(NotificationPreferenceService.mayDispatch(NotificationTemplate.CAMPAIGN_MARKETING, false)).isTrue();
        assertThat(NotificationPreferenceService.mayDispatch(NotificationTemplate.CAMPAIGN_MARKETING, true)).isFalse();
    }

    @Test
    void userCanToggleMarketingOptOutThroughTheApi() throws Exception {
        var user = users.save(new User("Pessoa", "Preferência", "+55119999007701"));
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(UserRole.PARTICIPANT));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mvc.perform(get("/notifications/preferences").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketingOptOut").value(false));

        mvc.perform(put("/notifications/preferences").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"marketingOptOut\":true}"))
                .andExpect(status().isForbidden());

        mvc.perform(put("/notifications/preferences").with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"marketingOptOut\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketingOptOut").value(true));

        assertThat(service.marketingOptedOut(user.getId())).isTrue();
        mvc.perform(get("/notifications/preferences").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketingOptOut").value(true));
    }
}
