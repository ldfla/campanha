package la.meleva.fundraiser.compliance;

import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ApprovalServiceTest {

    @Autowired private ApprovalService approvalService;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private jakarta.persistence.EntityManager em;

    private Long campaignId;
    private Long requesterId;
    private Long reviewerId;

    @BeforeEach
    void setUp() {
        var org = new Organization("Test Org", "ASSOCIATION");
        em.persist(org);

        var campaign = new Campaign(org.getId(), "Approval Test", "approval-test-" + System.nanoTime(), CampaignMode.PHILANTHROPIC_AUTHORIZED);
        campaignRepository.save(campaign);
        campaignId = campaign.getId();

        var requester = new User("Requester", "Test", "+5511777000001");
        em.persist(requester);
        requesterId = requester.getId();

        var reviewer = new User("Reviewer", "Test", "+5511777000002");
        em.persist(reviewer);
        reviewerId = reviewer.getId();

        em.flush();
    }

    @Test
    void canRequestApproval() {
        var request = approvalService.requestApproval(campaignId, GateType.G_LEGAL, requesterId);
        assertNotNull(request.getId());
        assertEquals(ApprovalStatus.PENDING, request.getStatus());
    }

    @Test
    void duplicateRequestReturnsSame() {
        var first = approvalService.requestApproval(campaignId, GateType.G_LEGAL, requesterId);
        var second = approvalService.requestApproval(campaignId, GateType.G_LEGAL, requesterId);
        assertEquals(first.getId(), second.getId());
    }

    @Test
    void selfApprovalBlocked() {
        var request = approvalService.requestApproval(campaignId, GateType.G_LEGAL, requesterId);
        var ex = assertThrows(ErrorCode.BusinessException.class, () ->
                approvalService.decide(request.getId(), requesterId, "APPROVED", "Auto"));
        assertEquals(ErrorCode.APPROVAL_SELF_APPROVAL, ex.errorCode());
    }

    @Test
    void reviewerCanApprove() {
        var request = approvalService.requestApproval(campaignId, GateType.G_LEGAL, requesterId);
        var decision = approvalService.decide(request.getId(), reviewerId, "APPROVED", "Tudo certo");
        assertEquals("APPROVED", decision.getDecision());
    }

    @Test
    void cannotDecideTwice() {
        var request = approvalService.requestApproval(campaignId, GateType.G_LEGAL, requesterId);
        approvalService.decide(request.getId(), reviewerId, "APPROVED", "OK");
        assertThrows(ErrorCode.BusinessException.class, () ->
                approvalService.decide(request.getId(), reviewerId, "REJECTED", "Mudei de ideia"));
    }

    @Test
    void allGatesNotApprovedWhenPending() {
        approvalService.requestApproval(campaignId, GateType.G_LEGAL, requesterId);
        assertFalse(approvalService.areAllGatesApproved(campaignId, CampaignMode.PHILANTHROPIC_AUTHORIZED));
    }

    @Test
    void demoModeAlwaysApproved() {
        assertTrue(approvalService.areAllGatesApproved(campaignId, CampaignMode.DEMO));
    }
}
