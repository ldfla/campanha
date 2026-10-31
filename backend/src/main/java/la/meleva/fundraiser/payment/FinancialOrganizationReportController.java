package la.meleva.fundraiser.payment;

import java.time.Instant;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/financial/organizations/{organizationId}")
public class FinancialOrganizationReportController {

    private final FinancialOrganizationReportService service;

    FinancialOrganizationReportController(FinancialOrganizationReportService service) {
        this.service = service;
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('FINANCE', 'CAMPAIGN_MANAGER', 'SYSTEM_ADMIN')")
    public FinancialOrganizationReportService.OrganizationReport report(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable Long organizationId,
            @RequestParam Instant from,
            @RequestParam Instant until) {
        return service.report(principal.userId(), organizationId, from, until);
    }
}
