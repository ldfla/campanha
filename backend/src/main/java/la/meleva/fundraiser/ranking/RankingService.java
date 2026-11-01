package la.meleva.fundraiser.ranking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.campaign.CapabilityPolicy;
import la.meleva.fundraiser.campaign.CapabilityPolicy.Capability;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.ranking.dto.CreateSnapshotRequest;
import la.meleva.fundraiser.ranking.dto.RankingRuleRequest;
import la.meleva.fundraiser.ranking.dto.RankingRuleView;
import la.meleva.fundraiser.ranking.dto.RankingView;
import la.meleva.fundraiser.ranking.dto.SnapshotExport;
import la.meleva.fundraiser.ranking.dto.SnapshotView;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de rankings: agregação idempotente, desempate determinístico e snapshots auditáveis.
 * O placar público nunca expõe identificador interno, nome completo ou telefone.
 */
@Service
public class RankingService {

    static final String TIE_BREAKER = "FIRST_EVENT_AT_THEN_PUBLIC_ID";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int SNAPSHOT_LIST_LIMIT = 20;
    private static final Pattern PAYLOAD_SCORE = Pattern.compile("\"score\":(\\d+)");
    private static final Set<CampaignStatus> RULE_WINDOW = EnumSet.of(
            CampaignStatus.DRAFT, CampaignStatus.LEGAL_REVIEW,
            CampaignStatus.AUTHORIZED, CampaignStatus.SCHEDULED);

    private final CampaignRepository campaigns;
    private final RankingRuleRepository rules;
    private final RankingSnapshotRepository snapshots;
    private final RankingPositionRepository positions;
    private final UserRepository users;
    private final AuditEventRepository audits;
    private final RankingQueries queries;
    private final RankingPseudonymizer pseudonymizer;
    private final Clock clock;

    @Autowired
    public RankingService(CampaignRepository campaigns, RankingRuleRepository rules,
                          RankingSnapshotRepository snapshots, RankingPositionRepository positions,
                          UserRepository users, AuditEventRepository audits, RankingQueries queries,
                          RankingPseudonymizer pseudonymizer) {
        this(campaigns, rules, snapshots, positions, users, audits, queries, pseudonymizer, Clock.systemUTC());
    }

