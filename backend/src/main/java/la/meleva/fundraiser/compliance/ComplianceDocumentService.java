package la.meleva.fundraiser.compliance;

import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Set;

@Service
public class ComplianceDocumentService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private final ComplianceDocumentRepository repository;
    private final CampaignRepository campaignRepository;
    private final MembershipRepository membershipRepository;
    private final AuditEventRepository auditEventRepository;
    private final PrivateDocumentStorage storage;
    ComplianceDocumentService(ComplianceDocumentRepository repository, CampaignRepository campaignRepository,
                              MembershipRepository membershipRepository, AuditEventRepository auditEventRepository,
                              PrivateDocumentStorage storage) {
        this.repository = repository;
        this.campaignRepository = campaignRepository;
        this.membershipRepository = membershipRepository;
        this.auditEventRepository = auditEventRepository;
        this.storage = storage;
    }

    @Transactional
    public ComplianceDocument upload(Long actorId, String campaignPublicId, String kind,
                                     DocumentClassification classification, MultipartFile file) {
        var campaign=campaignRepository.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        requireCampaignMembership(actorId, campaign.getOrganizationId());
        if (file.isEmpty() || file.getSize()>MAX_SIZE || !ALLOWED_TYPES.contains(file.getContentType())) throw ErrorCode.VALIDATION_ERROR.toException();
        try { byte[] content=file.getBytes(); String key=storage.store(file.getOriginalFilename(),file.getContentType(),content); return repository.save(new ComplianceDocument(campaign.getOrganizationId(),campaign.getId(),kind,key,sha256(content),classification)); }
        catch(IOException exception){throw ErrorCode.INTERNAL_ERROR.toException();}
    }

    @Transactional
    public byte[] downloadPrivate(Long actorId, String campaignPublicId, Long id) {
        var document = repository.findById(id).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        document.requirePrivate();
        var campaign = campaignRepository.findByPublicId(campaignPublicId)
                .orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if (!campaign.getId().equals(document.getCampaignId())) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
        requireCampaignMembership(actorId, campaign.getOrganizationId());
        var content = storage.load(document.getObjectKey());
        auditEventRepository.save(new AuditEvent(actorId, "COMPLIANCE_DOCUMENT_DOWNLOADED", "compliance_document",
                document.getId(), sha256((actorId + ":" + document.getId()).getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        return content;
    }

    private void requireCampaignMembership(Long actorId, Long organizationId) {
        if (actorId == null || membershipRepository.findActiveByUserAndOrganization(actorId, organizationId).isEmpty()) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
    }
    private String sha256(byte[] data){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
