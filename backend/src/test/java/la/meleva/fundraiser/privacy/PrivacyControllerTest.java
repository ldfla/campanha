package la.meleva.fundraiser.privacy;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PrivacyControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    @Test
    void exportsOwnDataMaskedAndAnonymizesWithAudit() throws Exception {
        var candidate = new User("Fulano", "Silva", "+5511955000001");
        candidate.setEmailNormalized("fulano@example.test");
        var user = users.save(candidate);
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(UserRole.PARTICIPANT));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mvc.perform(get("/privacy/me/export").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Fulano"))
                .andExpect(jsonPath("$.email").value("f***@example.test"))
                .andExpect(jsonPath("$.participations").value(0));

        mvc.perform(post("/privacy/me/anonymize").with(authentication(auth)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/privacy/me/anonymize").with(authentication(auth)).with(csrf()))
                .andExpect(status().isNoContent());

        var anonymized = users.findById(user.getId()).orElseThrow();
        assertThat(anonymized.getDisplayName()).isEqualTo("Titular");
        assertThat(anonymized.getPhoneNumber()).startsWith("+999");
        assertThat(anonymized.isActive()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='PRIVACY_ANONYMIZED' AND entity_id=?",
                Long.class, user.getId())).isEqualTo(1);
    }
}
