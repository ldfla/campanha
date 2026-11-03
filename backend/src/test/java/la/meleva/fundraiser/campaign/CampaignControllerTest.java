package la.meleva.fundraiser.campaign;

import jakarta.servlet.http.Cookie;
import la.meleva.fundraiser.TestcontainersConfiguration;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CampaignControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private CampaignPrizeRepository prizeRepository;

    private Cookie registerAndGetSession(String phone) throws Exception {
        return la.meleva.fundraiser.identity.AuthTestSupport.registerAndSignIn(mockMvc, "Campaign", "Manager", phone);
    }

    private Cookie loginWithRole(String phone, UserRole role) throws Exception {
        la.meleva.fundraiser.identity.AuthTestSupport.register(mockMvc, "Campaign", "Manager", phone);

        var user = userRepository.findByPhoneNumber(phone).orElseThrow();
        var org = organizationRepository.findAll().stream().findFirst()
                .orElseGet(() -> organizationRepository.save(new Organization("Test Org", "ASSOCIATION")));
        membershipRepository.save(new Membership(org, user, role));

        return la.meleva.fundraiser.identity.AuthTestSupport.verify(mockMvc, phone);
    }

    @Test
    void createCampaignRequiresManagerRole() throws Exception {
        Cookie session = registerAndGetSession("+5511888880001");

        mockMvc.perform(post("/campaigns")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": "Teste", "slug": "teste-1", "mode": "DEMO"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreateCampaign() throws Exception {
        Cookie session = loginWithRole("+5511888880002", UserRole.CAMPAIGN_MANAGER);

        mockMvc.perform(post("/campaigns")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": "Ação entre Amigos", "slug": "acao-amigos", "description": "Campanha demo", "mode": "DEMO"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Ação entre Amigos"))
                .andExpect(jsonPath("$.slug").value("acao-amigos"))
                .andExpect(jsonPath("$.mode").value("DEMO"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalNumbers").value(100000));
    }

    @Test
    void canGetCampaignBySlug() throws Exception {
        Cookie session = loginWithRole("+5511888880003", UserRole.CAMPAIGN_MANAGER);

        mockMvc.perform(post("/campaigns")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": "Slug Test", "slug": "slug-test", "mode": "DEMO"}
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/campaigns/slug/slug-test").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Slug Test"));
    }

    @Test
    void canUpdateDraftCampaign() throws Exception {
        Cookie session = loginWithRole("+5511888880004", UserRole.CAMPAIGN_MANAGER);

        MvcResult created = mockMvc.perform(post("/campaigns")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": "Original", "slug": "update-test", "mode": "DEMO"}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String campaignId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/campaigns/" + campaignId)
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": "Atualizado", "description": "Nova descrição"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Atualizado"))
                .andExpect(jsonPath("$.description").value("Nova descrição"));
    }

    @Test
    void canTransitionDraftToLegalReview() throws Exception {
        Cookie session = loginWithRole("+5511888880005", UserRole.CAMPAIGN_MANAGER);

        MvcResult created = mockMvc.perform(post("/campaigns")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": "Transition Test", "slug": "transition-test", "mode": "DEMO"}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String campaignId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/campaigns/" + campaignId + "/transition")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"targetStatus": "LEGAL_REVIEW"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LEGAL_REVIEW"));
    }

    @Test
    void managerCannotTransitionAnotherOrganizationsCampaign() throws Exception {
        Cookie session = loginWithRole("+5511888880011", UserRole.CAMPAIGN_MANAGER);
        var other = organizationRepository.save(new Organization("Outra organização", "ASSOCIATION"));
        var campaign = campaignRepository.save(new Campaign(other.getId(), "Campanha externa", "campanha-externa-transicao", CampaignMode.DEMO));
        mockMvc.perform(post("/campaigns/" + campaign.getPublicId() + "/transition")
                        .cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"LEGAL_REVIEW\"}"))
                .andExpect(status().isForbidden());
        org.assertj.core.api.Assertions.assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getStatus()).isEqualTo(CampaignStatus.DRAFT);
    }

    @Test
    void invalidTransitionIsRejected() throws Exception {
        Cookie session = loginWithRole("+5511888880006", UserRole.CAMPAIGN_MANAGER);

        MvcResult created = mockMvc.perform(post("/campaigns")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": "Invalid Trans", "slug": "invalid-trans", "mode": "DEMO"}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String campaignId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/campaigns/" + campaignId + "/transition")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"targetStatus": "OPEN"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_INVALID_TRANSITION"));
    }

    @Test
    void canAddAndListPrizes() throws Exception {
        Cookie session = loginWithRole("+5511888880007", UserRole.CAMPAIGN_MANAGER);

        MvcResult created = mockMvc.perform(post("/campaigns")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": "Prize Test", "slug": "prize-test", "mode": "DEMO"}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String campaignId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/campaigns/" + campaignId + "/prizes")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"type": "GOODS", "description": "Kit Canon D70", "valueCents": 1000000, "quantity": 1, "sortOrder": 1}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Kit Canon D70"))
                .andExpect(jsonPath("$.valueCents").value(1000000));

        mockMvc.perform(get("/campaigns/" + campaignId + "/prizes").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("GOODS"));
    }

    @Test
    void cashPrizeBlockedInAllModes() throws Exception {
        Cookie session = loginWithRole("+5511888880008", UserRole.CAMPAIGN_MANAGER);

        MvcResult created = mockMvc.perform(post("/campaigns")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": "Cash Test", "slug": "cash-test", "mode": "DEMO"}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String campaignId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/campaigns/" + campaignId + "/prizes")
                        .cookie(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"type": "CASH", "description": "Dinheiro", "valueCents": 100000, "quantity": 1, "sortOrder": 1}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PRIZE_CASH_NOT_ALLOWED"));
    }

    @Test
    void managerOfAnotherOrganizationCannotEditCampaignOrPrizes() throws Exception {
        Cookie outsider = loginWithRole("+5511888880021", UserRole.CAMPAIGN_MANAGER);
        var other = organizationRepository.save(new Organization("Organização vítima", "ASSOCIATION"));
        var campaign = campaignRepository.save(new Campaign(other.getId(), "Campanha vítima", "campanha-vitima-f25", CampaignMode.DEMO));
        var prize = prizeRepository.save(new CampaignPrize(campaign.getId(), CampaignPrize.PrizeType.GOODS, "Kit original", 1000, 1, 1));
        String prizeBody = """
                {"type": "GOODS", "description": "Kit trocado", "valueCents": 1, "quantity": 1, "sortOrder": 1}
                """;

        mockMvc.perform(put("/campaigns/" + campaign.getPublicId()).cookie(outsider).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Sequestrada\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/campaigns/" + campaign.getPublicId() + "/prizes").cookie(outsider).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(prizeBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/campaigns/" + campaign.getPublicId() + "/prizes/" + prize.getId()).cookie(outsider).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(prizeBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/campaigns/" + campaign.getPublicId() + "/prizes/" + prize.getId()).cookie(outsider).with(csrf()))
                .andExpect(status().isForbidden());

        var stored = prizeRepository.findById(prize.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stored.getDescription()).isEqualTo("Kit original");
        org.assertj.core.api.Assertions.assertThat(stored.getStatus()).isEqualTo(CampaignPrize.PrizeStatus.ACTIVE);
        org.assertj.core.api.Assertions.assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getTitle())
                .isEqualTo("Campanha vítima");
    }

    @Test
    void prizeFromAnotherCampaignIsNotFoundThroughOwnCampaignPath() throws Exception {
        Cookie session = loginWithRole("+5511888880022", UserRole.CAMPAIGN_MANAGER);
        String own = createCampaign(session, "f25-propria");
        var other = organizationRepository.save(new Organization("Organização terceira", "ASSOCIATION"));
        var foreign = campaignRepository.save(new Campaign(other.getId(), "Campanha terceira", "f25-terceira", CampaignMode.DEMO));
        var foreignPrize = prizeRepository.save(new CampaignPrize(foreign.getId(), CampaignPrize.PrizeType.GOODS, "Prêmio alheio", 1000, 1, 1));

        mockMvc.perform(put("/campaigns/" + own + "/prizes/" + foreignPrize.getId()).cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"GOODS\",\"description\":\"Trocado\",\"valueCents\":1,\"quantity\":1,\"sortOrder\":1}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/campaigns/" + own + "/prizes/" + foreignPrize.getId()).cookie(session).with(csrf()))
                .andExpect(status().isNotFound());
        org.assertj.core.api.Assertions.assertThat(prizeRepository.findById(foreignPrize.getId()).orElseThrow().getDescription())
                .isEqualTo("Prêmio alheio");
    }

    @Test
    void prizesAreFrozenOnceTheCampaignLeavesDraft() throws Exception {
        Cookie session = loginWithRole("+5511888880023", UserRole.CAMPAIGN_MANAGER);
        String campaignId = createCampaign(session, "f25-congelada");
        mockMvc.perform(post("/campaigns/" + campaignId + "/transition").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetStatus\":\"LEGAL_REVIEW\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/campaigns/" + campaignId + "/prizes").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"GOODS\",\"description\":\"Tardio\",\"valueCents\":1,\"quantity\":1,\"sortOrder\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_NOT_EDITABLE"));
    }

    @Test
    void rankingPositionPrizesAreParametrizedOncePerScopeAndPosition() throws Exception {
        Cookie session = loginWithRole("+5511888880024", UserRole.CAMPAIGN_MANAGER);
        String campaignId = createCampaign(session, "p05a09-posicoes");
        String first = "{\"type\":\"GOODS\",\"description\":\"Kit 1º divulgador\",\"valueCents\":150000,\"quantity\":1,"
                + "\"sortOrder\":1,\"rankingScope\":\"PROMOTER\",\"rankingPosition\":1}";

        mockMvc.perform(post("/campaigns/" + campaignId + "/prizes").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rankingScope").value("PROMOTER"))
                .andExpect(jsonPath("$.rankingPosition").value(1));
        mockMvc.perform(post("/campaigns/" + campaignId + "/prizes").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/campaigns/" + campaignId + "/prizes").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"GOODS\",\"description\":\"Sem posição\",\"valueCents\":1,\"quantity\":1,"
                                + "\"sortOrder\":2,\"rankingScope\":\"PARTICIPANT\"}"))
                .andExpect(status().isBadRequest());
    }

    private String createCampaign(Cookie session, String slug) throws Exception {
        MvcResult created = mockMvc.perform(post("/campaigns").cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Campanha " + slug + "\",\"slug\":\"" + slug + "\",\"mode\":\"DEMO\"}"))
                .andExpect(status().isCreated()).andReturn();
        return com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void campaignNotFoundReturns404() throws Exception {
        Cookie session = loginWithRole("+5511888880009", UserRole.CAMPAIGN_MANAGER);

        mockMvc.perform(get("/campaigns/nonexistent-id").cookie(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_NOT_FOUND"));
    }
}
