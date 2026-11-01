package la.meleva.fundraiser.compliance;

import java.time.Instant;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignPrize;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignService;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.campaign.dto.PrizeRequest;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.payment.OrderService;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CapitalizationModeIntegrationTest {

    @Autowired CampaignService campaigns;
    @Autowired CampaignRepository campaignRepository;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired CampaignAuthorizationRepository authorizations;
    @Autowired AuthorizationService authorizationService;
    @Autowired OrderService orders;
    @Autowired ApprovalService approvalService;
    @Autowired ApprovalRequestRepository approvalRequests;
    @Autowired MembershipRepository memberships;
    @Autowired AuthorizationService authService;
    @Autowired la.meleva.fundraiser.participation.ReservationCheckoutService checkout;
    @Autowired la.meleva.fundraiser.participation.ParticipationService participations;
    @Autowired la.meleva.fundraiser.participation.beneficiary.BeneficiaryService beneficiaries;
    @Autowired la.meleva.fundraiser.participation.purchase.PurchaseOfferService offers;

    @Test
    void cashPrizeAllowedOnlyInCapitalizationTitle() {
        var organization = organizations.save(new Organization("Org capitalização", "ASSOCIATION"));
        var demo = campaignRepository.save(new Campaign(organization.getId(), "Demo", "cap-demo", CampaignMode.DEMO));
        var title = campaignRepository.save(new Campaign(organization.getId(), "Título", "cap-title", CampaignMode.CAPITALIZATION_PRIZE_TITLE));

        assertThatThrownBy(() -> campaigns.addPrize(demo.getPublicId(),
                new PrizeRequest(CampaignPrize.PrizeType.CASH, "Dinheiro", 1000, 1, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.PRIZE_CASH_NOT_ALLOWED));

        assertThat(campaigns.addPrize(title.getPublicId(),
                new PrizeRequest(CampaignPrize.PrizeType.CASH, "Dinheiro", 1000, 1, 1))).isNotNull();
    }

    @Test
    void chargingRequiresVerifiedEffectiveAuthorization() {
        var organization = organizations.save(new Organization("Org cobrança", "ASSOCIATION"));
        var campaign = campaignRepository.save(new Campaign(organization.getId(), "Título", "cap-charge", CampaignMode.CAPITALIZATION_PRIZE_TITLE));
        var buyer = users.save(new User("Comprador", "Cap", "+5511999800001"));

        assertThatThrownBy(() -> orders.createOrder(campaign.getId(), buyer.getId(), 100, UUID.randomUUID().toString()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.CAMPAIGN_NOT_AUTHORIZED));

        var authorization = authorizations.save(new CampaignAuthorization(campaign.getId(), "ViaCap",
                "15414.657358/2026-61", null, null, "docs://susep"));
        assertThatThrownBy(() -> orders.createOrder(campaign.getId(), buyer.getId(), 100, UUID.randomUUID().toString()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.CAMPAIGN_NOT_AUTHORIZED));

        authorization.verify(Instant.now());
        authorizations.save(authorization);
        assertThat(authorizationService.hasEffectiveAuthorization(campaign.getId())).isTrue();
        assertThat(orders.createOrder(campaign.getId(), buyer.getId(), 100, UUID.randomUUID().toString())).isNotNull();
    }

    @Test
    void openingRequiresAllGatesAndEffectiveAuthorization() {
        var organization = organizations.save(new Organization("Org abertura", "ASSOCIATION"));
        var admin = users.save(new User("Admin", "Cap", "+5511999800003"));
        memberships.save(new Membership(organization, admin, UserRole.SYSTEM_ADMIN));
        var campaign = campaignRepository.save(new Campaign(organization.getId(), "Título", "cap-open", CampaignMode.CAPITALIZATION_PRIZE_TITLE));

        campaigns.transition(admin.getId(), campaign.getPublicId(), CampaignStatus.LEGAL_REVIEW);
        campaigns.transition(admin.getId(), campaign.getPublicId(), CampaignStatus.AUTHORIZED);
        assertThatThrownBy(() -> campaigns.transition(admin.getId(), campaign.getPublicId(), CampaignStatus.SCHEDULED))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.CAMPAIGN_GATES_INCOMPLETE));

        for (var gate : la.meleva.fundraiser.compliance.GateType.values()) {
            var request = approvalRequests.save(new ApprovalRequest(campaign.getId(), gate, admin.getId()));
            request.approve();
            approvalRequests.save(request);
        }
        assertThatThrownBy(() -> campaigns.transition(admin.getId(), campaign.getPublicId(), CampaignStatus.SCHEDULED))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.CAMPAIGN_GATES_INCOMPLETE));

        var authorization = authorizations.save(new CampaignAuthorization(campaign.getId(), "ViaCap", "cert", null, null, null));
        authorization.verify(Instant.now());
        authorizations.save(authorization);

        campaigns.transition(admin.getId(), campaign.getPublicId(), CampaignStatus.SCHEDULED);
        assertThat(campaignRepository.findByPublicId(campaign.getPublicId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.SCHEDULED);
    }

    @Test
    void capitalizationCheckoutReservesNumbersWhenAuthorizedAndOpen() {
        var organization = organizations.save(new Organization("Org checkout", "ASSOCIATION"));
        var admin = users.save(new User("Admin", "Checkout", "+5511999800011"));
        memberships.save(new Membership(organization, admin, UserRole.SYSTEM_ADMIN));
        var buyer = users.save(new User("Comprador", "Checkout", "+5511999800012"));
        var campaign = campaignRepository.save(new Campaign(organization.getId(), "Título", "cap-checkout", CampaignMode.CAPITALIZATION_PRIZE_TITLE));
        campaign.setNumberRange(0, 9);
        campaignRepository.save(campaign);
        participations.initializeSeries(campaign.getPublicId());

        var beneficiary = beneficiaries.create(admin.getId(), campaign.getPublicId(), "Entidade de teste");
        var offer = offers.create(admin.getId(), campaign.getPublicId(), 2, 200L);

        for (var gate : la.meleva.fundraiser.compliance.GateType.values()) {
            var request = approvalRequests.save(new ApprovalRequest(campaign.getId(), gate, admin.getId()));
            request.approve();
            approvalRequests.save(request);
        }
        var authorization = authorizations.save(new CampaignAuthorization(campaign.getId(), "ViaCap", "cert", null, null, null));
        authorization.verify(Instant.now());
        authorizations.save(authorization);
        campaigns.transition(admin.getId(), campaign.getPublicId(), CampaignStatus.LEGAL_REVIEW);
        campaigns.transition(admin.getId(), campaign.getPublicId(), CampaignStatus.AUTHORIZED);
        campaigns.transition(admin.getId(), campaign.getPublicId(), CampaignStatus.SCHEDULED);
        campaigns.transition(admin.getId(), campaign.getPublicId(), CampaignStatus.OPEN);

        // Regressão: o guard do ReservationService bloqueava a capitalização; deve reservar com autorização vigente.
        var view = checkout.start(buyer.getId(), campaign.getPublicId(), UUID.randomUUID().toString(),
                offer.id(), beneficiary.id(), null);
        assertThat(view.reservation().numbers()).hasSize(2);
    }

    @Test
    void authorizationOutsideValidityDoesNotReleaseCharging() {
        var organization = organizations.save(new Organization("Org vigência", "ASSOCIATION"));
        var campaign = campaignRepository.save(new Campaign(organization.getId(), "Título", "cap-expired", CampaignMode.CAPITALIZATION_PRIZE_TITLE));
        var expired = authorizations.save(new CampaignAuthorization(campaign.getId(), "ViaCap", "cert",
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600), null));
        expired.verify(Instant.now());
        authorizations.save(expired);

        assertThat(authorizationService.hasEffectiveAuthorization(campaign.getId())).isFalse();
        var buyer = users.save(new User("Comprador", "Vencido", "+5511999800002"));
        assertThatThrownBy(() -> orders.createOrder(campaign.getId(), buyer.getId(), 100, UUID.randomUUID().toString()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.CAMPAIGN_NOT_AUTHORIZED));
    }
}
