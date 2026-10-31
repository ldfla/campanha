package la.meleva.fundraiser.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ComplianceManagementIntegrationTest {
    @Autowired private ComplianceDocumentService documentService;
    @Autowired private ComplianceManagementService managementService;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private AuditEventRepository auditEventRepository;

    @Test
    void storesPrivateDocumentWithHashAndAuditsAuthorizedDownload() {
        Fixture fixture = fixture("1001");
        byte[] content = "evidência confidencial".getBytes(StandardCharsets.UTF_8);
        var document = documentService.upload(fixture.user().getId(), fixture.campaign().getPublicId(), "CERTIFICATE",
                DocumentClassification.PRIVATE, new MockMultipartFile("file", "certificado.pdf", "application/pdf", content));

        assertThat(document.getSha256()).hasSize(64);
        assertThat(documentService.downloadPrivate(fixture.user().getId(), fixture.campaign().getPublicId(), document.getId()))
                .isEqualTo(content);
        assertThat(auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAt("compliance_document", document.getId()))
                .hasSize(1);
    }

    @Test
    void rejectsForeignOrganizationAndInvalidDocumentType() {
        Fixture owner = fixture("1002");
        Fixture outsider = fixture("1003");
        var file = new MockMultipartFile("file", "certificado.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> documentService.upload(owner.user().getId(), owner.campaign().getPublicId(), "CERTIFICATE",
                DocumentClassification.PRIVATE, file)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> documentService.upload(outsider.user().getId(), owner.campaign().getPublicId(), "CERTIFICATE",
                DocumentClassification.PRIVATE, new MockMultipartFile("file", "ok.pdf", "application/pdf", new byte[] {1})))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void versionsRegulationAndRejectsInvalidAuthorizationDates() {
        Fixture fixture = fixture("1004");
        String hash = "a".repeat(64);
        var first = managementService.createRegulation(fixture.user().getId(), fixture.campaign().getPublicId(), hash, null);
        var second = managementService.createRegulation(fixture.user().getId(), fixture.campaign().getPublicId(), hash, null);

        assertThat(first.getVersion()).isEqualTo(1);
        assertThat(second.getVersion()).isEqualTo(2);
        assertThatThrownBy(() -> managementService.createAuthorization(fixture.user().getId(), fixture.campaign().getPublicId(),
                "Emissor", "CERT-1", Instant.parse("2026-01-02T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void publishableDocumentCannotBeDownloadedByPrivateEndpoint() {
        Fixture fixture = fixture("1005");
        var document = documentService.upload(fixture.user().getId(), fixture.campaign().getPublicId(), "REGULATION",
                DocumentClassification.PUBLISHABLE,
                new MockMultipartFile("file", "regulamento.pdf", "application/pdf", new byte[] {1}));

        assertThatThrownBy(() -> documentService.downloadPrivate(fixture.user().getId(), fixture.campaign().getPublicId(), document.getId()))
                .isInstanceOf(BusinessException.class);
    }

    private Fixture fixture(String suffix) {
        Organization organization = organizationRepository.save(new Organization("Organização " + suffix, "ASSOCIATION"));
        User user = userRepository.save(new User("Gestor", "Teste", "+551199999" + suffix));
        membershipRepository.save(new Membership(organization, user, UserRole.CAMPAIGN_MANAGER));
        Campaign campaign = campaignRepository.save(new Campaign(organization.getId(), "Campanha " + suffix, "campanha-" + suffix, CampaignMode.DEMO));
        return new Fixture(user, campaign);
    }

    private record Fixture(User user, Campaign campaign) {}
}
