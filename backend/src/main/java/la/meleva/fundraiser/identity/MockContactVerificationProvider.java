package la.meleva.fundraiser.identity;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Adaptador DEMO: não transmite mensagens e aceita somente o código de teste documentado. */
@Component
@Profile({"default", "local", "test"})
public class MockContactVerificationProvider implements ContactVerificationProvider {
    private static final String DEMO_CODE = "000000";
    @Override public void sendVerification(String phoneNumber) { /* nenhum dado pessoal é registrado */ }
    @Override public boolean verify(String phoneNumber, String code) { return DEMO_CODE.equals(code); }
}
