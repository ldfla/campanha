package la.meleva.fundraiser.award;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.*;
import la.meleva.fundraiser.identity.*;
import la.meleva.fundraiser.participation.*;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Import(TestcontainersConfiguration.class)
class AwardDeliveryServiceIntegrationTest {
    private static final AtomicInteger PHONE=new AtomicInteger(33100000);
    @Autowired AwardDeliveryService service; @Autowired AwardDeliveryRepository deliveries;
    @Autowired OrganizationRepository organizations; @Autowired UserRepository users; @Autowired MembershipRepository memberships;
    @Autowired CampaignRepository campaigns; @Autowired CampaignPrizeRepository prizes;
    @Autowired ParticipationRequestRepository requests; @Autowired ParticipationEntryRepository entries;
    @Autowired JdbcTemplate jdbc;

    @Test void validatesContactAndSchedulesOnlyEligibleSimulation() {
        var f=fixture("award-flow",true);
        var delivery=service.createSimulation(f.manager().getId(),f.campaign().getPublicId(),f.outcomeId());
        assertThat(service.createSimulation(f.manager().getId(),f.campaign().getPublicId(),f.outcomeId()).getId()).isEqualTo(delivery.getId());
        assertThatThrownBy(()->service.schedule(f.manager().getId(),f.campaign().getPublicId(),delivery.getId())).isInstanceOf(BusinessException.class);
        assertThat(service.validateBeneficiary(f.manager().getId(),f.campaign().getPublicId(),delivery.getId()).getStatus()).isEqualTo(AwardDeliveryStatus.VALIDATED);
        assertThat(service.schedule(f.manager().getId(),f.campaign().getPublicId(),delivery.getId()).getStatus()).isEqualTo(AwardDeliveryStatus.SCHEDULED);
        assertThatThrownBy(()->service.schedule(f.manager().getId(),f.campaign().getPublicId(),delivery.getId())).isInstanceOf(BusinessException.class);
    }

