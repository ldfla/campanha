package la.meleva.fundraiser.identity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mfa_enrollments")
public class MfaEnrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "secret_ciphertext", nullable = false) private byte[] secretCiphertext;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "enrolled_at", nullable = false) private Instant enrolledAt;
    @Column(name = "verified_at") private Instant verifiedAt;
    protected MfaEnrollment() {}
    public MfaEnrollment(Long userId, byte[] secretCiphertext) { this.userId = userId; this.secretCiphertext = secretCiphertext.clone(); this.status = "PENDING"; this.enrolledAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public boolean isVerified() { return "VERIFIED".equals(status); }
    public void verify() { this.status = "VERIFIED"; this.verifiedAt = Instant.now(); }
}
