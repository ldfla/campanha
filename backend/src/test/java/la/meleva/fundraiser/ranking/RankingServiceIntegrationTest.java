package la.meleva.fundraiser.ranking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignService;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.participation.ParticipationEntry;
import la.meleva.fundraiser.participation.ParticipationEntryRepository;
import la.meleva.fundraiser.participation.ParticipationService;
import la.meleva.fundraiser.ranking.dto.CreateSnapshotRequest;
import la.meleva.fundraiser.ranking.dto.RankingRuleRequest;
import la.meleva.fundraiser.ranking.dto.RankingView;
import la.meleva.fundraiser.referral.ReferralLink;
import la.meleva.fundraiser.referral.ReferralService;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RankingServiceIntegrationTest {

    @Autowired RankingService rankings;
    @Autowired CampaignService campaignService;
    @Autowired ParticipationService participation;
    @Autowired ReferralService referrals;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired CampaignRepository campaigns;
    @Autowired ParticipationEntryRepository entries;
    @Autowired RankingSnapshotRepository snapshots;
    @Autowired RankingPositionRepository positions;
    @Autowired JdbcTemplate jdbc;

    @Test
    void promoterRankingCountsOnlyEligibleReferralsAndIsRepeatable() {
        Fixture fixture = fixture("ranking-promoter");
        User leader = user("Ana", "Lima", "+551199990001");
        User second = user("Bruno", "Melo", "+551199990002");
        User third = user("Carla", "Melo", "+551199990003");
        User promoterA = user("Paulo", "Reis", "+551199990010");
        User promoterB = user("Rita", "Dias", "+551199990011");

        entry(fixture, leader);
        entry(fixture, second);
        entry(fixture, third);
        attribute(fixture, promoterA, entryOf(fixture, leader));
        attribute(fixture, promoterA, entryOf(fixture, second));
        attribute(fixture, promoterB, entryOf(fixture, third));

        RankingView view = rankings.current(fixture.campaign().getPublicId(), RankingScope.PROMOTER, 50);

        assertThat(view.scope()).isEqualTo(RankingScope.PROMOTER);
        assertThat(view.metric()).isEqualTo(RankingMetric.REFERRAL_CONFIRMED_PARTICIPATIONS);
        assertThat(view.simulation()).isTrue();
        assertThat(view.awardsEligible()).isFalse();
        assertThat(view.notice()).contains("SIMULAÇÃO");
        assertThat(view.entries()).hasSize(2);
        assertThat(view.entries().get(0).pseudonym()).isEqualTo("P.R. 0010");
        assertThat(view.entries().get(0).score()).isEqualTo(2);
        assertThat(view.entries().get(1).pseudonym()).isEqualTo("R.D. 0011");
        assertThat(view.entries().get(1).score()).isEqualTo(1);
        assertThat(rankings.current(fixture.campaign().getPublicId(), RankingScope.PROMOTER, 50).entries())
                .isEqualTo(view.entries());
    }

    @Test
    void tieIsBrokenByFirstEventThenOpaquePublicId() {
        Fixture fixture = fixture("ranking-tie");
        User a = user("Ana", "Lima", "+551199990021");
        User b = user("Bruno", "Melo", "+551199990022");
        User promoterX = user("Paulo", "Reis", "+551199990030");
        User promoterY = user("Rita", "Dias", "+551199990031");

        attribute(fixture, promoterX, entry(fixture, a));
        attribute(fixture, promoterY, entry(fixture, b));
        jdbc.update("UPDATE referral_attributions SET attributed_at = '2026-01-01 00:00:00' WHERE campaign_id = ?",
                fixture.campaign().getId());

        RankingView view = rankings.current(fixture.campaign().getPublicId(), RankingScope.PROMOTER, 50);
        String expectedFirst = promoterX.getPublicId().compareTo(promoterY.getPublicId()) < 0 ? "P.R. 0030" : "R.D. 0031";

        assertThat(view.entries()).hasSize(2);
        assertThat(view.entries().get(0).score()).isEqualTo(view.entries().get(1).score());
        assertThat(view.entries().get(0).pseudonym()).isEqualTo(expectedFirst);
    }

    @Test
    void snapshotIsFrozenHashedAndUnaffectedByLaterInvalidation() {
        Fixture fixture = fixture("ranking-snapshot");
        User leader = user("Ana", "Lima", "+551199990041");
        User second = user("Bruno", "Melo", "+551199990042");
        User third = user("Carla", "Melo", "+551199990043");
        entry(fixture, leader);
        entry(fixture, leader);
        ParticipationEntry secondEntry = entry(fixture, second);
        entry(fixture, third);

        RankingView live = rankings.current(fixture.campaign().getPublicId(), RankingScope.PARTICIPANT, 50);
        assertThat(live.simulation()).isTrue();
        assertThat(live.entries()).hasSize(3);
        assertThat(live.entries().get(0).pseudonym()).isEqualTo("A.L. 0041");
        assertThat(live.entries().get(0).score()).isEqualTo(2);

        var created = rankings.snapshot(leader.getId(), fixture.campaign().getPublicId(),
                new CreateSnapshotRequest(RankingScope.PARTICIPANT, null));
        var repeated = rankings.snapshot(leader.getId(), fixture.campaign().getPublicId(),
                new CreateSnapshotRequest(RankingScope.PARTICIPANT, created.cutoffAt()));

        assertThat(created.snapshotId()).isNotBlank();
        assertThat(created.snapshotHash()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(created.entryCount()).isEqualTo(3);
        assertThat(repeated.snapshotId()).isEqualTo(created.snapshotId());
        assertThat(repeated.snapshotHash()).isEqualTo(created.snapshotHash());

        RankingSnapshot stored = snapshots.findByPublicIdAndCampaignId(created.snapshotId(), fixture.campaign().getId())
                .orElseThrow();
        assertThat(positions.findByIdSnapshotIdOrderByPosition(stored.getId())).hasSize(3);

        var export = rankings.export(null, fixture.campaign().getPublicId(), created.snapshotId());
        assertThat(export.verified()).isTrue();
        assertThat(export.payloadJson()).contains("\"position\":1").contains("A.L. 0041");
        assertThat(export.payloadJson()).doesNotContain("Lima").doesNotContain("+551199990041");

        campaignService.transition(fixture.campaign().getPublicId(), CampaignStatus.LOCKED);
        participation.freezeAssignedNumbers(fixture.campaign().getPublicId());
        participation.invalidateFrozenEntry(leader.getId(), fixture.campaign().getPublicId(),
                secondEntry.getId(), "contestação de pagamento simulada");

        RankingView afterInvalidation = rankings.current(fixture.campaign().getPublicId(), RankingScope.PARTICIPANT, 50);
        assertThat(afterInvalidation.entries()).hasSize(2);
        assertThat(rankings.export(null, fixture.campaign().getPublicId(), created.snapshotId()).payloadJson())
                .isEqualTo(export.payloadJson());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE action = 'RANKING_SNAPSHOT_CREATED'", Long.class))
                .isPositive();
    }

    @Test
    void onlyEligibleEntriesOfTheirOwnCampaignAreScored() {
        Fixture first = fixture("ranking-isolation-a");
        Fixture other = fixture("ranking-isolation-b");
        User participant = user("Ana", "Lima", "+551199990051");
        User promoter = user("Paulo", "Reis", "+551199990052");

        entry(first, participant);
        entry(other, participant);
        attribute(first, promoter, entryOf(first, participant));
        attribute(other, promoter, entryOf(other, participant));

        RankingView firstView = rankings.current(first.campaign().getPublicId(), RankingScope.PROMOTER, 50);
        RankingView otherView = rankings.current(other.campaign().getPublicId(), RankingScope.PROMOTER, 50);

        assertThat(firstView.entries()).hasSize(1);
        assertThat(firstView.entries().get(0).score()).isEqualTo(1);
        assertThat(otherView.entries()).hasSize(1);
        assertThat(otherView.entries().get(0).score()).isEqualTo(1);
        assertThat(rankings.current(first.campaign().getPublicId(), RankingScope.PARTICIPANT, 50).entries())
                .hasSize(1);
    }

    @Test
    void rejectsMetricOutsideScopeAndRuleChangesAfterOpening() {
        Fixture fixture = fixture("ranking-rules");
        User manager = user("Marta", "Gestora", "+551199990061");

        assertThatThrownBy(() -> rankings.registerRule(manager.getId(), fixture.campaign().getPublicId(),
                new RankingRuleRequest(RankingScope.PARTICIPANT,
                        RankingMetric.REFERRAL_CONFIRMED_PARTICIPATIONS, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.RANKING_RULE_INVALID));

        assertThatThrownBy(() -> rankings.registerRule(manager.getId(), fixture.campaign().getPublicId(),
                new RankingRuleRequest(RankingScope.PROMOTER,
                        RankingMetric.REFERRAL_CONFIRMED_PARTICIPATIONS, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.CAMPAIGN_NOT_EDITABLE));

        Fixture draft = draftFixture("ranking-rules-draft");
        var firstRule = rankings.registerRule(manager.getId(), draft.campaign().getPublicId(),
                new RankingRuleRequest(RankingScope.PROMOTER,
                        RankingMetric.REFERRAL_CONFIRMED_PARTICIPATIONS, null));
        var secondRule = rankings.registerRule(manager.getId(), draft.campaign().getPublicId(),
                new RankingRuleRequest(RankingScope.PROMOTER,
                        RankingMetric.REFERRAL_CONFIRMED_PARTICIPATIONS, null));

        assertThat(firstRule.ruleVersion()).isEqualTo(1);
        assertThat(secondRule.ruleVersion()).isEqualTo(2);
        assertThat(firstRule.legallyApproved()).isFalse();
        assertThat(rankings.current(draft.campaign().getPublicId(), RankingScope.PROMOTER, 50).ruleVersion())
                .isEqualTo(2);

        assertThatThrownBy(() -> rankings.snapshot(manager.getId(), draft.campaign().getPublicId(),
                new CreateSnapshotRequest(RankingScope.PROMOTER, Instant.now().plusSeconds(60))))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    private Fixture fixture(String slug) {
        Fixture draft = draftFixture(slug);
        campaignService.transition(draft.campaign().getPublicId(), CampaignStatus.LEGAL_REVIEW);
        campaignService.transition(draft.campaign().getPublicId(), CampaignStatus.AUTHORIZED);
        campaignService.transition(draft.campaign().getPublicId(), CampaignStatus.SCHEDULED);
        campaignService.transition(draft.campaign().getPublicId(), CampaignStatus.OPEN);
        Campaign campaign = campaigns.findByPublicId(draft.campaign().getPublicId()).orElseThrow();
        participation.initializeSeries(campaign.getPublicId());
        return new Fixture(campaign, draft.organization());
    }

    private Fixture draftFixture(String slug) {
        Organization organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        Campaign campaign = campaigns.save(new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO));
        campaign.setNumberRange(0, 49);
        Campaign saved = campaigns.save(campaign);
        return new Fixture(saved, organization);
    }

    private User user(String name, String surname, String phone) {
        return users.save(new User(name, surname, phone));
    }

    private ParticipationEntry entry(Fixture fixture, User user) {
        participation.request(user.getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(), 1);
        return entryOf(fixture, user);
    }

    private ParticipationEntry entryOf(Fixture fixture, User user) {
        List<ParticipationEntry> found = entries.findByCampaignIdAndUserId(fixture.campaign().getId(), user.getId());
        return found.get(found.size() - 1);
    }

    private void attribute(Fixture fixture, User promoter, ParticipationEntry entry) {
        ReferralLink link = referrals.link(fixture.campaign().getId(), promoter.getId());
        referrals.attribute(fixture.campaign().getId(), link.getCode(), entry.getId());
    }

    private record Fixture(Campaign campaign, Organization organization) {}
}
