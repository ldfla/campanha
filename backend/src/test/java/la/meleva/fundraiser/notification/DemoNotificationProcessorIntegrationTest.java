package la.meleva.fundraiser.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.participation.ParticipationRequest;
import la.meleva.fundraiser.participation.ParticipationRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class DemoNotificationProcessorIntegrationTest {
    @Autowired DemoNotificationProcessor processor;
    @Autowired OutboxEventRepository outbox;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired OrganizationRepository organizations;
    @Autowired CampaignRepository campaigns;
    @Autowired ParticipationRequestRepository requests;

    @Test
    void simulationRollsBackReplaysWithoutDuplicatesAndDoesNotDeliverToContacts() {
        var user = users.save(new User("Pessoa", "Simulação", "+55119999007991"));
        var event = outbox.saveAndFlush(new OutboxEvent("USER", user.getId(),
                "NOTIFICATION_ACCOUNT_REGISTERED_V1", "{}"));
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP()) WHERE id=?", event.getId());
        String sourceId = "outbox:" + event.getId();
        var transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> {
            assertThat(processor.processNext(NotificationTemplate.ACCOUNT_REGISTERED)).isEqualTo(DemoNotificationProcessor.ProcessResult.SIMULATED);
            status.setRollbackOnly();
        });
        assertThat(count(sourceId)).isZero();
        assertThat(status(event.getId())).isEqualTo("PENDING");

        assertThat(processor.processNext(NotificationTemplate.ACCOUNT_REGISTERED)).isEqualTo(DemoNotificationProcessor.ProcessResult.SIMULATED);
        assertThat(count(sourceId)).isEqualTo(1L);
        assertThat(status(event.getId())).isEqualTo("PROCESSED");
        jdbc.update("UPDATE outbox_events SET status='PENDING', processed_at=NULL WHERE id=?", event.getId());
        assertThat(processor.processNext(NotificationTemplate.ACCOUNT_REGISTERED)).isEqualTo(DemoNotificationProcessor.ProcessResult.SIMULATED);
        assertThat(count(sourceId)).isEqualTo(1L);
        assertThat(status(event.getId())).isEqualTo("PROCESSED");

        var invalid = outbox.saveAndFlush(new OutboxEvent("USER", Long.MAX_VALUE,
                "NOTIFICATION_ACCOUNT_REGISTERED_V1", "{}"));
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP()) WHERE id=?", invalid.getId());
        assertThat(processor.processNext(NotificationTemplate.ACCOUNT_REGISTERED)).isEqualTo(DemoNotificationProcessor.ProcessResult.DEAD_LETTER);
        assertThat(count("outbox:" + invalid.getId())).isZero();
        assertThat(status(invalid.getId())).isEqualTo("DEAD_LETTER");
        var nextUser = users.save(new User("Outra", "Pessoa", "+55119999007993"));
        var next = outbox.saveAndFlush(new OutboxEvent("USER", nextUser.getId(),
                "NOTIFICATION_ACCOUNT_REGISTERED_V1", "{}"));
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP()) WHERE id=?", next.getId());
        assertThat(processor.processNext(NotificationTemplate.ACCOUNT_REGISTERED)).isEqualTo(DemoNotificationProcessor.ProcessResult.SIMULATED);
        assertThat(count("outbox:" + next.getId())).isEqualTo(1L);
        assertThatThrownBy(() -> processor.processNext(NotificationTemplate.RESULT_AVAILABLE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void participationSimulationRequiresDemoCampaignAndAssignedRequest() {
        var user = users.save(new User("Pessoa", "Participação", "+55119999007992"));
        var organization = organizations.save(new Organization("Organização de simulação", "ASSOCIATION"));
        var campaign = campaigns.save(new Campaign(organization.getId(), "Simulação", "notification-demo", CampaignMode.DEMO));
        var request = new ParticipationRequest(campaign.getId(), user.getId(), java.util.UUID.randomUUID().toString(), 1);
        request.markAssigned();
        request = requests.save(request);
        var event = outbox.saveAndFlush(new OutboxEvent("PARTICIPATION_REQUEST", request.getId(),
                "NOTIFICATION_PARTICIPATION_VALID_V1", "{}"));
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP()) WHERE id=?", event.getId());
        assertThat(processor.processNext(NotificationTemplate.PARTICIPATION_VALID)).isEqualTo(DemoNotificationProcessor.ProcessResult.SIMULATED);
        assertThat(count("outbox:" + event.getId())).isEqualTo(1L);

        var commercial = campaigns.save(new Campaign(organization.getId(), "Não DEMO", "notification-non-demo", CampaignMode.COMMERCIAL_PROMOTION));
        var nonDemoRequest = new ParticipationRequest(commercial.getId(), user.getId(), java.util.UUID.randomUUID().toString(), 1);
        nonDemoRequest.markAssigned();
        nonDemoRequest = requests.save(nonDemoRequest);
        var blocked = outbox.saveAndFlush(new OutboxEvent("PARTICIPATION_REQUEST", nonDemoRequest.getId(),
                "NOTIFICATION_PARTICIPATION_VALID_V1", "{}"));
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP()) WHERE id=?", blocked.getId());
        assertThat(processor.processNext(NotificationTemplate.PARTICIPATION_VALID)).isEqualTo(DemoNotificationProcessor.ProcessResult.DEAD_LETTER);
        assertThat(count("outbox:" + blocked.getId())).isZero();
        assertThat(status(blocked.getId())).isEqualTo("DEAD_LETTER");
    }

    private long count(String sourceId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM inbox_events WHERE source='NOTIFICATION_SIMULATION' AND source_event_id=?",
                Long.class, sourceId);
    }

    private String status(Long id) {
        return jdbc.queryForObject("SELECT status FROM outbox_events WHERE id=?", String.class, id);
    }
}
