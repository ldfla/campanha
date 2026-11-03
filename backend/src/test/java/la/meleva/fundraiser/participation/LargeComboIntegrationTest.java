package la.meleva.fundraiser.participation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import la.meleva.fundraiser.participation.purchase.PurchaseOffer;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferRepository;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferService;
import la.meleva.fundraiser.payment.MockPaymentProvider;
import la.meleva.fundraiser.payment.OrderRepository;
import la.meleva.fundraiser.payment.OrderService;
import la.meleva.fundraiser.payment.PaymentProvider;
import la.meleva.fundraiser.payment.PaymentRepository;
import la.meleva.fundraiser.referral.ReferralService;
import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Combos: mínimo de 5 números por R$ 5,00, máximo de 5.000 e o maior combo percorrendo o fluxo inteiro. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LargeComboIntegrationTest {
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired ParticipationService participations;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired PurchaseOfferRepository offerRepository;
    @Autowired PurchaseOfferService offerService;
    @Autowired ReservationCheckoutService checkout;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired MockPaymentProvider mock;
    @Autowired OrderService orderService;
    @Autowired MyNumbersService myNumbers;
    @Autowired ReferralService referrals;
    @Autowired JdbcTemplate jdbc;

    @Test
    void offersBelowMinimumOrAboveMaximumAreRefused() {
        Campaign draft = campaigns.save(new Campaign(organizations.save(new Organization("Org limites", "ASSOCIATION")).getId(),
                "Limites", "combo-limits", CampaignMode.DEMO));
        User manager = users.save(new User("Gestor", "Limites", null));
        memberships.save(new Membership(organizations.findById(draft.getOrganizationId()).orElseThrow(), manager, UserRole.CAMPAIGN_MANAGER));
        for (int[] invalid : new int[][] {{1, 100}, {4, 400}, {5, 499}, {5_001, 345_000}}) {
            assertThatThrownBy(() -> offerService.create(manager.getId(), draft.getPublicId(), invalid[0], invalid[1]))
                    .isInstanceOf(ErrorCode.BusinessException.class)
                    .extracting(error -> ((ErrorCode.BusinessException) error).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
        assertThat(offerService.create(manager.getId(), draft.getPublicId(), 5, 500).totalCents()).isEqualTo(500);
        // Última linha de defesa: nem SQL direto ativa uma oferta abaixo do mínimo.
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO campaign_purchase_offers (public_id, campaign_id, quantity, total_cents, currency, active)
                VALUES (?, ?, 1, 100, 'BRL', TRUE)""", UUID.randomUUID().toString(), draft.getId()))
                .hasMessageContaining("chk_purchase_offer_active_minimum");
    }

    @Test
    void offersAreListedByQuantityWithDiscountsComputedByTheServer() {
        Campaign campaign = openCampaign("combo-discounts", 10);
        // Fora de ordem de propósito: a listagem não depende da ordem de criação.
        offerRepository.save(new PurchaseOffer(campaign.getId(), 5_000, 345_000));
        offerRepository.save(new PurchaseOffer(campaign.getId(), 5, 500));
        offerRepository.save(new PurchaseOffer(campaign.getId(), 1_000, 74_000));
        offerRepository.save(new PurchaseOffer(campaign.getId(), 10, 950));
        offerRepository.save(new PurchaseOffer(campaign.getId(), 100, 8_700));
        var listed = offerService.list(campaign.getPublicId());
        assertThat(listed).extracting(PurchaseOfferService.OfferView::quantity).containsExactly(5, 10, 100, 1_000, 5_000);
        assertThat(listed).extracting(PurchaseOfferService.OfferView::unitCents).containsExactly(100L, 95L, 87L, 74L, 69L);
        assertThat(listed).extracting(PurchaseOfferService.OfferView::discountPct).containsExactly(0, 5, 13, 26, 31);
    }

    @Test
    void fiveThousandNumberComboCompletesReservationPaymentConfirmationAndMyNumbers() {
        Campaign campaign = openCampaign("combo-5000", 5_100);
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade combo"));
        PurchaseOffer offer = offerRepository.save(new PurchaseOffer(campaign.getId(), 5_000, 345_000));
        User promoter = users.save(new User("Promotor", "Combo", "+5511956100001"));
        participations.request(promoter.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1, beneficiary.getPublicId());
        String referral = referrals.link(campaign.getId(), promoter.getId()).getCode();
        User buyer = users.save(new User("Comprador", "Combo", "+5511956100002"));

        long startedAt = System.nanoTime();
        var started = checkout.start(buyer.getId(), campaign.getPublicId(), UUID.randomUUID().toString(),
                offer.getPublicId(), beneficiary.getPublicId(), referral);
        long reservedAt = System.nanoTime();
        assertThat(started.amountCents()).isEqualTo(345_000);
        assertThat(started.reservation().numbers()).hasSize(5_000).doesNotHaveDuplicates();

        var order = orders.findByPublicId(started.orderPublicId()).orElseThrow();
        var payment = payments.findByOrderId(order.getId()).orElseThrow();
        mock.setState(payment.getProviderChargeId(), PaymentProvider.ChargeState.PAID);
        orderService.reconcile(payment.getId());
        long confirmingAt = System.nanoTime();
        checkout.confirm(buyer.getId(), campaign.getPublicId(), started.reservation().requestPublicId());
        long confirmedAt = System.nanoTime();
        System.out.printf("[combo-5000] reserva=%d ms, confirmação=%d ms%n",
                (reservedAt - startedAt) / 1_000_000, (confirmedAt - confirmingAt) / 1_000_000);

        var mine = myNumbers.mine(buyer.getId(), campaign.getPublicId());
        assertThat(mine.numbers()).hasSize(5_000);
        assertThat(mine.confirmedCount()).isEqualTo(5_000);
        assertThat(mine.pendingCount()).isZero();
        assertThat(mine.confirmedAmountCents()).isEqualTo(345_000);
        assertThat(referrals.metrics(campaign.getId(), promoter.getId()).participations()).isEqualTo(5_000);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT purchase_modality) FROM campaign_numbers WHERE campaign_id=? AND purchase_modality='COMBO_5000'",
                Long.class, campaign.getId())).isEqualTo(1);

        long refundingAt = System.nanoTime();
        orderService.refund(payment.getId(), "estorno do maior combo");
        System.out.printf("[combo-5000] estorno=%d ms%n", (System.nanoTime() - refundingAt) / 1_000_000);
        assertThat(myNumbers.mine(buyer.getId(), campaign.getPublicId()).numbers()).isEmpty();
    }

    private Campaign openCampaign(String slug, int capacity) {
        var organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO);
        campaign.setNumberRange(0, capacity - 1);
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        campaign = campaigns.save(campaign);
        participations.initializeSeries(campaign.getPublicId());
        return campaign;
    }
}
