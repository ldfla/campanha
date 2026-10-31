package la.meleva.fundraiser.shared;

import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignPrize;
import la.meleva.fundraiser.campaign.CampaignPrizeRepository;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Dados exclusivamente sintéticos para desenvolvimento local; nunca ativos fora do profile local. */
@Component
@Profile("local")
class LocalDemoSeed implements ApplicationRunner {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignPrizeRepository campaignPrizeRepository;

    LocalDemoSeed(OrganizationRepository organizationRepository, UserRepository userRepository,
                  CampaignRepository campaignRepository, CampaignPrizeRepository campaignPrizeRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.campaignPrizeRepository = campaignPrizeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var organization = organizationRepository.findByLegalName("Organização Demonstração Local")
                .orElseGet(() -> organizationRepository.save(new Organization("Organização Demonstração Local", "ASSOCIATION")));
        userRepository.findByPhoneNumber("+5511999990000")
                .orElseGet(() -> userRepository.save(new User("Admin", "Demonstração", "+5511999990000")));
        var campaign = campaignRepository.findBySlug("acao-demo-local")
                .orElseGet(() -> campaignRepository.save(new Campaign(organization.getId(), "Ação demonstração local", "acao-demo-local", CampaignMode.DEMO)));
        if (campaignPrizeRepository.findByCampaignIdOrderBySortOrder(campaign.getId()).isEmpty()) {
            campaignPrizeRepository.save(new CampaignPrize(campaign.getId(), CampaignPrize.PrizeType.GOODS,
                    "Item sintético para desenvolvimento local", 0L, 1, 0));
        }
    }
}
