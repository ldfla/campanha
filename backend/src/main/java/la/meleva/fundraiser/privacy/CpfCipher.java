package la.meleva.fundraiser.privacy;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifra de campo do CPF (AES-GCM) com índice de busca por HMAC-SHA256. As chaves vêm de
 * {@code app.cpf.encryption-key} e {@code app.cpf.hmac-key} (Base64, 32 bytes). Sem chaves, o
 * armazenamento de CPF fica indisponível (não há fallback inseguro).
 */
@Component
public class CpfCipher {

    private static final int NONCE_SIZE = 12;
    private static final int KEY_SIZE = 32;
    private static final byte[] AAD = "cpf-envelope-v1".getBytes(StandardCharsets.UTF_8);

    private final SecretKeySpec encryptionKey;
    private final byte[] hmacKey;
    private final SecureRandom random = new SecureRandom();

    public CpfCipher(@Value("${app.cpf.encryption-key:}") String encodedEncryptionKey,
                     @Value("${app.cpf.hmac-key:}") String encodedHmacKey) {
        this.encryptionKey = decodeKey(encodedEncryptionKey);
        this.hmacKey = decodeBytes(encodedHmacKey);
    }

    public boolean configured() {
        return encryptionKey != null && hmacKey != null;
    }

    public String encrypt(String cpf) {
        requireConfigured();
        byte[] nonce = new byte[NONCE_SIZE];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(AAD);
            byte[] encrypted = cipher.doFinal(cpf.getBytes(StandardCharsets.UTF_8));
            byte[] buffer = new byte[NONCE_SIZE + encrypted.length];
            System.arraycopy(nonce, 0, buffer, 0, NONCE_SIZE);
            System.arraycopy(encrypted, 0, buffer, NONCE_SIZE, encrypted.length);
            return Base64.getEncoder().encodeToString(buffer);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("cpf.encrypt.failed", exception);
        }
    }

    public String decrypt(String payload) {
        requireConfigured();
        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            byte[] nonce = Arrays.copyOfRange(bytes, 0, NONCE_SIZE);
            byte[] encrypted = Arrays.copyOfRange(bytes, NONCE_SIZE, bytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("cpf.decrypt.failed", exception);
        }
    }

    /** Índice determinístico para busca exata/duplicidade, sem revelar o CPF. */
    public String hmac(String cpf) {
        requireConfigured();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(cpf.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("cpf.hmac.failed", exception);
        }
    }

    private void requireConfigured() {
        if (!configured()) throw ErrorCode.CPF_NOT_CONFIGURED.toException();
    }

    private static SecretKeySpec decodeKey(String encoded) {
        byte[] key = decodeBytes(encoded);
        if (key == null) return null;
        if (key.length != KEY_SIZE) throw new IllegalStateException("cpf.encryption_key.invalid");
        return new SecretKeySpec(key, "AES");
    }

    private static byte[] decodeBytes(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return decoded.length == 0 ? null : decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("cpf.key.invalid", exception);
        }
    }
}
