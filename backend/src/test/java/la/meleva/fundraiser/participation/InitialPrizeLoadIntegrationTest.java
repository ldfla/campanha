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
import la.meleva.fundraiser.referral.ReferralService;
import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InitialPrizeLoadIntegrationTest {

    @Autowired InitialPrizeLoadService prizeLoad;
    @Autowired ParticipationService participations;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired ReferralService referrals;
    @Autowired CampaignNumberBuyerRepository buyers;
    @Autowired JdbcTemplate jdbc;

    @Test
    void generatesTwoTiersWithoutCollisionAndIsIdempotent() {
        var f = fixture("prize-load-basic", 1000);
        var load = prizeLoad.load(f.manager().getId(), f.campaign().getPublicId());

        assertThat(load.tier1Count()).isEqualTo(10);
        assertThat(load.tier2Count()).isEqualTo(100);
        assertThat(load.tier1Cents()).isEqualTo(10_000L);
        assertThat(load.tier2Cents()).isEqualTo(1_000L);
        assertThat(load.fileSha256()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id=? AND prize_cents=10000",
                Long.class, f.campaign().getId())).isEqualTo(10L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id=? AND prize_cents=1000",
                Long.class, f.campaign().getId())).isEqualTo(100L);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT number_value) FROM campaign_numbers WHERE campaign_id=? AND prize_cents IS NOT NULL",
                Long.class, f.campaign().getId())).isEqualTo(110L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id=? AND prize_cents IS NOT NULL AND prize_defined_at IS NULL",
                Long.class, f.campaign().getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id=? AND prize_cents IS NULL AND prize_defined_at IS NOT NULL",
                Long.class, f.campaign().getId())).isZero();

        var repeated = prizeLoad.load(f.manager().getId(), f.campaign().getPublicId());
        assertThat(repeated.fileSha256()).isEqualTo(load.fileSha256());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_prize_loads WHERE campaign_id=?",
                Long.class, f.campaign().getId())).isEqualTo(1L);
    }

    @Test
    void verifiesHashAndDetectsPrizeTamperingButIgnoresSaleColumns() {
        var f = fixture("prize-load-hash", 1000);
        prizeLoad.load(f.manager().getId(), f.campaign().getPublicId());
        var verified = prizeLoad.verify(f.campaign().getPublicId());
        assertThat(verified.match()).isTrue();
        assertThat(verified.computedSha256()).isEqualTo(verified.storedSha256());

        open(f.campaign());
        var buyer = users.save(new User("Comprador", "Hash", "+5511977001001"));
        participations.request(buyer.getId(), f.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                f.beneficiary().getPublicId());

        // Colunas de operação (comprador/caridade) ficam fora do hash.
        assertThat(prizeLoad.verify(f.campaign().getPublicId()).match()).isTrue();

        Long numberId = jdbc.queryForObject("SELECT id FROM campaign_numbers WHERE campaign_id=? AND prize_cents=10000 LIMIT 1",
                Long.class, f.campaign().getId());
        jdbc.update("UPDATE campaign_numbers SET prize_cents=5000 WHERE id=?", numberId);
        assertThat(prizeLoad.verify(f.campaign().getPublicId()).match()).isFalse();
    }

    @Test
    void recordsBuyerSnapshotWithE164AndCharityAndReferrerOnlyWhenPromoterHasQuota() {
        var f = fixture("prize-load-buyer", 1000);
        open(f.campaign());

        var promoterWithoutQuota = users.save(new User("Sem", "Cota", "+5511977002001"));
        var codeWithoutQuota = referrals.link(f.campaign().getId(), promoterWithoutQuota.getId()).getCode();

        var buyer = users.save(new User("Comprador", "Auditável", "+5511977002002"));
        participations.request(buyer.getId(), f.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                f.beneficiary().getPublicId(), codeWithoutQuota);

        var buyerRow = buyers.findByNumberId(jdbc.queryForObject(
                "SELECT id FROM campaign_numbers WHERE campaign_id=? AND status='ASSIGNED' LIMIT 1",
                Long.class, f.campaign().getId())).orElseThrow();
        assertThat(buyerRow.getBuyerFirstName()).isEqualTo("Comprador");
        assertThat(buyerRow.getBuyerSurname()).isEqualTo("Auditável");
        assertThat(buyerRow.getBuyerPhone()).isEqualTo("+5511977002002");
        assertThat(buyerRow.getBuyerPseudonym()).contains("C.A.").contains("2002");
        assertThat(jdbc.queryForObject("SELECT charity_institution FROM campaign_numbers WHERE id=?",
                String.class, buyerRow.getNumberId())).isEqualTo("Entidade prize-load-buyer");
        // Divulgador sem cota não indica: telefone do indicador fica nulo e nada é atribuído.
        assertThat(jdbc.queryForObject("SELECT referrer_phone FROM campaign_numbers WHERE id=?",
                String.class, buyerRow.getNumberId())).isNull();
        assertThat(referrals.metrics(f.campaign().getId(), promoterWithoutQuota.getId()).participations()).isZero();

        // Agora o divulgador compra uma cota e passa a poder indicar.
        var promoterWithQuota = users.save(new User("Com", "Cota", "+5511977002003"));
        var codeWithQuota = referrals.link(f.campaign().getId(), promoterWithQuota.getId()).getCode();
        participations.request(promoterWithQuota.getId(), f.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                f.beneficiary().getPublicId());
        var secondBuyer = users.save(new User("Segundo", "Comprador", "+5511977002004"));
        participations.request(secondBuyer.getId(), f.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                f.beneficiary().getPublicId(), codeWithQuota);
        assertThat(referrals.metrics(f.campaign().getId(), promoterWithQuota.getId()).participations()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT referrer_phone FROM campaign_numbers WHERE campaign_id=? AND correlation_id=(SELECT public_id FROM participation_requests WHERE campaign_id=? AND user_id=? ORDER BY id DESC LIMIT 1) LIMIT 1",
                String.class, f.campaign().getId(), f.campaign().getId(), secondBuyer.getId()))
                .isEqualTo("+5511977002003");
    }

    @Test
    void rejectsPurchaseWithoutEntityIdAndWithNonE164Phone() {
        var f = fixture("prize-load-validation", 1000);
        open(f.campaign());
        var buyer = users.save(new User("Sem", "Cota", "+5511977003001"));
        assertThatThrownBy(() -> participations.request(buyer.getId(), f.campaign().getPublicId(),
                UUID.randomUUID().toString(), 1, null))
                .isInstanceOfSatisfying(ErrorCode.BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        var weirdPhone = users.save(new User("Telefone", "Estranho", "abc"));
        assertThatThrownBy(() -> participations.request(weirdPhone.getId(), f.campaign().getPublicId(),
                UUID.randomUUID().toString(), 1, f.beneficiary().getPublicId()))
                .isInstanceOfSatisfying(ErrorCode.BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    private record Fixture(Campaign campaign, User manager, BeneficiaryEntity beneficiary) { }

    private static final java.util.concurrent.atomic.AtomicInteger PHONE = new java.util.concurrent.atomic.AtomicInteger(4_100_000);

    private Fixture fixture(String slug, int capacity) {
        var organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        var manager = users.save(new User("Gestor", "Prêmios", "+5511977" + PHONE.getAndIncrement()));
        memberships.save(new Membership(organization, manager, UserRole.CAMPAIGN_MANAGER));
        var campaign = new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO);
        campaign.setNumberRange(0, capacity - 1);
        var saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(saved.getId(), "Entidade " + slug));
        return new Fixture(saved, manager, beneficiary);
    }

    private void open(Campaign campaign) {
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        campaigns.save(campaign);
    }
}
