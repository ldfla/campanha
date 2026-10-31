package la.meleva.fundraiser.notification;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class NotificationTemplateTest {
    @Test
    void essentialTemplatesAreVersionedAndAvoidLinksAndSensitiveCredentials() {
        assertThat(NotificationTemplate.values()).hasSize(5);
        for (var template : NotificationTemplate.values()) {
            assertThat(template.version()).isPositive();
            assertThat(template.category()).isEqualTo(NotificationTemplate.Category.ESSENTIAL);
            assertThat(template.subject()).isNotBlank();
            assertThat(template.body()).isNotBlank().doesNotContain("http://", "https://", "senha", "token");
        }
    }
}
