package la.meleva.fundraiser.payment;

import java.time.Instant;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/financial/campaigns/{campaignId}")
public class FinancialReportController {
    private final FinancialReportService service;
    private final FinancialReportExportService exports;

    FinancialReportController(FinancialReportService service, FinancialReportExportService exports) {
        this.service = service;
        this.exports = exports;
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('FINANCE', 'CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public FinancialReportService.Report report(@AuthenticationPrincipal AppUserDetails principal,
                                                @PathVariable String campaignId,
                                                @RequestParam Instant from,
                                                @RequestParam Instant until) {
        return service.report(principal.userId(), campaignId, from, until);
    }

    @GetMapping(value = "/reports/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('FINANCE', 'CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<String> export(@AuthenticationPrincipal AppUserDetails principal,
                                         @PathVariable String campaignId, @RequestParam Instant from,
                                         @RequestParam Instant until) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-simulado.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(exports.export(principal.userId(), campaignId, from, until));
    }
}
