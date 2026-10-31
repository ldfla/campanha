package la.meleva.fundraiser.drawing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Apuração determinística (P12): registra a regra, congela as participações elegíveis, computa os
 * vencedores a partir do resultado oficial revisado em dupla e publica com pacote de transparência.
 */
@Service
public class DrawingService {

    private final CampaignRepository campaigns;
    private final DrawRuleRepository rules;
    private final DrawRepository draws;
    private final DrawOutcomeRepository outcomes;
    private final FederalResultRepository federalResults;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final AuditEventRepository audits;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired
    public DrawingService(CampaignRepository campaigns, DrawRuleRepository rules, DrawRepository draws,
                          DrawOutcomeRepository outcomes, FederalResultRepository federalResults,
                          MembershipRepository memberships, UserRepository users, AuditEventRepository audits,
                          JdbcTemplate jdbc) {
        this(campaigns, rules, draws, outcomes, federalResults, memberships, users, audits, jdbc, Clock.systemUTC());
    }

    DrawingService(CampaignRepository campaigns, DrawRuleRepository rules, DrawRepository draws,
                   DrawOutcomeRepository outcomes, FederalResultRepository federalResults,
                   MembershipRepository memberships, UserRepository users, AuditEventRepository audits,
                   JdbcTemplate jdbc, Clock clock) {
        this.campaigns = campaigns; this.rules = rules; this.draws = draws; this.outcomes = outcomes;
        this.federalResults = federalResults; this.memberships = memberships; this.users = users;
        this.audits = audits; this.jdbc = jdbc; this.clock = clock;
    }

    @Transactional
    public RuleView registerRule(Long actorId, String campaignPublicId, String formulaJson) {
        var campaign = campaigns.findLockedByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        requireManager(actorId, campaign.getOrganizationId());
        if (!campaign.getStatus().isEditable()) throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
        FederalDrawRule parsed;
        try {
            parsed = FederalDrawRule.parse(formulaJson);
        } catch (RuntimeException exception) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        int version = rules.findTopByCampaignIdOrderByVersionDesc(campaign.getId()).map(rule -> rule.getVersion() + 1).orElse(1);
        var rule = rules.save(new DrawRule(campaign.getId(), version, FederalDrawRule.SOURCE, formulaJson, null, null));
        audits.save(new AuditEvent(actorId, "DRAW_RULE_REGISTERED", "draw_rule", rule.getId(), sha256(formulaJson)));
        return new RuleView(version, parsed.digits(), parsed.prizePositions());
    }

    @Transactional
    public DrawView freeze(Long actorId, String campaignPublicId) {
        var campaign = campaigns.findLockedByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        requireManager(actorId, campaign.getOrganizationId());
        if (campaign.getStatus() != CampaignStatus.LOCKED && campaign.getStatus() != CampaignStatus.DRAW_PENDING) {
            throw ErrorCode.DRAW_STATE_INVALID.toException();
        }
        var rule = rules.findTopByCampaignIdOrderByVersionDesc(campaign.getId())
                .orElseThrow(ErrorCode.DRAW_RULE_MISMATCH::toException);
        List<DrawingEngine.FrozenEntry> frozen = frozenEntries(campaign.getId());
        if (frozen.isEmpty()) throw ErrorCode.DRAW_STATE_INVALID.toException();
        String entriesHash = sha256(canonicalEntries(frozen));
        Instant now = Instant.now(clock);
        var draw = draws.save(new Draw(campaign.getId(), rule.getId()));
        draw.freeze(entriesHash, now);
        audits.save(new AuditEvent(actorId, "DRAW_FROZEN", "draw", draw.getId(), entriesHash));
        return view(draw, rule);
    }

