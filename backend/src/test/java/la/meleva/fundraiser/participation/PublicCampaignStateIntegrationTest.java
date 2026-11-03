package la.meleva.fundraiser.participation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.participation.purchase.PurchaseOffer;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** P06-08: o estado do CTA é decidido no servidor e lido sem sessão, sem dados pessoais. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PublicCampaignStateIntegrationTest {
    private static final String PATH = "/campaigns/public/{id}";

    @Autowired MockMvc mvc;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired ParticipationService participations;
    @Autowired PurchaseOfferRepository offers;

    @Test
    void openDemoCampaignEnablesPurchaseAnonymouslyWithoutPersonalData() throws Exception {
        Campaign campaign = campaign("public-open", CampaignMode.DEMO, 5, CampaignStatus.OPEN);

        mvc.perform(get(PATH, campaign.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.ctaState").value("OPEN"))
                .andExpect(jsonPath("$.purchaseEnabled").value(true))
                .andExpect(jsonPath("$.simulation").value(true))
                .andExpect(jsonPath("$.availableNumbers").value(5))
                .andExpect(jsonPath("$.buyers").doesNotExist())
                .andExpect(jsonPath("$.organizationId").doesNotExist());
    }

    @Test
    void soldOutDraftLockedAndSuspendedCampaignsDisablePurchase() throws Exception {
        Campaign soldOut = campaign("public-sold-out", CampaignMode.DEMO, 1, CampaignStatus.OPEN);
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(soldOut.getId(), "Entidade esgotada"));
        User buyer = users.save(new User("Ultimo", "Numero", "+551193300001"));
        participations.request(buyer.getId(), soldOut.getPublicId(), UUID.randomUUID().toString(), 1, beneficiary.getPublicId());

        expect(soldOut, "SOLD_OUT");
        expect(campaign("public-draft", CampaignMode.DEMO, 5, CampaignStatus.DRAFT), "NOT_STARTED");
        expect(campaign("public-locked", CampaignMode.DEMO, 5, CampaignStatus.LOCKED), "CLOSED");
        expect(campaign("public-suspended", CampaignMode.DEMO, 5, CampaignStatus.SUSPENDED), "SUSPENDED");
    }

    @Test
    void modesWithoutAnApprovedPaymentPathStayBlockedEvenWhenOpen() throws Exception {
        expect(campaign("public-commercial", CampaignMode.COMMERCIAL_PROMOTION, 5, CampaignStatus.OPEN), "BLOCKED");
        expect(campaign("public-capitalization", CampaignMode.CAPITALIZATION_PRIZE_TITLE, 5, CampaignStatus.OPEN), "BLOCKED");
        expect(campaign("public-philanthropic", CampaignMode.PHILANTHROPIC_AUTHORIZED, 5, CampaignStatus.OPEN), "BLOCKED");
    }

    @Test
    void openCampaignWithoutPublishedOfferIsNotOnSaleYet() throws Exception {
        Campaign campaign = campaign("public-no-offer", CampaignMode.DEMO, 5, CampaignStatus.OPEN);
        offers.findByCampaignIdAndActiveTrueOrderByIdAsc(campaign.getId(), org.springframework.data.domain.Pageable.unpaged())
                .forEach(offer -> offers.delete(offer));

        expect(campaign, "NOT_STARTED");
    }

    @Test
    void publishedOffersAndInstitutionsAreReadableBeforeSignUpButNotWritable() throws Exception {
        Campaign campaign = campaign("public-offers", CampaignMode.DEMO, 5, CampaignStatus.OPEN);
        beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade pública"));

        mvc.perform(get("/participations/campaigns/{id}/offers", campaign.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(1))
                .andExpect(jsonPath("$[0].totalCents").value(100));
        mvc.perform(get("/participations/campaigns/{id}/beneficiaries", campaign.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Entidade pública"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/participations/campaigns/{id}/offers", campaign.getPublicId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"quantity\":1,\"totalCents\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownCampaignIsNotFound() throws Exception {
        mvc.perform(get(PATH, "nao-existe")).andExpect(status().isNotFound());
    }

    private void expect(Campaign campaign, String ctaState) throws Exception {
        mvc.perform(get(PATH, campaign.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ctaState").value(ctaState))
                .andExpect(jsonPath("$.purchaseEnabled").value(false));
    }

    private Campaign campaign(String slug, CampaignMode mode, int capacity, CampaignStatus target) {
        Organization organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        Campaign campaign = new Campaign(organization.getId(), "Campanha " + slug, slug, mode);
        campaign.setNumberRange(0, capacity - 1);
        Campaign saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        offers.save(new PurchaseOffer(saved.getId(), 1, 100));
        List<CampaignStatus> path = switch (target) {
            case DRAFT -> List.of();
            case OPEN -> List.of(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED, CampaignStatus.SCHEDULED, CampaignStatus.OPEN);
            case LOCKED -> List.of(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED, CampaignStatus.SCHEDULED,
                    CampaignStatus.OPEN, CampaignStatus.LOCKED);
            case SUSPENDED -> List.of(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED, CampaignStatus.SCHEDULED,
                    CampaignStatus.OPEN, CampaignStatus.SUSPENDED);
            default -> throw new IllegalArgumentException(target.name());
        };
        path.forEach(saved::transitionTo);
        return campaigns.save(saved);
    }
}
