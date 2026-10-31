package la.meleva.fundraiser.notification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra somente uma simulação no banco; não encaminha mensagem a nenhum endereço. */
@Service
public class DemoNotificationProcessor {
    public enum ProcessResult { EMPTY, SIMULATED, DEAD_LETTER }
    private final OutboxEventRepository outbox;
    private final JdbcTemplate jdbc;

    DemoNotificationProcessor(OutboxEventRepository outbox, JdbcTemplate jdbc) {
        this.outbox = outbox;
        this.jdbc = jdbc;
    }

    @Transactional
    public ProcessResult processNext(NotificationTemplate template) {
        if (template == null) throw new IllegalArgumentException("Modelo obrigatório");
        String eventType = switch (template) {
            case ACCOUNT_REGISTERED -> "NOTIFICATION_ACCOUNT_REGISTERED_V1";
            case PARTICIPATION_VALID -> "NOTIFICATION_PARTICIPATION_VALID_V1";
            default -> throw new IllegalArgumentException("Modelo ainda não suportado");
        };
        var events = outbox.lockAvailableBatchByEventType(eventType, Instant.now(), 1);
        if (events.isEmpty()) return ProcessResult.EMPTY;
        var event = events.getFirst();
        if (!eligible(event, template)) {
            event.deadLetterInvalidSource();
            return ProcessResult.DEAD_LETTER;
        }
        String sourceEventId = "outbox:" + event.getId();
        String hash = sha256(eventType + ":" + sourceEventId);
        jdbc.update("INSERT IGNORE INTO inbox_events (source,source_event_id,payload_hash,received_at,processed_at) "
                        + "VALUES ('NOTIFICATION_SIMULATION',?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP())",
                sourceEventId, hash);
        event.processed();
        return ProcessResult.SIMULATED;
    }

    private boolean eligible(OutboxEvent event, NotificationTemplate template) {
        Integer count;
        if (template == NotificationTemplate.ACCOUNT_REGISTERED && "USER".equals(event.getAggregateType())) {
            count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id=? AND status='ACTIVE'", Integer.class,
                    event.getAggregateId());
        } else if (template == NotificationTemplate.PARTICIPATION_VALID
                && "PARTICIPATION_REQUEST".equals(event.getAggregateType())) {
            count = jdbc.queryForObject("SELECT COUNT(*) FROM participation_requests r "
                            + "JOIN campaigns c ON c.id=r.campaign_id JOIN users u ON u.id=r.user_id "
                            + "WHERE r.id=? AND r.status='ASSIGNED' AND c.mode='DEMO' AND u.status='ACTIVE'",
                    Integer.class, event.getAggregateId());
        } else {
            return false;
        }
        return count != null && count == 1;
    }

    private static String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
