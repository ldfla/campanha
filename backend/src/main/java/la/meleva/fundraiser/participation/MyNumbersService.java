package la.meleva.fundraiser.participation;

import java.util.List;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Meus números": leitura restrita às participações do próprio usuário autenticado. Contagens e o
 * valor confirmado são calculados no servidor; o cliente apenas exibe.
 */
@Service
public class MyNumbersService {
    static final int MAX_NUMBERS = 10_000;

    private final CampaignRepository campaigns;
    private final JdbcTemplate jdbc;

    MyNumbersService(CampaignRepository campaigns, JdbcTemplate jdbc) {
        this.campaigns = campaigns;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public MyNumbersView mine(Long userId, String campaignPublicId) {
        if (userId == null) throw ErrorCode.AUTH_INVALID_CREDENTIALS.toException();
        var campaign = campaigns.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        List<MyNumber> numbers = jdbc.query("""
                SELECT n.number_value, n.prize_cents, e.status FROM participation_entries e
                JOIN entry_number_assignments a ON a.entry_id = e.id
                JOIN campaign_numbers n ON n.id = a.campaign_number_id
                WHERE e.campaign_id = ? AND e.user_id = ? AND e.status NOT IN ('REJECTED', 'INVALIDATED')
                ORDER BY n.number_value LIMIT ?""",
                (rs, row) -> new MyNumber("%05d".formatted(rs.getInt("number_value")),
                        confirmedStatus(rs.getString("status")), rs.getLong("prize_cents")),
                campaign.getId(), userId, MAX_NUMBERS);
        long confirmed = countEntries(campaign.getId(), userId, "e.status IN ('ASSIGNED', 'FROZEN')");
        long pending = countEntries(campaign.getId(), userId, "e.status NOT IN ('ASSIGNED', 'FROZEN', 'REJECTED', 'INVALIDATED')");
        // Prêmio dos números já confirmados do titular (indicação de prêmio recebido — surpresa).
        Long prizeTotal = jdbc.queryForObject("""
                SELECT COALESCE(SUM(n.prize_cents), 0) FROM participation_entries e
                JOIN entry_number_assignments a ON a.entry_id = e.id
                JOIN campaign_numbers n ON n.id = a.campaign_number_id
                WHERE e.campaign_id = ? AND e.user_id = ? AND e.status IN ('ASSIGNED', 'FROZEN')""",
                Long.class, campaign.getId(), userId);
        Long amount = jdbc.queryForObject("""
                SELECT COALESCE(SUM(total_cents), 0) FROM orders
                WHERE campaign_id = ? AND user_id = ? AND status IN ('CONFIRMED', 'RECONCILED')""",
                Long.class, campaign.getId(), userId);
        return new MyNumbersView(campaign.getPublicId(), numbers, confirmed, pending,
                amount == null ? 0L : amount, prizeTotal == null ? 0L : prizeTotal, "BRL", campaign.getMode().isDemo());
    }

    private long countEntries(Long campaignId, Long userId, String statusFilter) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM participation_entries e
                JOIN entry_number_assignments a ON a.entry_id = e.id
                WHERE e.campaign_id = ? AND e.user_id = ? AND\s""" + statusFilter, Long.class, campaignId, userId);
        return count == null ? 0L : count;
    }

    private static String confirmedStatus(String entryStatus) {
        return "ASSIGNED".equals(entryStatus) || "FROZEN".equals(entryStatus) ? "CONFIRMED" : "PENDING";
    }

    /** {@code prizeCents} &gt; 0 indica número premiado (prêmio-surpresa da carga inicial). */
    public record MyNumber(String formattedNumber, String status, long prizeCents) { }

    public record MyNumbersView(String campaignPublicId, List<MyNumber> numbers, long confirmedCount,
                                long pendingCount, long confirmedAmountCents, long prizeTotalCents,
                                String currency, boolean simulation) {
        public MyNumbersView { numbers = List.copyOf(numbers); }
    }
}
