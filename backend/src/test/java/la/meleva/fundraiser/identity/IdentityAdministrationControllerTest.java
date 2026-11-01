package la.meleva.fundraiser.identity;

import la.meleva.fundraiser.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IdentityAdministrationControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;

    @Test
    void systemAdminCanCreateMembershipOnlyWithCsrf() throws Exception {
        var administrator = userRepository.save(new User("Admin", "Teste", "+5511999900001"));
        var target = userRepository.save(new User("Pessoa", "Alvo", "+5511999900002"));
        var organization = organizationRepository.save(new Organization("Organização de Teste", "ASSOCIATION"));
        var principal = new AppUserDetails(administrator.getId(), administrator.getPublicId(), administrator.getPhoneNumber(), List.of(UserRole.SYSTEM_ADMIN));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String payload = """
                {"userPublicId":"%s","organizationPublicId":"%s","role":"SUPPORT"}
                """.formatted(target.getPublicId(), organization.getPublicId());

        mockMvc.perform(post("/_cx/memberships").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/_cx/memberships").with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void nonSystemAdminCannotGrantRoles() throws Exception {
        var administrator = userRepository.save(new User("Security", "Admin", "+5511999900003"));
        var target = userRepository.save(new User("Pessoa", "Alvo", "+5511999900004"));
        var organization = organizationRepository.save(new Organization("Organização Dois", "ASSOCIATION"));
        var principal = new AppUserDetails(administrator.getId(), administrator.getPublicId(), administrator.getPhoneNumber(), List.of(UserRole.SECURITY_ADMIN));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String payload = """
                {"userPublicId":"%s","organizationPublicId":"%s","role":"SUPPORT"}
                """.formatted(target.getPublicId(), organization.getPublicId());
        mockMvc.perform(post("/_cx/memberships").with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
    }
}
