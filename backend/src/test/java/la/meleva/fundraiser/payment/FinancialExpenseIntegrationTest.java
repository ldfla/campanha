package la.meleva.fundraiser.payment;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.AuthTestSupport;
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
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FinancialExpenseIntegrationTest {
    private static final String KEY = "5835e57d-e22a-447e-a399-2d74a993467d";
    private static final Instant OCCURRED = Instant.parse("2025-01-02T03:04:05Z");
    @Autowired MockMvc mvc;
    @Autowired OrganizationRepository organizations;
    @Autowired CampaignRepository campaigns;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired FinancialExpenseRepository expenses;
    @Autowired FinancialExpenseService service;
    @Autowired AuditEventRepository audits;
    @Autowired JdbcTemplate jdbc;

    private static String body(String amount) {
        return "{\"idempotencyKey\":\"" + KEY + "\",\"category\":\"PLATFORM\",\"amountCents\":" + amount
                + ",\"description\":\"Taxa declarada\",\"occurredAt\":\"2025-01-02T03:04:05Z\"}";
    }

    @Test
    void sessionCsrfOrganizationValidationAndIdempotency() throws Exception {
        String phone = "+5511977714511";
        AuthTestSupport.register(mvc, "Pessoa", "Financeira", phone);
        User user = users.findByPhoneNumber(phone).orElseThrow();
        var org = organizations.save(new Organization("Custos A", "ASSOCIATION"));
        var otherOrg = organizations.save(new Organization("Custos B", "ASSOCIATION"));
        var campaign = campaigns.save(new Campaign(org.getId(), "Custos", "custos-p14-a", CampaignMode.DEMO));
        var other = campaigns.save(new Campaign(otherOrg.getId(), "Custos", "custos-p14-b", CampaignMode.DEMO));
        memberships.save(new Membership(org, user, UserRole.FINANCE));
        // FINANCE em outra organização não concede acesso financeiro por um vínculo de gestor.
        memberships.save(new Membership(otherOrg, user, UserRole.CAMPAIGN_MANAGER));
        var session = AuthTestSupport.verify(mvc, phone);
        String path = "/financial/campaigns/" + campaign.getPublicId() + "/expenses";
        mvc.perform(post(path).cookie(session).contentType(MediaType.APPLICATION_JSON).content(body("1500")))
                .andExpect(status().isForbidden());
        var first = mvc.perform(post(path).cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("1500"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLARADA"))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.amountCents").value(1500)).andReturn();
        String id = JsonPath.read(first.getResponse().getContentAsString(), "$.id");
        mvc.perform(post(path).cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("1500"))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(post(path).cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("1501"))).andExpect(status().isConflict());
        mvc.perform(post(path).cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("-1"))).andExpect(status().isBadRequest());
        mvc.perform(post(path).cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("1.5"))).andExpect(status().isBadRequest());
        mvc.perform(get(path).cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/financial/campaigns/" + other.getPublicId() + "/expenses").cookie(session))
                .andExpect(status().isForbidden());
        mvc.perform(post("/financial/campaigns/" + other.getPublicId() + "/expenses").cookie(session)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body("1500")))
                .andExpect(status().isForbidden());
        var persisted = expenses.findByCampaignIdAndIdempotencyKey(campaign.getId(), KEY).orElseThrow();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("financial_expense", persisted.id())).hasSize(1);
        String recordedHash = jdbc.queryForObject("SELECT event_hash FROM audit_events WHERE entity_type = ? AND entity_id = ?",
                String.class, "financial_expense", persisted.id());
        assertThat(recordedHash).isEqualTo(persisted.auditHash());
        String newKey = "3e723d54-a1e6-4f57-a970-c5b11f246bb9";
        mvc.perform(post(path).cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("9007199254740991").replace(KEY, newKey)
                                .replace("2025-01-02T03:04:05Z", "1970-01-01T00:00:01Z")))
                .andExpect(status().isOk());
        var boundary = expenses.findByCampaignIdAndIdempotencyKey(campaign.getId(), newKey).orElseThrow();
        assertThat(boundary.amountCents()).isEqualTo(9_007_199_254_740_991L);
        assertThat(boundary.occurredAt()).isEqualTo(Instant.parse("1970-01-01T00:00:01Z"));
    }

    @Test
    void simultaneousRequestsWithSameKeyCreateOneExpenseAndAudit() throws Exception {
        var org = organizations.save(new Organization("Custos concorrentes", "ASSOCIATION"));
        var campaign = campaigns.save(new Campaign(org.getId(), "Custos concorrentes", "custos-p14-conc", CampaignMode.DEMO));
        var user = users.save(new User("Pessoa", "Concorrente", "+5511977714512"));
        memberships.save(new Membership(org, user, UserRole.FINANCE));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < 16; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return service.create(user.getId(), campaign.getPublicId(), KEY,
                            FinancialSimulation.CostCategory.PLATFORM, 1500, "Taxa declarada", OCCURRED).id();
                }));
            }
            start.countDown();
            var ids = new java.util.HashSet<String>();
            for (var future : futures) ids.add(future.get());
            assertThat(ids).hasSize(1);
        }
        var persisted = expenses.findByCampaignIdAndIdempotencyKey(campaign.getId(), KEY).orElseThrow();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAt("financial_expense", persisted.id())).hasSize(1);
    }
}
