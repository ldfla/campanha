package la.meleva.fundraiser.support;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Reduz dados de texto de chamados do titular dentro da transação de anonimização. */
@Service
public class SupportPrivacyService {
    private final JdbcTemplate jdbc;

    SupportPrivacyService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void redactForUser(Long userId) {
        Objects.requireNonNull(userId, "userId");
        jdbc.update("UPDATE support_requests SET subject=?, message=?, idempotency_key=NULL, "
                        + "idempotency_fingerprint=NULL WHERE user_id=?",
                "Conteúdo removido pelo titular", "Conteúdo removido pelo titular", userId);
    }
}
