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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FinancialOrganizationReportControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationRepository organizations;
    @Autowired CampaignRepository campaigns;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;

    @Test
    void aggregatesDemoCampaignsOfTheOrganizationAndHidesOthers() throws Exception {
        var org = organizations.save(new Organization("Orçamento A", "ASSOCIATION"));
        var otherOrg = organizations.save(new Organization("Orçamento B", "ASSOCIATION"));
        var first = campaigns.save(new Campaign(org.getId(), "Campanha 1", "org-report-1", CampaignMode.DEMO));
        var second = campaigns.save(new Campaign(org.getId(), "Campanha 2", "org-report-2", CampaignMode.DEMO));
        var foreign = campaigns.save(new Campaign(otherOrg.getId(), "Campanha externa", "org-report-3", CampaignMode.DEMO));
        var finance = users.save(new User("Financeira", "Organização", "+55119999008301"));
        memberships.save(new Membership(org, finance, UserRole.FINANCE));
        var financePrincipal = new AppUserDetails(finance.getId(), finance.getPublicId(), finance.getPhoneNumber(), List.of(UserRole.FINANCE));
        var auth = new UsernamePasswordAuthenticationToken(financePrincipal, null, financePrincipal.getAuthorities());

        var event = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(60);
        insertOrder(first, finance, event);
        insertOrder(second, finance, event);
        insertOrder(foreign, finance, event);

        String from = event.minusSeconds(1).toString();
        String until = event.plusSeconds(1).toString();
        mvc.perform(get("/financial/organizations/{id}/reports", org.getId())
                        .param("from", from).param("until", until).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulation").value(true))
                .andExpect(jsonPath("$.campaigns.length()").value(2))
                .andExpect(jsonPath("$.ordersCreated.count").value(2))
                .andExpect(jsonPath("$.ordersCreated.cents").value(2000))
                .andExpect(jsonPath("$.notice").isString());
    }

    @Test
    void deniesAccessWithoutOrganizationMembership() throws Exception {
        var org = organizations.save(new Organization("Orçamento restrito", "ASSOCIATION"));
        var outsider = users.save(new User("Pessoa", "Externa", "+55119999008302"));
        var outsiderPrincipal = new AppUserDetails(outsider.getId(), outsider.getPublicId(), outsider.getPhoneNumber(), List.of(UserRole.FINANCE));
        var auth = new UsernamePasswordAuthenticationToken(outsiderPrincipal, null, outsiderPrincipal.getAuthorities());
        mvc.perform(get("/financial/organizations/{id}/reports", org.getId())
                        .param("from", Instant.now().minusSeconds(10).toString())
                        .param("until", Instant.now().toString()).with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    private void insertOrder(Campaign campaign, User user, Instant at) {
        jdbc.update("INSERT INTO orders(public_id,campaign_id,user_id,status,total_cents,currency,mode,idempotency_key,created_at,updated_at,version) "
                        + "VALUES (?,?,?,'CREATED',1000,'BRL','DEMO',?,?,?,0)",
                UUID.randomUUID().toString(), campaign.getId(), user.getId(), UUID.randomUUID().toString(),
                Timestamp.from(at), Timestamp.from(at));
    }
}
