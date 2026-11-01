package la.meleva.fundraiser.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignService;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.AuthTestSupport;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.participation.ParticipationEntry;
import la.meleva.fundraiser.participation.ParticipationEntryRepository;
import la.meleva.fundraiser.participation.ParticipationService;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.ranking.dto.CreateSnapshotRequest;
import la.meleva.fundraiser.referral.ReferralLink;
import la.meleva.fundraiser.referral.ReferralService;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** P08-07: cookie stuffing, replay, troca de URL, contas vinculadas e manipulação de relatório. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReferralFraudIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired RankingService rankings;
    @Autowired CampaignService campaignService;
    @Autowired ParticipationService participation;
    @Autowired ReferralService referrals;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired CampaignRepository campaigns;
    @Autowired ParticipationEntryRepository entries;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired MembershipRepository memberships;
    @Autowired JdbcTemplate jdbc;

    @Test
    void manualAttributionOfSomeoneElsesEntryIsNotExposed() throws Exception {
        Fixture fixture = fixture("fraud-manual-endpoint");
        User victim = user("Vera", "Lima", "+551198880001");
        ParticipationEntry victimEntry = buy(fixture, victim, null);
        var attackerSession = AuthTestSupport.registerAndSignIn(mvc, "Ataque", "Teste", "+5511988800002");
        User attacker = users.findByPhoneNumber("+5511988800002").orElseThrow();
        buy(fixture, attacker, null);
        ReferralLink attackerLink = referrals.link(fixture.campaign().getId(), attacker.getId());

        mvc.perform(post("/referrals/campaigns/" + fixture.campaign().getId() + "/attributions/"
                        + attackerLink.getCode() + "/" + victimEntry.getId()).cookie(attackerSession).with(csrf()))
                .andExpect(status().is4xxClientError());

        assertThat(promoterRanking(fixture)).isEmpty();
        assertThat(referrals.metrics(fixture.campaign().getId(), attacker.getId()).participations()).isZero();
    }

    @Test
    void cookieStuffingAndReplayCannotRewriteAnExistingAttribution() {
        Fixture fixture = fixture("fraud-stuffing");
        User honest = promoterWithQuota(fixture, "Paulo", "Reis", "+551198880011");
        User stuffer = promoterWithQuota(fixture, "Rita", "Dias", "+551198880012");
        String honestCode = referrals.link(fixture.campaign().getId(), honest.getId()).getCode();
        String stufferCode = referrals.link(fixture.campaign().getId(), stuffer.getId()).getCode();
        ParticipationEntry entry = buy(fixture, user("Bia", "Melo", "+551198880013"), honestCode);

        assertThatThrownBy(() -> referrals.attributeIfValid(fixture.campaign().getId(), stufferCode, entry.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.REFERRAL_ALREADY_ATTRIBUTED));
        assertThatThrownBy(() -> referrals.attributeIfValid(fixture.campaign().getId(), honestCode, entry.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.REFERRAL_ALREADY_ATTRIBUTED));

        var ranking = promoterRanking(fixture);
        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).pseudonym()).isEqualTo("P.R. 0011");
        assertThat(ranking.get(0).score()).isEqualTo(1);
    }

    @Test
    void replayedClicksInflateOnlyTheClickMetricNeverTheRanking() {
        Fixture fixture = fixture("fraud-click-replay");
        User promoter = promoterWithQuota(fixture, "Paulo", "Reis", "+551198880021");
        String code = referrals.link(fixture.campaign().getId(), promoter.getId()).getCode();

        for (int i = 0; i < 30; i++) {
            referrals.click(fixture.campaign().getId(), code, "mesmo-navegador");
        }

        var metrics = referrals.metrics(fixture.campaign().getId(), promoter.getId());
        assertThat(metrics.clicks()).isEqualTo(30);
        assertThat(metrics.participations()).isZero();
        assertThat(promoterRanking(fixture)).isEmpty();
        assertThat(jdbc.queryForList("SELECT anonymous_id_hash FROM referral_clicks", String.class))
                .noneMatch(hash -> hash.contains("mesmo-navegador"));
    }

    @Test
    void codeFromAnotherCampaignIsIgnoredOnPurchaseAndRejectedOnClick() {
        Fixture origin = fixture("fraud-url-origin");
        Fixture target = fixture("fraud-url-target");
        User promoter = promoterWithQuota(origin, "Paulo", "Reis", "+551198880031");
        String originCode = referrals.link(origin.campaign().getId(), promoter.getId()).getCode();

        ParticipationEntry entry = buy(target, user("Bia", "Melo", "+551198880032"), originCode);

        assertThat(entry.getStatus().name()).isEqualTo("ASSIGNED");
        assertThat(promoterRanking(target)).isEmpty();
        assertThat(promoterRanking(origin)).isEmpty();
        assertThatThrownBy(() -> referrals.click(target.campaign().getId(), originCode, "navegador-x"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED));
        assertThatThrownBy(() -> referrals.click(target.campaign().getId(), "codigoInexist", "navegador-x"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void promoterWithoutOwnQuotaEarnsNoAttribution() {
        Fixture fixture = fixture("fraud-no-quota");
        User promoter = user("Paulo", "Reis", "+551198880041");
        String code = referrals.link(fixture.campaign().getId(), promoter.getId()).getCode();

        buy(fixture, user("Bia", "Melo", "+551198880042"), code);

        assertThat(promoterRanking(fixture)).isEmpty();
        assertThat(referrals.metrics(fixture.campaign().getId(), promoter.getId()).participations()).isZero();
    }

    @Test
    void linkedAccountsCannotSelfReferAndInvalidatedSockEntriesLeaveTheRanking() {
        Fixture fixture = fixture("fraud-linked");
        User promoter = promoterWithQuota(fixture, "Paulo", "Reis", "+551198880051");
        String code = referrals.link(fixture.campaign().getId(), promoter.getId()).getCode();

        assertThatThrownBy(() -> buy(fixture, promoter, code))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.REFERRAL_SELF_REFERRAL));
        assertThat(entries.findByCampaignIdAndUserId(fixture.campaign().getId(), promoter.getId())).hasSize(1);

        ParticipationEntry sock = buy(fixture, user("Conta", "Vinculada", "+551198880052"), code);
        assertThat(promoterRanking(fixture)).hasSize(1);

        campaignService.transition(fixture.manager().getId(), fixture.campaign().getPublicId(), CampaignStatus.LOCKED);
        participation.freezeAssignedNumbers(fixture.campaign().getPublicId());
        participation.invalidateFrozenEntry(fixture.manager().getId(), fixture.campaign().getPublicId(),
                sock.getId(), "contas vinculadas confirmadas em análise antifraude");

        assertThat(promoterRanking(fixture)).isEmpty();
    }

    @Test
    void directDatabaseEditOfSnapshotPositionsOrPayloadIsDetected() {
        Fixture fixture = fixture("fraud-report-tamper");
        User promoterA = promoterWithQuota(fixture, "Paulo", "Reis", "+551198880061");
        User promoterB = promoterWithQuota(fixture, "Rita", "Dias", "+551198880062");
        String codeA = referrals.link(fixture.campaign().getId(), promoterA.getId()).getCode();
        String codeB = referrals.link(fixture.campaign().getId(), promoterB.getId()).getCode();
        buy(fixture, user("Bia", "Melo", "+551198880063"), codeA);
        buy(fixture, user("Caio", "Melo", "+551198880064"), codeA);
        buy(fixture, user("Duda", "Melo", "+551198880065"), codeB);

        var snapshot = rankings.snapshot(fixture.manager().getId(), fixture.campaign().getPublicId(),
                new CreateSnapshotRequest(RankingScope.PROMOTER, null));
        assertThat(rankings.export(null, fixture.campaign().getPublicId(), snapshot.snapshotId()).verified()).isTrue();
        Long snapshotId = jdbc.queryForObject("SELECT id FROM ranking_snapshots WHERE public_id = ?", Long.class,
                snapshot.snapshotId());

        jdbc.update("UPDATE ranking_positions SET score = 99 WHERE snapshot_id = ? AND position = 2", snapshotId);
        assertTampered(fixture, snapshot.snapshotId());
        jdbc.update("UPDATE ranking_positions SET score = 1 WHERE snapshot_id = ? AND position = 2", snapshotId);
        assertThat(rankings.export(null, fixture.campaign().getPublicId(), snapshot.snapshotId()).verified()).isTrue();

        jdbc.update("UPDATE ranking_positions SET position = CASE position WHEN 1 THEN 3 ELSE position END "
                + "WHERE snapshot_id = ?", snapshotId);
        assertTampered(fixture, snapshot.snapshotId());
        jdbc.update("UPDATE ranking_positions SET position = 1 WHERE snapshot_id = ? AND position = 3", snapshotId);

        jdbc.update("UPDATE ranking_snapshots SET payload_json = REPLACE(payload_json, '\"score\":2', '\"score\":9') "
                + "WHERE id = ?", snapshotId);
        assertTampered(fixture, snapshot.snapshotId());
    }

    @Test
    void referralMetricsRequirePromoterRoleAndNeverExposeAnotherPromoter() throws Exception {
        Fixture fixture = fixture("fraud-metrics");
        var participantSession = AuthTestSupport.registerAndSignIn(mvc, "Leitor", "Curioso", "+5511988800071");

        mvc.perform(get("/referrals/campaigns/" + fixture.campaign().getId() + "/metrics").cookie(participantSession))
                .andExpect(status().isForbidden());
        mvc.perform(get("/referrals/campaigns/" + fixture.campaign().getId() + "/metrics"))
                .andExpect(status().isUnauthorized());
    }

    private void assertTampered(Fixture fixture, String snapshotId) {
        assertThatThrownBy(() -> rankings.export(null, fixture.campaign().getPublicId(), snapshotId))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.RANKING_SNAPSHOT_HASH_MISMATCH));
    }

    private List<la.meleva.fundraiser.ranking.dto.RankingView.Row> promoterRanking(Fixture fixture) {
        return rankings.current(fixture.campaign().getPublicId(), RankingScope.PROMOTER, 50).entries();
    }

    private User promoterWithQuota(Fixture fixture, String name, String surname, String phone) {
        User promoter = user(name, surname, phone);
        buy(fixture, promoter, null);
        return promoter;
    }

    private ParticipationEntry buy(Fixture fixture, User buyer, String referralCode) {
        participation.request(buyer.getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                fixture.beneficiary().getPublicId(), referralCode);
        List<ParticipationEntry> found = entries.findByCampaignIdAndUserId(fixture.campaign().getId(), buyer.getId());
        return found.get(found.size() - 1);
    }

    private Fixture fixture(String slug) {
        Organization organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        User manager = users.save(new User("Gestor", "Teste", null));
        memberships.save(new Membership(organization, manager, UserRole.CAMPAIGN_MANAGER));
        Campaign draft = campaigns.save(new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO));
        draft.setNumberRange(0, 49);
        Campaign campaign = campaigns.save(draft);
        for (CampaignStatus next : List.of(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED,
                CampaignStatus.SCHEDULED, CampaignStatus.OPEN)) {
            campaignService.transition(manager.getId(), campaign.getPublicId(), next);
        }
        participation.initializeSeries(campaign.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade " + slug));
        return new Fixture(campaigns.findByPublicId(campaign.getPublicId()).orElseThrow(), manager, beneficiary);
    }

    private User user(String name, String surname, String phone) {
        return users.save(new User(name, surname, phone));
    }

    private record Fixture(Campaign campaign, User manager, BeneficiaryEntity beneficiary) {}
}
