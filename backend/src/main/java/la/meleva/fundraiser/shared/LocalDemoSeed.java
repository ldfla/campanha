package la.meleva.fundraiser.shared;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
import la.meleva.fundraiser.participation.CampaignNumber;
import la.meleva.fundraiser.participation.CampaignNumberRepository;
import la.meleva.fundraiser.participation.InitialPrizeLoadService;
import la.meleva.fundraiser.participation.ParticipationService;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryService;
import la.meleva.fundraiser.participation.purchase.PurchaseOffer;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferRepository;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados exclusivamente sintéticos para desenvolvimento local (profile `local`). Nunca ativos fora
 * do profile local. Cria:
 * <ul>
 *   <li>a campanha <b>DEMO</b> (`acao-demo-local`, OPEN) com beneficiário e prêmio sintético;</li>
 *   <li>uma campanha de <b>capitalização premiável</b> (`acao-capitalizacao-local`, OPEN, ADR-0007)
 *       com autorização SUSEP sintética verificada, todos os gates aprovados, beneficiário e
 *       ofertas de compra (combos) já publicadas.</li>
 * </ul>
 * O seed é idempotente por slug.
 */
@Component
@Profile({"local", "staging"})
class LocalDemoSeed implements ApplicationRunner {

    private static final String DEMO_PHONE = "+5511999990000";
    private static final String ORG_NAME = "Organização Demonstração Local";
    private static final int LOCAL_MAX_NUMBER = 19_999;
    /** Quantidade → total em centavos: R$ 1,00 por número no combo de 5, com 5%, 13%, 26% e 31% de desconto nos demais. */
    private static final Map<Integer, Long> COMBOS = orderedCombos();

    private static Map<Integer, Long> orderedCombos() {
        var combos = new LinkedHashMap<Integer, Long>();
        combos.put(5, 500L);
        combos.put(10, 950L);
        combos.put(100, 8_700L);
        combos.put(1_000, 74_000L);
        combos.put(5_000, 345_000L);
        return combos;
    }

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignPrizeRepository campaignPrizeRepository;
    private final CampaignAuthorizationRepository authorizationRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final CampaignService campaignService;
    private final ParticipationService participationService;
    private final BeneficiaryService beneficiaryService;
    private final PurchaseOfferService purchaseOfferService;
    private final PurchaseOfferRepository purchaseOfferRepository;
    private final CampaignNumberRepository campaignNumberRepository;
    private final JdbcTemplate jdbc;
    private final InitialPrizeLoadService prizeLoadService;

    LocalDemoSeed(OrganizationRepository organizationRepository, UserRepository userRepository,
                  MembershipRepository membershipRepository, CampaignRepository campaignRepository,
                  CampaignPrizeRepository campaignPrizeRepository,
                  CampaignAuthorizationRepository authorizationRepository,
                  ApprovalRequestRepository approvalRequestRepository, CampaignService campaignService,
                  ParticipationService participationService, BeneficiaryService beneficiaryService,
                  PurchaseOfferService purchaseOfferService, PurchaseOfferRepository purchaseOfferRepository,
                  CampaignNumberRepository campaignNumberRepository, JdbcTemplate jdbc,
                  InitialPrizeLoadService prizeLoadService) {
        this.purchaseOfferRepository = purchaseOfferRepository;
        this.campaignNumberRepository = campaignNumberRepository;
        this.jdbc = jdbc;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.campaignRepository = campaignRepository;
        this.campaignPrizeRepository = campaignPrizeRepository;
        this.authorizationRepository = authorizationRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.campaignService = campaignService;
        this.participationService = participationService;
        this.beneficiaryService = beneficiaryService;
        this.purchaseOfferService = purchaseOfferService;
        this.prizeLoadService = prizeLoadService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var organization = organizationRepository.findByLegalName(ORG_NAME)
                .orElseGet(() -> organizationRepository.save(new Organization(ORG_NAME, "ASSOCIATION")));
        var admin = userRepository.findByPhoneNumber(DEMO_PHONE)
                .orElseGet(() -> userRepository.save(new User("Admin", "Demonstração", DEMO_PHONE)));
        if (membershipRepository.findActiveByUserAndOrganization(admin.getId(), organization.getId()).isEmpty()) {
            membershipRepository.save(new Membership(organization, admin, UserRole.SYSTEM_ADMIN));
        }

        seedDemoCampaign(organization, admin.getId());
        seedCapitalizationCampaign(organization, admin.getId());
    }