    @Test void blocksUnverifiedContactAndCrossOrganizationAccess() {
        var owner=fixture("award-owner",false); var outsider=fixture("award-outsider",true);
        var delivery=service.createSimulation(owner.manager().getId(),owner.campaign().getPublicId(),owner.outcomeId());
        assertThatThrownBy(()->service.validateBeneficiary(owner.manager().getId(),owner.campaign().getPublicId(),delivery.getId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.createSimulation(outsider.manager().getId(),owner.campaign().getPublicId(),owner.outcomeId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.validateBeneficiary(outsider.manager().getId(),outsider.campaign().getPublicId(),delivery.getId())).isInstanceOf(BusinessException.class);
    }

    @Test void refusesSchedulingWhenContactOrAccountIsRevoked() {
        var f=fixture("award-contact-revocation",true);
        var delivery=service.createSimulation(f.manager().getId(),f.campaign().getPublicId(),f.outcomeId());
        service.validateBeneficiary(f.manager().getId(),f.campaign().getPublicId(),delivery.getId());
        jdbc.update("UPDATE users SET status='SUSPENDED' WHERE id=?",f.beneficiary().getId());
        assertThatThrownBy(()->service.schedule(f.manager().getId(),f.campaign().getPublicId(),delivery.getId())).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE users SET status='ACTIVE',phone_number='   ',email_normalized='   ',email_verified_at=UTC_TIMESTAMP() WHERE id=?",f.beneficiary().getId());
        assertThatThrownBy(()->service.schedule(f.manager().getId(),f.campaign().getPublicId(),delivery.getId())).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE users SET phone_number=?,email_normalized=? WHERE id=?","\t\n\u00a0","\u200b",f.beneficiary().getId());
        assertThatThrownBy(()->service.schedule(f.manager().getId(),f.campaign().getPublicId(),delivery.getId())).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE users SET phone_number=NULL,email_normalized=? WHERE id=?","\ufe0f\u034f",f.beneficiary().getId());
        assertThatThrownBy(()->service.schedule(f.manager().getId(),f.campaign().getPublicId(),delivery.getId())).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE users SET email_normalized=? WHERE id=?","\u3164",f.beneficiary().getId());
        assertThatThrownBy(()->service.schedule(f.manager().getId(),f.campaign().getPublicId(),delivery.getId())).isInstanceOf(BusinessException.class);
        assertThat(deliveries.findById(delivery.getId()).orElseThrow().getStatus()).isEqualTo(AwardDeliveryStatus.VALIDATED);
        jdbc.update("UPDATE users SET email_normalized='person@example.test',email_verified_at=UTC_TIMESTAMP() WHERE id=?",f.beneficiary().getId());
        assertThat(service.schedule(f.manager().getId(),f.campaign().getPublicId(),delivery.getId()).getStatus()).isEqualTo(AwardDeliveryStatus.SCHEDULED);
    }

    @Test void refusesOutcomeWithoutMatchingFrozenAssignmentOrDemoDecision() {
        var f=fixture("award-invalid-outcome",true);
        jdbc.update("UPDATE draw_outcomes SET winning_number=1 WHERE id=?",f.outcomeId());
        assertThatThrownBy(()->service.createSimulation(f.manager().getId(),f.campaign().getPublicId(),f.outcomeId())).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE draw_outcomes SET winning_number=0,decision_code='NO_WINNER' WHERE id=?",f.outcomeId());
        assertThatThrownBy(()->service.createSimulation(f.manager().getId(),f.campaign().getPublicId(),f.outcomeId())).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM award_deliveries WHERE outcome_id=?",Long.class,f.outcomeId())).isZero();
    }

    @Test void serializesConcurrentCreationForTheSameOutcome() throws Exception {
        var f=fixture("award-race",true);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var futures=java.util.stream.IntStream.range(0,24).mapToObj(i->executor.submit(()->service.createSimulation(f.manager().getId(),f.campaign().getPublicId(),f.outcomeId()).getId())).toList();
            var ids=new java.util.HashSet<Long>(); for(var future:futures) ids.add(future.get(30,TimeUnit.SECONDS));
            assertThat(ids).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM award_deliveries WHERE outcome_id=?",Long.class,f.outcomeId())).isEqualTo(1);
        }
    }

    private Fixture fixture(String slug,boolean verified){
        var org=organizations.save(new Organization("Organização "+slug,"ASSOCIATION"));
        var manager=users.save(new User("Gestor","Teste",phone()));
        memberships.save(new Membership(org,manager,UserRole.CAMPAIGN_MANAGER));
        var beneficiary=new User("Pessoa","Teste",phone()); if(verified)beneficiary.markPhoneVerified(); beneficiary=users.save(beneficiary);
        var campaign=campaigns.save(new Campaign(org.getId(),slug,slug,CampaignMode.DEMO));
        var request=requests.save(new ParticipationRequest(campaign.getId(),beneficiary.getId(),UUID.randomUUID().toString(),1));
        var entry=new ParticipationEntry(campaign.getId(),beneficiary.getId(),request.getId(),"DEMO_SIMULATION"); entry.markAssigned(); entry.freeze(); entries.save(entry);
        var prize=prizes.save(new CampaignPrize(campaign.getId(),CampaignPrize.PrizeType.GOODS,"Prêmio simulado",100,1,1));
        jdbc.update("INSERT INTO campaign_numbers (campaign_id,series_code,number_value,status,entry_id) VALUES (?,'A',0,'FROZEN',?)",campaign.getId(),entry.getId());
        Long numberId=jdbc.queryForObject("SELECT id FROM campaign_numbers WHERE campaign_id=? AND number_value=0",Long.class,campaign.getId());
        jdbc.update("INSERT INTO entry_number_assignments (entry_id,campaign_number_id) VALUES (?,?)",entry.getId(),numberId);
        jdbc.update("INSERT INTO draw_rules (campaign_id,version,source_kind,formula_json) VALUES (?,1,'DEMO',JSON_OBJECT('simulation',TRUE))",campaign.getId());
        Long ruleId=jdbc.queryForObject("SELECT id FROM draw_rules WHERE campaign_id=?",Long.class,campaign.getId());
        String contest="DEMO-"+UUID.randomUUID(); jdbc.update("INSERT INTO federal_results (contest_identifier,result_payload,verified_by_two) VALUES (?,JSON_OBJECT('simulation',TRUE),TRUE)",contest);
        Long resultId=jdbc.queryForObject("SELECT id FROM federal_results WHERE contest_identifier=?",Long.class,contest);
        String drawId=UUID.randomUUID().toString(); jdbc.update("INSERT INTO draws (public_id,campaign_id,draw_rule_id,federal_result_id,status) VALUES (?,?,?,?, 'PUBLISHED')",drawId,campaign.getId(),ruleId,resultId);
        Long drawPk=jdbc.queryForObject("SELECT id FROM draws WHERE public_id=?",Long.class,drawId);
        jdbc.update("INSERT INTO draw_outcomes (draw_id,prize_id,winning_number,entry_id,decision_code) VALUES (?,?,0,?,'DEMO_MATCH')",drawPk,prize.getId(),entry.getId());
        Long outcome=jdbc.queryForObject("SELECT id FROM draw_outcomes WHERE draw_id=?",Long.class,drawPk);
        return new Fixture(campaign,manager,beneficiary,outcome);
    }
    private String phone(){return "+55118"+PHONE.getAndIncrement();}
    private record Fixture(Campaign campaign,User manager,User beneficiary,Long outcomeId) {}
}
