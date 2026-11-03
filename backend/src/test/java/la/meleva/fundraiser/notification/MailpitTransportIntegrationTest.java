package la.meleva.fundraiser.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * P15-06: envio DEMO chega ao Mailpit local com destinatário sintético; configurações inseguras não sobem.
 * Usa o contexto compartilhado (sem MySQL extra) e monta o transporte apontando para o contêiner Mailpit.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers
class MailpitTransportIntegrationTest {
    @Container
    static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:latest")
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/api/v1/info").forPort(8025));

    @Autowired OutboxEventRepository outbox;
    @Autowired UserRepository users;
    @Autowired NotificationPreferenceService preferences;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void essentialNotificationIsDeliveredToMailpitWithSyntheticRecipientOnly() throws Exception {
        var sender = new JavaMailSenderImpl();
        sender.setHost(MAILPIT.getHost());
        sender.setPort(MAILPIT.getMappedPort(1025));
        var transport = new MailpitNotificationTransport(sender, "localhost", false);
        var processor = new DemoNotificationProcessor(outbox, jdbc, transport, preferences, Clock.systemUTC());

        User user = users.save(new User("Maria", "Silva", "+5511944440001"));
        var event = outbox.saveAndFlush(new OutboxEvent("USER", user.getId(), "NOTIFICATION_ACCOUNT_REGISTERED_V1", "{}"));
        // Outras classes de teste deixam eventos de cadastro pendentes no mesmo banco.
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(DAY,1,UTC_TIMESTAMP()) WHERE status='PENDING' "
                + "AND event_type='NOTIFICATION_ACCOUNT_REGISTERED_V1' AND id<>?", event.getId());
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP()) WHERE id=?", event.getId());

        DemoNotificationProcessor.ProcessResult result = new TransactionTemplate(txManager)
                .execute(status -> processor.processNext(NotificationTemplate.ACCOUNT_REGISTERED));
        assertThat(result).isEqualTo(DemoNotificationProcessor.ProcessResult.SIMULATED);

        String messages = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://"
                        + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + "/api/v1/messages")).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        assertThat(messages).contains(user.getPublicId() + "@demo.invalid").contains("[DEMO] Cadastro recebido")
                .doesNotContain("+5511944440001").doesNotContain("Maria");
    }

    @Test
    void refusesRemoteSmtpAndLiveOperations() {
        var sender = new JavaMailSenderImpl();
        assertThatThrownBy(() -> new MailpitNotificationTransport(sender, "smtp.gmail.com", false))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new MailpitNotificationTransport(sender, "localhost", true))
                .isInstanceOf(IllegalStateException.class);
    }
}
