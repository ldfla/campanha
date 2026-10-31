package la.meleva.fundraiser.participation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ParticipationConcurrencyIntegrationTest {
    @Autowired private ParticipationService participationService;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private CampaignNumberRepository numberRepository;
    @Autowired private AuditEventRepository auditEventRepository;

    @Test
    void allocatesOneHundredConcurrentRequestsWithoutDuplicateNumbers() throws Exception {
        Campaign campaign = openCampaign("concorrencia", 0, 149);
        participationService.initializeSeries(campaign.getPublicId());
        List<User> users = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            users.add(userRepository.save(new User("Pessoa", "Teste", "+5511987%04d".formatted(index))));
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<ParticipationAllocation>> tasks = users.stream()
                    .<Callable<ParticipationAllocation>>map(user -> () -> participationService.request(user.getId(),
                            campaign.getPublicId(), UUID.randomUUID().toString(), 1))
                    .toList();
            List<Future<ParticipationAllocation>> results = executor.invokeAll(tasks);
            Set<Integer> numbers = results.stream().map(this::get).map(result -> result.numbers().getFirst().numberValue())
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(numbers).hasSize(100);
            assertThat(participationService.availableCount(campaign.getPublicId())).isEqualTo(50);
        }
    }

    @Test
    void repeatsSameIdempotencyKeyWithoutNewAssignment() {
        Campaign campaign = openCampaign("idempotencia", 0, 2);
        participationService.initializeSeries(campaign.getPublicId());
        User user = userRepository.save(new User("Pessoa", "Idempotente", "+5511987999999"));
        String key = UUID.randomUUID().toString();

        ParticipationAllocation first = participationService.request(user.getId(), campaign.getPublicId(), key, 1);
        ParticipationAllocation repeated = participationService.request(user.getId(), campaign.getPublicId(), key, 1);

        assertThat(repeated.numbers()).isEqualTo(first.numbers());
        assertThat(participationService.availableCount(campaign.getPublicId())).isEqualTo(2);
    }

    @Test
    void generatesAllOneHundredThousandFiveDigitPositionsIncludingBoundaries() {
        Campaign campaign = openCampaign("serie-completa", 0, 99999);

        assertThat(participationService.initializeSeries(campaign.getPublicId())).isEqualTo(100000);
        assertThat(numberRepository.countByCampaignId(campaign.getId())).isEqualTo(100000);
        assertThat(numberRepository.findByCampaignIdAndSeriesCodeAndNumberValue(campaign.getId(), "A", 0)).isPresent();
        assertThat(numberRepository.findByCampaignIdAndSeriesCodeAndNumberValue(campaign.getId(), "A", 99999)).isPresent();
    }

    @Test
    void freezesAssignedNumbersOnlyAfterCampaignIsLocked() {
        Campaign campaign = openCampaign("congelamento", 0, 1);
        participationService.initializeSeries(campaign.getPublicId());
        User user = userRepository.save(new User("Pessoa", "Congelamento", "+5511987888888"));
        participationService.request(user.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1);
        campaign.transitionTo(CampaignStatus.LOCKED);
        campaignRepository.save(campaign);

        assertThat(participationService.freezeAssignedNumbers(campaign.getPublicId())).isEqualTo(1);
        assertThat(numberRepository.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.FROZEN)).isEqualTo(1);
        Long entryId = numberRepository.findByCampaignIdAndStatus(campaign.getId(), NumberStatus.FROZEN).getFirst().getEntryId();
        participationService.invalidateFrozenEntry(user.getId(), campaign.getPublicId(), entryId, "Evidência inválida");
        assertThat(numberRepository.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.INVALIDATED)).isEqualTo(1);
        assertThat(auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAt("participation_entry", entryId)).hasSize(1);
    }

    @Test
    void preserves00001AndRejectsExhaustedPool() {
        Campaign campaign = openCampaign("esgotamento", 0, 1);
        participationService.initializeSeries(campaign.getPublicId());
        User first = userRepository.save(new User("Pessoa", "Um", "+5511987111111"));
        User second = userRepository.save(new User("Pessoa", "Dois", "+5511987222222"));
        User third = userRepository.save(new User("Pessoa", "Três", "+5511987333333"));
        participationService.request(first.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1);
        ParticipationAllocation secondAllocation = participationService.request(second.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1);
        assertThat(secondAllocation.numbers().getFirst().formattedNumber()).isEqualTo("00001");
        assertThatThrownBy(() -> participationService.request(third.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1))
                .isInstanceOf(BusinessException.class);
    }

    private Campaign openCampaign(String slug, int min, int max) {
        Organization organization = organizationRepository.save(new Organization("Organização " + slug, "ASSOCIATION"));
        Campaign campaign = new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO);
        campaign.setNumberRange(min, max);
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        return campaignRepository.save(campaign);
    }

    private ParticipationAllocation get(Future<ParticipationAllocation> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError("Atribuição concorrente falhou", exception);
        }
    }
}
