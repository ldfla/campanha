package la.meleva.fundraiser.participation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.referral.ReferralService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BeneficiaryParticipationIntegrationTest {
    @Autowired ParticipationService service;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired JdbcTemplate jdbc;
    @Autowired ReferralService referrals;

    @Test
    void recordsOneSelectionAndRejectsChangedReplayOrInactiveEntity() {
        var organization = organizations.save(new Organization("Organização de teste", "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Campanha de teste", "beneficiary-test", CampaignMode.DEMO);
        campaign.setNumberRange(0, 4);
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        campaign = campaigns.save(campaign);
        service.initializeSeries(campaign.getPublicId());
        var user = users.save(new User("Pessoa", "Teste", "+55119999001231"));
        var promoter = users.save(new User("Pessoa", "Divulgadora", "+55119999001232"));
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade de teste"));
        var inactive = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade inativa"));
        inactive.deactivate();
        beneficiaries.saveAndFlush(inactive);
        String campaignId = campaign.getPublicId();
        String key = UUID.randomUUID().toString();
        String referralCode = referrals.link(campaign.getId(), promoter.getId()).getCode();
        // O divulgador precisa ter ao menos 1 cota comprada para indicar.
        service.request(promoter.getId(), campaignId, UUID.randomUUID().toString(), 1, beneficiary.getPublicId());

        var allocation = service.request(user.getId(), campaignId, key, 2, beneficiary.getPublicId(), referralCode);
        assertThat(service.request(user.getId(), campaignId, key, 2, beneficiary.getPublicId(), referralCode)).isEqualTo(allocation);
        assertThat(referrals.metrics(campaign.getId(), promoter.getId()).participations()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM beneficiary_selections WHERE request_id=(SELECT id FROM participation_requests WHERE public_id=?) AND beneficiary_id=?",
                Long.class, allocation.requestPublicId(), beneficiary.getId())).isEqualTo(1L);
        assertThatThrownBy(() -> service.request(user.getId(), campaignId, key, 2, inactive.getPublicId()))
                .isInstanceOf(ErrorCode.BusinessException.class)
                .extracting(exception -> ((ErrorCode.BusinessException) exception).errorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
        assertThatThrownBy(() -> service.request(user.getId(), campaignId, UUID.randomUUID().toString(), 1, inactive.getPublicId()))
                .isInstanceOf(ErrorCode.BusinessException.class)
                .extracting(exception -> ((ErrorCode.BusinessException) exception).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> service.request(user.getId(), campaignId, UUID.randomUUID().toString(), 1, UUID.randomUUID().toString()))
                .isInstanceOf(ErrorCode.BusinessException.class)
                .extracting(exception -> ((ErrorCode.BusinessException) exception).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> service.request(promoter.getId(), campaignId, UUID.randomUUID().toString(), 1,
                beneficiary.getPublicId(), referralCode)).isInstanceOf(ErrorCode.BusinessException.class)
                .extracting(exception -> ((ErrorCode.BusinessException) exception).errorCode()).isEqualTo(ErrorCode.REFERRAL_SELF_REFERRAL);
        service.request(user.getId(), campaignId, UUID.randomUUID().toString(), 1, beneficiary.getPublicId(), "codigo-invalido");
        assertThat(referrals.metrics(campaign.getId(), promoter.getId()).participations()).isEqualTo(2);
        assertThat(service.availableCount(campaignId)).isEqualTo(1);
    }
}
