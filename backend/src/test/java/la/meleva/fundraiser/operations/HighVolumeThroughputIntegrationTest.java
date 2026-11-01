package la.meleva.fundraiser.operations;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Alto volume (P18): demonstra capacidade para o pico esperado de **10.000 transações/hora**
 * (≈ 2,78 tx/s). Medição real; não representa capacidade de produção.
 */
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=50",
        "spring.datasource.hikari.connection-timeout=60000"})
@Import(TestcontainersConfiguration.class)
class HighVolumeThroughputIntegrationTest {

    private static final int TARGET_PER_HOUR = 10_000;
    private static final Duration WINDOW = Duration.ofSeconds(20);
    private static final int WORKERS = 64;
    private static final int USERS = 500;

    @Autowired ParticipationService participations;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;

    @Test
    void sustainsTenThousandTransactionsPerHour() throws Exception {
        var organization = organizations.save(new Organization("Organização volume", "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Volume", "volume-10k", CampaignMode.DEMO);
        campaign.setNumberRange(0, 99_999);
        var saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(saved.getId(), "Entidade volume"));
        saved.transitionTo(CampaignStatus.LEGAL_REVIEW);
        saved.transitionTo(CampaignStatus.AUTHORIZED);
        saved.transitionTo(CampaignStatus.SCHEDULED);
        saved.transitionTo(CampaignStatus.OPEN);
        campaigns.save(saved);

        var people = IntStream.range(0, USERS).mapToObj(index ->
                users.save(new User("Volume", "N" + index, "+551196640" + "%04d".formatted(index)))).toList();
        String campaignId = saved.getPublicId();
        String entityId = beneficiary.getPublicId();
        List<User> pool = List.copyOf(people);

        var completed = new AtomicLong();
        var errors = new AtomicLong();
        long deadline = System.nanoTime() + WINDOW.toNanos();
        var started = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = IntStream.range(0, WORKERS).<Callable<Void>>mapToObj(worker -> () -> {
                var random = new Random(worker);
                while (System.nanoTime() < deadline) {
                    var user = pool.get(random.nextInt(pool.size()));
                    try {
                        participations.request(user.getId(), campaignId, UUID.randomUUID().toString(), 1, entityId);
                        completed.incrementAndGet();
                    } catch (RuntimeException exception) {
                        errors.incrementAndGet();
                    }
                }
                return null;
            }).toList();
            for (var future : executor.invokeAll(tasks)) {
                future.get();
            }
        }
        double seconds = Math.max(1.0, (System.nanoTime() - started) / 1_000_000_000.0);
        long perHour = Math.round(completed.get() * 3600.0 / seconds);
        System.out.println("[P18-HV] window_s=" + Math.round(seconds) + " completed=" + completed.get()
                + " errors=" + errors.get() + " tx_per_hour=" + perHour);

        assertThat(completed.get()).isGreaterThan(0);
        assertThat(errors.get()).isLessThan(completed.get());
        assertThat(perHour).as("pico de 10.000 transações/hora").isGreaterThanOrEqualTo(TARGET_PER_HOUR);
    }
}
