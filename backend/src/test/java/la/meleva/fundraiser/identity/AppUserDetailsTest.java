package la.meleva.fundraiser.identity;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AppUserDetailsTest {

    @Test
    void authoritiesMapFromRoles() {
        var details = new AppUserDetails(1L, "uuid", "+5511999990001",
                List.of(UserRole.PARTICIPANT, UserRole.PROMOTER));

        var authorities = details.getAuthorities().stream()
                .map(Object::toString).toList();
        assertTrue(authorities.contains("ROLE_PARTICIPANT"));
        assertTrue(authorities.contains("ROLE_PROMOTER"));
        assertEquals(2, authorities.size());
    }

    @Test
    void isAdminReturnsTrueForAdminRoles() {
        var admin = new AppUserDetails(1L, "uuid", "+5511999990001",
                List.of(UserRole.SYSTEM_ADMIN));
        assertTrue(admin.isAdmin());

        var secAdmin = new AppUserDetails(1L, "uuid", "+5511999990001",
                List.of(UserRole.SECURITY_ADMIN));
        assertTrue(secAdmin.isAdmin());
    }

    @Test
    void isAdminReturnsFalseForNonAdmin() {
        var participant = new AppUserDetails(1L, "uuid", "+5511999990001",
                List.of(UserRole.PARTICIPANT));
        assertFalse(participant.isAdmin());
    }

    @Test
    void hasRoleChecksCorrectly() {
        var details = new AppUserDetails(1L, "uuid", "+5511999990001",
                List.of(UserRole.FINANCE, UserRole.DRAW_AUDITOR));
        assertTrue(details.hasRole(UserRole.FINANCE));
        assertTrue(details.hasRole(UserRole.DRAW_AUDITOR));
        assertFalse(details.hasRole(UserRole.SYSTEM_ADMIN));
    }
}
