package la.meleva.fundraiser.notification;

import java.util.Objects;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decide se um modelo pode ser despachado e mantém o opt-out de marketing.
 * Comunicação essencial nunca é bloqueada por opt-out; marketing é opcional.
 */
@Service
public class NotificationPreferenceService {
    private final NotificationPreferenceRepository preferences;
    private final UserRepository users;

    NotificationPreferenceService(NotificationPreferenceRepository preferences, UserRepository users) {
        this.preferences = preferences;
        this.users = users;
    }

    public static boolean mayDispatch(NotificationTemplate template, boolean marketingOptOut) {
        Objects.requireNonNull(template, "template");
        return template.category() != NotificationTemplate.Category.MARKETING || !marketingOptOut;
    }

    @Transactional(readOnly = true)
    public PreferenceView view(Long userId) {
        return preferences.findByUserId(requireUser(userId))
                .map(preference -> new PreferenceView(preference.isMarketingOptOut()))
                .orElseGet(() -> new PreferenceView(false));
    }

    @Transactional
    public PreferenceView update(Long userId, boolean marketingOptOut) {
        Long id = requireUser(userId);
        var preference = preferences.findByUserId(id)
                .orElseGet(() -> preferences.save(new NotificationPreference(id)));
        preference.setMarketingOptOut(marketingOptOut);
        return new PreferenceView(preferences.save(preference).isMarketingOptOut());
    }

    @Transactional(readOnly = true)
    public boolean marketingOptedOut(Long userId) {
        return preferences.findByUserId(userId).map(NotificationPreference::isMarketingOptOut).orElse(false);
    }

    private Long requireUser(Long userId) {
        if (userId == null) throw ErrorCode.VALIDATION_ERROR.toException();
        if (users.findById(userId).filter(user -> user.isActive()).isEmpty()) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
        return userId;
    }

    public record PreferenceView(boolean marketingOptOut) { }
}
