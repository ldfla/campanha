package la.meleva.fundraiser.participation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga inicial de prêmios nos números (00000–99999 por padrão).
 *
 * <p>Duas camadas de colunas em {@code campaign_numbers}:
 * <ul>
 *   <li><b>Inicialização</b> — {@code number_value}, {@code prize_cents}, {@code prize_defined_at}:
 *       sorteadas uma única vez, hasheadas e conferíveis. Nunca reescritas.</li>
 *   <li><b>Operação</b> — dados do comprador, caridade, indicador, correlação: atualizáveis e
 *       deliberadamente fora do hash.</li>
 * </ul>
 */
@Service
public class InitialPrizeLoadService {

    public static final int TIER1_COUNT = 10;
    public static final int TIER2_COUNT = 100;
    /** R$ 100,00 */
    public static final long TIER1_CENTS = 10_000L;
    /** R$ 10,00 */
    public static final long TIER2_CENTS = 1_000L;
    private static final String CSV_HEADER = "number,prize_cents,prize_defined_at";

    private final CampaignRepository campaigns;
    private final CampaignNumberRepository numbers;
    private final CampaignPrizeLoadRepository loads;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final AuditEventRepository audits;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SecureRandom random;

    @Autowired
    public InitialPrizeLoadService(CampaignRepository campaigns, CampaignNumberRepository numbers,
                                   CampaignPrizeLoadRepository loads, MembershipRepository memberships,
                                   UserRepository users, AuditEventRepository audits, JdbcTemplate jdbc) {
        this(campaigns, numbers, loads, memberships, users, audits, jdbc, Clock.systemUTC(), new SecureRandom());
    }

    InitialPrizeLoadService(CampaignRepository campaigns, CampaignNumberRepository numbers,
                            CampaignPrizeLoadRepository loads, MembershipRepository memberships,
                            UserRepository users, AuditEventRepository audits, JdbcTemplate jdbc,
                            Clock clock, SecureRandom random) {
        this.campaigns = campaigns; this.numbers = numbers; this.loads = loads; this.memberships = memberships;
        this.users = users; this.audits = audits; this.jdbc = jdbc; this.clock = clock; this.random = random;
    }

