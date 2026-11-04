package la.meleva.fundraiser.payment;

import com.jayway.jsonpath.JsonPath;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.AuthTestSupport;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FinancialScenarioIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrganizationRepository organizations;
    @Autowired CampaignRepository campaigns;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;

    private static final String INPUT = """
            {"priceCents":100,"unitCostCents":56,"capacity":100,"quantity":100,
             "feeBasisPoints":1000,"taxBasisPoints":0,"targetProfitCents":2000,
             "fixedCosts":[{"category":"PRIZE","cents":1000}]}
            """;

    @Test
    void persistsEditsAndProtectsCampaignAndVersions() throws Exception {
        String phone = "+5511977714040";
        AuthTestSupport.register(mvc, "Gestora", "Financeira", phone);
        var user = users.findByPhoneNumber(phone).orElseThrow();
        var org = organizations.save(new Organization("Cenários financeiros", "ASSOCIATION"));
        var otherOrg = organizations.save(new Organization("Outra entidade financeira", "ASSOCIATION"));
        var campaign = campaigns.save(new Campaign(org.getId(), "Cenário", "p14-cenario", CampaignMode.DEMO));
        var other = campaigns.save(new Campaign(otherOrg.getId(), "Outro", "p14-outro-cenario", CampaignMode.DEMO));
        memberships.save(new Membership(org, user, UserRole.CAMPAIGN_MANAGER));
        var cookie = AuthTestSupport.verify(mvc, phone);
        String path = "/financial/campaigns/" + campaign.getPublicId() + "/scenarios";
        String body = "{\"name\":\"Planejamento\",\"scenario\":" + INPUT + "}";
        mvc.perform(post(path).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        var created = mvc.perform(post(path).cookie(cookie).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.hypothetical").value(true)).andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.scenario.fixedCosts[0].cents").value(1000)).andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mvc.perform(get(path).cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Planejamento"));
        String update = "{\"name\":\"Atualizado\",\"version\":0,\"scenario\":"
                + INPUT.replace("\"cents\":1000", "\"cents\":1500") + "}";
        mvc.perform(put(path + "/" + id).cookie(cookie).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.scenario.fixedCosts[0].cents").value(1500));
        mvc.perform(put(path + "/" + id).cookie(cookie).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isConflict());
        mvc.perform(get("/financial/campaigns/" + other.getPublicId() + "/scenarios").cookie(cookie))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"priceCents\":100", "\"priceCents\":100.5")))
                .andExpect(status().isBadRequest());
    }
}
