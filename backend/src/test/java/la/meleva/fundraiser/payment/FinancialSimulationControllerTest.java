package la.meleva.fundraiser.payment;

import java.util.List;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FinancialSimulationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired OrganizationRepository organizations;
    @Autowired CampaignRepository campaigns;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;

    private static final String BODY = """
            {"priceCents":100,"unitCostCents":56,"capacity":100,"quantity":100,
             "feeBasisPoints":1000,"taxBasisPoints":0,"targetProfitCents":2000,
             "fixedCosts":[{"category":"PRIZE","cents":1000}]}
            """;

    @Test
    void requiresActiveMembershipAndCsrfAndRejectsCrossOrganizationAccess() throws Exception {
        var org = organizations.save(new Organization("Organização financeira", "ASSOCIATION"));
        var other = organizations.save(new Organization("Outra organização financeira", "ASSOCIATION"));
        var campaign = campaigns.save(new Campaign(org.getId(), "Financeiro", "financeiro-p14", CampaignMode.DEMO));
        var otherCampaign = campaigns.save(new Campaign(other.getId(), "Outro financeiro", "outro-financeiro-p14", CampaignMode.DEMO));
        var user = users.save(new User("Pessoa", "Financeira", "+55119999008101"));
        var membership = memberships.save(new Membership(org, user, UserRole.FINANCE));
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(UserRole.FINANCE));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String path = "/financial/campaigns/{campaignId}/simulations";

        mvc.perform(post(path, campaign.getPublicId()).with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(post(path, campaign.getPublicId()).with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hypothetical").value(true))
                .andExpect(jsonPath("$.currency").value("BRL")).andExpect(jsonPath("$.profitCents").value(2400))
                .andExpect(jsonPath("$.breakEvenQuantity").value(30));
        mvc.perform(post(path, otherCampaign.getPublicId()).with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(post(path, campaign.getPublicId()).with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"quantity\":100", "\"quantity\":101")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path, campaign.getPublicId()).with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"cents\":1000", "\"cents\":-1")))
                .andExpect(status().isBadRequest());
        for (String fractional : List.of(
                BODY.replace("\"priceCents\":100", "\"priceCents\":100.99"),
                BODY.replace("\"quantity\":100", "\"quantity\":99.9"),
                BODY.replace("\"feeBasisPoints\":1000", "\"feeBasisPoints\":1000.5"),
                BODY.replace("\"cents\":1000", "\"cents\":1000.5"))) {
            mvc.perform(post(path, campaign.getPublicId()).with(authentication(auth)).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(fractional)).andExpect(status().isBadRequest());
        }
        membership.deactivate();
        memberships.saveAndFlush(membership);
        mvc.perform(post(path, campaign.getPublicId()).with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }
}
