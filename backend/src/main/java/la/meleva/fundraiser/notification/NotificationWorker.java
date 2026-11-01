package la.meleva.fundraiser.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Esvazia o outbox de notificações em lotes limitados; cada evento roda na própria transação do processador. */
@Component
@ConditionalOnProperty(name = "app.notifications.worker-enabled", havingValue = "true")
class NotificationWorker {
    private static final int MAX_PER_TEMPLATE = 50;
    private final DemoNotificationProcessor processor;

    NotificationWorker(DemoNotificationProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${app.notifications.worker-delay-ms:60000}")
    void drain() {
        for (NotificationTemplate template : DemoNotificationProcessor.SUPPORTED) {
            for (int i = 0; i < MAX_PER_TEMPLATE; i++) {
                if (processor.processNext(template) == DemoNotificationProcessor.ProcessResult.EMPTY) break;
            }
        }
    }
}
