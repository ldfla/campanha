package la.meleva.fundraiser.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador DEMO de MFA: aceita somente o código de teste documentado. Existe apenas nos perfis
 * locais e recusa iniciar com operação real habilitada (falha fechada).
 */
@Component
@Profile({"default", "local", "test"})
class DemoMfaCodeVerifier implements MfaCodeVerifier {
    private static final byte[] DEMO_CODE = "000000".getBytes(StandardCharsets.US_ASCII);

    DemoMfaCodeVerifier(@Value("${app.live-operations-enabled:false}") boolean liveOperationsEnabled) {
        if (liveOperationsEnabled) {
            throw new IllegalStateException("mfa.demo.verifier.forbidden.with.live.operations");
        }
    }

    @Override
    public boolean verify(MfaEnrollment enrollment, String code) {
        return code != null && MessageDigest.isEqual(DEMO_CODE, code.getBytes(StandardCharsets.US_ASCII));
    }
}
