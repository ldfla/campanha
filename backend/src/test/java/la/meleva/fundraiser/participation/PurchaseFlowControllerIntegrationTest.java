package la.meleva.fundraiser.participation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.Objects;
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
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.payment.MockPaymentProvider;
import la.meleva.fundraiser.payment.OrderService;
import la.meleva.fundraiser.payment.PaymentProvider;
import la.meleva.fundraiser.referral.ReferralService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PurchaseFlowControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired CampaignRepository campaigns;
    @Autowired ParticipationService participations;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired ReferralService referrals;
    @Autowired MockPaymentProvider provider;
    @Autowired OrderService orderService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void exposesServerPricedOffersAndConfirmsOnlyAfterReconciledMockPayment() throws Exception {
        var manager = register("Gestor", "+5511988880001");
        var organization = organizations.save(new Organization("Organização compra", "ASSOCIATION"));
        memberships.save(new Membership(organization, users.findByPhoneNumber(manager.phone()).orElseThrow(),
                UserRole.CAMPAIGN_MANAGER));
        var managerSession = login(manager.phone());

        var buyer = register("Comprador", "+5511988880002");
        var buyerSession = login(buyer.phone());

        var promoter = users.save(new User("Divulgadora", "Compra", "+5511988880003"));
        var campaign = draftCampaign(organization, "purchase-flow", 5);
        var referralCode = referrals.link(campaign.getId(), promoter.getId()).getCode();
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade real"));

        String offersPath = "/participations/campaigns/{campaignId}/offers";
        String checkoutPath = "/participations/campaigns/{campaignId}/checkout";

        // Ofertas com preço do servidor: só o gestor cria; participante recebe 403.
        mvc.perform(post(offersPath, campaign.getPublicId()).cookie(managerSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2,\"totalCents\":250}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalCents").value(250));
        mvc.perform(post(offersPath, campaign.getPublicId()).cookie(buyerSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2,\"totalCents\":1}"))
                .andExpect(status().isForbidden());
        var offers = mvc.perform(get(offersPath, campaign.getPublicId()).cookie(buyerSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].totalCents").value(250)).andReturn();
        String offerId = JsonPath.read(offers.getResponse().getContentAsString(), "$[0].id");

        open(campaign);
        // O divulgador precisa ter ao menos 1 cota comprada para indicar.
        participations.request(promoter.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1,
                beneficiary.getPublicId());

        // Checkout exige CSRF e sessão.
        String checkoutBody = "{\"idempotencyKey\":\"" + UUID.randomUUID() + "\",\"offerId\":\"" + offerId
                + "\",\"entityId\":\"" + beneficiary.getPublicId() + "\",\"referralCode\":\"" + referralCode + "\"}";
        mvc.perform(post(checkoutPath, campaign.getPublicId()).cookie(buyerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutBody))
                .andExpect(status().isForbidden());
        var started = mvc.perform(post(checkoutPath, campaign.getPublicId()).cookie(buyerSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulation").value(true))
                .andExpect(jsonPath("$.amountCents").value(250))
                .andExpect(jsonPath("$.numbers.length()").value(2)).andReturn();
        String requestPublicId = JsonPath.read(started.getResponse().getContentAsString(), "$.requestPublicId");
        String orderPublicId = JsonPath.read(started.getResponse().getContentAsString(), "$.orderPublicId");

        // Reserva ainda não atribui números nem pontua indicação.
        assertThat(countNumbers(campaign.getId(), NumberStatus.RESERVED)).isEqualTo(2);
        assertThat(referrals.metrics(campaign.getId(), promoter.getId()).participations()).isZero();

        String confirmPath = "/participations/campaigns/{campaignId}/checkout/{requestId}/confirm";
        mvc.perform(post(confirmPath, campaign.getPublicId(), requestPublicId).cookie(buyerSession).with(csrf()))
                .andExpect(status().isConflict());

        // Reconciliação do provedor mock confirma a cobrança e libera a atribuição.
        Long paymentId = jdbc.queryForObject(
                "SELECT p.id FROM payments p JOIN orders o ON o.id=p.order_id WHERE o.public_id=?",
                Long.class, orderPublicId);
        String chargeId = jdbc.queryForObject("SELECT provider_charge_id FROM payments WHERE id=?", String.class, paymentId);
        provider.setState(chargeId, PaymentProvider.ChargeState.PAID);
        orderService.reconcile(paymentId);

        mvc.perform(post(confirmPath, campaign.getPublicId(), requestPublicId).cookie(buyerSession).with(csrf()))
                .andExpect(status().isNoContent());
        // Idempotente: repetir a confirmação não duplica efeitos.
        mvc.perform(post(confirmPath, campaign.getPublicId(), requestPublicId).cookie(buyerSession).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(countNumbers(campaign.getId(), NumberStatus.ASSIGNED)).isEqualTo(3);
        assertThat(referrals.metrics(campaign.getId(), promoter.getId()).participations()).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM beneficiary_selections WHERE request_id=(SELECT id FROM participation_requests WHERE public_id=?) AND beneficiary_id=?",
                Long.class, requestPublicId, beneficiary.getId())).isEqualTo(1L);
    }

    @Test
    void rejectsCheckoutForNonDemoCampaign() throws Exception {
        var manager = register("Gestor", "+5511988880011");
        var organization = organizations.save(new Organization("Organização promo", "COMPANY"));
        memberships.save(new Membership(organization, users.findByPhoneNumber(manager.phone()).orElseThrow(),
                UserRole.CAMPAIGN_MANAGER));
        var managerSession = login(manager.phone());
        var campaign = draftCampaign(organization, "purchase-flow-promo", 5, CampaignMode.COMMERCIAL_PROMOTION);

        var offer = mvc.perform(post("/participations/campaigns/{campaignId}/offers", campaign.getPublicId())
                        .cookie(managerSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1,\"totalCents\":65}"))
                .andExpect(status().isCreated()).andReturn();
        String offerId = JsonPath.read(offer.getResponse().getContentAsString(), "$.id");
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade promo"));

        open(campaign);

        var buyer = register("Comprador", "+5511988880012");
        var buyerSession = login(buyer.phone());
        mvc.perform(post("/participations/campaigns/{campaignId}/checkout", campaign.getPublicId())
                        .cookie(buyerSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"" + UUID.randomUUID() + "\",\"offerId\":\"" + offerId
                                + "\",\"entityId\":\"" + beneficiary.getPublicId() + "\"}"))
                .andExpect(status().isForbidden());
    }

    private long countNumbers(Long campaignId, NumberStatus status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id=? AND status=?",
                Long.class, campaignId, status.name());
    }

    private record Registered(String phone) { }

    private Registered register(String firstName, String phone) throws Exception {
        la.meleva.fundraiser.identity.AuthTestSupport.register(mvc, firstName, "Teste", phone);
        return new Registered(phone);
    }

    private Cookie login(String phone) throws Exception {
        return la.meleva.fundraiser.identity.AuthTestSupport.verify(mvc, phone);
    }

    private Campaign draftCampaign(Organization organization, String slug, int capacity) {
        return draftCampaign(organization, slug, capacity, CampaignMode.DEMO);
    }

    private Campaign draftCampaign(Organization organization, String slug, int capacity, CampaignMode mode) {
        var campaign = new Campaign(organization.getId(), "Campanha compra", slug, mode);
        campaign.setNumberRange(0, capacity - 1);
        return campaigns.save(campaign);
    }

    private void open(Campaign campaign) {
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        campaigns.save(campaign);
        participations.initializeSeries(campaign.getPublicId());
    }
}
