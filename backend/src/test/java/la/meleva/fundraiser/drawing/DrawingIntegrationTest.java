package la.meleva.fundraiser.drawing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignPrize;
import la.meleva.fundraiser.campaign.CampaignPrizeRepository;
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
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, DrawingIntegrationTest.FakeFederal.class})
class DrawingIntegrationTest {

    @TestConfiguration
    static class FakeFederal {
        static volatile List<String> prizes = List.of("069015", "015794", "050550", "059017", "013549");

        @Bean
        @Primary
        FederalLotteryClient federalLotteryClient() {
            return contestNumber -> {
                String dezenas = String.join("\",\"", prizes);
                String payload = "{\"numero\":" + contestNumber + ",\"dataApuracao\":\"17/12/2025\",\"listaDezenas\":[\""
                        + dezenas + "\"]}";
                return new FederalContest(contestNumber, LocalDate.of(2025, 12, 17), List.copyOf(prizes), payload,
                        "https://servicebus2.caixa.gov.br/portaldeloterias/api/federal/" + contestNumber);
            };
        }
    }

    private static final AtomicInteger PHONE = new AtomicInteger(500);

    @Autowired DrawingService drawings;
    @Autowired FederalResultService federalResults;
    @Autowired ParticipationService participations;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired CampaignPrizeRepository prizes;
    @Autowired JdbcTemplate jdbc;

    @Test
    void freezesComputesPublishesAndRejectsUnverifiedResult() {
        var organization = organizations.save(new Organization("Organização apuração", "ASSOCIATION"));
        var manager = member(organization, "Gestor", UserRole.CAMPAIGN_MANAGER);
        var auditorA = member(organization, "AuditorA", UserRole.DRAW_AUDITOR);
        var auditorB = member(organization, "AuditorB", UserRole.DRAW_AUDITOR);

        var campaign = draftCampaign(organization, "drawing-flow", 10);
        var rule = drawings.registerRule(manager.getId(), campaign.getPublicId(),
                "{\"source\":\"FEDERAL\",\"digits\":5,\"prizePositions\":[1,2,3,4,5]}");
        assertThat(rule.version()).isEqualTo(1);

        campaign = open(campaign);
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade apuração"));
        var buyerA = users.save(new User("Compra", "A", phone()));
        var buyerB = users.save(new User("Compra", "B", phone()));
        participations.request(buyerA.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1, beneficiary.getPublicId());
        participations.request(buyerB.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1, beneficiary.getPublicId());
        campaign = campaigns.findById(campaign.getId()).orElseThrow();
        campaign.transitionTo(CampaignStatus.LOCKED);
        campaigns.saveAndFlush(campaign);
        final String campaignId = campaign.getPublicId();
        participations.freezeAssignedNumbers(campaign.getPublicId());

        var assigned = jdbc.queryForList("SELECT number_value FROM campaign_numbers WHERE campaign_id=? AND status='FROZEN' ORDER BY number_value",
                Integer.class, campaign.getId());
        assertThat(assigned).hasSize(2);
        FakeFederal.prizes = List.of("%05d".formatted(assigned.get(0)), "%05d".formatted(assigned.get(1)),
                "099999", "088888", "077777");

        var imported = federalResults.importResult(auditorA.getId(), 6027);
        assertThat(imported.verifiedByTwo()).isFalse();
        assertThat(imported.sourceDocumentHash()).hasSize(64);
        assertThat(federalResults.importResult(auditorA.getId(), 6027).id()).isEqualTo(imported.id());

        var draw = drawings.freeze(manager.getId(), campaign.getPublicId());
        assertThat(draw.entriesHash()).hasSize(64);
        assertThat(draw.status()).isEqualTo(DrawStatus.PENDING.name());

        assertThatThrownBy(() -> drawings.compute(auditorA.getId(), campaignId, draw.publicId(), "6027"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.DRAW_RESULTS_NOT_VERIFIED));

        federalResults.review(auditorA.getId(), imported.id());
        var verified = federalResults.review(auditorB.getId(), imported.id());
        assertThat(verified.verifiedByTwo()).isTrue();

        var computed = drawings.compute(auditorA.getId(), campaign.getPublicId(), draw.publicId(), "6027");
        assertThat(computed.status()).isEqualTo(DrawStatus.COMPUTED.name());

        Long drawId = jdbc.queryForObject("SELECT id FROM draws WHERE public_id=?", Long.class, draw.publicId());
        var outcomes = jdbc.queryForList("SELECT decision_code FROM draw_outcomes WHERE draw_id=? ORDER BY prize_id", String.class, drawId);
        assertThat(outcomes).hasSize(5);
        assertThat(outcomes.stream().filter("MATCH"::equals).count()).isEqualTo(2);

        var published = drawings.publish(auditorA.getId(), campaign.getPublicId(), draw.publicId());
        assertThat(published.status()).isEqualTo(DrawStatus.PUBLISHED.name());
        assertThatThrownBy(() -> drawings.publish(auditorA.getId(), campaignId, draw.publicId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.DRAW_ALREADY_PUBLISHED));

        var transparency = drawings.transparency(campaign.getPublicId(), draw.publicId());
        assertThat(transparency.entriesHash()).isEqualTo(draw.entriesHash());
        assertThat(transparency.contestIdentifier()).isEqualTo("6027");
        assertThat(transparency.resultPayload()).contains("listaDezenas");
        assertThat(transparency.outcomes()).hasSize(5);
    }

    @Test
    void refusesImportWhenOfficialSourceIsDisabled() {
        assertThatThrownBy(() -> new DisabledFederalLotteryClient().fetch(6027))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.FEDERAL_SOURCE_UNAVAILABLE));
    }

    private String phone() {
        return "+5511966" + "%06d".formatted(PHONE.getAndIncrement());
    }

    private User member(Organization organization, String name, UserRole role) {
        var user = users.save(new User(name, "Teste", phone()));
        memberships.save(new Membership(organization, user, role));
        return user;
    }

    private Campaign draftCampaign(Organization organization, String slug, int capacity) {
        var campaign = new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO);
        campaign.setNumberRange(0, capacity - 1);
        var saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        for (int position = 1; position <= 5; position++) {
            prizes.save(new CampaignPrize(saved.getId(), CampaignPrize.PrizeType.GOODS,
                    "Prêmio " + position, 100_000L * position, 1, position));
        }
        return saved;
    }

    private Campaign open(Campaign campaign) {
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        return campaigns.save(campaign);
    }
}
