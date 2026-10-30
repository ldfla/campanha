package la.meleva.fundraiser.referral;

import static org.assertj.core.api.Assertions.assertThat;

import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.participation.ParticipationEntry;
import la.meleva.fundraiser.participation.ParticipationEntryRepository;
import la.meleva.fundraiser.participation.ParticipationRequest;
import la.meleva.fundraiser.participation.ParticipationRequestRepository;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReferralServiceIntegrationTest {
    @Autowired private ReferralService service;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ParticipationRequestRepository requestRepository;
    @Autowired private ParticipationEntryRepository entryRepository;

    @Test
    void createsStableNonEnumerableLinkAndCountsHashedClicks() {
        Organization org = organizationRepository.save(new Organization("Org referral", "ASSOCIATION"));
        User promoter = userRepository.save(new User("Promotor", "Teste", "+5511987666666"));
        Campaign campaign = campaignRepository.save(new Campaign(org.getId(), "Campanha referral", "referral-test", CampaignMode.DEMO));
        ReferralLink first = service.link(campaign.getId(), promoter.getId());
        ReferralLink repeated = service.link(campaign.getId(), promoter.getId());
        service.click(campaign.getId(), first.getCode(), "navegador-anonimo-1");

        assertThat(first.getCode()).matches("^[A-Za-z0-9]{12}$");
        assertThat(repeated.getCode()).isEqualTo(first.getCode());
        assertThat(service.metrics(campaign.getId(), promoter.getId()).clicks()).isEqualTo(1);
        assertThat(service.metrics(campaign.getId(), promoter.getId()).participations()).isZero();
    }

    @Test
    void attributesOnlyOnceAndBlocksSelfReferral() {
        Organization org = organizationRepository.save(new Organization("Org attribution", "ASSOCIATION"));
        Campaign campaign = campaignRepository.save(new Campaign(org.getId(), "Campanha attribution", "attribution-test", CampaignMode.DEMO));
        User promoter = userRepository.save(new User("Promotor", "Teste", "+5511987555555"));
        User referred = userRepository.save(new User("Indicado", "Teste", "+5511987444444"));
        ReferralLink link = service.link(campaign.getId(), promoter.getId());
        ParticipationRequest request = requestRepository.save(new ParticipationRequest(campaign.getId(), referred.getId(), java.util.UUID.randomUUID().toString(), 1));
        ParticipationEntry entry = entryRepository.save(new ParticipationEntry(campaign.getId(), referred.getId(), request.getId(), "DEMO_SIMULATION"));
        entry.markAssigned();
        entryRepository.save(entry);

        service.attribute(campaign.getId(), link.getCode(), entry.getId());
        assertThat(service.metrics(campaign.getId(), promoter.getId()).participations()).isEqualTo(1);
        assertThatThrownBy(() -> service.attribute(campaign.getId(), link.getCode(), entry.getId())).isInstanceOf(BusinessException.class);

        ParticipationRequest selfRequest = requestRepository.save(new ParticipationRequest(campaign.getId(), promoter.getId(), java.util.UUID.randomUUID().toString(), 1));
        ParticipationEntry selfEntry = entryRepository.save(new ParticipationEntry(campaign.getId(), promoter.getId(), selfRequest.getId(), "DEMO_SIMULATION"));
        selfEntry.markAssigned();
        entryRepository.save(selfEntry);
        assertThatThrownBy(() -> service.attribute(campaign.getId(), link.getCode(), selfEntry.getId())).isInstanceOf(BusinessException.class);
    }
}
