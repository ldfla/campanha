package la.meleva.fundraiser.payment;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Agrega eventos simulados; não declara conciliação com PSP nem caixa real. */
@Service
public class FinancialReportService {
    private final FinancialAccessService access;
    private final JdbcTemplate jdbc;

    FinancialReportService(FinancialAccessService access, JdbcTemplate jdbc) {
        this.access = access;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Report report(Long actorId, String campaignId, Instant from, Instant until) {
        var campaign = access.requireAccess(actorId, campaignId);
        if (!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_OPERATION_BLOCKED.toException();
        if (from == null || until == null || !from.isBefore(until)
                || Duration.between(from, until).compareTo(Duration.ofDays(366)) > 0) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        long id = campaign.getId();
        Totals orders = sum("SELECT COUNT(*), COALESCE(SUM(o.total_cents),0) FROM orders o "
                + "WHERE o.campaign_id=? AND o.mode='DEMO' AND o.currency='BRL' AND o.created_at>=? AND o.created_at<?", id, from, until);
        Totals charges = sum("SELECT COUNT(*), COALESCE(SUM(p.amount_cents),0) FROM payments p JOIN orders o ON o.id=p.order_id "
                + "WHERE o.campaign_id=? AND o.mode='DEMO' AND o.currency='BRL' AND p.currency='BRL' AND p.provider='MOCK' "
                + "AND p.created_at>=? AND p.created_at<?", id, from, until);
        Totals confirmed = sum("SELECT COUNT(*), COALESCE(SUM(p.amount_cents),0) FROM payments p JOIN orders o ON o.id=p.order_id "
                + "WHERE o.campaign_id=? AND o.mode='DEMO' AND o.currency='BRL' AND p.currency='BRL' AND p.provider='MOCK' "
                + "AND p.confirmed_at>=? AND p.confirmed_at<?", id, from, until);
        Totals refunds = sum("SELECT COUNT(*), COALESCE(SUM(r.amount_cents),0) FROM refunds r JOIN payments p ON p.id=r.payment_id "
                + "JOIN orders o ON o.id=p.order_id WHERE o.campaign_id=? AND o.mode='DEMO' AND o.currency='BRL' "
                + "AND p.currency='BRL' AND r.currency='BRL' AND p.provider='MOCK' AND r.status='COMPLETED' "
                + "AND r.completed_at>=? AND r.completed_at<?", id, from, until);
        Totals ledger = sum("SELECT COUNT(*), COALESCE(SUM(l.amount_cents),0) FROM financial_ledger l "
                + "JOIN orders o ON o.public_id=l.source_ref AND o.campaign_id=l.campaign_id "
                + "WHERE l.campaign_id=? AND o.mode='DEMO' AND o.currency='BRL' AND l.currency='BRL' "
                + "AND EXISTS (SELECT 1 FROM payments p WHERE p.order_id=o.id AND p.provider='MOCK' AND p.currency='BRL') "
                + "AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id=o.id AND (p.provider<>'MOCK' OR p.currency<>'BRL')) "
                + "AND l.posted_at>=? AND l.posted_at<?", id, from, until);
        Totals refundLedger = sum("SELECT COUNT(*), COALESCE(SUM(l.amount_cents),0) FROM financial_ledger l "
                + "JOIN orders o ON o.public_id=l.source_ref AND o.campaign_id=l.campaign_id "
                + "WHERE l.campaign_id=? AND o.mode='DEMO' AND o.currency='BRL' AND l.currency='BRL' "
                + "AND l.entry_type='PAYMENT_REFUND' "
                + "AND EXISTS (SELECT 1 FROM payments p WHERE p.order_id=o.id AND p.provider='MOCK' AND p.currency='BRL') "
                + "AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id=o.id AND (p.provider<>'MOCK' OR p.currency<>'BRL')) "
                + "AND l.posted_at>=? AND l.posted_at<?", id, from, until);
        return new Report(campaignId, from, until, "BRL", true, orders, charges, confirmed, refunds, ledger, refundLedger,
                "Dados simulados. Cada total usa a data do respectivo evento; diferenças exigem investigação e não comprovam conciliação.");
    }

    private Totals sum(String sql, long id, Instant from, Instant until) {
        return jdbc.queryForObject(sql, (row, index) -> new Totals(row.getLong(1), exact(row.getBigDecimal(2))),
                id, Timestamp.from(from), Timestamp.from(until));
    }

    private static long exact(BigDecimal value) {
        return value.longValueExact();
    }

    public record Totals(long count, long cents) { }
    public record Report(String campaignId, Instant fromInclusive, Instant untilExclusive, String currency,
                         boolean simulation, Totals ordersCreated, Totals chargesCreated,
                         Totals paymentsConfirmed, Totals refundsCompleted, Totals ledgerPosted,
                         Totals refundLedgerPosted, String notice) { }
}