    @Transactional
    public LoadView load(Long actorId, String campaignPublicId) {
        var campaign = campaigns.findLockedByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        requireManager(actorId, campaign);
        if (!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        if (!campaign.getStatus().isEditable()) throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
        var existing = loads.findByCampaignId(campaign.getId());
        if (existing.isPresent()) return view(existing.get());

        int min = campaign.getNumberMin();
        int max = campaign.getNumberMax();
        int capacity = max - min + 1;
        if (numbers.countByCampaignId(campaign.getId()) != capacity) throw ErrorCode.NUMBER_POOL_EXHAUSTED.toException();
        if (capacity < TIER1_COUNT + TIER2_COUNT) throw ErrorCode.VALIDATION_ERROR.toException();

        Set<Integer> alreadyPrized = new LinkedHashSet<>(jdbc.queryForList(
                "SELECT number_value FROM campaign_numbers WHERE campaign_id=? AND prize_cents IS NOT NULL",
                Integer.class, campaign.getId()));
        Set<Integer> tier1 = draw(min, max, TIER1_COUNT, alreadyPrized);
        alreadyPrized.addAll(tier1);
        Set<Integer> tier2 = draw(min, max, TIER2_COUNT, alreadyPrized);

        Instant definedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        var sqlTimestamp = java.sql.Timestamp.from(definedAt);
        List<Object[]> batch = new ArrayList<>(tier1.size() + tier2.size());
        for (int value : tier1) batch.add(new Object[]{TIER1_CENTS, sqlTimestamp, campaign.getId(), value});
        for (int value : tier2) batch.add(new Object[]{TIER2_CENTS, sqlTimestamp, campaign.getId(), value});
        jdbc.batchUpdate("UPDATE campaign_numbers SET prize_cents=?, prize_defined_at=?, version=version+1, updated_at=? "
                        + "WHERE campaign_id=? AND number_value=? AND prize_cents IS NULL",
                batch.stream().map(row -> new Object[]{row[0], row[1], row[1], row[2], row[3]}).toList());

        String sha256 = sha256(exportCsv(campaignPublicId));
        var load = loads.save(new CampaignPrizeLoad(campaign.getId(), min, max, TIER1_COUNT, TIER2_COUNT,
                TIER1_CENTS, TIER2_CENTS, definedAt, sha256, Instant.now(clock)));
        audits.save(new AuditEvent(actorId, "PRIZE_LOAD_GENERATED", "campaign", campaign.getId(), sha256));
        return view(load);
    }

    /** Sorteia números distintos fora de {@code excluded}; ao colidir, sorteia outro. */
    private Set<Integer> draw(int min, int max, int count, Set<Integer> excluded) {
        var chosen = new LinkedHashSet<Integer>();
        int guard = 0;
        int limit = (max - min + 1) * 4 + count * 4;
        while (chosen.size() < count) {
            int candidate = min + random.nextInt(max - min + 1);
            if (!excluded.contains(candidate)) chosen.add(candidate);
            if (++guard > limit) throw ErrorCode.VALIDATION_ERROR.toException();
        }
        return chosen;
    }

    @Transactional(readOnly = true)
    public String exportCsv(String campaignPublicId) {
        var campaign = campaigns.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        jdbc.query("SELECT number_value, prize_cents, prize_defined_at FROM campaign_numbers "
                        + "WHERE campaign_id=? ORDER BY number_value",
                rs -> {
                    int value = rs.getInt("number_value");
                    long prize = rs.getLong("prize_cents");
                    boolean hasPrize = !rs.wasNull();
                    LocalDateTime defined = rs.getObject("prize_defined_at", LocalDateTime.class);
                    csv.append("%05d".formatted(value)).append(',')
                            .append(hasPrize ? Long.toString(prize) : "").append(',')
                            .append(defined == null ? "" : DateTimeFormatter.ISO_INSTANT.format(defined.toInstant(ZoneOffset.UTC)))
                            .append('\n');
                }, campaign.getId());
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public VerifyView verify(String campaignPublicId) {
        var campaign = campaigns.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        var load = loads.findByCampaignId(campaign.getId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        String computed = sha256(exportCsv(campaignPublicId));
        boolean match = MessageDigest.isEqual(computed.getBytes(StandardCharsets.US_ASCII),
                load.getFileSha256().getBytes(StandardCharsets.US_ASCII));
        return new VerifyView(load.getFileSha256(), computed, match, load.getDefinedAt());
    }

    @Transactional(readOnly = true)
    public LoadView current(String campaignPublicId) {
        var campaign = campaigns.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        return view(loads.findByCampaignId(campaign.getId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException));
    }

    private void requireManager(Long actorId, Campaign campaign) {
        if (actorId == null || users.findById(actorId).filter(user -> user.isActive()).isEmpty()) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
        boolean allowed = memberships.findActiveByUserAndOrganization(actorId, campaign.getOrganizationId()).stream()
                .anyMatch(membership -> membership.getRole() == UserRole.CAMPAIGN_MANAGER
                        || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
    }

    private static LoadView view(CampaignPrizeLoad load) {
        return new LoadView(load.getNumberMin(), load.getNumberMax(), load.getTier1Count(), load.getTier1Cents(),
                load.getTier2Count(), load.getTier2Cents(), load.getDefinedAt(), load.getFileSha256(), load.getGeneratedAt());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("prize.load.hash.unavailable", exception);
        }
    }

    public record LoadView(int numberMin, int numberMax, int tier1Count, long tier1Cents, int tier2Count,
                           long tier2Cents, Instant definedAt, String fileSha256, Instant generatedAt) {
        public LoadView { Objects.requireNonNull(fileSha256, "fileSha256"); }
    }

    public record VerifyView(String storedSha256, String computedSha256, boolean match, Instant definedAt) { }
}
