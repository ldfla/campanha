package la.meleva.fundraiser.support;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import com.jayway.jsonpath.JsonPath;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.audit.AuditEventRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SupportControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;
    @Autowired SupportRequestRepository requests;
    @Autowired AuditEventRepository audits;
    @Autowired SupportService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void supportAgentCannotReadOtherOrganizationsRequests() throws Exception {
        var requester = users.save(new User("Pessoa", "Atendida", "+55119999007111"));
        var support = users.save(new User("Agente", "Outro", "+55119999007112"));
        var firstOrg = organizations.save(new Organization("Suporte organização A", "ASSOCIATION"));
        var secondOrg = organizations.save(new Organization("Suporte organização B", "ASSOCIATION"));
        memberships.save(new Membership(secondOrg, support, UserRole.SUPPORT));
        var created = mvc.perform(post("/support/requests").with(authentication(auth(requester, UserRole.PARTICIPANT))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Chamado privado A\",\"message\":\"Assunto pessoal\",\"organizationId\":\""
                                + firstOrg.getPublicId() + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.organizationId").value(firstOrg.getPublicId()))
                .andReturn();
        String requestId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        var saved = requests.findByPublicId(requestId).orElseThrow();
        String hash = jdbc.queryForObject("SELECT event_hash FROM audit_events WHERE entity_type=? AND entity_id=?",
                String.class, "support_request", saved.getId());
        String input = requestId + ":" + firstOrg.getPublicId();
        String expected = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(hash).isEqualTo(expected);
        mvc.perform(get("/support/requests").param("organizationId", secondOrg.getPublicId())
                        .with(authentication(auth(support, UserRole.SUPPORT))))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/support/requests").param("organizationId", firstOrg.getPublicId())
                        .with(authentication(auth(support, UserRole.SUPPORT))))
                .andExpect(status().isForbidden());
    }

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
        var created = mvc.perform(post("/support/requests").with(authentication(requesterAuth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Dúvida\",\"message\":\"Como confirmo meu número?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.subject").value("Dúvida")).andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/support/requests").with(authentication(requesterAuth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].subject").value("Dúvida"));
        mvc.perform(get("/support/requests").with(authentication(supportAuth)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/support/requests").param("organizationId", org.getPublicId()).with(authentication(supportAuth)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        String assignment = "{\"organizationId\":\"" + org.getPublicId() + "\"}";
        String path = "/support/requests/" + id + "/organization";
        mvc.perform(post(path).with(authentication(supportAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(assignment)).andExpect(status().isForbidden());
        mvc.perform(post(path).with(authentication(requesterAuth)).contentType(MediaType.APPLICATION_JSON)
                .content(assignment)).andExpect(status().isForbidden());
        mvc.perform(post(path).with(authentication(requesterAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(assignment)).andExpect(status().isOk());
        var saved = requests.findByPublicId(id).orElseThrow();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(2);
        var other = organizations.save(new Organization("Outro destino", "ASSOCIATION"));
        mvc.perform(post(path).with(authentication(requesterAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationId\":\"" + other.getPublicId() + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post(path).with(authentication(requesterAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(assignment)).andExpect(status().isOk());
        mvc.perform(get("/support/requests").param("organizationId", org.getPublicId()).with(authentication(supportAuth)))
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

    @Test
    void authorCanReadBeyondFirstHundredUnassignedRequests() throws Exception {
        var requester = users.save(new User("Pessoa", "Paginada", "+55119999007113"));
        var entries = new ArrayList<SupportRequest>();
        for (int i = 0; i < 101; i++) entries.add(new SupportRequest(requester.getId(), "Chamado " + i, "Mensagem"));
        requests.saveAll(entries);
        var auth = auth(requester, UserRole.PARTICIPANT);
        mvc.perform(get("/support/requests").param("page", "1").param("size", "100").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].subject").value("Chamado 100"));
        mvc.perform(get("/support/requests").param("size", "101").with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentRoutingHasOneDestinationAndOneAuditEvent() throws Exception {
        var requester = users.save(new User("Pessoa", "Concorrente", "+55119999007114"));
        var first = organizations.save(new Organization("Destino concorrente A", "ASSOCIATION"));
        var second = organizations.save(new Organization("Destino concorrente B", "ASSOCIATION"));
        var request = requests.save(new SupportRequest(requester.getId(), "Dúvida", "Mensagem"));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (var org : List.of(first, second)) futures.add(executor.submit(() -> {
                start.await();
                try { service.assign(requester.getId(), request.getPublicId(), org.getPublicId()); return true; }
                catch (la.meleva.fundraiser.shared.ErrorCode.BusinessException ex) {
                    assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.SUPPORT_ORGANIZATION_CONFLICT);
                    return false;
                }
            }));
            start.countDown();
            assertThat(futures.stream().map(f -> {
                try { return f.get(); } catch (Exception ex) { throw new IllegalStateException(ex); }
            }).filter(Boolean::booleanValue).count()).isEqualTo(1);
        }
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", request.getId())).hasSize(1);
    }

    private UsernamePasswordAuthenticationToken auth(User user, UserRole role) {
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(role));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
