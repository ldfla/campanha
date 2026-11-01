package la.meleva.fundraiser.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Padrão: nada sai da plataforma; a inbox registra a simulação. */
@Component
@ConditionalOnProperty(name = "app.notifications.demo-smtp.enabled", havingValue = "false", matchIfMissing = true)
class RecordingOnlyNotificationTransport implements NotificationTransport {
    @Override
    public void send(NotificationMessage message) {
        // Intencionalmente vazio: sem canal autorizado, a entrega é só registrada.
    }
}
