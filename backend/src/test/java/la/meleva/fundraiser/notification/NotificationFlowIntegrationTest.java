package la.meleva.fundraiser.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** P15-02/P15-03: emissão por evento de domínio, retry de falha transitória e deduplicação pela inbox. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NotificationFlowIntegrationTest {
    private static final String ADMIN_EVENT = "NOTIFICATION_ADMINISTRATIVE_CHANGE_V1";

    @Autowired CampaignService campaignService;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired MembershipRepository memberships;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired ParticipationService participation;
    @Autowired OutboxEventRepository outbox;
    @Autowired NotificationPreferenceService preferences;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void lockingTheCampaignNotifiesEachParticipantOnceWithoutPersonalData() {
        Fixture f = fixture("notify-admin-fanout");

        assertThat(adminEvents(f)).isEmpty();
        campaignService.transition(f.manager().getId(), f.campaign().getPublicId(), CampaignStatus.LOCKED);

        assertThat(adminEvents(f)).extracting(Row::aggregateId)
                .containsExactlyInAnyOrderElementsOf(f.buyers().stream().map(User::getId).toList());
        assertThat(adminEvents(f)).allSatisfy(row -> assertThat(row.payload())
                .contains(f.campaign().getPublicId()).contains("LOCKED")
                .doesNotContain(f.buyers().getFirst().getPhoneNumber()).doesNotContain("Pessoa"));
    }

    @Test
    void transientTransportFailureIsRetriedWithBackoffAndDeliveredOnlyOnce() {
        Fixture f = fixture("notify-retry");
        campaignService.transition(f.manager().getId(), f.campaign().getPublicId(), CampaignStatus.LOCKED);
        isolate(f);
        var transport = new FlakyTransport(2);
        var processor = new DemoNotificationProcessor(outbox, jdbc, transport, preferences, java.time.Clock.systemUTC());
        var tx = new TransactionTemplate(txManager);

        DemoNotificationProcessor.ProcessResult first = tx.execute(s -> processor.processNext(NotificationTemplate.ADMINISTRATIVE_CHANGE));
        DemoNotificationProcessor.ProcessResult second = tx.execute(s -> processor.processNext(NotificationTemplate.ADMINISTRATIVE_CHANGE));
        assertThat(first).isEqualTo(DemoNotificationProcessor.ProcessResult.RETRY);
        assertThat(second).isEqualTo(DemoNotificationProcessor.ProcessResult.RETRY);
        assertThat(jdbc.queryForList("SELECT CONCAT(attempt_count,'|',error_message,'|',available_at > UTC_TIMESTAMP()) "
                + "FROM outbox_events WHERE event_type=? AND attempt_count > 0 AND "
                + "JSON_UNQUOTE(JSON_EXTRACT(payload_json,'$.campaignId'))=?", String.class, ADMIN_EVENT, f.campaign().getPublicId()))
                .containsExactly("1|IllegalStateException|1", "1|IllegalStateException|1");

        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP()) WHERE event_type=? "
                + "AND JSON_UNQUOTE(JSON_EXTRACT(payload_json,'$.campaignId'))=?", ADMIN_EVENT, f.campaign().getPublicId());
        var results = new ArrayList<DemoNotificationProcessor.ProcessResult>();
        DemoNotificationProcessor.ProcessResult result;
        while ((result = tx.execute(s -> processor.processNext(NotificationTemplate.ADMINISTRATIVE_CHANGE)))
                != DemoNotificationProcessor.ProcessResult.EMPTY) {
            results.add(result);
        }

        assertThat(results).containsOnly(DemoNotificationProcessor.ProcessResult.SIMULATED).hasSize(3);
        assertThat(transport.recipients).containsExactlyInAnyOrderElementsOf(
                f.buyers().stream().map(User::getPublicId).toList());
        assertThat(adminEvents(f)).extracting(Row::status).containsOnly("PROCESSED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_events i JOIN outbox_events o "
                + "ON i.source_event_id = CONCAT('outbox:', o.id) WHERE o.event_type=? "
                + "AND JSON_UNQUOTE(JSON_EXTRACT(o.payload_json,'$.campaignId'))=?", Long.class, ADMIN_EVENT,
                f.campaign().getPublicId())).isEqualTo(3);
    }

    @Test
    void exhaustedAttemptsGoToDeadLetterAndMarketingIsNeverEmitted() {
        Fixture f = fixture("notify-dead-letter");
        campaignService.transition(f.manager().getId(), f.campaign().getPublicId(), CampaignStatus.LOCKED);
        isolate(f);
        jdbc.update("UPDATE outbox_events SET attempt_count=4 WHERE event_type=? "
                + "AND JSON_UNQUOTE(JSON_EXTRACT(payload_json,'$.campaignId'))=?", ADMIN_EVENT, f.campaign().getPublicId());
        var processor = new DemoNotificationProcessor(outbox, jdbc, new FlakyTransport(Integer.MAX_VALUE), preferences,
                java.time.Clock.systemUTC());

        DemoNotificationProcessor.ProcessResult exhausted =
                new TransactionTemplate(txManager).execute(s -> processor.processNext(NotificationTemplate.ADMINISTRATIVE_CHANGE));
        assertThat(exhausted).isEqualTo(DemoNotificationProcessor.ProcessResult.DEAD_LETTER);
        assertThatThrownBy(() -> processor.processNext(NotificationTemplate.CAMPAIGN_MARKETING))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Eventos de outras classes de teste no mesmo banco ficam fora deste cenário. */
    private void isolate(Fixture f) {
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(DAY,1,UTC_TIMESTAMP()) WHERE status='PENDING' "
                + "AND event_type=? AND (JSON_UNQUOTE(JSON_EXTRACT(payload_json,'$.campaignId')) IS NULL "
                + "OR JSON_UNQUOTE(JSON_EXTRACT(payload_json,'$.campaignId')) <> ?)", ADMIN_EVENT, f.campaign().getPublicId());
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP()) WHERE event_type=? "
                + "AND JSON_UNQUOTE(JSON_EXTRACT(payload_json,'$.campaignId'))=?", ADMIN_EVENT, f.campaign().getPublicId());
    }

    private List<Row> adminEvents(Fixture f) {
        return jdbc.query("SELECT aggregate_id, payload_json, status FROM outbox_events WHERE event_type=? "
                        + "AND JSON_UNQUOTE(JSON_EXTRACT(payload_json,'$.campaignId'))=?",
                (rs, row) -> new Row(rs.getLong(1), rs.getString(2), rs.getString(3)), ADMIN_EVENT, f.campaign().getPublicId());
    }

    private Fixture fixture(String slug) {
        Organization organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        User manager = users.save(new User("Gestor", "Avisos", null));
        memberships.save(new Membership(organization, manager, UserRole.CAMPAIGN_MANAGER));
        Campaign draft = new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO);
        draft.setNumberRange(0, 19);
        Campaign campaign = campaigns.save(draft);
        for (CampaignStatus next : List.of(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED,
                CampaignStatus.SCHEDULED, CampaignStatus.OPEN)) {
            campaignService.transition(manager.getId(), campaign.getPublicId(), next);
        }
        participation.initializeSeries(campaign.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade " + slug));
        String suffix = "%04d".formatted(Math.abs(slug.hashCode()) % 10000);
        var buyers = new ArrayList<User>();
        for (int i = 1; i <= 3; i++) {
            User buyer = users.save(new User("Pessoa", "N" + i, "+55119660" + suffix + i));
            participation.request(buyer.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1, beneficiary.getPublicId());
            buyers.add(buyer);
        }
        participation.request(buyers.getFirst().getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1,
                beneficiary.getPublicId());
        return new Fixture(campaigns.findByPublicId(campaign.getPublicId()).orElseThrow(), manager, buyers);
    }

    private record Fixture(Campaign campaign, User manager, List<User> buyers) {}

    private record Row(long aggregateId, String payload, String status) {}

    /** Falha nas primeiras {@code failures} chamadas, como um SMTP fora do ar. */
    private static final class FlakyTransport implements NotificationTransport {
        private final AtomicInteger remainingFailures;
        final List<String> recipients = new java.util.concurrent.CopyOnWriteArrayList<>();

        FlakyTransport(int failures) { this.remainingFailures = new AtomicInteger(failures); }

        @Override
        public void send(NotificationMessage message) {
            if (remainingFailures.getAndDecrement() > 0) throw new IllegalStateException("smtp indisponível");
            recipients.add(message.recipientPublicId());
        }
    }
}
