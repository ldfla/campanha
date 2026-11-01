package la.meleva.fundraiser.shared;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignPrize;
import la.meleva.fundraiser.campaign.CampaignPrizeRepository;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignService;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.compliance.ApprovalRequest;
import la.meleva.fundraiser.compliance.ApprovalRequestRepository;
import la.meleva.fundraiser.compliance.CampaignAuthorization;
import la.meleva.fundraiser.compliance.CampaignAuthorizationRepository;
import la.meleva.fundraiser.compliance.GateType;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.participation.ParticipationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados exclusivamente sintéticos para desenvolvimento local (profile `local`). Nunca ativos fora
 * do profile local. Cria a campanha DEMO e uma campanha de **capitalização premiável** já com
 * autorização verificada e gates aprovados, para demonstrar o fluxo do ADR-0007 localmente.
 */
@Component
@Profile("local")
class LocalDemoSeed implements ApplicationRunner {

    private static final String DEMO_PHONE = "+5511999990000";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignPrizeRepository campaignPrizeRepository;
    private final CampaignAuthorizationRepository authorizationRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final CampaignService campaignService;
    private final ParticipationService participationService;

    LocalDemoSeed(OrganizationRepository organizationRepository, UserRepository userRepository,
                  MembershipRepository membershipRepository, CampaignRepository campaignRepository,
                  CampaignPrizeRepository campaignPrizeRepository,
                  CampaignAuthorizationRepository authorizationRepository,
                  ApprovalRequestRepository approvalRequestRepository, CampaignService campaignService,
                  ParticipationService participationService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.campaignRepository = campaignRepository;
        this.campaignPrizeRepository = campaignPrizeRepository;
        this.authorizationRepository = authorizationRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.campaignService = campaignService;
        this.participationService = participationService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var organization = organizationRepository.findByLegalName("Organização Demonstração Local")
                .orElseGet(() -> organizationRepository.save(new Organization("Organização Demonstração Local", "ASSOCIATION")));
        var admin = userRepository.findByPhoneNumber(DEMO_PHONE)
                .orElseGet(() -> userRepository.save(new User("Admin", "Demonstração", DEMO_PHONE)));
        if (membershipRepository.findActiveByUserAndOrganization(admin.getId(), organization.getId()).isEmpty()) {
            membershipRepository.save(new Membership(organization, admin, UserRole.SYSTEM_ADMIN));
        }

        seedDemoCampaign(organization);
        seedCapitalizationCampaign(organization, admin.getId());
    }

    private void seedDemoCampaign(Organization organization) {
        var campaign = campaignRepository.findBySlug("acao-demo-local")
                .orElseGet(() -> campaignRepository.save(new Campaign(organization.getId(), "Ação demonstração local", "acao-demo-local", CampaignMode.DEMO)));
        if (campaignPrizeRepository.findByCampaignIdOrderBySortOrder(campaign.getId()).isEmpty()) {
            campaignPrizeRepository.save(new CampaignPrize(campaign.getId(), CampaignPrize.PrizeType.GOODS,
                    "Item sintético para desenvolvimento local", 0L, 1, 0));
        }
    }

    private void seedCapitalizationCampaign(Organization organization, Long adminId) {
        var existing = campaignRepository.findBySlug("acao-capitalizacao-local");
        if (existing.isPresent()) return;

        var campaign = campaignRepository.save(new Campaign(organization.getId(),
                "Título de capitalização (local)", "acao-capitalizacao-local", CampaignMode.CAPITALIZATION_PRIZE_TITLE));
        campaign.setNumberRange(0, 999);
        campaignRepository.save(campaign);
        participationService.initializeSeries(campaign.getPublicId());

        campaignPrizeRepository.save(new CampaignPrize(campaign.getId(), CampaignPrize.PrizeType.CASH,
                "Bônus em dinheiro (sintético)", 1_000L, 1, 1));
        campaignPrizeRepository.save(new CampaignPrize(campaign.getId(), CampaignPrize.PrizeType.GOODS,
                "Bem sintético (local)", 0L, 1, 2));

        // Autorização SUSEP (sintética, apenas local) verificada e vigente.
        var authorization = authorizationRepository.save(new CampaignAuthorization(campaign.getId(), "ViaCap",
                "15414.657358/2026-61", Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(365, ChronoUnit.DAYS), "docs://local/susep-sintetico"));
        authorization.verify(Instant.now());
        authorizationRepository.save(authorization);

        // Aprova todos os gates e abre a campanha.
        for (GateType gate : GateType.values()) {
            var request = approvalRequestRepository.save(new ApprovalRequest(campaign.getId(), gate, adminId));
            request.approve();
            approvalRequestRepository.save(request);
        }
        campaignService.transition(adminId, campaign.getPublicId(), CampaignStatus.LEGAL_REVIEW);
        campaignService.transition(adminId, campaign.getPublicId(), CampaignStatus.AUTHORIZED);
        campaignService.transition(adminId, campaign.getPublicId(), CampaignStatus.SCHEDULED);
        campaignService.transition(adminId, campaign.getPublicId(), CampaignStatus.OPEN);
    }
}
