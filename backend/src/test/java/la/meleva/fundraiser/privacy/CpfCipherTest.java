package la.meleva.fundraiser.privacy;

import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfCipherTest {

    private static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Test void encryptsWithRandomNonceAndHmacsDeterministically() {
        var cipher = new CpfCipher(KEY, KEY);
        String encrypted = cipher.encrypt("11144477735");
        assertThat(encrypted).doesNotContain("11144477735");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("11144477735");
        assertThat(cipher.encrypt("11144477735")).isNotEqualTo(encrypted);
        assertThat(cipher.hmac("11144477735")).hasSize(64).isEqualTo(cipher.hmac("11144477735"));
        assertThat(cipher.hmac("11144477735")).isNotEqualTo(cipher.hmac("52998224725"));
    }

    @Test void withoutKeysFailsSafelyInsteadOfStoringPlaintext() {
        var cipher = new CpfCipher("", "");
        assertThat(cipher.configured()).isFalse();
        assertThatThrownBy(() -> cipher.encrypt("11144477735"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(la.meleva.fundraiser.shared.ErrorCode.CPF_NOT_CONFIGURED));
        assertThatThrownBy(() -> cipher.hmac("11144477735")).isInstanceOf(BusinessException.class);
    }
}
