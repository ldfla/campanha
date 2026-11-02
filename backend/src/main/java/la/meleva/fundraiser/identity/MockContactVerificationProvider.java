package la.meleva.fundraiser.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador DEMO: não transmite mensagens e aceita somente o código de teste documentado.
 * Recusa iniciar com operação real habilitada (falha fechada).
 */
@Component
@Profile({"default", "local", "staging", "test"})
public class MockContactVerificationProvider implements ContactVerificationProvider {
    private static final byte[] DEMO_CODE = "000000".getBytes(StandardCharsets.US_ASCII);

    MockContactVerificationProvider(@Value("${app.live-operations-enabled:false}") boolean liveOperationsEnabled) {
        if (liveOperationsEnabled) {
            throw new IllegalStateException("contact.mock.provider.forbidden.with.live.operations");
        }
    }

    @Override public void sendVerification(String phoneNumber) { /* nenhum dado pessoal é registrado */ }

    @Override public boolean verify(String phoneNumber, String code) {
        return code != null && MessageDigest.isEqual(DEMO_CODE, code.getBytes(StandardCharsets.US_ASCII));
    }
}
