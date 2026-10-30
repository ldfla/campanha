package la.meleva.fundraiser.compliance.dto;

import la.meleva.fundraiser.compliance.ComplianceDocument;

public record ComplianceDocumentResponse(Long id, String kind, String sha256, String classification) {
    public static ComplianceDocumentResponse from(ComplianceDocument document) {
        return new ComplianceDocumentResponse(document.getId(), document.getKind(), document.getSha256(),
                document.getClassification().name());
    }
}
