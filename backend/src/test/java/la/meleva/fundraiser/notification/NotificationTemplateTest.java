package la.meleva.fundraiser.notification;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class NotificationTemplateTest {
    @Test
    void templatesAreVersionedAndAvoidLinksAndSensitiveCredentials() {
        assertThat(NotificationTemplate.values()).hasSize(6);
        for (var template : NotificationTemplate.values()) {
            assertThat(template.version()).isPositive();
            assertThat(template.subject()).isNotBlank();
            assertThat(template.body()).isNotBlank().doesNotContain("http://", "https://", "senha", "token");
        }
        assertThat(NotificationTemplate.values())
                .filteredOn(template -> template.category() == NotificationTemplate.Category.ESSENTIAL)
                .hasSize(5);
        assertThat(NotificationTemplate.CAMPAIGN_MARKETING.category())
                .isEqualTo(NotificationTemplate.Category.MARKETING);
    }
}
