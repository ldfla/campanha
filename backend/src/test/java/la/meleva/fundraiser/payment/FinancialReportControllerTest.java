package la.meleva.fundraiser.payment;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FinancialReportControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationRepository organizations;
    @Autowired CampaignRepository campaigns;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;

    @Test
    void aggregatesEachEventDateAndIsolatesCampaignsAndOrganizations() throws Exception {
        var org = organizations.save(new Organization("Relatórios A", "ASSOCIATION"));
        var otherOrg = organizations.save(new Organization("Relatórios B", "ASSOCIATION"));
        var campaign = campaigns.save(new Campaign(org.getId(), "Relatório A", "report-a-p14", CampaignMode.DEMO));
        var other = campaigns.save(new Campaign(otherOrg.getId(), "Relatório B", "report-b-p14", CampaignMode.DEMO));
        var user = users.save(new User("Pessoa", "Relatório", "+55119999008202"));
        memberships.save(new Membership(org, user, UserRole.FINANCE));
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(UserRole.FINANCE));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        var event = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(60);
        var from = event.minusSeconds(1);
        var until = event.plusSeconds(1);
        String orderRef = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO orders(public_id,campaign_id,user_id,status,total_cents,currency,mode,idempotency_key,created_at,updated_at,version) VALUES (?,?,?,'REFUNDED',1000,'BRL','DEMO',?,?,?,0)",
                orderRef, campaign.getId(), user.getId(), UUID.randomUUID().toString(), Timestamp.from(event), Timestamp.from(event));
        Long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE public_id=?", Long.class, orderRef);
        jdbc.update("INSERT INTO payments(order_id,provider,status,amount_cents,currency,created_at,updated_at,confirmed_at) VALUES (?,'MOCK','REFUNDED',1000,'BRL',?,?,?)",
                orderId, Timestamp.from(event), Timestamp.from(event), Timestamp.from(event));
        Long paymentId = jdbc.queryForObject("SELECT id FROM payments WHERE order_id=?", Long.class, orderId);
        jdbc.update("INSERT INTO refunds(payment_id,amount_cents,currency,status,requested_at,completed_at) VALUES (?,400,'BRL','COMPLETED',?,?)",
                paymentId, Timestamp.from(event), Timestamp.from(event));
        jdbc.update("INSERT INTO financial_ledger(campaign_id,source_ref,entry_type,amount_cents,currency,posted_at) VALUES (?,?,'PAYMENT_CONFIRMED',1000,'BRL',?),(?,?,'PAYMENT_REFUND',-400,'BRL',?)",
                campaign.getId(), orderRef, Timestamp.from(event), campaign.getId(), orderRef, Timestamp.from(event));
        String endpoint = "/financial/campaigns/{campaignId}/reports";
        mvc.perform(get(endpoint, campaign.getPublicId()).with(authentication(auth))
                        .param("from", from.toString()).param("until", until.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.simulation").value(true))
                .andExpect(jsonPath("$.ordersCreated.cents").value(1000))
                .andExpect(jsonPath("$.chargesCreated.cents").value(1000))
                .andExpect(jsonPath("$.paymentsConfirmed.cents").value(1000))
                .andExpect(jsonPath("$.refundsCompleted.cents").value(400))
                .andExpect(jsonPath("$.ledgerPosted.cents").value(600))
                .andExpect(jsonPath("$.refundLedgerPosted.cents").value(-400));
        mvc.perform(get(endpoint, campaign.getPublicId()).with(authentication(auth))
                        .param("from", until.toString()).param("until", until.plusSeconds(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ordersCreated.count").value(0));
        mvc.perform(get(endpoint, other.getPublicId()).with(authentication(auth))
                        .param("from", from.toString()).param("until", until.toString()))
                .andExpect(status().isForbidden());
        String export = endpoint + "/export";
        mvc.perform(get(export, campaign.getPublicId()).with(authentication(auth))
                        .param("from", from.toString()).param("until", until.toString()))
                .andExpect(status().isOk()).andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("SIMULAÇÃO")))
                .andExpect(content().string(containsString("\"lancamentos_ledger\",\"2\",\"600\"")));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='FINANCIAL_SIMULATION_REPORT_EXPORTED' AND actor_id=? AND entity_id=?", Long.class, user.getId(), campaign.getId()));
        mvc.perform(get(export, other.getPublicId()).with(authentication(auth))
                        .param("from", from.toString()).param("until", until.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get(endpoint, campaign.getPublicId()).with(authentication(auth))
                        .param("from", until.toString()).param("until", from.toString()))
                .andExpect(status().isBadRequest());
    }
}
