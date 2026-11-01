package la.meleva.fundraiser.drawing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Objects;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importação e revisão em dupla do resultado oficial da Loteria Federal (P12). Nenhum número é
 * inventado: o payload bruto da fonte é preservado e hasheado; a apuração só avança após duas
 * revisões independentes.
 */
@Service
public class FederalResultService {

    private static final int REQUIRED_REVIEWS = 2;

    private final FederalLotteryClient client;
    private final FederalResultRepository results;
    private final FederalResultReviewRepository reviews;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final AuditEventRepository audits;
    private final Clock clock;

    @Autowired
    public FederalResultService(FederalLotteryClient client, FederalResultRepository results,
                                FederalResultReviewRepository reviews, UserRepository users,
                                MembershipRepository memberships, AuditEventRepository audits) {
        this(client, results, reviews, users, memberships, audits, Clock.systemUTC());
    }

    FederalResultService(FederalLotteryClient client, FederalResultRepository results,
                         FederalResultReviewRepository reviews, UserRepository users,
                         MembershipRepository memberships, AuditEventRepository audits, Clock clock) {
        this.client = client; this.results = results; this.reviews = reviews; this.users = users;
        this.memberships = memberships; this.audits = audits; this.clock = clock;
    }

    @Transactional
    public ResultView importResult(Long actorId, int contestNumber) {
        requireReviewer(actorId);
        var existing = results.findByContestIdentifier(String.valueOf(contestNumber));
        if (existing.isPresent()) return view(existing.get());
        FederalContest contest = client.fetch(contestNumber);
        String hash = sha256(contest.rawPayload());
        var result = results.save(new FederalResult(String.valueOf(contest.contestNumber()), "CAIXA_LOTERIAS",
                contest.drawnOn().atStartOfDay(ZoneOffset.UTC).toInstant(), contest.sourceUrl(), hash, contest.rawPayload()));
        audits.save(new AuditEvent(actorId, "FEDERAL_RESULT_IMPORTED", "federal_result", result.getId(), hash));
        return view(result);
    }

    @Transactional
    public ResultView review(Long actorId, Long federalResultId) {
        requireReviewer(actorId);
        if (federalResultId == null) throw ErrorCode.VALIDATION_ERROR.toException();
        var result = results.findLockedById(federalResultId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (reviews.existsByFederalResultIdAndReviewerUserId(federalResultId, actorId)) return view(result);
        reviews.save(new FederalResultReview(federalResultId, actorId));
        if (reviews.countByFederalResultId(federalResultId) >= REQUIRED_REVIEWS && !result.isVerifiedByTwo()) {
            Instant now = Instant.now(clock);
            result.markVerified(now);
            audits.save(new AuditEvent(actorId, "FEDERAL_RESULT_VERIFIED", "federal_result", result.getId(),
                    result.getSourceDocumentHash()));
        }
        return view(result);
    }

    @Transactional(readOnly = true)
    public ResultView get(String contestIdentifier) {
        return results.findByContestIdentifier(contestIdentifier)
                .map(FederalResultService::view)
                .orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
    }

    @Transactional
    public ResultView getVerified(Long federalResultId) {
        var result = results.findById(federalResultId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!result.isVerifiedByTwo()) throw ErrorCode.DRAW_RESULTS_NOT_VERIFIED.toException();
        return view(result);
    }

    private void requireReviewer(Long actorId) {
        if (actorId == null || users.findById(actorId).filter(user -> user.isActive()).isEmpty()) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
        boolean allowed = memberships.findActiveByUserId(actorId).stream()
                .anyMatch(membership -> membership.getRole() == UserRole.DRAW_AUDITOR
                        || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
    }

    private static ResultView view(FederalResult result) {
        return new ResultView(result.getId(), result.getContestIdentifier(), result.getSourceKind(),
                result.getDrawnAt(), result.getSourceUrl(), result.getSourceDocumentHash(),
                result.isVerifiedByTwo(), result.getResultPayload());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("federal.hash.unavailable", exception);
        }
    }

    public record ResultView(Long id, String contestIdentifier, String sourceKind, Instant drawnAt,
                             String sourceUrl, String sourceDocumentHash, boolean verifiedByTwo, String payload) {
        public ResultView { Objects.requireNonNull(contestIdentifier, "contestIdentifier"); }
    }
}
