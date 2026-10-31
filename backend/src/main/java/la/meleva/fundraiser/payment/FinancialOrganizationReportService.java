package la.meleva.fundraiser.payment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Agrega o relatório simulado de todas as campanhas DEMO de uma organização. */
@Service
public class FinancialOrganizationReportService {

    private final CampaignRepository campaigns;
    private final FinancialReportService reports;
    private final MembershipRepository memberships;
    private final UserRepository users;

    FinancialOrganizationReportService(CampaignRepository campaigns, FinancialReportService reports,
                                       MembershipRepository memberships, UserRepository users) {
        this.campaigns = campaigns;
        this.reports = reports;
        this.memberships = memberships;
        this.users = users;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OrganizationReport report(Long actorId, Long organizationId, Instant from, Instant until) {
        if (organizationId == null) throw ErrorCode.VALIDATION_ERROR.toException();
        if (from == null || until == null || !from.isBefore(until)
                || Duration.between(from, until).compareTo(Duration.ofDays(366)) > 0) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        var actor = users.findById(actorId).filter(user -> user.isActive())
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        boolean allowed = memberships.findActiveByUserAndOrganization(actor.getId(), organizationId).stream()
                .anyMatch(membership -> membership.getRole() == UserRole.FINANCE
                        || membership.getRole() == UserRole.CAMPAIGN_MANAGER
                        || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();

        var perCampaign = campaigns.findByOrganizationId(organizationId).stream()
                .filter(campaign -> campaign.getMode().isDemo())
                .map(campaign -> reports.report(actorId, campaign.getPublicId(), from, until))
                .toList();
        return new OrganizationReport(organizationId, from, until, "BRL", true,
                perCampaign.stream().map(FinancialOrganizationReportService::line).toList(),
                total(perCampaign, FinancialReportService.Report::ordersCreated),
                total(perCampaign, FinancialReportService.Report::chargesCreated),
                total(perCampaign, FinancialReportService.Report::paymentsConfirmed),
                total(perCampaign, FinancialReportService.Report::refundsCompleted),
                total(perCampaign, FinancialReportService.Report::ledgerPosted),
                total(perCampaign, FinancialReportService.Report::refundLedgerPosted),
                "Dados simulados por campanha DEMO. Não representa caixa real nem conciliação com PSP.");
    }

    private static CampaignLine line(FinancialReportService.Report report) {
        return new CampaignLine(report.campaignId(), report.ordersCreated(), report.chargesCreated(),
                report.paymentsConfirmed(), report.refundsCompleted(), report.ledgerPosted(),
                report.refundLedgerPosted());
    }

    private static FinancialReportService.Totals total(List<FinancialReportService.Report> reports,
                                                       java.util.function.Function<FinancialReportService.Report,
                                                               FinancialReportService.Totals> extractor) {
        long count = 0;
        long cents = 0;
        for (var report : reports) {
            var totals = extractor.apply(report);
            count = Math.addExact(count, totals.count());
            cents = Math.addExact(cents, totals.cents());
        }
        return new FinancialReportService.Totals(count, cents);
    }

    public record CampaignLine(String campaignId, FinancialReportService.Totals ordersCreated,
                               FinancialReportService.Totals chargesCreated,
                               FinancialReportService.Totals paymentsConfirmed,
                               FinancialReportService.Totals refundsCompleted,
                               FinancialReportService.Totals ledgerPosted,
                               FinancialReportService.Totals refundLedgerPosted) { }

    public record OrganizationReport(Long organizationId, Instant fromInclusive, Instant untilExclusive,
                                     String currency, boolean simulation, List<CampaignLine> campaigns,
                                     FinancialReportService.Totals ordersCreated,
                                     FinancialReportService.Totals chargesCreated,
                                     FinancialReportService.Totals paymentsConfirmed,
                                     FinancialReportService.Totals refundsCompleted,
                                     FinancialReportService.Totals ledgerPosted,
                                     FinancialReportService.Totals refundLedgerPosted, String notice) {
        public OrganizationReport { campaigns = List.copyOf(campaigns); }
    }
}