    private void seedDemoCampaign(Organization organization, Long adminId) {
        if (campaignRepository.findBySlug("acao-demo-local").isPresent()) return;

        var campaign = campaignRepository.save(new Campaign(organization.getId(),
                "Ação demonstração local", "acao-demo-local", CampaignMode.DEMO));
        campaign.setNumberRange(0, 999);
        campaignRepository.save(campaign);
        participationService.initializeSeries(campaign.getPublicId());

        beneficiaryService.create(adminId, campaign.getPublicId(), ORG_NAME);
        campaignPrizeRepository.save(new CampaignPrize(campaign.getId(), CampaignPrize.PrizeType.GOODS,
                "Item sintético para desenvolvimento local", 0L, 1, 0));

        // Carga inicial de prêmios-surpresa (10 de R$ 100 + 100 de R$ 10) antes de abrir.
        prizeLoadService.load(adminId, campaign.getPublicId());

        open(adminId, campaign);
    }

    private void seedCapitalizationCampaign(Organization organization, Long adminId) {
        var existing = campaignRepository.findBySlug("acao-capitalizacao-local");
        if (existing.isPresent()) {
            upgradeToCurrentCombos(existing.get());
            return;
        }

        var campaign = campaignRepository.save(new Campaign(organization.getId(),
                "Título de capitalização (local)", "acao-capitalizacao-local", CampaignMode.CAPITALIZATION_PRIZE_TITLE));
        campaign.setNumberRange(0, LOCAL_MAX_NUMBER);
        campaignRepository.save(campaign);
        participationService.initializeSeries(campaign.getPublicId());

        campaignPrizeRepository.save(new CampaignPrize(campaign.getId(), CampaignPrize.PrizeType.CASH,
                "Bônus em dinheiro (sintético)", 1_000L, 1, 1));
        campaignPrizeRepository.save(new CampaignPrize(campaign.getId(), CampaignPrize.PrizeType.GOODS,
                "Bem sintético (local)", 0L, 1, 2));

        beneficiaryService.create(adminId, campaign.getPublicId(), ORG_NAME);

        COMBOS.forEach((quantity, totalCents) ->
                purchaseOfferService.create(adminId, campaign.getPublicId(), quantity, totalCents));

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

        open(adminId, campaign);
    }

    /** Banco local criado antes dos combos atuais: a campanha já aberta recebe as ofertas e os números que faltam. */
    private void upgradeToCurrentCombos(Campaign campaign) {
        var current = purchaseOfferRepository.findByCampaignIdAndActiveTrueOrderByIdAsc(campaign.getId(), PageRequest.of(0, 100));
        COMBOS.forEach((quantity, totalCents) -> {
            if (current.stream().noneMatch(offer -> offer.getQuantity() == quantity)) {
                purchaseOfferRepository.save(new PurchaseOffer(campaign.getId(), quantity, totalCents));
            }
        });
        if (campaign.getNumberMax() >= LOCAL_MAX_NUMBER) return;
        var missing = new ArrayList<CampaignNumber>();
        for (int value = campaign.getNumberMax() + 1; value <= LOCAL_MAX_NUMBER; value++) {
            missing.add(new CampaignNumber(campaign.getId(), "A", value));
        }
        campaignNumberRepository.saveAll(missing);
        jdbc.update("UPDATE campaigns SET number_max = ? WHERE id = ?", LOCAL_MAX_NUMBER, campaign.getId());
    }

    private void open(Long adminId, Campaign campaign) {
        campaignService.transition(adminId, campaign.getPublicId(), CampaignStatus.LEGAL_REVIEW);
        campaignService.transition(adminId, campaign.getPublicId(), CampaignStatus.AUTHORIZED);
        campaignService.transition(adminId, campaign.getPublicId(), CampaignStatus.SCHEDULED);
        campaignService.transition(adminId, campaign.getPublicId(), CampaignStatus.OPEN);
    }
}
