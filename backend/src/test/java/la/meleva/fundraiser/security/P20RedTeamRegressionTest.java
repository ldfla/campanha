package la.meleva.fundraiser.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.AuthTestSupport;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import com.jayway.jsonpath.JsonPath;
import la.meleva.fundraiser.participation.ParticipationService;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Regressões dos achados do Red Team P20 (docs/security/red-team/P20-findings.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class P20RedTeamRegressionTest {

    @Autowired MockMvc mvc;
    @Autowired OrganizationRepository organizations;
    @Autowired CampaignRepository campaigns;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired ParticipationService participations;
    @Autowired JdbcTemplate jdbc;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;

    private Campaign openCampaign(String slug, CampaignMode mode, int capacity) {
        var organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Campanha " + slug, slug, mode);
        campaign.setNumberRange(0, capacity - 1);
        campaign = campaigns.save(campaign);
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        campaign = campaigns.save(campaign);
        participations.initializeSeries(campaign.getPublicId());
        return campaign;
    }

    private String participate(Cookie session, Campaign campaign, String entityId, int count) throws Exception {
        return mvc.perform(post("/participations/campaigns/{id}", campaign.getPublicId()).cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"" + UUID.randomUUID() + "\",\"count\":" + count
                                + ",\"entityId\":\"" + entityId + "\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    /** F04: participação sem cobrança em campanha paga não atribui número elegível. */
    @Test
    void freeParticipationIsRejectedOutsideDemo() throws Exception {
        var campaign = openCampaign("p20-capitalizacao", CampaignMode.CAPITALIZATION_PRIZE_TITLE, 3);
        var entity = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade capitalização"));
        Cookie session = AuthTestSupport.registerAndSignIn(mvc, "Comprador", "Gratis", "+5511977700001");

        mvc.perform(post("/participations/campaigns/{id}", campaign.getPublicId()).cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"" + UUID.randomUUID() + "\",\"count\":1,\"entityId\":\""
                                + entity.getPublicId() + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_LIVE_OPERATIONS_DISABLED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id=? AND status<>'AVAILABLE'",
                Long.class, campaign.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participation_entries WHERE campaign_id=?",
                Long.class, campaign.getId())).isZero();
    }

    /** F05: a lista de números premiados fica lacrada enquanto a campanha vende; o hash continua público. */
    @Test
    void prizeListIsSealedWhileSalesAreOpen() throws Exception {
        var campaign = openCampaign("p20-premios-lacrados", CampaignMode.DEMO, 3);
        mvc.perform(get("/participations/campaigns/{id}/numbers/prize-load/export", campaign.getPublicId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRIZE_LOAD_SEALED"));

        campaign = campaigns.findById(campaign.getId()).orElseThrow();
        campaign.transitionTo(CampaignStatus.LOCKED);
        campaigns.save(campaign);
        String csv = mvc.perform(get("/participations/campaigns/{id}/numbers/prize-load/export", campaign.getPublicId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(csv).startsWith("number,prize_cents,prize_defined_at").contains("00000,");
    }

    /** F08: "Meus números" sem sessão não devolve dado; com sessão, só os números do próprio titular. */
    @Test
    void myNumbersIsScopedToSessionOwner() throws Exception {
        var campaign = openCampaign("p20-meus-numeros", CampaignMode.DEMO, 5);
        var entity = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade meus números"));
        String path = "/participations/campaigns/{id}/numbers/mine";

        String anonymous = mvc.perform(get(path, campaign.getPublicId()))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(anonymous).doesNotContain("formattedNumber").doesNotContain("00000");

        Cookie alice = AuthTestSupport.registerAndSignIn(mvc, "Alice", "Dona", "+5511977700002");
        Cookie bruno = AuthTestSupport.registerAndSignIn(mvc, "Bruno", "Outro", "+5511977700003");
        participate(alice, campaign, entity.getPublicId(), 2);
        participate(bruno, campaign, entity.getPublicId(), 1);

        mvc.perform(get(path, campaign.getPublicId()).cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numbers.length()").value(2))
                .andExpect(jsonPath("$.confirmedCount").value(2))
                .andExpect(jsonPath("$.confirmedAmountCents").value(0))
                .andExpect(jsonPath("$.simulation").value(true));
        // P20-F11: atribuição aleatória — não se assume um valor específico de número.
        String brunoView = mvc.perform(get(path, campaign.getPublicId()).cookie(bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numbers.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(brunoView).doesNotContain("Alice").doesNotContain("977700002");
    }

    /** F09: métricas internas do actuator não ficam acessíveis a participante. */
    @Test
    void actuatorMetricsRequireAdministrativeRole() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        Cookie participant = AuthTestSupport.registerAndSignIn(mvc, "Curioso", "Metricas", "+5511977700004");
        mvc.perform(get("/actuator/metrics").cookie(participant)).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    /** IDOR: outro usuário não confirma o checkout alheio; o dono não confirma antes do pagamento. */
    @Test
    void checkoutConfirmationIsBoundToOwnerAndToReconciledPayment() throws Exception {
        var organization = organizations.save(new Organization("Organização p20 idor", "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Campanha idor", "p20-idor", CampaignMode.DEMO);
        campaign.setNumberRange(0, 4);
        campaign = campaigns.save(campaign);
        AuthTestSupport.register(mvc, "Gestor", "Idor", "+5511977700010");
        memberships.save(new Membership(organization, users.findByPhoneNumber("+5511977700010").orElseThrow(),
                UserRole.CAMPAIGN_MANAGER));
        Cookie manager = AuthTestSupport.verify(mvc, "+5511977700010");
        var offer = mvc.perform(post("/participations/campaigns/{id}/offers", campaign.getPublicId()).cookie(manager)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2,\"totalCents\":250}"))
                .andExpect(status().isCreated()).andReturn();
        String offerId = JsonPath.read(offer.getResponse().getContentAsString(), "$.id");
        var entity = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade idor"));
        campaign = campaigns.findById(campaign.getId()).orElseThrow();
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        campaigns.save(campaign);
        participations.initializeSeries(campaign.getPublicId());

        Cookie owner = AuthTestSupport.registerAndSignIn(mvc, "Dona", "Checkout", "+5511977700011");
        Cookie attacker = AuthTestSupport.registerAndSignIn(mvc, "Atacante", "Checkout", "+5511977700012");
        var checkout = mvc.perform(post("/participations/campaigns/{id}/checkout", campaign.getPublicId()).cookie(owner)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"" + UUID.randomUUID() + "\",\"offerId\":\"" + offerId
                                + "\",\"entityId\":\"" + entity.getPublicId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountCents").value(250)).andReturn();
        String requestId = JsonPath.read(checkout.getResponse().getContentAsString(), "$.requestPublicId");
        String confirm = "/participations/campaigns/{id}/checkout/{requestId}/confirm";

        mvc.perform(post(confirm, campaign.getPublicId(), requestId).cookie(attacker).with(csrf()))
                .andExpect(status().isForbidden());
        // P20-F17 resolvido: pagamento ainda não conciliado responde 409 (não 502).
        mvc.perform(post(confirm, campaign.getPublicId(), requestId).cookie(owner).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_RECONCILED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id=? AND status='ASSIGNED'",
                Long.class, campaign.getId())).isZero();
    }
}
