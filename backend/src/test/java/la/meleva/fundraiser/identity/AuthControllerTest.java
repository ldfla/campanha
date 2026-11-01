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

import static org.assertj.core.api.Assertions.assertThat;
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

    private MvcResult requestCode(String phone) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\"}"))
                .andExpect(status().isAccepted()).andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions verifyCode(String phone, String code, Cookie... cookies)
            throws Exception {
        var request = post("/auth/login/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"" + code + "\"}");
        if (cookies.length > 0) request.cookie(cookies);
        return mockMvc.perform(request);
    }

    private void grantRole(String phone, UserRole role) {
        var user = userRepository.findByPhoneNumber(phone).orElseThrow();
        var organization = organizationRepository.save(new Organization("Organização " + phone, "ASSOCIATION"));
        membershipRepository.save(new Membership(organization, user, role));
    }

    @Test
    void registerCreatesUserWithoutAuthenticatedSession() throws Exception {
        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"firstName": "João", "surname": "Silva", "phoneNumber": "+5511999990001"}
                        """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("CODE_SENT"))
                .andExpect(jsonPath("$.firstName").doesNotExist())
                .andReturn();
        assertThat(userRepository.findByPhoneNumber("+5511999990001")).isPresent();
        Cookie session = result.getResponse().getCookie("SESSION");
        if (session != null) {
            mockMvc.perform(get("/auth/me").cookie(session)).andExpect(status().isUnauthorized());
        }
    }

    /** P20-F01/F06: cadastro repetido devolve a mesma resposta e não cria segunda conta nem evento. */
    @Test
    void registerDuplicatePhoneIsIndistinguishableAndIdempotent() throws Exception {
        String first = mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"surname\":\"User\",\"phoneNumber\":\"+5511999990002\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Maria\",\"surname\":\"Outra\",\"phoneNumber\":\"+5511999990002\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertThat(second).isEqualTo(first);
        var user = userRepository.findByPhoneNumber("+5511999990002").orElseThrow();
        assertThat(user.getDisplayName()).isEqualTo("Test");
        assertThat(jdbc.queryForObject(
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
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
            authService.register(new la.meleva.fundraiser.identity.dto.RegisterRequest("Pessoa", "Teste", "+5511999990098"));
            userId.set(userRepository.findByPhoneNumber("+5511999990098").orElseThrow().getId());
            status.setRollbackOnly();
        });
        assertThat(userRepository.findByPhoneNumber("+5511999990098")).isEmpty();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type='USER' AND aggregate_id=? AND event_type='NOTIFICATION_ACCOUNT_REGISTERED_V1'",
                Long.class, userId.get())).isZero();
    }

    /** P20-F01 (PoC): conhecer o telefone não basta — solicitar código não autentica. */
    @Test
    void loginRequestWithPhoneOnlyNeverAuthenticates() throws Exception {
        AuthTestSupport.register(mockMvc, "Vítima", "Teste", "+5511999990003");
        var result = requestCode("+5511999990003");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("Vítima").doesNotContain("firstName");
        Cookie session = result.getResponse().getCookie("SESSION");
        if (session != null) {
            mockMvc.perform(get("/auth/me").cookie(session)).andExpect(status().isUnauthorized());
        }
    }

    /** P20-F06: a resposta da solicitação é idêntica para telefone cadastrado e inexistente. */
    @Test
    void antiEnumerationLoginDoesNotRevealExistence() throws Exception {
        AuthTestSupport.register(mockMvc, "Test", "User", "+5511999990013");
        String known = requestCode("+5511999990013").getResponse().getContentAsString();
        String unknown = requestCode("+5511000000001").getResponse().getContentAsString();
        assertThat(unknown).isEqualTo(known);
    }

    @Test
    void verifyWithDemoCodeCreatesSession() throws Exception {
        Cookie session = AuthTestSupport.registerAndSignIn(mockMvc, "Test", "User", "+5511999990004");
        mockMvc.perform(get("/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.surname").value("User"))
                .andExpect(jsonPath("$.phoneNumber").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("99999"))));
        assertThat(userRepository.findByPhoneNumber("+5511999990004").orElseThrow().getPhoneVerifiedAt()).isNotNull();
    }

    @Test
    void verifyRejectsWrongCodeAndUnknownPhoneWithSameError() throws Exception {
        AuthTestSupport.register(mockMvc, "Test", "User", "+5511999990014");
        verifyCode("+5511999990014", "123456").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        verifyCode("+5511999999999", AuthTestSupport.DEMO_CODE).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    /** Força bruta do código: após 5 falhas, nem o código correto é aceito durante o bloqueio. */
    @Test
    void verifyLocksAfterFiveWrongCodes() throws Exception {
        AuthTestSupport.register(mockMvc, "Test", "User", "+5511999990015");
        for (int attempt = 0; attempt < 5; attempt++) {
            verifyCode("+5511999990015", "11111" + attempt).andExpect(status().isUnauthorized());
        }
        verifyCode("+5511999990015", AuthTestSupport.DEMO_CODE).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_LOCKED"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        Cookie session = AuthTestSupport.registerAndSignIn(mockMvc, "Test", "User", "+5511999990005");

        mockMvc.perform(post("/auth/logout").cookie(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demoContactVerificationAcceptsOnlyFixedSandboxCode() throws Exception {
        AuthTestSupport.register(mockMvc, "Test", "User", "+5511999990010");

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
        AuthTestSupport.register(mockMvc, "Test", "User", phone);
        grantRole(phone, UserRole.SYSTEM_ADMIN);

        var pending = verifyCode(phone, AuthTestSupport.DEMO_CODE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_MFA_REQUIRED")).andReturn();
        Cookie pendingSession = pending.getResponse().getCookie("SESSION");
        assertThat(pendingSession).isNotNull();
        mockMvc.perform(get("/auth/me").cookie(pendingSession)).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/mfa/verify").cookie(pendingSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("SYSTEM_ADMIN"));
    }

    /** P20-F02 (PoC): MFA sem o primeiro fator na mesma sessão é rejeitado. */
    @Test
    void mfaWithoutFirstFactorInSessionIsRejected() throws Exception {
        String phone = "+5511999990016";
        AuthTestSupport.register(mockMvc, "Test", "User", phone);
        grantRole(phone, UserRole.SYSTEM_ADMIN);
        verifyCode(phone, AuthTestSupport.DEMO_CODE).andExpect(status().isForbidden());

        var attacker = mockMvc.perform(post("/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"000000\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_MFA_REQUIRED")).andReturn();
        Cookie attackerSession = attacker.getResponse().getCookie("SESSION");
        if (attackerSession != null) {
            mockMvc.perform(get("/_cx/anything").cookie(attackerSession)).andExpect(status().isUnauthorized());
        }
    }

    /** P20-F02: força bruta do segundo fator bloqueia mesmo com o código correto em seguida. */
    @Test
    void mfaLocksAfterFiveWrongCodes() throws Exception {
        String phone = "+5511999990017";
        AuthTestSupport.register(mockMvc, "Test", "User", phone);
        grantRole(phone, UserRole.SECURITY_ADMIN);
        Cookie pending = verifyCode(phone, AuthTestSupport.DEMO_CODE).andExpect(status().isForbidden())
                .andReturn().getResponse().getCookie("SESSION");
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/auth/mfa/verify").cookie(pending).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"12345" + attempt + "\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/auth/mfa/verify").cookie(pending).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"000000\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void verifyRotatesExistingSessionCookie() throws Exception {
        String phone = "+5511999990012";
        Cookie initial = AuthTestSupport.registerAndSignIn(mockMvc, "Test", "User", phone);
        MvcResult result = verifyCode(phone, AuthTestSupport.DEMO_CODE, initial)
                .andExpect(status().isOk()).andReturn();
        Cookie rotated = result.getResponse().getCookie("SESSION");
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(initial.getValue());
    }

    /** P20-F07: "55DDDNÚMERO" sem "+" identifica o mesmo titular de "+55DDDNÚMERO". */
    @Test
    void phoneWithCountryCodeWithoutPlusMapsToSameAccount() throws Exception {
        AuthTestSupport.register(mockMvc, "Test", "User", "+5511999990018");
        Cookie session = AuthTestSupport.verify(mockMvc, "5511999990018");
        mockMvc.perform(get("/auth/me").cookie(session)).andExpect(status().isOk());
        assertThat(userRepository.findByPhoneNumber("+555511999990018")).isEmpty();
    }
}
