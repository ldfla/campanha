package la.meleva.fundraiser.ranking;

import java.util.List;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.ranking.dto.CreateSnapshotRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RankingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired RankingService rankings;

    @Test
    void publicLeaderboardDoesNotRequireAuthenticationAndIsLabeledAsSimulation() throws Exception {
        Campaign campaign = draft("controller-public");

        mockMvc.perform(get("/rankings/campaigns/{campaignId}/PARTICIPANT", campaign.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignPublicId").value(campaign.getPublicId()))
                .andExpect(jsonPath("$.simulation").value(true))
                .andExpect(jsonPath("$.entries").isEmpty())
                .andExpect(jsonPath("$.notice", containsString("SIMULAÇÃO")));

        mockMvc.perform(get("/rankings/campaigns/{campaignId}/INVALID", campaign.getPublicId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ruleRegistrationRequiresRoleAndCsrf() throws Exception {
        Campaign campaign = draft("controller-rules");
        User manager = users.save(new User("Marta", "Gestora", "+551199990071"));
        AppUserDetails principal = new AppUserDetails(manager.getId(), manager.getPublicId(),
                manager.getPhoneNumber(), List.of(UserRole.CAMPAIGN_MANAGER));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String body = """
                {"scope":"PROMOTER","metric":"REFERRAL_CONFIRMED_PARTICIPATIONS"}
                """;

        mockMvc.perform(post("/rankings/campaigns/{campaignId}/rules", campaign.getPublicId())
                        .with(authentication(auth)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/rankings/campaigns/{campaignId}/rules", campaign.getPublicId())
                        .with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleVersion").value(1))
                .andExpect(jsonPath("$.legallyApproved").value(false));

        User participant = users.save(new User("Pessoa", "Comum", "+551199990072"));
        AppUserDetails participantPrincipal = new AppUserDetails(participant.getId(), participant.getPublicId(),
                participant.getPhoneNumber(), List.of(UserRole.PARTICIPANT));
        var participantAuth = new UsernamePasswordAuthenticationToken(participantPrincipal, null,
                participantPrincipal.getAuthorities());

        mockMvc.perform(post("/rankings/campaigns/{campaignId}/rules", campaign.getPublicId())
                        .with(authentication(participantAuth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void snapshotCreationAndExportAreScopedToTheCampaign() throws Exception {
        Campaign campaign = draft("controller-snapshot");
        Campaign other = draft("controller-snapshot-other");
        User auditor = users.save(new User("Dora", "Auditora", "+551199990081"));
        AppUserDetails principal = new AppUserDetails(auditor.getId(), auditor.getPublicId(),
                auditor.getPhoneNumber(), List.of(UserRole.DRAW_AUDITOR));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/rankings/campaigns/{campaignId}/snapshots", campaign.getPublicId())
                        .with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"scope\":\"PARTICIPANT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.snapshotId").isString())
                .andExpect(jsonPath("$.snapshotHash").isString());

        var created = rankings.snapshot(auditor.getId(), campaign.getPublicId(),
                new CreateSnapshotRequest(RankingScope.PARTICIPANT, null));

        mockMvc.perform(get("/rankings/campaigns/{campaignId}/snapshots/{snapshotId}/export",
                        campaign.getPublicId(), created.snapshotId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.snapshotHash").value(created.snapshotHash()));

        mockMvc.perform(get("/rankings/campaigns/{campaignId}/snapshots/{snapshotId}", other.getPublicId(),
                        created.snapshotId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void snapshotWithoutScopeIsRejected() throws Exception {
        Campaign campaign = draft("controller-snapshot-invalid");
        User auditor = users.save(new User("Dora", "Auditora", "+551199990082"));
        AppUserDetails principal = new AppUserDetails(auditor.getId(), auditor.getPublicId(),
                auditor.getPhoneNumber(), List.of(UserRole.DRAW_AUDITOR));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/rankings/campaigns/{campaignId}/snapshots", campaign.getPublicId())
                        .with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cutoffAt\":null}"))
                .andExpect(status().isBadRequest());
    }

    private Campaign draft(String slug) {
        Organization organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        return campaigns.save(new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO));
    }
}
