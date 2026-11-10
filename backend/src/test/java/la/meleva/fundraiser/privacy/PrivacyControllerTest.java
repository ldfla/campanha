package la.meleva.fundraiser.privacy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.support.SupportRequest;
import la.meleva.fundraiser.support.SupportRequestRepository;
import la.meleva.fundraiser.support.SupportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PrivacyControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired SupportRequestRepository supportRequests;
    @Autowired SupportService support;
    @Autowired PrivacyService privacy;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

    @Test
    void correctsOnlyAuthenticatedUsersNameWithValidationAndAudit() throws Exception {
        var user = users.save(new User("Nome", "Original", "+5511955000030"));
        var other = users.save(new User("Outra", "Pessoa", "+5511955000031"));
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(UserRole.PARTICIPANT));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String body = "{\"firstName\":\"  Maria  \",\"surname\":\"  Silva  \"}";

        mvc.perform(put("/privacy/me/name").with(authentication(auth)).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(put("/privacy/me/name").with(authentication(auth)).with(csrf()).contentType("application/json")
                        .content("{\"firstName\":\"  \",\"surname\":\"Silva\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/privacy/me/name").with(authentication(auth)).with(csrf()).contentType("application/json")
                        .content("{\"firstName\":\"Maria\",\"surname\":\"" + "A".repeat(151) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/privacy/me/name").with(authentication(auth)).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isNoContent());
        mvc.perform(put("/privacy/me/name").with(authentication(auth)).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isNoContent());
        assertThat(users.findById(user.getId()).orElseThrow().getDisplayName()).isEqualTo("Maria");
        assertThat(users.findById(user.getId()).orElseThrow().getSurname()).isEqualTo("Silva");
        assertThat(users.findById(other.getId()).orElseThrow().getDisplayName()).isEqualTo("Outra");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='PRIVACY_NAME_CORRECTED' AND entity_id=?",
                Long.class, user.getId())).isEqualTo(1);
        mvc.perform(put("/privacy/me/name").with(authentication(auth)).with(csrf()).contentType("application/json")
                        .content("{\"firstName\":\"Maria\",\"surname\":\"" + "A".repeat(150) + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(users.findById(user.getId()).orElseThrow().getSurname()).isEqualTo("A".repeat(150));
    }

    @Test
    void rejectsNameCorrectionForDeactivatedUser() throws Exception {
        var user = users.save(new User("Pessoa", "Inativa", "+5511955000032"));
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(UserRole.PARTICIPANT));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        privacy.anonymize(user.getId());
        mvc.perform(put("/privacy/me/name").with(authentication(auth)).with(csrf()).contentType("application/json")
                        .content("{\"firstName\":\"Maria\",\"surname\":\"Silva\"}"))
                .andExpect(status().isForbidden());
        assertThat(users.findById(user.getId()).orElseThrow().getDisplayName()).isEqualTo("Titular");
    }

    @Test
    void exportsOwnDataMaskedAndAnonymizesWithAudit() throws Exception {
        var candidate = new User("Fulano", "Silva", "+5511955000001");
        candidate.setEmailNormalized("fulano@example.test");
        var user = users.save(candidate);
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(UserRole.PARTICIPANT));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mvc.perform(get("/privacy/me/export").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Fulano"))
                .andExpect(jsonPath("$.email").value("f***@example.test"))
                .andExpect(jsonPath("$.participations").value(0));

        mvc.perform(post("/privacy/me/anonymize").with(authentication(auth)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/privacy/me/anonymize").with(authentication(auth)).with(csrf()))
                .andExpect(status().isNoContent());

        var anonymized = users.findById(user.getId()).orElseThrow();
        assertThat(anonymized.getDisplayName()).isEqualTo("Titular");
        assertThat(anonymized.getPhoneNumber()).startsWith("+999");
        assertThat(anonymized.isActive()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='PRIVACY_ANONYMIZED' AND entity_id=?",
                Long.class, user.getId())).isEqualTo(1);
    }

    @Test
    void anonymizationRedactsOnlyOwnersSupportContentAndDerivedFingerprint() throws Exception {
        var user = users.save(new User("Pessoa", "Autora", "+5511955000011"));
        var other = users.save(new User("Pessoa", "Outra", "+5511955000012"));
        var ownView = support.open(user.getId(), "Assunto pessoal", "Mensagem pessoal", null,
                "3d9505f4-7994-4f45-b228-21bca4044a22");
        var own = supportRequests.findByPublicId(ownView.id()).orElseThrow();
        var theirs = supportRequests.save(new SupportRequest(other.getId(), "Outro assunto", "Outra mensagem"));
        assertThat(jdbc.queryForObject("SELECT idempotency_fingerprint FROM support_requests WHERE id=?", String.class, own.getId()))
                .isNotBlank();
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(UserRole.PARTICIPANT));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mvc.perform(post("/privacy/me/anonymize").with(authentication(auth)).with(csrf()))
                .andExpect(status().isNoContent());
        var redacted = jdbc.queryForMap("SELECT subject,message,idempotency_key,idempotency_fingerprint,status FROM support_requests WHERE id=?", own.getId());
        assertThat(redacted.get("subject")).isEqualTo("Conteúdo removido pelo titular");
        assertThat(redacted.get("message")).isEqualTo("Conteúdo removido pelo titular");
        assertThat(redacted.get("idempotency_key")).isNull();
        assertThat(redacted.get("idempotency_fingerprint")).isNull();
        assertThat(redacted.get("status")).isEqualTo("OPEN");
        assertThat(jdbc.queryForObject("SELECT message FROM support_requests WHERE id=?", String.class, theirs.getId()))
                .isEqualTo("Outra mensagem");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='PRIVACY_ANONYMIZED' AND entity_id=?",
                Long.class, user.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='SUPPORT_REQUEST_OPENED' AND entity_id=?",
                Long.class, own.getId())).isEqualTo(1);
    }

    @Test
    void failedAnonymizationRollsBackSupportRedactionAndUserChanges() {
        var user = users.save(new User("Pessoa", "Reversão", "+5511955000013"));
        var view = support.open(user.getId(), "Assunto original", "Mensagem original", null,
                "3d9505f4-7994-4f45-b228-21bca4044a24");
        var own = supportRequests.findByPublicId(view.id()).orElseThrow();
        String originalFingerprint = jdbc.queryForObject("SELECT idempotency_fingerprint FROM support_requests WHERE id=?", String.class, own.getId());
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> {
                    privacy.anonymize(user.getId());
                    throw new IllegalStateException("falha sintética");
                })).isInstanceOf(IllegalStateException.class).hasMessage("falha sintética");
        assertThat(jdbc.queryForObject("SELECT message FROM support_requests WHERE id=?", String.class, own.getId()))
                .isEqualTo("Mensagem original");
        assertThat(jdbc.queryForObject("SELECT idempotency_fingerprint FROM support_requests WHERE id=?", String.class, own.getId()))
                .isEqualTo(originalFingerprint).isNotBlank();
        assertThat(users.findById(user.getId()).orElseThrow().isActive()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='PRIVACY_ANONYMIZED' AND entity_id=?",
                Long.class, user.getId())).isZero();
    }

    @Test
    void concurrentSupportOpeningAndAnonymizationSerializeInBothOrders() throws Exception {
        for (int i = 0; i < 4; i++) {
            var user = users.save(new User("Pessoa", "Concorrente", "+551195500002" + i));
            boolean openingFirst = i < 2;
            String key = i % 2 == 0 ? "3d9505f4-7994-4f45-b228-21bca4044a23" : null;
            var locked = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var secondStarted = new CountDownLatch(1);
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactions)
                        .executeWithoutResult(tx -> {
                            if (openingFirst) support.open(user.getId(), "Assunto", "Mensagem", null, key);
                            else privacy.anonymize(user.getId());
                            locked.countDown();
                            try {
                                if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(e);
                            }
                        }));
                try {
                    assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
                    var second = executor.submit(() -> {
                        secondStarted.countDown();
                        if (openingFirst) privacy.anonymize(user.getId());
                        else support.open(user.getId(), "Assunto", "Mensagem", null, key);
                    });
                    assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
                    org.assertj.core.api.Assertions.assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
                            .isInstanceOf(TimeoutException.class);
                    release.countDown();
                    first.get(10, TimeUnit.SECONDS);
                    if (openingFirst) {
                        second.get(10, TimeUnit.SECONDS);
                        assertThat(jdbc.queryForObject("SELECT message FROM support_requests WHERE user_id=?", String.class, user.getId()))
                                .isEqualTo("Conteúdo removido pelo titular");
                        var row = jdbc.queryForMap("SELECT subject,idempotency_key,idempotency_fingerprint FROM support_requests WHERE user_id=?", user.getId());
                        assertThat(row.get("subject")).isEqualTo("Conteúdo removido pelo titular");
                        assertThat(row.get("idempotency_key")).isNull();
                        assertThat(row.get("idempotency_fingerprint")).isNull();
                    } else {
                        org.assertj.core.api.Assertions.assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS))
                                .isInstanceOf(java.util.concurrent.ExecutionException.class)
                                .hasCauseInstanceOf(la.meleva.fundraiser.shared.ErrorCode.BusinessException.class)
                                .satisfies(error -> assertThat(((la.meleva.fundraiser.shared.ErrorCode.BusinessException) error.getCause()).errorCode())
                                        .isEqualTo(la.meleva.fundraiser.shared.ErrorCode.AUTH_RESOURCE_ACCESS_DENIED));
                        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM support_requests WHERE user_id=?", Long.class, user.getId()))
                                .isZero();
                    }
                } finally {
                    release.countDown();
                }
            }
        }
    }
}
