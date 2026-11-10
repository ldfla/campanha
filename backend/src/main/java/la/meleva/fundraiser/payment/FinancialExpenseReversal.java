package la.meleva.fundraiser.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "financial_expense_reversals")
class FinancialExpenseReversal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, length = 36) private String publicId;
    @Column(name = "campaign_id", nullable = false) private Long campaignId;
    @Column(name = "expense_id", nullable = false) private Long expenseId;
    @Column(name = "actor_id", nullable = false) private Long actorId;
    @Column(name = "idempotency_key", nullable = false, length = 36) private String idempotencyKey;
    @Column(nullable = false, length = 200) private String reason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected FinancialExpenseReversal() { }
    FinancialExpenseReversal(Long campaignId, Long expenseId, Long actorId, String key, String reason, Instant createdAt) {
        this.publicId = UUID.randomUUID().toString();
        this.campaignId = campaignId;
        this.expenseId = expenseId;
        this.actorId = actorId;
        this.idempotencyKey = key;
        this.reason = reason;
        this.createdAt = createdAt;
    }
    boolean matches(Long expense, Long actor, String key, String proposedReason) {
        return expenseId.equals(expense) && actorId.equals(actor) && idempotencyKey.equals(key) && reason.equals(proposedReason);
    }
    Long id() { return id; }
    String publicId() { return publicId; }
    String reason() { return reason; }
    Instant createdAt() { return createdAt; }
    String auditHash() {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String value : new String[]{"financial-expense-reversal-v1", publicId, campaignId.toString(),
                    expenseId.toString(), actorId.toString(), idempotencyKey, reason, createdAt.toString()}) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
