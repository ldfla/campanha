package la.meleva.fundraiser.participation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignService;
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
import la.meleva.fundraiser.payment.MockPaymentProvider;
import la.meleva.fundraiser.payment.OrderRepository;
import la.meleva.fundraiser.payment.OrderService;
import la.meleva.fundraiser.payment.Payment;
import la.meleva.fundraiser.payment.PaymentProvider;
import la.meleva.fundraiser.payment.PaymentRepository;
import la.meleva.fundraiser.ranking.RankingScope;
import la.meleva.fundraiser.ranking.RankingService;
import la.meleva.fundraiser.ranking.dto.CreateSnapshotRequest;
import la.meleva.fundraiser.ranking.dto.RankingView;
import la.meleva.fundraiser.referral.ReferralService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** P10-07: estorno integral invalida as entradas do pedido e o efeito chega aos rankings, sem tocar snapshots lacrados. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RefundRankingIntegrationTest {
    @Autowired ReservationCheckoutService checkout;
    @Autowired ParticipationService participation;
    @Autowired OrderService orderService;
    @Autowired MockPaymentProvider mock;
    @Autowired RankingService rankings;
    @Autowired ReferralService referrals;
    @Autowired CampaignService campaignService;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired PurchaseOfferRepository offers;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired ParticipationEntryRepository entries;
    @Autowired JdbcTemplate jdbc;

    @Test
    void operatorRefundRemovesBuyerAndReferralFromLiveRankingsButNotFromSealedSnapshot() {
        Fixture fixture = fixture("refund-ranking-open");
        User promoter = demoBuyer(fixture, "Paulo", "Reis", "+551197770001");
        String code = referrals.link(fixture.campaign().getId(), promoter.getId()).getCode();
        User buyer = users.save(new User("Bia", "Melo", "+551197770002"));
        Payment payment = paidCheckout(fixture, buyer, code);

        assertThat(scoreOf(fixture, RankingScope.PARTICIPANT, "B.M. 0002")).isEqualTo(2);
        assertThat(scoreOf(fixture, RankingScope.PROMOTER, "P.R. 0001")).isEqualTo(2);
        var sealed = rankings.snapshot(fixture.manager().getId(), fixture.campaign().getPublicId(),
                new CreateSnapshotRequest(RankingScope.PROMOTER, null));
        String sealedPayload = rankings.export(null, fixture.campaign().getPublicId(), sealed.snapshotId()).payloadJson();

        orderService.refund(payment.getId(), "desistência dentro do prazo");

        assertThat(scoreOf(fixture, RankingScope.PARTICIPANT, "B.M. 0002")).isZero();
        assertThat(rankings.current(fixture.campaign().getPublicId(), RankingScope.PROMOTER, 50).entries()).isEmpty();
        assertThat(entries.findByCampaignIdAndUserId(fixture.campaign().getId(), buyer.getId()))
                .extracting(ParticipationEntry::getStatus).containsOnly(ParticipationStatus.INVALIDATED);
        assertThat(numberStatusesOf(fixture, buyer)).containsOnly("INVALIDATED");
        assertThat(refundAudits(fixture, buyer)).isEqualTo(2);
        var export = rankings.export(null, fixture.campaign().getPublicId(), sealed.snapshotId());
        assertThat(export.verified()).isTrue();
        assertThat(export.payloadJson()).isEqualTo(sealedPayload);
        assertThat(participation.availableCount(fixture.campaign().getPublicId())).isEqualTo(10 - 1 - 2);
    }

    @Test
    void providerRefundAfterFreezeInvalidatesFrozenEntriesOnceEvenIfReconciledAgain() {
        Fixture fixture = fixture("refund-ranking-frozen");
        User buyer = users.save(new User("Caio", "Lima", "+551197770011"));
        User other = demoBuyer(fixture, "Duda", "Reis", "+551197770012");
        Payment payment = paidCheckout(fixture, buyer, null);
        campaignService.transition(fixture.manager().getId(), fixture.campaign().getPublicId(), CampaignStatus.LOCKED);
        participation.freezeAssignedNumbers(fixture.campaign().getPublicId());

        mock.setState(payment.getProviderChargeId(), PaymentProvider.ChargeState.REFUNDED);
        orderService.reconcile(payment.getId());
        orderService.reconcile(payment.getId());

        assertThat(entries.findByCampaignIdAndUserId(fixture.campaign().getId(), buyer.getId()))
                .extracting(ParticipationEntry::getStatus).containsOnly(ParticipationStatus.INVALIDATED);
        assertThat(entries.findByCampaignIdAndUserId(fixture.campaign().getId(), other.getId()))
                .extracting(ParticipationEntry::getStatus).containsOnly(ParticipationStatus.FROZEN);
        assertThat(refundAudits(fixture, buyer)).isEqualTo(2);
        RankingView participants = rankings.current(fixture.campaign().getPublicId(), RankingScope.PARTICIPANT, 50);
        assertThat(participants.entries()).extracting(RankingView.Row::pseudonym).containsExactly("D.R. 0012");
    }

    private Payment paidCheckout(Fixture fixture, User buyer, String referralCode) {
        var view = checkout.start(buyer.getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(),
                fixture.offer().getPublicId(), fixture.beneficiary().getPublicId(), referralCode);
        var order = orders.findByPublicId(view.orderPublicId()).orElseThrow();
        Payment payment = payments.findByOrderId(order.getId()).orElseThrow();
        mock.setState(payment.getProviderChargeId(), PaymentProvider.ChargeState.PAID);
        orderService.reconcile(payment.getId());
        checkout.confirm(buyer.getId(), fixture.campaign().getPublicId(), view.reservation().requestPublicId());
        return payments.findById(payment.getId()).orElseThrow();
    }

    private User demoBuyer(Fixture fixture, String name, String surname, String phone) {
        User user = users.save(new User(name, surname, phone));
        participation.request(user.getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                fixture.beneficiary().getPublicId());
        return user;
    }

    private long scoreOf(Fixture fixture, RankingScope scope, String pseudonym) {
        return rankings.current(fixture.campaign().getPublicId(), scope, 50).entries().stream()
                .filter(row -> row.pseudonym().equals(pseudonym)).mapToLong(RankingView.Row::score).sum();
    }

    private List<String> numberStatusesOf(Fixture fixture, User buyer) {
        return jdbc.queryForList("SELECT cn.status FROM campaign_numbers cn JOIN participation_entries pe ON pe.id = cn.entry_id "
                + "WHERE pe.campaign_id = ? AND pe.user_id = ?", String.class, fixture.campaign().getId(), buyer.getId());
    }

    private long refundAudits(Fixture fixture, User buyer) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_events ae JOIN participation_entries pe ON pe.id = ae.entity_id "
                + "WHERE ae.action = 'PARTICIPATION_REFUNDED' AND ae.entity_type = 'participation_entry' "
                + "AND pe.campaign_id = ? AND pe.user_id = ?", Long.class, fixture.campaign().getId(), buyer.getId());
    }

    private Fixture fixture(String slug) {
        Organization organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        User manager = users.save(new User("Gestor", "Teste", null));
        memberships.save(new Membership(organization, manager, UserRole.CAMPAIGN_MANAGER));
        Campaign draft = new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO);
        draft.setNumberRange(0, 9);
        Campaign campaign = campaigns.save(draft);
        PurchaseOffer offer = offers.save(new PurchaseOffer(campaign.getId(), 2, 250));
        for (CampaignStatus next : List.of(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED,
                CampaignStatus.SCHEDULED, CampaignStatus.OPEN)) {
            campaignService.transition(manager.getId(), campaign.getPublicId(), next);
        }
        participation.initializeSeries(campaign.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade " + slug));
        return new Fixture(campaigns.findByPublicId(campaign.getPublicId()).orElseThrow(), manager, beneficiary, offer);
    }

    private record Fixture(Campaign campaign, User manager, BeneficiaryEntity beneficiary, PurchaseOffer offer) {}
}
