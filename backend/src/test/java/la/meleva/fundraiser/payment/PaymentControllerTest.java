package la.meleva.fundraiser.payment;

import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)
class PaymentControllerTest {
    @Autowired MockMvc mvc; @Autowired OrganizationRepository organizations; @Autowired UserRepository users; @Autowired CampaignRepository campaigns;
    @Test void createsSandboxOrderWithCsrfAndBlocksCommercialCharge() throws Exception {
        var org=organizations.save(new Organization("Org controller payment","ASSOCIATION")); var user=users.save(new User("Api","Test","+5511989000111"));
        var auth=new UsernamePasswordAuthenticationToken(new AppUserDetails(user.getId(),user.getPublicId(),user.getPhoneNumber(),java.util.List.of(UserRole.PARTICIPANT)),null,java.util.List.of(()->"ROLE_PARTICIPANT"));
        var demo=campaigns.save(new Campaign(org.getId(),"Demo","payment-controller-demo",CampaignMode.DEMO));
        mvc.perform(post("/payments/orders").with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"campaignId\":"+demo.getId()+",\"amountCents\":5600,\"idempotencyKey\":\"controller-idemp-001\"}" )).andExpect(status().isCreated());
        var commercial=campaigns.save(new Campaign(org.getId(),"Commercial","payment-controller-commercial",CampaignMode.COMMERCIAL_PROMOTION));
        mvc.perform(post("/payments/orders").with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"campaignId\":"+commercial.getId()+",\"amountCents\":5600,\"idempotencyKey\":\"controller-idemp-002\"}" )).andExpect(status().isForbidden());
    }
}
