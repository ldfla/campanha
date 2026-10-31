package la.meleva.fundraiser.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.participation.ParticipationAllocation;
import la.meleva.fundraiser.participation.ParticipationService;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Carga e recuperação (P18). As medições reais são impressas e registradas em docs/progress/P18.md —
 * nenhum valor é inventado.
 */
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=50",
        "spring.datasource.hikari.connection-timeout=60000"})
@Import(TestcontainersConfiguration.class)
class LoadAndFreezeIntegrationTest {

    private static final int CONCURRENT_PARTICIPATIONS = 250;

    @Autowired ParticipationService participations;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired JdbcTemplate jdbc;

    @Test
    void generatesFullSeriesAssignsConcurrentlyAndUsesIndexes() throws Exception {
        var organization = organizations.save(new Organization("Organização carga", "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Carga 100k", "load-100k", CampaignMode.DEMO);
        campaign.setNumberRange(0, 99_999);
        var saved = campaigns.save(campaign);

        long started = System.nanoTime();
        assertThat(participations.initializeSeries(saved.getPublicId())).isEqualTo(100_000);
        long seriesMillis = (System.nanoTime() - started) / 1_000_000;

        var beneficiary = beneficiaries.save(new BeneficiaryEntity(saved.getId(), "Entidade carga"));
        saved.transitionTo(CampaignStatus.LEGAL_REVIEW);
        saved.transitionTo(CampaignStatus.AUTHORIZED);
        saved.transitionTo(CampaignStatus.SCHEDULED);
        saved.transitionTo(CampaignStatus.OPEN);
        campaigns.save(saved);

        var people = new ArrayList<User>();
        for (int index = 0; index < CONCURRENT_PARTICIPATIONS; index++) {
            people.add(users.save(new User("Carga", "N" + index, "+551196620" + "%04d".formatted(index))));
        }

        started = System.nanoTime();
        long[] latencies;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = people.stream().<Callable<long[]>>map(user -> () -> {
                long requestStarted = System.nanoTime();
                ParticipationAllocation allocation = participations.request(user.getId(), saved.getPublicId(),
                        UUID.randomUUID().toString(), 1, beneficiary.getPublicId());
                return new long[]{System.nanoTime() - requestStarted,
                        allocation.numbers().getFirst().numberValue()};
            }).toList();
            var futures = executor.invokeAll(tasks, 180, TimeUnit.SECONDS);
            var numbers = new HashSet<Integer>();
            latencies = new long[futures.size()];
            for (int index = 0; index < futures.size(); index++) {
                long[] result = futures.get(index).get();
                latencies[index] = result[0];
                assertThat(numbers.add((int) result[1])).isTrue();
            }
            assertThat(numbers).hasSize(CONCURRENT_PARTICIPATIONS);
        }
        long assignMillis = (System.nanoTime() - started) / 1_000_000;
        java.util.Arrays.sort(latencies);
        long p50 = latencies[latencies.length / 2] / 1_000_000;
        long p95 = latencies[(int) Math.ceil(latencies.length * 0.95) - 1] / 1_000_000;
        long p99 = latencies[(int) Math.ceil(latencies.length * 0.99) - 1] / 1_000_000;
        System.out.println("[P18] series_ms=" + seriesMillis + " assign_" + CONCURRENT_PARTICIPATIONS + "_ms="
                + assignMillis + " p50_ms=" + p50 + " p95_ms=" + p95 + " p99_ms=" + p99);

        var plan = jdbc.queryForList(
                "EXPLAIN SELECT id FROM campaign_numbers WHERE campaign_id=? AND status='AVAILABLE' ORDER BY number_value LIMIT 10",
                saved.getId());
        assertThat(plan).isNotEmpty();
        assertThat(plan.get(0).get("key")).as("consulta de disponibilidade deve usar índice").isNotNull();
    }

    @Test
    void freezesAssignedNumbersAndBlocksLateParticipations() {
        var organization = organizations.save(new Organization("Organização freeze", "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Freeze", "load-freeze", CampaignMode.DEMO);
        campaign.setNumberRange(0, 99);
        var saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(saved.getId(), "Entidade freeze"));
        saved.transitionTo(CampaignStatus.LEGAL_REVIEW);
        saved.transitionTo(CampaignStatus.AUTHORIZED);
        saved.transitionTo(CampaignStatus.SCHEDULED);
        saved.transitionTo(CampaignStatus.OPEN);
        campaigns.save(saved);

        for (int index = 0; index < 5; index++) {
            var buyer = users.save(new User("Freeze", "N" + index, "+551196630" + "%04d".formatted(index)));
            participations.request(buyer.getId(), saved.getPublicId(), UUID.randomUUID().toString(), 1, beneficiary.getPublicId());
        }
        var locked = campaigns.findById(saved.getId()).orElseThrow();
        locked.transitionTo(CampaignStatus.LOCKED);
        campaigns.saveAndFlush(locked);

        assertThat(participations.freezeAssignedNumbers(saved.getPublicId())).isEqualTo(5);
        assertThat(participations.freezeAssignedNumbers(saved.getPublicId())).isZero();

        var late = users.save(new User("Atrasado", "N", "+5511966399999"));
        assertThatThrownBy(() -> participations.request(late.getId(), saved.getPublicId(),
                UUID.randomUUID().toString(), 1, beneficiary.getPublicId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.PARTICIPATION_CAMPAIGN_CLOSED));
    }
}
