package la.meleva.fundraiser.participation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BeneficiaryControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired CampaignRepository campaigns;
    @Autowired ParticipationService participations;

    @Test
    void managerRegistersEntityAndParticipantSelectsItUsingSession() throws Exception {
        String phone = "+55119999001239";
        la.meleva.fundraiser.identity.AuthTestSupport.register(mvc, "Pessoa", "Teste", phone);
        var user = users.findByPhoneNumber(phone).orElseThrow();
        var organization = organizations.save(new Organization("Organização sessão", "ASSOCIATION"));
        memberships.save(new Membership(organization, user, UserRole.CAMPAIGN_MANAGER));
        var session = la.meleva.fundraiser.identity.AuthTestSupport.verify(mvc, phone);
        var campaign = campaigns.save(new Campaign(organization.getId(), "Campanha sessão", "beneficiary-session", CampaignMode.DEMO));
        String endpoint = "/participations/campaigns/{campaignId}/beneficiaries";
        var created = mvc.perform(post(endpoint, campaign.getPublicId()).cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Entidade escolhida\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Entidade escolhida")).andReturn();
        String entityId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mvc.perform(get(endpoint, campaign.getPublicId()).cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(entityId));
        campaign.setNumberRange(0, 1);
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        campaigns.save(campaign);
        participations.initializeSeries(campaign.getPublicId());
        String body = "{\"idempotencyKey\":\"" + UUID.randomUUID() + "\",\"count\":1,\"entityId\":\"" + entityId + "\"}";
        mvc.perform(post("/participations/campaigns/{campaignId}", campaign.getPublicId()).cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.numbers[0].formattedNumber").value("00000"));
    }
}
