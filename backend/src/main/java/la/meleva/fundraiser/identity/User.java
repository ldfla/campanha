package la.meleva.fundraiser.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private String publicId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "surname", nullable = false)
    private String surname;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email_normalized", length = 320)
    private String emailNormalized;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    protected User() {}

    public User(String displayName, String surname, String phoneNumber) {
        this.publicId = UUID.randomUUID().toString();
        this.displayName = displayName;
        this.surname = surname;
        this.phoneNumber = phoneNumber;
        this.status = UserStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }

    public String getPublicId() { return publicId; }

    public String getDisplayName() { return displayName; }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.updatedAt = Instant.now();
    }

    public String getSurname() { return surname; }

    public void setSurname(String surname) {
        this.surname = surname;
        this.updatedAt = Instant.now();
    }

    public String getPhoneNumber() { return phoneNumber; }

    public String getEmailNormalized() { return emailNormalized; }

    public void setEmailNormalized(String email) {
        this.emailNormalized = email;
        this.emailVerifiedAt = null;
        this.updatedAt = Instant.now();
    }

    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }

    public Instant getPhoneVerifiedAt() { return phoneVerifiedAt; }

    public void markPhoneVerified() {
        this.phoneVerifiedAt = Instant.now();
        this.updatedAt = this.phoneVerifiedAt;
    }

    public UserStatus getStatus() { return status; }

    public boolean isActive() { return status == UserStatus.ACTIVE; }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    /** Remove PII do titular (LGPD): mantém o registro e a trilha, substitui os dados pessoais. */
    public void anonymize(String anonymizedPhone) {
        this.displayName = "Titular";
        this.surname = "Anonimizado";
        this.phoneNumber = anonymizedPhone;
        this.emailNormalized = null;
        this.emailVerifiedAt = null;
        this.phoneVerifiedAt = null;
        this.status = UserStatus.DEACTIVATED;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public int getVersion() { return version; }

    public enum UserStatus {
        ACTIVE, SUSPENDED, DEACTIVATED
    }
}
