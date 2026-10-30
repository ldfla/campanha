package la.meleva.fundraiser;

import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.participation.CampaignNumber;
import la.meleva.fundraiser.participation.CampaignNumberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DataIntegrityIntegrationTest {
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private CampaignNumberRepository campaignNumberRepository;

    @Test
    void flywayCreatesEveryBusinessTableFromAnEmptySchema() {
        List<String> requiredTables = List.of("campaign_numbers", "participation_requests", "participation_entries",
                "entry_number_assignments", "eligibility_events", "reservation_leases", "orders", "order_items",
                "payments", "payment_events", "refunds", "financial_ledger", "reconciliation_runs", "referral_links",
                "referral_clicks", "referral_attributions", "ranking_rules", "ranking_snapshots", "ranking_positions",
                "draw_rules", "federal_results", "draws", "draw_outcomes", "surprise_commitments", "award_deliveries",
                "audit_events", "outbox_events", "inbox_events");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN (" + "?,".repeat(requiredTables.size() - 1) + "?)", Integer.class, requiredTables.toArray());
        assertThat(count).isEqualTo(requiredTables.size());
    }

    @Test
    void databaseRejectsDuplicateNumberInSameCampaign() {
        Campaign campaign = newCampaign("org-numeros", "numeros-unicos");
        campaignNumberRepository.saveAndFlush(new CampaignNumber(campaign.getId(), "A", 0));
        assertThatThrownBy(() -> campaignNumberRepository.saveAndFlush(new CampaignNumber(campaign.getId(), "A", 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void organizationsRemainIsolatedAndForeignKeysAreEnforced() {
        Campaign first = newCampaign("org-um", "campanha-um");
        Campaign second = newCampaign("org-dois", "campanha-dois");
        assertThat(campaignRepository.findByOrganizationId(first.getOrganizationId())).extracting(Campaign::getId).contains(first.getId()).doesNotContain(second.getId());
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO campaign_numbers (campaign_id, series_code, number_value, status, created_at, updated_at, version) VALUES (?, 'A', 1, 'AVAILABLE', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0)", Long.MAX_VALUE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Campaign newCampaign(String organizationName, String slug) {
        Organization organization = organizationRepository.saveAndFlush(new Organization(organizationName, "ASSOCIATION"));
        return campaignRepository.saveAndFlush(new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO));
    }
}