    RankingService(CampaignRepository campaigns, RankingRuleRepository rules,
                   RankingSnapshotRepository snapshots, RankingPositionRepository positions,
                   UserRepository users, AuditEventRepository audits, RankingQueries queries,
                   RankingPseudonymizer pseudonymizer, Clock clock) {
        this.campaigns = campaigns;
        this.rules = rules;
        this.snapshots = snapshots;
        this.positions = positions;
        this.users = users;
        this.audits = audits;
        this.queries = queries;
        this.pseudonymizer = pseudonymizer;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RankingView current(String campaignPublicId, RankingScope scope, Integer limit) {
        Campaign campaign = campaign(campaignPublicId);
        requireScope(scope);
        CapabilityPolicy.forMode(campaign.getMode()).require(Capability.SHOW_RANKINGS);
        Instant now = now();
        RankingRule rule = effectiveRule(campaign, scope, now);
        List<Ranked> ranked = ranked(campaign, scope, now);
        return view(campaign, rule, now, now, "LIVE", ranked.subList(0, Math.min(ranked.size(), clamp(limit))));
    }

    @Transactional
    public RankingRuleView registerRule(Long actorId, String campaignPublicId, RankingRuleRequest request) {
        Campaign campaign = campaign(campaignPublicId);
        CapabilityPolicy.forMode(campaign.getMode()).require(Capability.SHOW_RANKINGS);
        if (request.scope() == null || request.metric() == null || !request.metric().appliesTo(request.scope())) {
            throw ErrorCode.RANKING_RULE_INVALID.toException();
        }
        if (!RULE_WINDOW.contains(campaign.getStatus())) {
            throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
        }
        Instant effectiveAt = request.effectiveAt() == null
                ? now()
                : request.effectiveAt().truncatedTo(ChronoUnit.MICROS);
        int version = rules.findTopByCampaignIdAndScopeOrderByRuleVersionDesc(campaign.getId(), request.scope())
                .map(rule -> rule.getRuleVersion() + 1)
                .orElse(1);
        RankingRule rule = rules.saveAndFlush(new RankingRule(campaign.getId(), request.scope(), version,
                request.metric(), TIE_BREAKER, false, effectiveAt));
        audits.save(new AuditEvent(actorId, "RANKING_RULE_REGISTERED", "ranking_rule", rule.getId(),
                sha256(campaignPublicId + ":" + version + ":" + request.metric())));
        return new RankingRuleView(rule.getRuleVersion(), rule.getScope(), rule.getMetric(),
                rule.getTieBreaker(), rule.isLegallyApproved(), rule.getEffectiveAt());
    }

    @Transactional
    public SnapshotView snapshot(Long actorId, String campaignPublicId, CreateSnapshotRequest request) {
        Campaign campaign = campaign(campaignPublicId);
        requireScope(request.scope());
        CapabilityPolicy.forMode(campaign.getMode()).require(Capability.SHOW_RANKINGS);
        Instant now = now();
        Instant cutoff = request.cutoffAt() == null ? now : request.cutoffAt().truncatedTo(ChronoUnit.MICROS);
        if (cutoff.isAfter(now)) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        RankingRule rule = persistedRule(campaign, request.scope(), now);
        var existing = snapshots.findByRankingRuleIdAndCutoffAt(rule.getId(), cutoff);
        if (existing.isPresent()) {
            return snapshotView(campaign, existing.get());
        }
        List<Ranked> ranked = ranked(campaign, request.scope(), cutoff);
        String payload = payload(campaign, rule, cutoff, now, ranked);
        RankingSnapshot snapshot = snapshots.saveAndFlush(new RankingSnapshot(campaign.getId(), rule.getId(),
                request.scope(), cutoff, now, payload, sha256(payload), ranked.size()));
        List<RankingPosition> rows = new ArrayList<>(ranked.size());
        for (int index = 0; index < ranked.size(); index++) {
            Ranked candidate = ranked.get(index);
            rows.add(new RankingPosition(snapshot.getId(), candidate.userId(), index + 1,
                    candidate.score(), tiebreak(candidate)));
        }
        positions.saveAll(rows);
        audits.save(new AuditEvent(actorId, "RANKING_SNAPSHOT_CREATED", "ranking_snapshot",
                snapshot.getId(), snapshot.getSnapshotHash()));
        return snapshotView(campaign, snapshot);
    }

    @Transactional(readOnly = true)
    public List<SnapshotView> listSnapshots(String campaignPublicId, RankingScope scope) {
        Campaign campaign = campaign(campaignPublicId);
        requireScope(scope);
        return snapshots.findByCampaignIdAndScopeOrderByCutoffAtDesc(campaign.getId(), scope).stream()
                .limit(SNAPSHOT_LIST_LIMIT)
                .map(snapshot -> snapshotView(campaign, snapshot))
                .toList();
    }

    @Transactional(readOnly = true)
    public SnapshotView snapshot(String campaignPublicId, String snapshotPublicId) {
        Campaign campaign = campaign(campaignPublicId);
        RankingSnapshot snapshot = snapshots.findByPublicIdAndCampaignId(snapshotPublicId, campaign.getId())
                .orElseThrow(ErrorCode.RANKING_SNAPSHOT_NOT_FOUND::toException);
        return snapshotView(campaign, snapshot);
    }

    @Transactional
    public SnapshotExport export(Long actorId, String campaignPublicId, String snapshotPublicId) {
        Campaign campaign = campaign(campaignPublicId);
        RankingSnapshot snapshot = snapshots.findByPublicIdAndCampaignId(snapshotPublicId, campaign.getId())
                .orElseThrow(ErrorCode.RANKING_SNAPSHOT_NOT_FOUND::toException);
        if (!sha256(snapshot.getPayloadJson()).equals(snapshot.getSnapshotHash())) {
            throw ErrorCode.RANKING_SNAPSHOT_HASH_MISMATCH.toException();
        }
        if (!positionsMatchPayload(positions.findByIdSnapshotIdOrderByPosition(snapshot.getId()), snapshot)) {
            throw ErrorCode.RANKING_SNAPSHOT_HASH_MISMATCH.toException();
        }
        audits.save(new AuditEvent(actorId, "RANKING_SNAPSHOT_EXPORTED", "ranking_snapshot",
                snapshot.getId(), sha256(snapshot.getSnapshotHash() + ":" + (actorId == null ? "public" : actorId))));
        return new SnapshotExport(snapshot.getPublicId(), campaign.getPublicId(), snapshot.getScope(),
                snapshot.getCutoffAt(), snapshot.getGeneratedAt(), snapshot.getSnapshotHash(), true,
                snapshot.getPayloadJson());
    }

    /** As linhas de posição não entram no hash; conferi-las contra o payload lacrado detecta edição direta no banco. */
    private static boolean positionsMatchPayload(List<RankingPosition> rows, RankingSnapshot snapshot) {
        if (rows.size() != snapshot.getEntryCount()) {
            return false;
        }
        Matcher scores = PAYLOAD_SCORE.matcher(snapshot.getPayloadJson());
        for (int index = 0; index < rows.size(); index++) {
            RankingPosition row = rows.get(index);
            if (row.getPosition() != index + 1 || !scores.find()
                    || Long.parseLong(scores.group(1)) != row.getScore()) {
                return false;
            }
        }
        return !scores.find();
    }

    private List<Ranked> ranked(Campaign campaign, RankingScope scope, Instant cutoff) {
        List<RankingQueries.UserScore> scores = queries.scores(scope, campaign.getId(), cutoff);
        if (scores.isEmpty()) {
            return List.of();
        }
        Map<Long, User> byId = users.findAllById(scores.stream().map(RankingQueries.UserScore::userId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<RankingCalculator.EligibleFact> facts = new ArrayList<>(scores.size());
        for (RankingQueries.UserScore score : scores) {
            User user = byId.get(score.userId());
            if (user != null) {
                facts.add(new RankingCalculator.EligibleFact(score.userId(), user.getPublicId(),
                        score.firstEventAt(), score.score()));
            }
        }
        List<Ranked> ranked = new ArrayList<>(facts.size());
        for (RankingCalculator.Position position : RankingCalculator.calculate(facts)) {
            User user = byId.get(position.userId());
            ranked.add(new Ranked(position.userId(), position.publicId(),
                    pseudonymizer.pseudonym(user.getDisplayName(), user.getSurname(), user.getPhoneNumber()),
                    position.score(), position.firstEligibleAt()));
        }
        return List.copyOf(ranked);
    }

    private RankingRule effectiveRule(Campaign campaign, RankingScope scope, Instant at) {
        return rules.findTopByCampaignIdAndScopeAndEffectiveAtLessThanEqualOrderByRuleVersionDesc(
                        campaign.getId(), scope, at)
                .orElseGet(() -> defaultRule(campaign, scope));
    }

    private RankingRule defaultRule(Campaign campaign, RankingScope scope) {
        return new RankingRule(campaign.getId(), scope, 0, metricFor(scope), TIE_BREAKER, false, Instant.EPOCH);
    }

    private RankingRule persistedRule(Campaign campaign, RankingScope scope, Instant now) {
        RankingRule effective = effectiveRule(campaign, scope, now);
        if (effective.getId() != null) {
            return effective;
        }
        if (rules.findTopByCampaignIdAndScopeOrderByRuleVersionDesc(campaign.getId(), scope).isPresent()) {
            throw ErrorCode.RANKING_RULE_NOT_FOUND.toException();
        }
        return rules.saveAndFlush(new RankingRule(campaign.getId(), scope, 1, metricFor(scope),
                TIE_BREAKER, false, now));
    }

    private RankingView view(Campaign campaign, RankingRule rule, Instant cutoffAt, Instant generatedAt,
                             String consistency, List<Ranked> ranked) {
        boolean simulation = simulation(campaign, rule);
        List<RankingView.Row> rows = new ArrayList<>(ranked.size());
        for (int index = 0; index < ranked.size(); index++) {
            Ranked candidate = ranked.get(index);
            rows.add(new RankingView.Row(index + 1, candidate.pseudonym(), candidate.score(),
                    candidate.firstEventAt()));
        }
        return new RankingView(campaign.getPublicId(), campaign.getTitle(), rule.getScope(), rule.getMetric(),
                rule.getRuleVersion(), rule.getTieBreaker(), cutoffAt, generatedAt, consistency, simulation,
                awardsEligible(campaign, rule), notice(rule.getScope(), simulation), rows);
    }

    private SnapshotView snapshotView(Campaign campaign, RankingSnapshot snapshot) {
        RankingRule rule = rules.findById(snapshot.getRankingRuleId())
                .orElseThrow(ErrorCode.RANKING_RULE_NOT_FOUND::toException);
        return new SnapshotView(snapshot.getPublicId(), campaign.getPublicId(), snapshot.getScope(),
                rule.getMetric(), rule.getRuleVersion(), snapshot.getCutoffAt(), snapshot.getGeneratedAt(),
                snapshot.getSnapshotHash(), snapshot.getEntryCount());
    }

    private boolean simulation(Campaign campaign, RankingRule rule) {
        return !rule.isLegallyApproved() || campaign.getMode() == CampaignMode.DEMO;
    }

    private boolean awardsEligible(Campaign campaign, RankingRule rule) {
        return rule.isLegallyApproved() && campaign.getMode() != CampaignMode.DEMO;
    }

    private String notice(RankingScope scope, boolean simulation) {
        if (!simulation) {
            return "Placar oficial consolidado a partir de eventos confirmados.";
        }
        return scope == RankingScope.PARTICIPANT
                ? "Ranking de participantes em modo SIMULAÇÃO: não gera direito a prêmio até aprovação legal do regulamento."
                : "Ranking de divulgadores em modo SIMULAÇÃO: não gera comissão até aprovação legal e contratual.";
    }

    private String payload(Campaign campaign, RankingRule rule, Instant cutoffAt, Instant generatedAt,
                           List<Ranked> ranked) {
        List<String> fields = new ArrayList<>(9);
        fields.add("\"campaignPublicId\":" + quote(campaign.getPublicId()));
        fields.add("\"scope\":" + quote(rule.getScope().name()));
        fields.add("\"metric\":" + quote(rule.getMetric().name()));
        fields.add("\"ruleVersion\":" + rule.getRuleVersion());
        fields.add("\"tieBreaker\":" + quote(rule.getTieBreaker()));
        fields.add("\"cutoffAt\":" + quote(iso(cutoffAt)));
        fields.add("\"generatedAt\":" + quote(iso(generatedAt)));
        StringBuilder entries = new StringBuilder(ranked.size() * 96);
        entries.append('[');
        for (int index = 0; index < ranked.size(); index++) {
            Ranked candidate = ranked.get(index);
            if (index > 0) {
                entries.append(',');
            }
            entries.append('{')
                    .append("\"position\":").append(index + 1).append(',')
                    .append("\"pseudonym\":").append(quote(candidate.pseudonym())).append(',')
                    .append("\"score\":").append(candidate.score()).append(',')
                    .append("\"firstEventAt\":").append(quote(iso(candidate.firstEventAt())))
                    .append('}');
        }
        entries.append(']');
        fields.add("\"entries\":" + entries);
        return "{" + String.join(",", fields) + "}";
    }

    private String quote(String value) {
        StringBuilder quoted = new StringBuilder(value.length() + 2);
        quoted.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (character < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) character));
                    } else {
                        quoted.append(character);
                    }
                }
            }
        }
        return quoted.append('"').toString();
    }

    private String tiebreak(Ranked candidate) {
        return candidate.firstEventAt().toEpochMilli() + ":" + candidate.publicId();
    }

    private RankingMetric metricFor(RankingScope scope) {
        return scope == RankingScope.PROMOTER
                ? RankingMetric.REFERRAL_CONFIRMED_PARTICIPATIONS
                : RankingMetric.CONFIRMED_PARTICIPATIONS;
    }

    private Campaign campaign(String publicId) {
        return campaigns.findByPublicId(publicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
    }

    private void requireScope(RankingScope scope) {
        if (scope == null) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
    }

    private int clamp(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, limit));
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private String iso(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    record Ranked(long userId, String publicId, String pseudonym, long score, Instant firstEventAt) {}
}
