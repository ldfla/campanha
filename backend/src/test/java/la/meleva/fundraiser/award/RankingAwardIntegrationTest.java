package la.meleva.fundraiser.award;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignPrize;
import la.meleva.fundraiser.campaign.CampaignPrizeRepository;
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
import la.meleva.fundraiser.participation.ParticipationService;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.ranking.RankingScope;
import la.meleva.fundraiser.referral.ReferralService;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** P05A-09: premiação por posição do ranking ligada ao fluxo de entrega (P13), só em DEMO e sem dinheiro. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RankingAwardIntegrationTest {
    @Autowired AwardDeliveryService awards;
    @Autowired AwardDeliveryRepository deliveries;
    @Autowired CampaignService campaignService;
    @Autowired CampaignRepository campaigns;
    @Autowired CampaignPrizeRepository prizes;
    @Autowired ParticipationService participation;
    @Autowired ReferralService referrals;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired JdbcTemplate jdbc;

    @Test
    void lockedCampaignAwardsGoodsPositionsOnceAndReportsCashAndVacantPositions() {
        Fixture f = fixture("ranking-award-flow");
        assertThatThrownBy(() -> awards.createRankingSimulations(f.manager().getId(), f.campaign().getPublicId(), RankingScope.PROMOTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.RANKING_AWARD_REQUIRES_LOCK));
        lock(f);

        var result = awards.createRankingSimulations(f.manager().getId(), f.campaign().getPublicId(), RankingScope.PROMOTER);

        assertThat(result.created()).extracting(AwardDeliveryService.RankingAward::position).containsExactly(1);
        assertThat(result.cashBlockedPositions()).containsExactly(2);
        assertThat(result.vacantPositions()).containsExactly(3);
        AwardDelivery delivery = deliveries.findById(result.created().getFirst().deliveryId()).orElseThrow();
        assertThat(delivery.getBeneficiaryUserId()).isEqualTo(f.leader().getId());
        assertThat(delivery.getOutcomeId()).isNull();
        assertThat(delivery.getStatus()).isEqualTo(AwardDeliveryStatus.PENDING_VERIFICATION);

        var repeated = awards.createRankingSimulations(f.manager().getId(), f.campaign().getPublicId(), RankingScope.PROMOTER);
        assertThat(repeated.snapshotId()).isEqualTo(result.snapshotId());
        assertThat(repeated.created()).isEqualTo(result.created());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM award_deliveries WHERE ranking_snapshot_id IS NOT NULL "
                + "AND beneficiary_user_id = ?", Long.class, f.leader().getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action = 'AWARD_RANKING_SIMULATION_CREATED' "
                + "AND entity_id = ?", Long.class, delivery.getId())).isEqualTo(1);
    }

    @Test
    void rankingDeliveryFollowsTheSameVerifiedContactAndEvidenceFlow() {
        Fixture f = fixture("ranking-award-delivery");
        lock(f);
        Long deliveryId = awards.createRankingSimulations(f.manager().getId(), f.campaign().getPublicId(), RankingScope.PROMOTER)
                .created().getFirst().deliveryId();

        assertThatThrownBy(() -> awards.validateBeneficiary(f.manager().getId(), f.campaign().getPublicId(), deliveryId))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.PARTICIPATION_NOT_ELIGIBLE));
        jdbc.update("UPDATE users SET phone_verified_at = UTC_TIMESTAMP() WHERE id = ?", f.leader().getId());

        awards.validateBeneficiary(f.manager().getId(), f.campaign().getPublicId(), deliveryId);
        awards.schedule(f.manager().getId(), f.campaign().getPublicId(), deliveryId);
        var delivered = awards.deliver(f.manager().getId(), f.campaign().getPublicId(), deliveryId, "s3://evidencias/entrega-1.jpg");

        assertThat(delivered.getStatus()).isEqualTo(AwardDeliveryStatus.DELIVERED);
        assertThat(delivered.getDeliveryEvidenceSha256()).hasSize(64);
        assertThat(awards.summary(f.manager().getId(), f.campaign().getPublicId()).byStatus())
                .containsExactly(new AwardDeliveryService.StatusCount("DELIVERED", 1));
        // P15-02: cada mudança da entrega gera um aviso ao vencedor (criação, validação, agendamento, entrega).
        assertThat(jdbc.queryForList("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload_json,'$.status')) FROM outbox_events "
                + "WHERE event_type='NOTIFICATION_DELIVERY_UPDATE_V1' AND aggregate_type='AWARD_DELIVERY' AND aggregate_id=? "
                + "ORDER BY id", String.class, deliveryId))
                .containsExactly("PENDING_VERIFICATION", "VALIDATED", "SCHEDULED", "DELIVERED");
    }

    @Test
    void outsiderCannotGenerateAndTamperedSnapshotBlocksTheFlow() {
        Fixture f = fixture("ranking-award-guard");
        lock(f);
        var otherOrg = organizations.save(new Organization("Outra org premiação", "ASSOCIATION"));
        User outsider = users.save(new User("Gestor", "Externo", null));
        memberships.save(new Membership(otherOrg, outsider, UserRole.CAMPAIGN_MANAGER));

        assertThatThrownBy(() -> awards.createRankingSimulations(outsider.getId(), f.campaign().getPublicId(), RankingScope.PROMOTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED));

        var result = awards.createRankingSimulations(f.manager().getId(), f.campaign().getPublicId(), RankingScope.PROMOTER);
        jdbc.update("UPDATE ranking_positions p JOIN ranking_snapshots s ON s.id = p.snapshot_id SET p.score = 99 "
                + "WHERE s.public_id = ? AND p.position = 1", result.snapshotId());

        assertThatThrownBy(() -> awards.createRankingSimulations(f.manager().getId(), f.campaign().getPublicId(), RankingScope.PROMOTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.RANKING_SNAPSHOT_HASH_MISMATCH));
    }

    private void lock(Fixture f) {
        campaignService.transition(f.manager().getId(), f.campaign().getPublicId(), CampaignStatus.LOCKED);
        participation.freezeAssignedNumbers(f.campaign().getPublicId());
    }

    private Fixture fixture(String slug) {
        Organization organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        User manager = users.save(new User("Gestor", "Premiação", null));
        memberships.save(new Membership(organization, manager, UserRole.CAMPAIGN_MANAGER));
        Campaign draft = new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO);
        draft.setNumberRange(0, 49);
        Campaign campaign = campaigns.save(draft);
        // Posição 2 em dinheiro gravada direto no repositório: simula configuração indevida que a política barra.
        prizes.save(rankingPrize(campaign, CampaignPrize.PrizeType.GOODS, "Kit 1º divulgador", 1));
        prizes.save(rankingPrize(campaign, CampaignPrize.PrizeType.CASH, "Pix 2º divulgador", 2));
        prizes.save(rankingPrize(campaign, CampaignPrize.PrizeType.GOODS, "Kit 3º divulgador", 3));
        for (CampaignStatus next : List.of(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED,
                CampaignStatus.SCHEDULED, CampaignStatus.OPEN)) {
            campaignService.transition(manager.getId(), campaign.getPublicId(), next);
        }
        participation.initializeSeries(campaign.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade " + slug));
        String suffix = "%04d".formatted(Math.abs(slug.hashCode()) % 10000);
        User leader = buy(campaign, beneficiary, "Ana", "Lider", "+55119770" + suffix + "1", null);
        User second = buy(campaign, beneficiary, "Beto", "Segundo", "+55119770" + suffix + "2", null);
        String leaderCode = referrals.link(campaign.getId(), leader.getId()).getCode();
        String secondCode = referrals.link(campaign.getId(), second.getId()).getCode();
        buy(campaign, beneficiary, "Caio", "Indicado", "+55119770" + suffix + "3", leaderCode);
        buy(campaign, beneficiary, "Duda", "Indicada", "+55119770" + suffix + "4", leaderCode);
        buy(campaign, beneficiary, "Edu", "Indicado", "+55119770" + suffix + "5", secondCode);
        return new Fixture(campaigns.findByPublicId(campaign.getPublicId()).orElseThrow(), manager, leader);
    }

    private static CampaignPrize rankingPrize(Campaign campaign, CampaignPrize.PrizeType type, String description, int position) {
        var prize = new CampaignPrize(campaign.getId(), type, description, 150_000, 1, position);
        prize.assignRankingPosition(CampaignPrize.RankingScope.PROMOTER, position);
        return prize;
    }

    private User buy(Campaign campaign, BeneficiaryEntity beneficiary, String name, String surname, String phone, String code) {
        User user = users.save(new User(name, surname, phone));
        participation.request(user.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1, beneficiary.getPublicId(), code);
        return user;
    }

    private record Fixture(Campaign campaign, User manager, User leader) {}
}
