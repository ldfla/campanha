package la.meleva.fundraiser.identity;

import jakarta.servlet.http.Cookie;
import la.meleva.fundraiser.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired private AuthService authService;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactions;

    private Cookie[] performRegister(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"firstName": "Test", "surname": "User", "phoneNumber": "%s"}
                        """.formatted(phone)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getCookies();
    }

    private Cookie sessionCookie(Cookie[] cookies) {
        return Arrays.stream(cookies)
                .filter(c -> "SESSION".equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("SESSION cookie not found"));
    }

    @Test
    void registerCreatesUserAndReturnsCreated() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"firstName": "João", "surname": "Silva", "phoneNumber": "+5511999990001"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("João"))
                .andExpect(jsonPath("$.surname").value("Silva"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.roles[0]").value("PARTICIPANT"));
    }

    @Test
    void registerRejectsDuplicatePhone() throws Exception {
        performRegister("+5511999990002");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"firstName": "Maria", "surname": "Outra", "phoneNumber": "+5511999990002"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_DUPLICATE_CREDENTIAL"));
        var user = userRepository.findByPhoneNumber("+5511999990002").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type='USER' AND aggregate_id=? AND event_type='NOTIFICATION_ACCOUNT_REGISTERED_V1' AND payload_json=JSON_OBJECT()",
                Long.class, user.getId())).isEqualTo(1L);
    }

    @Test
    void registerRejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"firstName": "", "surname": "", "phoneNumber": "abc"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registrationAndNotificationEventRollbackTogether() {
        var userId = new java.util.concurrent.atomic.AtomicReference<Long>();
        try {
            new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
                authService.register(new la.meleva.fundraiser.identity.dto.RegisterRequest("Pessoa", "Teste", "+5511999990098"));
                userId.set(userRepository.findByPhoneNumber("+5511999990098").orElseThrow().getId());
                status.setRollbackOnly();
            });
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        org.assertj.core.api.Assertions.assertThat(userRepository.findByPhoneNumber("+5511999990098")).isEmpty();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type='USER' AND aggregate_id=? AND event_type='NOTIFICATION_ACCOUNT_REGISTERED_V1'",
                Long.class, userId.get())).isZero();
    }

    @Test
    void loginReturnsOkForExistingUser() throws Exception {
        performRegister("+5511999990003");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"phoneNumber": "+5511999990003"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"));
    }

    @Test
    void loginRejectsUnknownPhone() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"phoneNumber": "+5511999999999"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentUserWhenAuthenticated() throws Exception {
        Cookie[] cookies = performRegister("+5511999990004");
        Cookie session = sessionCookie(cookies);

        mockMvc.perform(get("/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.surname").value("User"));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        Cookie[] cookies = performRegister("+5511999990005");
        Cookie session = sessionCookie(cookies);

        mockMvc.perform(post("/auth/logout").cookie(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void antiEnumerationLoginDoesNotRevealExistence() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"phoneNumber": "+5511000000001"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void demoContactVerificationAcceptsOnlyFixedSandboxCode() throws Exception {
        performRegister("+5511999990010");

        mockMvc.perform(post("/auth/verify-contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"phoneNumber": "+5511999990010", "code": "000000"}
                        """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/verify-contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"phoneNumber": "+5511999990010", "code": "123456"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void systemAdminMustCompleteDemoMfaBeforeSessionIsCreated() throws Exception {
        String phone = "+5511999990011";
        performRegister(phone);
        var user = userRepository.findByPhoneNumber(phone).orElseThrow();
        var organization = organizationRepository.save(new Organization("Organização MFA", "ASSOCIATION"));
        membershipRepository.save(new Membership(organization, user, UserRole.SYSTEM_ADMIN));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{" + "\"phoneNumber\":\"" + phone + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_MFA_REQUIRED"));
        mockMvc.perform(post("/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"phoneNumber\":\"" + phone + "\",\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("SYSTEM_ADMIN"));
    }

    @Test
    void loginRotatesExistingSessionCookie() throws Exception {
        String phone = "+5511999990012";
        Cookie initial = sessionCookie(performRegister(phone));
        MvcResult result = mockMvc.perform(post("/auth/login").cookie(initial)
                        .contentType(MediaType.APPLICATION_JSON).content("{" + "\"phoneNumber\":\"" + phone + "\"}"))
                .andExpect(status().isOk()).andReturn();
        Cookie rotated = sessionCookie(result.getResponse().getCookies());
        org.assertj.core.api.Assertions.assertThat(rotated.getValue()).isNotEqualTo(initial.getValue());
    }
}
