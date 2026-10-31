package la.meleva.fundraiser.identity;

import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Limite local por conta; em ambiente distribuído deve ser substituído por armazenamento compartilhado aprovado. */
@Component
class LoginAttemptTracker {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_SECONDS = 15 * 60;
    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    void checkAllowed(String phoneNumber) {
        var state = attempts.get(hash(phoneNumber));
        if (state != null && state.lockedUntil != null && Instant.now().isBefore(state.lockedUntil)) {
            throw ErrorCode.AUTH_ACCOUNT_LOCKED.toException();
        }
    }

    void recordFailure(String phoneNumber) {
        attempts.compute(hash(phoneNumber), (key, previous) -> {
            var next = previous == null ? new AttemptState() : previous;
            next.failures++;
            if (next.failures >= MAX_FAILURES) next.lockedUntil = Instant.now().plusSeconds(LOCK_SECONDS);
            return next;
        });
    }

    void recordSuccess(String phoneNumber) { attempts.remove(hash(phoneNumber)); }

    private String hash(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private static final class AttemptState { private int failures; private Instant lockedUntil; }
}
