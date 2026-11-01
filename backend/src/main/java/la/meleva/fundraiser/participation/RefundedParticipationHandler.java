package la.meleva.fundraiser.participation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.participation.purchase.ReservationPaymentLinkRepository;
import la.meleva.fundraiser.payment.PaymentRefundedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Estorno integral invalida as entradas do pedido na mesma transação do estorno. Rankings e apuração
 * só contam entradas ASSIGNED/FROZEN, então o efeito é imediato; snapshots já lacrados não mudam.
 */
@Component
class RefundedParticipationHandler {
    private final ReservationPaymentLinkRepository links;
    private final ParticipationEntryRepository entries;
    private final CampaignNumberRepository numbers;
    private final AuditEventRepository audits;

    RefundedParticipationHandler(ReservationPaymentLinkRepository links, ParticipationEntryRepository entries,
                                 CampaignNumberRepository numbers, AuditEventRepository audits) {
        this.links = links;
        this.entries = entries;
        this.numbers = numbers;
        this.audits = audits;
    }

    @EventListener
    void onRefund(PaymentRefundedEvent event) {
        links.findByOrderId(event.orderId()).ifPresent(link -> {
            for (ParticipationEntry entry : entries.findByRequestId(link.getRequestId())) {
                if (!entry.getCampaignId().equals(event.campaignId()) || !entry.invalidateRefunded()) continue;
                numbers.findByEntryId(entry.getId()).forEach(CampaignNumber::invalidateRefunded);
                audits.save(new AuditEvent(null, "PARTICIPATION_REFUNDED", "participation_entry", entry.getId(),
                        sha256(event.campaignId() + ":" + entry.getId() + ":payment:" + event.paymentId())));
            }
        });
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
