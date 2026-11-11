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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    @Autowired javax.sql.DataSource dataSource;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

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

    @Test
    void concurrentReopeningsWithSameRevisionHaveOneWinner() throws Exception {
        var owner = users.save(new User("Pessoa", "Concorrente", "+55119999007301"));
        var agent = users.save(new User("Agente", "Concorrente", "+55119999007302"));
        var org = organizations.save(new Organization("Organização concorrente", "ASSOCIATION"));
        memberships.save(new Membership(org, agent, UserRole.SUPPORT));
        var entry = new SupportRequest(owner.getId(), "Dúvida", "Mensagem");
        entry.assignOrganization(org.getId());
        var saved = requests.save(entry);
        service.transition(agent.getId(), saved.getPublicId(), SupportRequest.SupportStatus.IN_PROGRESS);
        service.transition(agent.getId(), saved.getPublicId(), SupportRequest.SupportStatus.CLOSED);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 2; i++) futures.add(executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
                try {
                    service.transition(agent.getId(), saved.getPublicId(), SupportRequest.SupportStatus.IN_PROGRESS, 2L);
                    return true;
                } catch (la.meleva.fundraiser.shared.ErrorCode.BusinessException ex) {
                    assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.SUPPORT_STATUS_TRANSITION_CONFLICT);
                    return false;
                }
            }));
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = new ArrayList<Boolean>();
            for (var future : futures) results.add(future.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(results).containsExactlyInAnyOrder(true, false);
        }
        assertThat(requests.findById(saved.getId()).orElseThrow().getStatusRevision()).isEqualTo(3);
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(3);
    }

    @Test
    void reopeningRollsBackStatusRevisionFlagAndAuditTogether() {
        var owner = users.save(new User("Pessoa", "Reversão", "+55119999007303"));
        var agent = users.save(new User("Agente", "Reversão", "+55119999007304"));
        var org = organizations.save(new Organization("Organização reversão", "ASSOCIATION"));
        memberships.save(new Membership(org, agent, UserRole.SUPPORT));
        var entry = new SupportRequest(owner.getId(), "Dúvida", "Mensagem");
        entry.assignOrganization(org.getId());
        var saved = requests.save(entry);
        service.transition(agent.getId(), saved.getPublicId(), SupportRequest.SupportStatus.IN_PROGRESS);
        service.transition(agent.getId(), saved.getPublicId(), SupportRequest.SupportStatus.CLOSED);
        assertThatThrownBy(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                .executeWithoutResult(tx -> {
                    service.transition(agent.getId(), saved.getPublicId(), SupportRequest.SupportStatus.IN_PROGRESS, 2L);
                    throw new IllegalStateException("falha sintética");
                })).isInstanceOf(IllegalStateException.class).hasMessage("falha sintética");
        var row = jdbc.queryForMap("SELECT status,status_revision,has_reopened FROM support_requests WHERE id=?", saved.getId());
        assertThat(row.get("status")).isEqualTo("CLOSED");
        assertThat(((Number) row.get("status_revision")).longValue()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT has_reopened FROM support_requests WHERE id=?", Boolean.class, saved.getId())).isFalse();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(2);
    }

    @Test
    void onlyOrganizationSupportCanAdvanceCloseAndReopenTicketWithAudit() throws Exception {
        var requester = users.save(new User("Pessoa", "Solicitante", "+55119999007121"));
        var agent = users.save(new User("Agente", "Destino", "+55119999007122"));
        var outsider = users.save(new User("Agente", "Externo", "+55119999007123"));
        var org = organizations.save(new Organization("Atendimento A", "ASSOCIATION"));
        var other = organizations.save(new Organization("Atendimento B", "ASSOCIATION"));
        memberships.save(new Membership(org, agent, UserRole.SUPPORT));
        memberships.save(new Membership(other, outsider, UserRole.SUPPORT));
        var ownerAuth = auth(requester, UserRole.PARTICIPANT);
        var agentAuth = auth(agent, UserRole.SUPPORT);
        var outsiderAuth = auth(outsider, UserRole.SUPPORT);
        var created = mvc.perform(post("/support/requests").with(authentication(ownerAuth)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"subject\":\"Dúvida\",\"message\":\"Ajuda\",\"organizationId\":\""
                        + org.getPublicId() + "\"}")).andExpect(status().isCreated()).andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        String path = "/support/requests/" + id + "/status";
        String progress = "{\"status\":\"IN_PROGRESS\"}";
        mvc.perform(patch(path).with(authentication(agentAuth)).contentType(MediaType.APPLICATION_JSON)
                .content(progress)).andExpect(status().isForbidden());
        mvc.perform(patch(path).with(authentication(outsiderAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(progress)).andExpect(status().isForbidden());
        mvc.perform(patch(path).with(authentication(ownerAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(progress)).andExpect(status().isForbidden());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}")).andExpect(status().isConflict());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(progress)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(progress)).andExpect(status().isOk());
        mvc.perform(get("/support/requests").param("organizationId", org.getPublicId()).param("status", "IN_PROGRESS")
                .with(authentication(agentAuth))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(patch(path).with(authentication(outsiderAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(progress)).andExpect(status().isForbidden());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"OPEN\"}")).andExpect(status().isConflict());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(progress)).andExpect(status().isConflict());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\",\"revision\":2}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS")).andExpect(jsonPath("$.revision").value(3));
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\",\"revision\":2}")).andExpect(status().isConflict());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\",\"revision\":3}")).andExpect(status().isOk());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}")).andExpect(status().isConflict());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\",\"revision\":1}")).andExpect(status().isConflict());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\",\"revision\":3}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(4));
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\",\"revision\":2}")).andExpect(status().isConflict());
        mvc.perform(patch(path).with(authentication(agentAuth)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\",\"revision\":4}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(5));
        var saved = requests.findByPublicId(id).orElseThrow();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(6);
    }

    @Test
    void concurrentStatusRetriesCreateOneAuditEvent() throws Exception {
        var requester = users.save(new User("Pessoa", "Atendida", "+55119999007131"));
        var agent = users.save(new User("Agente", "Concorrente", "+55119999007132"));
        var org = organizations.save(new Organization("Atendimento concorrente", "ASSOCIATION"));
        memberships.save(new Membership(org, agent, UserRole.SUPPORT));
        var entry = new SupportRequest(requester.getId(), "Dúvida", "Mensagem");
        entry.assignOrganization(org.getId());
        var saved = requests.save(entry);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < 12; i++) futures.add(executor.submit(() -> {
                start.await();
                return service.transition(agent.getId(), saved.getPublicId(), SupportRequest.SupportStatus.IN_PROGRESS).status();
            }));
            start.countDown();
            for (var future : futures) assertThat(future.get()).isEqualTo("IN_PROGRESS");
        }
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(1);
    }

    @Test
    void outsiderIsDeniedBeforeWaitingForTicketLock() throws Exception {
        var requester = users.save(new User("Pessoa", "Atendida", "+55119999007151"));
        var outsider = users.save(new User("Agente", "Externo", "+55119999007152"));
        var ownerOrg = organizations.save(new Organization("Chamado bloqueado A", "ASSOCIATION"));
        var outsiderOrg = organizations.save(new Organization("Chamado bloqueado B", "ASSOCIATION"));
        memberships.save(new Membership(outsiderOrg, outsider, UserRole.SUPPORT));
        var entry = new SupportRequest(requester.getId(), "Dúvida", "Mensagem");
        entry.assignOrganization(ownerOrg.getId());
        var saved = requests.save(entry);
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var holder = executor.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                    .executeWithoutResult(tx -> {
                        requests.lockByPublicId(saved.getPublicId()).orElseThrow();
                        locked.countDown();
                        try {
                            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                throw new IllegalStateException("Tempo de espera excedido");
                        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                    }));
            try {
                assertThat(locked.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                var denied = executor.submit(() -> {
                    try {
                        service.transition(outsider.getId(), saved.getPublicId(), SupportRequest.SupportStatus.IN_PROGRESS);
                        return null;
                    } catch (la.meleva.fundraiser.shared.ErrorCode.BusinessException ex) { return ex.errorCode(); }
                });
                assertThat(denied.get(5, java.util.concurrent.TimeUnit.SECONDS))
                        .isEqualTo(la.meleva.fundraiser.shared.ErrorCode.AUTH_RESOURCE_ACCESS_DENIED);
            } finally { release.countDown(); }
            holder.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).isEmpty();
    }

    @Test
    void organizationSuspensionWaitsForStatusTransitionAndBlocksTheNextOne() throws Exception {
        var requester = users.save(new User("Pessoa", "Atendida", "+55119999007141"));
        var agent = users.save(new User("Agente", "Suporte", "+55119999007142"));
        var org = organizations.save(new Organization("Organização suspensa", "ASSOCIATION"));
        memberships.save(new Membership(org, agent, UserRole.SUPPORT));
        var entry = new SupportRequest(requester.getId(), "Dúvida", "Mensagem");
        entry.assignOrganization(org.getId());
        var saved = requests.save(entry);
        assertOrganizationSuspensionWaits(org.getId(), () -> service.transition(agent.getId(), saved.getPublicId(),
                SupportRequest.SupportStatus.IN_PROGRESS));
        assertThatThrownBy(() -> service.transition(agent.getId(), saved.getPublicId(), SupportRequest.SupportStatus.CLOSED))
                .isInstanceOfSatisfying(la.meleva.fundraiser.shared.ErrorCode.BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.AUTH_RESOURCE_ACCESS_DENIED));
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(1);
    }

    @Test
    void organizationSuspensionWaitsForAuthorizedQueueRead() throws Exception {
        var agent = users.save(new User("Agente", "Fila", "+55119999007143"));
        var org = organizations.save(new Organization("Fila suspensa", "ASSOCIATION"));
        memberships.save(new Membership(org, agent, UserRole.SUPPORT));
        assertOrganizationSuspensionWaits(org.getId(), () -> service.list(agent.getId(), org.getPublicId(), 0, 100,
                SupportRequest.SupportStatus.OPEN));
        assertThatThrownBy(() -> service.list(agent.getId(), org.getPublicId(), 0, 100, SupportRequest.SupportStatus.OPEN))
                .isInstanceOf(la.meleva.fundraiser.shared.ErrorCode.BusinessException.class);
    }

    private void assertOrganizationSuspensionWaits(Long organizationId, Runnable operation) throws Exception {
        var completed = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var action = executor.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                    .executeWithoutResult(tx -> {
                        operation.run();
                        completed.countDown();
                        try {
                            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                throw new IllegalStateException("Tempo de espera excedido");
                        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                    }));
            assertThat(completed.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var suspension = executor.submit(() -> {
                try (var connection = dataSource.getConnection(); var sql = connection.createStatement()) {
                    sql.execute("SET SESSION innodb_lock_wait_timeout=1");
                    try (var update = connection.prepareStatement("UPDATE organizations SET status='SUSPENDED' WHERE id=?")) {
                        update.setLong(1, organizationId);
                        update.executeUpdate();
                        return 0;
                    } catch (java.sql.SQLException ex) {
                        return ex.getErrorCode();
                    } finally {
                        sql.execute("SET SESSION innodb_lock_wait_timeout=DEFAULT");
                    }
                }
            });
            try {
                assertThat(suspension.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(1205);
            } finally { release.countDown(); }
            action.get(10, java.util.concurrent.TimeUnit.SECONDS);
            jdbc.update("UPDATE organizations SET status='SUSPENDED' WHERE id=?", organizationId);
        }
    }

    @Test
    void keyedOpeningReplaysAndRejectsChangedPayloadWithoutAffectingLegacyRequests() throws Exception {
        var user = users.save(new User("Pessoa", "Chave", "+55119999007161"));
        var other = users.save(new User("Pessoa", "Outra", "+55119999007162"));
        var org = organizations.save(new Organization("Abertura idempotente", "ASSOCIATION"));
        var userAuth = auth(user, UserRole.PARTICIPANT);
        String key = "641a160e-cafe-4bc4-9bb0-055c4b7cae71";
        String body = "{\"subject\":\"Dúvida\",\"message\":\"Mensagem\",\"organizationId\":\"" + org.getPublicId() + "\"}";
        var first = mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        String id = JsonPath.read(first.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .header("Idempotency-Key", key.toUpperCase(java.util.Locale.ROOT)).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("Dúvida", " Dúvida ").replace(org.getPublicId(), org.getPublicId().toUpperCase(java.util.Locale.ROOT))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("Mensagem", "Outra mensagem"))).andExpect(status().isConflict());
        var otherResult = mvc.perform(post("/support/requests").with(authentication(auth(other, UserRole.PARTICIPANT))).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        assertThat((String) JsonPath.read(otherResult.getResponse().getContentAsString(), "$.id")).isNotEqualTo(id);
        var saved = requests.findByPublicId(id).orElseThrow();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(1);
        var legacy = mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
        assertThat((String) JsonPath.read(legacy.getResponse().getContentAsString(), "$.id")).isNotEqualTo(id);
        var anotherLegacy = mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
        assertThat((String) JsonPath.read(anotherLegacy.getResponse().getContentAsString(), "$.id"))
                .isNotEqualTo((String) JsonPath.read(legacy.getResponse().getContentAsString(), "$.id"));
        mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .header("Idempotency-Key", "chave-inválida").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        jdbc.update("UPDATE users SET status='SUSPENDED' WHERE id=?", user.getId());
        mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void keyedOpeningReplaysAfterRoutingEvenWhenDestinationIsSuspended() throws Exception {
        var user = users.save(new User("Pessoa", "Destino", "+55119999007163"));
        var org = organizations.save(new Organization("Destino posterior", "ASSOCIATION"));
        var userAuth = auth(user, UserRole.PARTICIPANT);
        String key = "59e9f47e-600a-453f-9a70-d995c03d524e";
        String body = "{\"subject\":\"Dúvida\",\"message\":\"Mensagem\"}";
        var created = mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Dúvida\",\"message\":\"Mensagem\",\"organizationId\":\"unassigned\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/support/requests/" + id + "/organization").with(authentication(userAuth)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"organizationId\":\"" + org.getPublicId() + "\"}"))
                .andExpect(status().isOk());
        jdbc.update("UPDATE organizations SET status='SUSPENDED' WHERE id=?", org.getId());
        mvc.perform(post("/support/requests").with(authentication(userAuth)).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.organizationId").value(org.getPublicId()));
        var saved = requests.findByPublicId(id).orElseThrow();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(2);
    }

    @Test
    void concurrentKeyedOpeningsCreateOneRequestAndOneAuditEvent() throws Exception {
        var user = users.save(new User("Pessoa", "Concorrente", "+55119999007164"));
        var org = organizations.save(new Organization("Aberturas concorrentes", "ASSOCIATION"));
        String key = "bfb48a04-14c4-4f12-89ee-983d2d3e8fa5";
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < 12; i++) futures.add(executor.submit(() -> {
                start.await();
                return service.open(user.getId(), "Dúvida", "Mensagem", org.getPublicId(), key).id();
            }));
            start.countDown();
            var ids = new java.util.HashSet<String>();
            for (var future : futures) ids.add(future.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(ids).hasSize(1);
        }
        var saved = requests.findByUserIdAndIdempotencyKey(user.getId(), key).orElseThrow();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(1);
    }

    @Test
    void concurrentDifferentPayloadsForOneKeyHaveOneWinnerAndOneConflict() throws Exception {
        var user = users.save(new User("Pessoa", "Conflito", "+55119999007165"));
        String key = "cd7ad24e-33ad-4ea0-90ad-a804c412a65f";
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (String message : List.of("Mensagem A", "Mensagem B")) futures.add(executor.submit(() -> {
                start.await();
                try { service.open(user.getId(), "Dúvida", message, null, key); return true; }
                catch (la.meleva.fundraiser.shared.ErrorCode.BusinessException ex) {
                    assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.IDEMPOTENCY_CONFLICT);
                    return false;
                }
            }));
            start.countDown();
            int successes = 0;
            for (var future : futures) if (future.get(20, java.util.concurrent.TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
        }
        var saved = requests.findByUserIdAndIdempotencyKey(user.getId(), key).orElseThrow();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("support_request", saved.getId())).hasSize(1);
    }

    private UsernamePasswordAuthenticationToken auth(User user, UserRole role) {
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(role));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