    @Transactional
    public DrawView compute(Long actorId, String campaignPublicId, String drawPublicId, String contestIdentifier) {
        var campaign = campaigns.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        requireAuditor(actorId);
        var draw = draws.findByPublicId(drawPublicId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!draw.getCampaignId().equals(campaign.getId())) throw ErrorCode.RESOURCE_NOT_FOUND.toException();
        if (draw.getStatus() == DrawStatus.PUBLISHED) throw ErrorCode.DRAW_ALREADY_PUBLISHED.toException();
        if (draw.getEntriesHash() == null) throw ErrorCode.DRAW_STATE_INVALID.toException();
        var result = federalResults.findByContestIdentifier(contestIdentifier).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!result.isVerifiedByTwo()) throw ErrorCode.DRAW_RESULTS_NOT_VERIFIED.toException();
        var rule = rules.findById(draw.getDrawRuleId()).orElseThrow(ErrorCode.DRAW_RULE_MISMATCH::toException);
        FederalDrawRule parsedRule = FederalDrawRule.parse(rule.getFormulaJson());
        List<String> federalPrizes = federalPrizes(result.getResultPayload());
        List<DrawingEngine.FrozenEntry> frozen = frozenEntries(campaign.getId());
        var resolved = DrawingEngine.compute(parsedRule, federalPrizes, frozen);
        List<Long> prizeIds = orderedPrizeIds(campaign.getId());
        outcomes.deleteByDrawId(draw.getId());
        Instant now = Instant.now(clock);
        for (DrawingEngine.Resolved position : resolved) {
            if (position.winningNumber() == null) continue;
            if (position.position() > prizeIds.size()) throw ErrorCode.DRAW_RULE_MISMATCH.toException();
            outcomes.save(new DrawOutcome(draw.getId(), prizeIds.get(position.position() - 1), position.winningNumber(),
                    position.entryId(), position.decisionCode(),
                    sha256(result.getSourceDocumentHash() + ":" + position.position() + ":" + position.winningNumber())));
        }
        if (draw.getStatus() != DrawStatus.RESULTS_IMPORTED) draw.bindResult(result.getId(), now);
        draw.computed(now);
        audits.save(new AuditEvent(actorId, "DRAW_COMPUTED", "draw", draw.getId(), result.getSourceDocumentHash()));
        return view(draw, rule);
    }

    @Transactional
    public DrawView publish(Long actorId, String campaignPublicId, String drawPublicId) {
        var campaign = campaigns.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        requireAuditor(actorId);
        var draw = draws.findByPublicId(drawPublicId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!draw.getCampaignId().equals(campaign.getId())) throw ErrorCode.RESOURCE_NOT_FOUND.toException();
        if (draw.getFederalResultId() == null) throw ErrorCode.DRAW_RESULTS_NOT_VERIFIED.toException();
        var result = federalResults.findById(draw.getFederalResultId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!result.isVerifiedByTwo()) throw ErrorCode.DRAW_RESULTS_NOT_VERIFIED.toException();
        Instant now = Instant.now(clock);
        draw.publish(now);
        audits.save(new AuditEvent(actorId, "DRAW_PUBLISHED", "draw", draw.getId(), result.getSourceDocumentHash()));
        var rule = rules.findById(draw.getDrawRuleId()).orElseThrow(ErrorCode.DRAW_RULE_MISMATCH::toException);
        return view(draw, rule);
    }

    @Transactional(readOnly = true)
    public TransparencyView transparency(String campaignPublicId, String drawPublicId) {
        var campaign = campaigns.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        var draw = draws.findByPublicId(drawPublicId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!draw.getCampaignId().equals(campaign.getId())) throw ErrorCode.RESOURCE_NOT_FOUND.toException();
        var rule = rules.findById(draw.getDrawRuleId()).orElseThrow(ErrorCode.DRAW_RULE_MISMATCH::toException);
        var result = draw.getFederalResultId() == null ? null
                : federalResults.findById(draw.getFederalResultId()).orElse(null);
        List<OutcomeLine> lines = outcomes.findByDrawId(draw.getId()).stream()
                .map(outcome -> new OutcomeLine(outcome.getPrizeId(), outcome.getWinningNumber(), outcome.getEntryId(),
                        outcome.getDecisionCode()))
                .toList();
        return new TransparencyView(campaign.getPublicId(), draw.getPublicId(), draw.getStatus().name(),
                rule.getVersion(), rule.getFormulaJson(), rule.getApprovedRegulationSha256(), draw.getEntriesHash(),
                draw.getFrozenAt(), draw.getPublishedAt(),
                result == null ? null : result.getContestIdentifier(),
                result == null ? null : result.getSourceDocumentHash(),
                result == null ? null : result.getSourceUrl(),
                result == null ? null : result.getResultPayload(),
                lines);
    }

    private List<DrawingEngine.FrozenEntry> frozenEntries(Long campaignId) {
        return jdbc.query("SELECT e.id AS entry_id, n.number_value AS number_value FROM participation_entries e "
                        + "JOIN campaign_numbers n ON n.entry_id=e.id AND n.campaign_id=e.campaign_id "
                        + "WHERE e.campaign_id=? AND e.status='FROZEN' AND n.status='FROZEN' ORDER BY n.number_value",
                (rs, rowNum) -> new DrawingEngine.FrozenEntry(rs.getLong("entry_id"), rs.getInt("number_value")), campaignId);
    }

    private static String canonicalEntries(List<DrawingEngine.FrozenEntry> frozen) {
        StringBuilder builder = new StringBuilder();
        for (DrawingEngine.FrozenEntry entry : frozen) {
            builder.append(entry.numberValue()).append(':').append(entry.entryId()).append('\n');
        }
        return builder.toString();
    }

    private List<Long> orderedPrizeIds(Long campaignId) {
        return jdbc.queryForList("SELECT id FROM campaign_prizes WHERE campaign_id=? ORDER BY sort_order, id", Long.class, campaignId);
    }

    private static List<String> federalPrizes(String payload) {
        JsonNode root = JsonMapper.builder().build().readTree(payload);
        List<String> prizes = new ArrayList<>();
        for (JsonNode node : root.path("listaDezenas")) prizes.add(node.asString());
        if (prizes.isEmpty()) throw ErrorCode.DRAW_RESULTS_NOT_VERIFIED.toException();
        return prizes;
    }

    private void requireManager(Long actorId, Long organizationId) {
        if (actorId == null || users.findById(actorId).filter(user -> user.isActive()).isEmpty()) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
        boolean allowed = memberships.findActiveByUserAndOrganization(actorId, organizationId).stream()
                .anyMatch(membership -> membership.getRole() == UserRole.CAMPAIGN_MANAGER
                        || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
    }

    private void requireAuditor(Long actorId) {
        if (actorId == null || users.findById(actorId).filter(user -> user.isActive()).isEmpty()) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
        boolean allowed = memberships.findActiveByUserId(actorId).stream()
                .anyMatch(membership -> membership.getRole() == UserRole.DRAW_AUDITOR
                        || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
    }

    private static DrawView view(Draw draw, DrawRule rule) {
        return new DrawView(draw.getPublicId(), draw.getStatus().name(), rule.getVersion(), draw.getEntriesHash(),
                draw.getFrozenAt(), draw.getPublishedAt(), draw.getFederalResultId());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("draw.hash.unavailable", exception);
        }
    }

    public record RuleView(int version, int digits, List<Integer> prizePositions) {
        public RuleView { prizePositions = List.copyOf(prizePositions); }
    }

    public record DrawView(String publicId, String status, int ruleVersion, String entriesHash, Instant frozenAt,
                           Instant publishedAt, Long federalResultId) { }

    public record OutcomeLine(Long prizeId, int winningNumber, Long entryId, String decisionCode) { }

    public record TransparencyView(String campaignPublicId, String drawPublicId, String status, int ruleVersion,
                                   String formulaJson, String approvedRegulationSha256, String entriesHash,
                                   Instant frozenAt, Instant publishedAt, String contestIdentifier,
                                   String sourceDocumentHash, String sourceUrl, String resultPayload,
                                   List<OutcomeLine> outcomes) {
        public TransparencyView { outcomes = List.copyOf(outcomes); Objects.requireNonNull(drawPublicId, "drawPublicId"); }
    }
}
