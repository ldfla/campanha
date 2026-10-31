package la.meleva.fundraiser.privacy;

import java.time.Instant;
import java.util.List;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direitos do titular (LGPD): exportação dos próprios dados e anonimização. Não expõe dados de
 * terceiros; a trilha de auditoria é preservada.
 */
@Service
public class PrivacyService {

    private final UserRepository users;
    private final AuditEventRepository audits;
    private final CpfService cpfs;
    private final JdbcTemplate jdbc;

    PrivacyService(UserRepository users, AuditEventRepository audits, CpfService cpfs, JdbcTemplate jdbc) {
        this.users = users;
        this.audits = audits;
        this.cpfs = cpfs;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ExportView export(Long userId) {
        var user = users.findById(userId).orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        long participations = count("SELECT COUNT(*) FROM participation_entries WHERE user_id=?", userId);
        long referrals = count("SELECT COUNT(*) FROM referral_attributions ra JOIN referral_links rl ON rl.id=ra.referral_link_id "
                + "WHERE rl.promoter_user_id=?", userId);
        List<String> memberships = jdbc.query("SELECT organization_id, role FROM memberships WHERE user_id=? AND status='ACTIVE'",
                (rs, rowNum) -> rs.getLong(1) + ":" + rs.getString(2), userId);
        String email = user.getEmailNormalized();
        String cpf = cpfs.masked(userId).orElse(null);
        return new ExportView(user.getPublicId(), user.getDisplayName(), user.getSurname(), user.getPhoneNumber(),
                email == null ? null : maskEmail(email), cpf, user.getStatus().name(), user.getCreatedAt(),
                participations, referrals, memberships);
    }

    @Transactional
    public void anonymize(Long userId) {
        var user = users.findLockedById(userId).orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        if (!user.isActive()) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        String anonymizedPhone = "+999" + "%09d".formatted(user.getId());
        user.anonymize(anonymizedPhone);
        users.saveAndFlush(user);
        jdbc.update("UPDATE campaign_number_buyers SET buyer_first_name='Titular', buyer_surname='Anonimizado', "
                + "buyer_phone=? WHERE buyer_user_id=?", anonymizedPhone, userId);
        audits.save(new AuditEvent(userId, "PRIVACY_ANONYMIZED", "user", userId, sha256(user.getPublicId())));
    }

    private long count(String sql, Long userId) {
        Long value = jdbc.queryForObject(sql, Long.class, userId);
        return value == null ? 0 : value;
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record ExportView(String publicId, String displayName, String surname, String phoneNumber, String email,
                             String cpf, String status, Instant createdAt, long participations, long referrals,
                             List<String> memberships) {
        public ExportView { memberships = List.copyOf(memberships); }
    }
}
