package la.meleva.fundraiser.notification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processa uma notificação do outbox por vez: valida a origem, respeita o opt-out de marketing, entrega pelo
 * transporte configurado e registra na inbox (deduplicação). Falha do transporte reagenda com backoff (P15-03).
 */
@Service
public class DemoNotificationProcessor {
    public enum ProcessResult { EMPTY, SIMULATED, SKIPPED_OPT_OUT, RETRY, DEAD_LETTER }

    /** Marketing não é emitido enquanto não houver base legal aprovada (G-LEGAL). */
    static final Set<NotificationTemplate> SUPPORTED = EnumSet.of(NotificationTemplate.ACCOUNT_REGISTERED,
            NotificationTemplate.PARTICIPATION_VALID, NotificationTemplate.ADMINISTRATIVE_CHANGE,
            NotificationTemplate.RESULT_AVAILABLE, NotificationTemplate.DELIVERY_UPDATE);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(30);

    private final OutboxEventRepository outbox;
    private final JdbcTemplate jdbc;
    private final NotificationTransport transport;
    private final NotificationPreferenceService preferences;
    private final Clock clock;

    @Autowired
    DemoNotificationProcessor(OutboxEventRepository outbox, JdbcTemplate jdbc, NotificationTransport transport,
                              NotificationPreferenceService preferences) {
        this(outbox, jdbc, transport, preferences, Clock.systemUTC());
    }

    DemoNotificationProcessor(OutboxEventRepository outbox, JdbcTemplate jdbc, NotificationTransport transport,
                              NotificationPreferenceService preferences, Clock clock) {
        this.outbox = outbox;
        this.jdbc = jdbc;
        this.transport = transport;
        this.preferences = preferences;
        this.clock = clock;
    }

    @Transactional
    public ProcessResult processNext(NotificationTemplate template) {
        if (template == null) throw new IllegalArgumentException("Modelo obrigatório");
        if (!SUPPORTED.contains(template)) throw new IllegalArgumentException("Modelo ainda não suportado");
        Instant now = Instant.now(clock);
        var events = outbox.lockAvailableBatchByEventType(NotificationEnqueuer.eventType(template), now, 1);
        if (events.isEmpty()) return ProcessResult.EMPTY;
        var event = events.getFirst();
        List<Recipient> recipients = recipient(event, template);
        if (recipients.size() != 1) {
            event.deadLetterInvalidSource();
            return ProcessResult.DEAD_LETTER;
        }
        Recipient recipient = recipients.getFirst();
        String sourceEventId = "outbox:" + event.getId();
        if (alreadyDelivered(sourceEventId)) {
            event.processed();
            return ProcessResult.SIMULATED;
        }
        if (!NotificationPreferenceService.mayDispatch(template, preferences.marketingOptedOut(recipient.userId()))) {
            event.processed();
            return ProcessResult.SKIPPED_OPT_OUT;
        }
        try {
            transport.send(new NotificationTransport.NotificationMessage(recipient.publicId(), template));
        } catch (RuntimeException failure) {
            event.recordFailure(reason(failure), now.plus(backoff(event.getAttemptCount())));
            return "DEAD_LETTER".equals(event.getStatus()) ? ProcessResult.DEAD_LETTER : ProcessResult.RETRY;
        }
        jdbc.update("INSERT IGNORE INTO inbox_events (source,source_event_id,payload_hash,received_at,processed_at) "
                        + "VALUES ('NOTIFICATION_SIMULATION',?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP())",
                sourceEventId, sha256(event.getEventType() + ":" + sourceEventId));
        event.processed();
        return ProcessResult.SIMULATED;
    }

    private boolean alreadyDelivered(String sourceEventId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM inbox_events WHERE source='NOTIFICATION_SIMULATION' "
                + "AND source_event_id=?", Integer.class, sourceEventId);
        return count != null && count > 0;
    }

    /** Destinatário ativo e campanha DEMO; qualquer outra origem vai para DEAD_LETTER. */
    private List<Recipient> recipient(OutboxEvent event, NotificationTemplate template) {
        String sql = switch (template) {
            case ACCOUNT_REGISTERED -> "USER".equals(event.getAggregateType())
                    ? "SELECT u.id, u.public_id FROM users u WHERE u.id=? AND u.status='ACTIVE'" : null;
            case PARTICIPATION_VALID -> "PARTICIPATION_REQUEST".equals(event.getAggregateType())
                    ? "SELECT u.id, u.public_id FROM participation_requests r JOIN campaigns c ON c.id=r.campaign_id "
                    + "JOIN users u ON u.id=r.user_id WHERE r.id=? AND r.status='ASSIGNED' AND c.mode='DEMO' AND u.status='ACTIVE'"
                    : null;
            case ADMINISTRATIVE_CHANGE, RESULT_AVAILABLE -> "USER".equals(event.getAggregateType())
                    ? "SELECT u.id, u.public_id FROM users u JOIN outbox_events o ON o.id=? "
                    + "JOIN campaigns c ON c.public_id=JSON_UNQUOTE(JSON_EXTRACT(o.payload_json,'$.campaignId')) "
                    + "WHERE u.id=o.aggregate_id AND u.status='ACTIVE' AND c.mode='DEMO'"
                    : null;
            case DELIVERY_UPDATE -> "AWARD_DELIVERY".equals(event.getAggregateType())
                    ? "SELECT u.id, u.public_id FROM award_deliveries d JOIN users u ON u.id=d.beneficiary_user_id "
                    + "LEFT JOIN draw_outcomes o ON o.id=d.outcome_id LEFT JOIN draws dr ON dr.id=o.draw_id "
                    + "LEFT JOIN ranking_snapshots s ON s.id=d.ranking_snapshot_id "
                    + "JOIN campaigns c ON c.id=COALESCE(dr.campaign_id, s.campaign_id) "
                    + "WHERE d.id=? AND u.status='ACTIVE' AND c.mode='DEMO'"
                    : null;
            case CAMPAIGN_MARKETING -> null;
        };
        if (sql == null) return List.of();
        Long key = template == NotificationTemplate.ADMINISTRATIVE_CHANGE || template == NotificationTemplate.RESULT_AVAILABLE
                ? event.getId() : event.getAggregateId();
        return jdbc.query(sql, (rs, row) -> new Recipient(rs.getLong(1), rs.getString(2)), key);
    }

    private static Duration backoff(int attemptsSoFar) {
        Duration next = Duration.ofMinutes(1L << Math.min(attemptsSoFar, 5));
        return next.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : next;
    }

    /** Só o tipo do erro: mensagens do transporte podem conter endereços. */
    private static String reason(RuntimeException failure) {
        return failure instanceof BusinessException business ? business.errorCode().name() : failure.getClass().getSimpleName();
    }

    private static String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record Recipient(long userId, String publicId) { }
}
