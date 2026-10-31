package la.meleva.fundraiser.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/** Preferência de notificação por usuário. Marketing é opcional; comunicação essencial não. */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false, updatable = false, unique = true) private Long userId;
    @Column(name = "marketing_opt_out", nullable = false) private boolean marketingOptOut;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected NotificationPreference() {}

    public NotificationPreference(Long userId) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.marketingOptOut = false;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getUserId() { return userId; }
    public boolean isMarketingOptOut() { return marketingOptOut; }

    public void setMarketingOptOut(boolean optOut) {
        this.marketingOptOut = optOut;
        this.updatedAt = Instant.now();
    }
}
