package la.meleva.fundraiser.ranking;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Consultas de agregação do ranking. Somente leitura e sem dados pessoais:
 * retornam identificador interno, pontuação e instante do primeiro evento elegível.
 *
 * <p>O banco opera em UTC (invariante declarada em AGENTS.md). Os parâmetros e resultados
 * são convertidos explicitamente em {@link ZoneOffset#UTC} para não depender do timezone
 * padrão da JVM — o Hibernate grava os instantes com {@code hibernate.jdbc.time_zone=UTC}.</p>
 */
@Component
class RankingQueries {

    private static final String PROMOTER_SQL = """
            SELECT rl.promoter_user_id AS user_id, COUNT(*) AS total, MIN(ra.attributed_at) AS first_event_at
            FROM referral_attributions ra
            JOIN referral_links rl ON rl.id = ra.referral_link_id
            JOIN participation_entries pe ON pe.id = ra.entry_id
            WHERE ra.campaign_id = ? AND ra.status = 'ATTRIBUTED'
              AND pe.status IN ('ASSIGNED', 'FROZEN')
              AND ra.attributed_at IS NOT NULL AND ra.attributed_at <= ?
            GROUP BY rl.promoter_user_id
            """;

    private static final String PARTICIPANT_SQL = """
            SELECT pe.user_id AS user_id, COUNT(*) AS total, MIN(pe.issued_at) AS first_event_at
            FROM participation_entries pe
            WHERE pe.campaign_id = ? AND pe.status IN ('ASSIGNED', 'FROZEN')
              AND pe.issued_at IS NOT NULL AND pe.issued_at <= ?
            GROUP BY pe.user_id
            """;

    private final JdbcTemplate jdbc;

    RankingQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<UserScore> scores(RankingScope scope, long campaignId, Instant cutoff) {
        String sql = switch (scope) {
            case PROMOTER -> PROMOTER_SQL;
            case PARTICIPANT -> PARTICIPANT_SQL;
        };
        LocalDateTime utcCutoff = cutoff.atOffset(ZoneOffset.UTC).toLocalDateTime();
        return jdbc.query(sql, (rs, rowNum) -> new UserScore(
                rs.getLong("user_id"),
                rs.getLong("total"),
                rs.getObject("first_event_at", LocalDateTime.class).toInstant(ZoneOffset.UTC)), campaignId, utcCutoff);
    }

    record UserScore(long userId, long score, Instant firstEventAt) {}
}
