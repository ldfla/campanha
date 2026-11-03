package la.meleva.fundraiser.participation;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Indicação de prêmio recebido: "Meus números" deve refletir o prêmio-surpresa do número do titular
 * (a carga inicial grava {@code prize_cents} em {@code campaign_numbers}).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MyNumbersPrizeIntegrationTest {

    @Autowired ParticipationService participations;
    @Autowired MyNumbersService myNumbers;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired JdbcTemplate jdbc;

    @Test
    void mineIncludesSurprisePrizeOfOwnedNumber() {
        var org = organizations.save(new Organization("Organização prêmio", "ASSOCIATION"));
        var user = users.save(new User("Pessoa", "Premio", "+5511987000001"));
        var campaign = new Campaign(org.getId(), "Prêmio", "premio-teste", CampaignMode.DEMO);
        campaign.setNumberRange(0, 9);
        for (var status : new CampaignStatus[]{CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED,
                CampaignStatus.SCHEDULED, CampaignStatus.OPEN}) {
            campaign.transitionTo(status);
        }
        var saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        var entity = beneficiaries.save(new BeneficiaryEntity(saved.getId(), "Entidade prêmio"));

        var allocation = participations.request(user.getId(), saved.getPublicId(),
                UUID.randomUUID().toString(), 1, entity.getPublicId());
        int numberValue = Integer.parseInt(allocation.numbers().getFirst().formattedNumber());

        // Carga inicial atribuiu 1000 (R$10) a este número.
        jdbc.update("UPDATE campaign_numbers SET prize_cents=1000, prize_defined_at=UTC_TIMESTAMP() "
                + "WHERE campaign_id=? AND number_value=?", saved.getId(), numberValue);

        var view = myNumbers.mine(user.getId(), saved.getPublicId());
        assertThat(view.numbers()).hasSize(1);
        assertThat(view.numbers().getFirst().prizeCents()).isEqualTo(1000);
        assertThat(view.numbers().getFirst().status()).isEqualTo("CONFIRMED");
        assertThat(view.prizeTotalCents()).isEqualTo(1000);
    }

    @Test
    void mineWithoutPrizeReportsZero() {
        var org = organizations.save(new Organization("Organização sem prêmio", "ASSOCIATION"));
        var user = users.save(new User("Pessoa", "SemPremio", "+5511987000002"));
        var campaign = new Campaign(org.getId(), "Sem prêmio", "sem-premio-teste", CampaignMode.DEMO);
        campaign.setNumberRange(0, 9);
        for (var status : new CampaignStatus[]{CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED,
                CampaignStatus.SCHEDULED, CampaignStatus.OPEN}) {
            campaign.transitionTo(status);
        }
        var saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        var entity = beneficiaries.save(new BeneficiaryEntity(saved.getId(), "Entidade sem prêmio"));

        participations.request(user.getId(), saved.getPublicId(), UUID.randomUUID().toString(), 1, entity.getPublicId());

        var view = myNumbers.mine(user.getId(), saved.getPublicId());
        assertThat(view.numbers().getFirst().prizeCents()).isZero();
        assertThat(view.prizeTotalCents()).isZero();
    }
}
