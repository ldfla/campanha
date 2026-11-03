package la.meleva.fundraiser.compliance;

import jakarta.validation.Valid;
import la.meleva.fundraiser.compliance.dto.ComplianceDocumentResponse;
import la.meleva.fundraiser.compliance.dto.CreateAuthorizationRequest;
import la.meleva.fundraiser.compliance.dto.CreateRegulationRequest;
import la.meleva.fundraiser.identity.AppUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/_cx/campaigns/{campaignId}")
public class ComplianceManagementController {
    private final ComplianceDocumentService documentService;
    private final ComplianceManagementService managementService;

    ComplianceManagementController(ComplianceDocumentService documentService,
                                   ComplianceManagementService managementService) {
        this.documentService = documentService;
        this.managementService = managementService;
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN', 'LEGAL_REVIEWER')")
    public ResponseEntity<ComplianceDocumentResponse> upload(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String campaignId, @RequestParam String kind,
            @RequestParam DocumentClassification classification, @RequestParam MultipartFile file) {
        var document = documentService.upload(principal.userId(), campaignId, kind, classification, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ComplianceDocumentResponse.from(document));
    }

    @PostMapping("/regulations")
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN', 'LEGAL_REVIEWER')")
    public ResponseEntity<Long> createRegulation(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String campaignId, @Valid @org.springframework.web.bind.annotation.RequestBody CreateRegulationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managementService.createRegulation(principal.userId(), campaignId, request.documentSha256(), request.documentUri(), request.surpriseRequired()).getId());
    }

    @PostMapping("/authorizations")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LEGAL_REVIEWER')")
    public ResponseEntity<Long> createAuthorization(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String campaignId, @Valid @org.springframework.web.bind.annotation.RequestBody CreateAuthorizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managementService.createAuthorization(principal.userId(), campaignId,
                request.issuer(), request.certificateNo(), request.validFrom(), request.validTo(), request.documentUri()).getId());
    }

    @PostMapping("/authorizations/{authorizationId}/verify")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'COMPLIANCE_REVIEWER')")
    public ResponseEntity<Long> verifyAuthorization(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String campaignId, @PathVariable Long authorizationId) {
        return ResponseEntity.ok(managementService.verifyAuthorization(principal.userId(), campaignId, authorizationId).getId());
    }

    @GetMapping("/documents/{documentId}/private-content")
    @PreAuthorize("hasAnyRole('CAMPAIGN_MANAGER', 'SYSTEM_ADMIN', 'LEGAL_REVIEWER')")
    public ResponseEntity<byte[]> downloadPrivate(@AuthenticationPrincipal AppUserDetails principal,
            @PathVariable String campaignId, @PathVariable Long documentId) {
        byte[] content = documentService.downloadPrivate(principal.userId(), campaignId, documentId);
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(content);
    }
}
