package la.meleva.fundraiser.identity;

/** Verificação do segundo fator; a implementação real (TOTP/WebAuthn) depende de aprovação (G-SECURITY). */
public interface MfaCodeVerifier {
    boolean verify(MfaEnrollment enrollment, String code);
}
