package la.meleva.fundraiser.operations;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.participation.ParticipationService;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentFreezeIntegrationTest {
    @Autowired ParticipationService participations;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;

    @Test
    void participationCommittedBeforeFreezeIsIncluded() throws Exception {
        var fixture = fixture("freeze-participation-first", "+5511966500001");
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var competing = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                participations.request(fixture.user().getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                        fixture.beneficiary().getPublicId());
                locked.countDown();
                await(release);
            }));
            try {
                assertThat(locked.await(15, TimeUnit.SECONDS)).isTrue();
                var second = executor.submit(() -> {
                    competing.countDown();
                    return new TransactionTemplate(transactions).execute(tx -> lockAndFreeze(fixture.campaign()));
                });
                assertThat(competing.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                first.get(20, TimeUnit.SECONDS);
                assertThat(second.get(20, TimeUnit.SECONDS)).isEqualTo(1);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id=? AND status='FROZEN'",
                        Long.class, fixture.campaign().getId())).isEqualTo(1);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void freezeCommittedBeforeParticipationRejectsTheRequest() throws Exception {
        var fixture = fixture("freeze-lock-first", "+5511966500002");
        participations.request(fixture.user().getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                fixture.beneficiary().getPublicId());
        var late = users.save(new User("Pessoa", "Tardia", "+5511966500003"));
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var competing = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> new TransactionTemplate(transactions).execute(tx -> {
                int frozen = lockAndFreeze(fixture.campaign());
                locked.countDown();
                await(release);
                return frozen;
            }));
            try {
                assertThat(locked.await(15, TimeUnit.SECONDS)).isTrue();
                var second = executor.submit(() -> {
                    competing.countDown();
                    return participations.request(late.getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                            fixture.beneficiary().getPublicId());
                });
                assertThat(competing.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(1);
                assertThatThrownBy(() -> second.get(20, TimeUnit.SECONDS))
                        .isInstanceOf(java.util.concurrent.ExecutionException.class)
                        .hasCauseInstanceOf(ErrorCode.BusinessException.class)
                        .satisfies(error -> assertThat(((ErrorCode.BusinessException) error.getCause()).errorCode())
                                .isEqualTo(ErrorCode.PARTICIPATION_CAMPAIGN_CLOSED));
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id=? AND status='FROZEN'",
                        Long.class, fixture.campaign().getId())).isEqualTo(1);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participation_requests WHERE campaign_id=? AND user_id=?",
                        Long.class, fixture.campaign().getId(), late.getId())).isZero();
            } finally {
                release.countDown();
            }
        }
    }

    private int lockAndFreeze(Campaign campaign) {
        var locked = campaigns.findLockedByPublicId(campaign.getPublicId()).orElseThrow();
        locked.transitionTo(CampaignStatus.LOCKED);
        campaigns.saveAndFlush(locked);
        return participations.freezeAssignedNumbers(locked.getPublicId());
    }

    private Fixture fixture(String slug, String phone) {
        var organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Congelamento DEMO", slug, CampaignMode.DEMO);
        campaign.setNumberRange(0, 3);
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        var saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(saved.getId(), "Entidade " + slug));
        return new Fixture(saved, users.save(new User("Pessoa", "Teste", phone)), beneficiary);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private record Fixture(Campaign campaign, User user, BeneficiaryEntity beneficiary) { }
}
