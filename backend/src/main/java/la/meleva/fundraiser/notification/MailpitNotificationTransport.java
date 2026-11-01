package la.meleva.fundraiser.notification;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * P15-06: envio DEMO para o Mailpit local. Falha fechado: só host de loopback/serviço local, nunca com
 * operação real ligada, e o destinatário é sempre sintético em {@code .invalid} (RFC 2606), sem rota real.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.demo-smtp.enabled", havingValue = "true")
class MailpitNotificationTransport implements NotificationTransport {
    static final String SINK_DOMAIN = "demo.invalid";
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "mailpit", "fundraiser-mailpit");

    private final JavaMailSender mail;

    MailpitNotificationTransport(JavaMailSender mail, @Value("${spring.mail.host:}") String host,
                                 @Value("${app.live-operations-enabled:false}") boolean liveOperationsEnabled) {
        if (liveOperationsEnabled) throw new IllegalStateException("Transporte DEMO não pode operar com operações reais ligadas");
        if (!LOCAL_HOSTS.contains(host)) throw new IllegalStateException("Transporte DEMO só aceita SMTP local");
        this.mail = mail;
    }

    @Override
    public void send(NotificationMessage message) {
        var email = new SimpleMailMessage();
        email.setFrom("nao-responda@" + SINK_DOMAIN);
        email.setTo(message.recipientPublicId() + "@" + SINK_DOMAIN);
        email.setSubject("[DEMO] " + message.template().subject());
        email.setText(message.template().body()
                + "\n\nMensagem simulada do ambiente de demonstração (template v" + message.template().version() + ").");
        mail.send(email);
    }
}
