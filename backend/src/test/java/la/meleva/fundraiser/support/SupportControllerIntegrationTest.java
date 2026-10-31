package la.meleva.fundraiser.support;

import java.util.List;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SupportControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;

    @Test
    void participantOpensRequestAndOnlySupportCanListIt() throws Exception {
        var requester = users.save(new User("Pessoa", "Atendida", "+55119999007101"));
        var support = users.save(new User("Agente", "Suporte", "+55119999007102"));
        var org = organizations.save(new Organization("Organização suporte", "ASSOCIATION"));
        memberships.save(new Membership(org, support, UserRole.SUPPORT));

        var requesterAuth = auth(requester, UserRole.PARTICIPANT);
        var supportAuth = auth(support, UserRole.SUPPORT);

        mvc.perform(post("/support/requests").with(authentication(requesterAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Dúvida\",\"message\":\"Como confirmo meu número?\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/support/requests").with(authentication(requesterAuth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Dúvida\",\"message\":\"Como confirmo meu número?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.subject").value("Dúvida"));

        mvc.perform(get("/support/requests").with(authentication(requesterAuth)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/support/requests").with(authentication(supportAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Dúvida"))
                .andExpect(jsonPath("$[0].message").value("Como confirmo meu número?"));
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        var requester = users.save(new User("Pessoa", "Vazia", "+55119999007103"));
        mvc.perform(post("/support/requests").with(authentication(auth(requester, UserRole.PARTICIPANT))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"subject\":\"x\",\"message\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    private UsernamePasswordAuthenticationToken auth(User user, UserRole role) {
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(role));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
