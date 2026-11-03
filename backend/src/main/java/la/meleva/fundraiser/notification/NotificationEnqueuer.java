package la.meleva.fundraiser.notification;

import java.util.EnumSet;
import java.util.Set;
import la.meleva.fundraiser.award.AwardDeliveryChangedEvent;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.campaign.CampaignStatusChangedEvent;
import la.meleva.fundraiser.drawing.DrawPublishedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * P15-02: transforma eventos de domínio em notificações no outbox, na mesma transação da mudança.
 * O payload leva só identificadores públicos e estado — nunca nome, telefone ou e-mail.
 */
@Component
class NotificationEnqueuer {
    /** Mudanças que alteram o que o participante pode fazer ou esperar da campanha. */
    private static final Set<CampaignStatus> ADMINISTRATIVE = EnumSet.of(
            CampaignStatus.SUSPENDED, CampaignStatus.CANCELLED, CampaignStatus.LOCKED);

    private static final String FAN_OUT = """
            INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload_json, status, available_at,
                                       attempt_count, max_attempts, created_at)
            SELECT 'USER', p.user_id, ?, JSON_OBJECT('campaignId', c.public_id, 'reference', ?), 'PENDING',
                   UTC_TIMESTAMP(), 0, 5, UTC_TIMESTAMP()
            FROM (SELECT DISTINCT user_id FROM participation_entries
                  WHERE campaign_id = ? AND status IN ('ASSIGNED', 'FROZEN')) p
            JOIN campaigns c ON c.id = ?
            """;

    private final JdbcTemplate jdbc;

    NotificationEnqueuer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener
    void onCampaignStatus(CampaignStatusChangedEvent event) {
        boolean resumed = event.to() == CampaignStatus.OPEN && event.from() == CampaignStatus.SUSPENDED;
        if (!ADMINISTRATIVE.contains(event.to()) && !resumed) return;
        jdbc.update(FAN_OUT, eventType(NotificationTemplate.ADMINISTRATIVE_CHANGE), event.to().name(),
                event.campaignId(), event.campaignId());
    }

    @EventListener
    void onDrawPublished(DrawPublishedEvent event) {
        String drawPublicId = jdbc.queryForObject("SELECT public_id FROM draws WHERE id = ?", String.class, event.drawId());
        jdbc.update(FAN_OUT, eventType(NotificationTemplate.RESULT_AVAILABLE), drawPublicId,
                event.campaignId(), event.campaignId());
    }

    @EventListener
    void onDelivery(AwardDeliveryChangedEvent event) {
        jdbc.update("INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload_json, status, available_at, "
                        + "attempt_count, max_attempts, created_at) VALUES ('AWARD_DELIVERY', ?, ?, JSON_OBJECT('status', ?), "
                        + "'PENDING', UTC_TIMESTAMP(), 0, 5, UTC_TIMESTAMP())",
                event.deliveryId(), eventType(NotificationTemplate.DELIVERY_UPDATE), event.status().name());
    }

    static String eventType(NotificationTemplate template) {
        return "NOTIFICATION_" + template.name() + "_V1";
    }
}
