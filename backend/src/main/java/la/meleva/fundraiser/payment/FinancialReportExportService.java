package la.meleva.fundraiser.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialReportExportService {
    private final FinancialAccessService access;
    private final FinancialReportService reports;
    private final AuditEventRepository audits;

    FinancialReportExportService(FinancialAccessService access, FinancialReportService reports,
                                 AuditEventRepository audits) {
        this.access = access;
        this.reports = reports;
        this.audits = audits;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public String export(Long actorId, String campaignId, Instant from, Instant until) {
        var report = reports.report(actorId, campaignId, from, until);
        var campaign = access.requireAccess(actorId, campaignId);
        StringBuilder csv = new StringBuilder();
        row(csv, "SIMULAÇÃO", "campanha", "inicio_inclusivo", "fim_exclusivo", "moeda", "evento", "quantidade", "centavos");
        row(csv, report, "pedidos_criados", report.ordersCreated());
        row(csv, report, "cobrancas_criadas", report.chargesCreated());
        row(csv, report, "pagamentos_confirmados", report.paymentsConfirmed());
        row(csv, report, "estornos_concluidos", report.refundsCompleted());
        row(csv, report, "lancamentos_ledger", report.ledgerPosted());
        row(csv, report, "estornos_ledger", report.refundLedgerPosted());
        String data = csv.toString();
        audits.save(new AuditEvent(actorId, "FINANCIAL_SIMULATION_REPORT_EXPORTED", "campaign", campaign.getId(),
                sha256(data)));
        return data;
    }

    private static void row(StringBuilder csv, FinancialReportService.Report report, String event,
                            FinancialReportService.Totals totals) {
        row(csv, "SIMULAÇÃO", report.campaignId(), report.fromInclusive().toString(), report.untilExclusive().toString(),
                report.currency(), event, Long.toString(totals.count()), Long.toString(totals.cents()));
    }

    private static void row(StringBuilder csv, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) csv.append(',');
            csv.append(cell(values[index]));
        }
        csv.append("\r\n");
    }

    static String cell(String value) {
        int index = 0;
        while (index < value.length() && (Character.isWhitespace(value.charAt(index))
                || Character.isISOControl(value.charAt(index)))) index++;
        boolean dangerous = index < value.length() && "=+-@".indexOf(value.charAt(index)) >= 0;
        if (index > 0 || dangerous) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
