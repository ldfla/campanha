package la.meleva.fundraiser.drawing;

import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.campaign.CampaignService;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import la.meleva.fundraiser.identity.AppUserDetails;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="app.surprise.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SurpriseCommitmentIntegrationTest {
    @Autowired SurpriseCommitmentService service;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired MembershipRepository memberships;
    @Autowired MockMvc mvc;
    @Autowired CampaignService campaignService;
    @Autowired SurpriseRevealApprovalRepository approvals;
    @Autowired PlatformTransactionManager transactionManager;

    @Test void encryptsSecretAndBlocksPrematureRevealAndReplacement() {
        var org=organizations.save(new Organization("Organização surpresa","ASSOCIATION"));
        var actor=actor(org,"+5511999777001");
        var campaign=campaigns.save(new Campaign(org.getId(),"Surpresa","surpresa-criptografica",CampaignMode.DEMO));
        var commitment=service.commit(actor.getId(),campaign.getPublicId(),1);
        assertThat(commitment.hash()).matches("[0-9a-f]{64}");
        assertThat(commitment.simulation()).isTrue();
        assertThat(commitment.context().campaignId()).isEqualTo(campaign.getPublicId());
        assertThat(commitment.context().regulationVersion()).isZero();
        assertThat(service.published(campaign.getPublicId())).isEqualTo(commitment);
        assertThat(jdbc.queryForObject("SELECT encrypted_secret FROM surprise_commitments WHERE campaign_id=?",String.class,campaign.getId())).isNotBlank();
        assertThatThrownBy(()->service.commit(actor.getId(),campaign.getPublicId(),1)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.reveal(actor.getId(),campaign.getPublicId())).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM surprise_commitments WHERE campaign_id=?",Long.class,campaign.getId())).isEqualTo(1);
    }

    @Test void blocksRevelationAfterDrawnWithoutApprovedTrigger() {
        var org=organizations.save(new Organization("Organização auditoria surpresa","ASSOCIATION"));
        var actor=actor(org,"+5511999777002");
        var campaign=campaigns.save(new Campaign(org.getId(),"Surpresa","surpresa-auditoria",CampaignMode.DEMO));
        service.commit(actor.getId(),campaign.getPublicId(),1);
        campaign=campaigns.findById(campaign.getId()).orElseThrow();
        for (var status : new CampaignStatus[]{CampaignStatus.LEGAL_REVIEW,CampaignStatus.AUTHORIZED,
                CampaignStatus.SCHEDULED,CampaignStatus.OPEN,CampaignStatus.LOCKED,CampaignStatus.DRAW_PENDING,CampaignStatus.DRAWN}) {
            campaign.transitionTo(status);
        }
        campaigns.saveAndFlush(campaign);
        String campaignId=campaign.getPublicId();
        assertThatThrownBy(()->service.reveal(actor.getId(),campaignId))
                .isInstanceOfSatisfying(BusinessException.class,ex -> assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.CAMPAIGN_GATES_INCOMPLETE));
    }

    @Test void blocksCrossOrganizationCommit() {
        var owner=organizations.save(new Organization("Organização titular","ASSOCIATION"));
        var outsider=organizations.save(new Organization("Organização externa","ASSOCIATION"));
        var actor=actor(outsider,"+5511999777003");
        var campaign=campaigns.save(new Campaign(owner.getId(),"Surpresa","surpresa-outra-organizacao",CampaignMode.DEMO));
        assertThatThrownBy(()->service.commit(actor.getId(),campaign.getPublicId(),1)).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM surprise_commitments WHERE campaign_id=?",Long.class,campaign.getId())).isZero();
    }

    @Test void apiRequiresCsrfAndOnlyPublishesCommitmentHash() throws Exception {
        var org=organizations.save(new Organization("Organização API surpresa","ASSOCIATION"));
        var actor=actor(org,"+5511999777004");
        var campaign=campaigns.save(new Campaign(org.getId(),"Surpresa","surpresa-api",CampaignMode.DEMO));
        var principal=new AppUserDetails(actor.getId(),actor.getPublicId(),actor.getPhoneNumber(),List.of(UserRole.SYSTEM_ADMIN));
        var auth=new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities());
        String endpoint="/surprises/campaigns/"+campaign.getPublicId()+"/commitment";
        mvc.perform(post(endpoint).with(authentication(auth)).contentType(MediaType.APPLICATION_JSON).content("{\"ruleVersion\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(endpoint).with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"ruleVersion\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.hash").isString())
                .andExpect(jsonPath("$.numbers").doesNotExist()).andExpect(jsonPath("$.saltHex").doesNotExist());
        mvc.perform(get(endpoint)).andExpect(status().isOk()).andExpect(jsonPath("$.hash").isString())
                .andExpect(jsonPath("$.numbers").doesNotExist()).andExpect(jsonPath("$.saltHex").doesNotExist());
    }

    @Test void enabledSurpriseCannotLeaveDraftBeforeCommitmentExists() {
        var org=organizations.save(new Organization("Organização gate surpresa","ASSOCIATION"));
        var actor=actor(org,"+5511999777005");
        var campaign=campaigns.save(new Campaign(org.getId(),"Surpresa","surpresa-gate-abertura",CampaignMode.DEMO));
        service.enable(actor.getId(),campaign.getPublicId());
        assertThatThrownBy(()->campaignService.transition(actor.getId(),campaign.getPublicId(),CampaignStatus.LEGAL_REVIEW))
                .isInstanceOfSatisfying(BusinessException.class,ex->assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.CAMPAIGN_GATES_INCOMPLETE));
        assertThat(campaigns.findById(campaign.getId()).orElseThrow().getStatus()).isEqualTo(CampaignStatus.DRAFT);
        var published=service.commit(actor.getId(),campaign.getPublicId(),1);
        assertThat(service.published(campaign.getPublicId()).hash()).isEqualTo(published.hash());
        campaignService.transition(actor.getId(),campaign.getPublicId(),CampaignStatus.LEGAL_REVIEW);
        campaignService.transition(actor.getId(),campaign.getPublicId(),CampaignStatus.AUTHORIZED);
        campaignService.transition(actor.getId(),campaign.getPublicId(),CampaignStatus.SCHEDULED);
        campaignService.transition(actor.getId(),campaign.getPublicId(),CampaignStatus.OPEN);
        assertThat(campaigns.findById(campaign.getId()).orElseThrow().getStatus()).isEqualTo(CampaignStatus.OPEN);
    }

    @Test void databaseRequiresDistinctReviewersAndDistinctRoles() {
        var org=organizations.save(new Organization("Organização dupla revisão","ASSOCIATION"));
        var creator=actor(org,"+5511999777006");
        var other=actor(org,"+5511999777007");
        var campaign=campaigns.save(new Campaign(org.getId(),"Surpresa","surpresa-dupla-revisao",CampaignMode.DEMO));
        service.commit(creator.getId(),campaign.getPublicId(),1);
        Long commitmentId=jdbc.queryForObject("SELECT id FROM surprise_commitments WHERE campaign_id=?",Long.class,campaign.getId());
        approvals.saveAndFlush(new SurpriseRevealApproval(commitmentId,creator.getId(),SurpriseRevealApproval.ApprovalRole.DRAW_AUDITOR));
        assertThatThrownBy(()->approvals.saveAndFlush(new SurpriseRevealApproval(commitmentId,creator.getId(),SurpriseRevealApproval.ApprovalRole.COMPLIANCE_REVIEWER)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->approvals.saveAndFlush(new SurpriseRevealApproval(commitmentId,other.getId(),SurpriseRevealApproval.ApprovalRole.DRAW_AUDITOR)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void demoRevelationRequiresTwoDistinctActiveReviewersAndVerifiesProof() throws Exception {
        var org=organizations.save(new Organization("Organização revelação demo","ASSOCIATION"));
        var creator=actor(org,"+5511999777008");
        var auditor=users.save(new User("Auditor","Teste","+5511999777009"));
        var reviewer=users.save(new User("Revisor","Teste","+5511999777010"));
        memberships.save(new Membership(org,creator,UserRole.DRAW_AUDITOR));
        memberships.save(new Membership(org,auditor,UserRole.DRAW_AUDITOR));
        memberships.save(new Membership(org,auditor,UserRole.COMPLIANCE_REVIEWER));
        memberships.save(new Membership(org,reviewer,UserRole.COMPLIANCE_REVIEWER));
        var campaign=campaigns.save(new Campaign(org.getId(),"Surpresa","surpresa-revisoes-demo",CampaignMode.DEMO));
        var published=service.commit(creator.getId(),campaign.getPublicId(),1);
        assertThat(published.releasePolicy()).isEqualTo(SurpriseProof.DEMO_RELEASE_POLICY);
        assertThatThrownBy(()->service.approveReveal(auditor.getId(),campaign.getPublicId(),SurpriseRevealApproval.ApprovalRole.DRAW_AUDITOR)).isInstanceOf(BusinessException.class);
        for(var status:new CampaignStatus[]{CampaignStatus.LEGAL_REVIEW,CampaignStatus.AUTHORIZED,CampaignStatus.SCHEDULED,CampaignStatus.OPEN,CampaignStatus.LOCKED,CampaignStatus.DRAW_PENDING,CampaignStatus.DRAWN})
            campaignService.transition(creator.getId(),campaign.getPublicId(),status);
        assertThatThrownBy(()->service.approveReveal(auditor.getId(),campaign.getPublicId(),SurpriseRevealApproval.ApprovalRole.DRAW_AUDITOR))
                .isInstanceOfSatisfying(BusinessException.class,ex->assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.CAMPAIGN_GATES_INCOMPLETE));
        long resultId=insertSimulatedDraw(campaign.getId());
        assertThatThrownBy(()->service.approveReveal(auditor.getId(),campaign.getPublicId(),SurpriseRevealApproval.ApprovalRole.DRAW_AUDITOR))
                .isInstanceOfSatisfying(BusinessException.class,ex->assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.CAMPAIGN_GATES_INCOMPLETE));
        jdbc.update("UPDATE federal_results SET verified_by_two=TRUE WHERE id=?",resultId);
        assertThatThrownBy(()->service.approveReveal(creator.getId(),campaign.getPublicId(),SurpriseRevealApproval.ApprovalRole.DRAW_AUDITOR)).isInstanceOf(BusinessException.class);
        service.approveReveal(auditor.getId(),campaign.getPublicId(),SurpriseRevealApproval.ApprovalRole.DRAW_AUDITOR);
        service.approveReveal(auditor.getId(),campaign.getPublicId(),SurpriseRevealApproval.ApprovalRole.DRAW_AUDITOR);
        assertThatThrownBy(()->service.approveReveal(auditor.getId(),campaign.getPublicId(),SurpriseRevealApproval.ApprovalRole.COMPLIANCE_REVIEWER)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.reveal(creator.getId(),campaign.getPublicId())).isInstanceOf(BusinessException.class);
        service.approveReveal(reviewer.getId(),campaign.getPublicId(),SurpriseRevealApproval.ApprovalRole.COMPLIANCE_REVIEWER);
        reviewer.suspend(); users.saveAndFlush(reviewer);
        assertThatThrownBy(()->service.reveal(creator.getId(),campaign.getPublicId()))
                .isInstanceOfSatisfying(BusinessException.class,ex->assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.AUTH_RESOURCE_ACCESS_DENIED));
        jdbc.update("UPDATE users SET status='ACTIVE' WHERE id=?",reviewer.getId());
        var secret=service.reveal(creator.getId(),campaign.getPublicId());
        assertThat(SurpriseProof.verify(secret,published.context(),published.hash())).isTrue();
        assertThat(secret.numbers()).hasSize(10).doesNotHaveDuplicates();
        assertSuspensionWaitsForReveal(creator.getId(),reviewer.getId(),campaign.getPublicId());
        assertThatThrownBy(()->service.reveal(creator.getId(),campaign.getPublicId())).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE users SET status='ACTIVE' WHERE id=?",reviewer.getId());
        service.reveal(creator.getId(),campaign.getPublicId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE entity_type='surprise_commitment' AND action='SURPRISE_REVEALED' AND entity_id=(SELECT id FROM surprise_commitments WHERE campaign_id=?)",Long.class,campaign.getId())).isEqualTo(1);
        jdbc.update("UPDATE surprise_commitments SET commitment_sha256=? WHERE campaign_id=?","0".repeat(64),campaign.getId());
        assertThatThrownBy(()->service.reveal(creator.getId(),campaign.getPublicId())).isInstanceOf(BusinessException.class);
    }

    private void assertSuspensionWaitsForReveal(Long actorId,Long reviewerId,String campaignId) throws Exception {
        var revealLocked=new java.util.concurrent.CountDownLatch(1);
        var releaseReveal=new java.util.concurrent.CountDownLatch(1);
        var suspensionStarted=new java.util.concurrent.CountDownLatch(1);
        try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var reveal=executor.submit(()->new TransactionTemplate(transactionManager).executeWithoutResult(status->{
                service.reveal(actorId,campaignId); revealLocked.countDown();
                try { if(!releaseReveal.await(10,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("test.reveal.timeout"); }
                catch(InterruptedException ex){Thread.currentThread().interrupt(); throw new IllegalStateException(ex);}
            }));
            assertThat(revealLocked.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var suspension=executor.submit(()->{suspensionStarted.countDown();jdbc.update("UPDATE users SET status='SUSPENDED' WHERE id=?",reviewerId);});
            try {
                assertThat(suspensionStarted.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(()->suspension.get(500,java.util.concurrent.TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
            } finally { releaseReveal.countDown(); }
            reveal.get(10,java.util.concurrent.TimeUnit.SECONDS);
            suspension.get(10,java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    private long insertSimulatedDraw(Long campaignId) {
        jdbc.update("INSERT INTO draw_rules (campaign_id,version,source_kind,formula_json) VALUES (?,1,'DEMO',JSON_OBJECT('simulation',TRUE))",campaignId);
        Long ruleId=jdbc.queryForObject("SELECT id FROM draw_rules WHERE campaign_id=? AND version=1",Long.class,campaignId);
        String contest="DEMO-"+java.util.UUID.randomUUID();
        jdbc.update("INSERT INTO federal_results (contest_identifier,result_payload,verified_by_two) VALUES (?,JSON_OBJECT('simulation',TRUE),FALSE)",contest);
        Long resultId=jdbc.queryForObject("SELECT id FROM federal_results WHERE contest_identifier=?",Long.class,contest);
        jdbc.update("INSERT INTO draws (public_id,campaign_id,draw_rule_id,federal_result_id,status) VALUES (?,?,?,?, 'PUBLISHED')",java.util.UUID.randomUUID().toString(),campaignId,ruleId,resultId);
        return resultId;
    }

    private User actor(Organization org,String phone){
        var user=users.save(new User("Pessoa","Auditoria",phone));
        memberships.save(new Membership(org,user,UserRole.SYSTEM_ADMIN));
        return user;
    }
}
