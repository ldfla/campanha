package la.meleva.fundraiser.drawing;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SurpriseSecretCipherTest {
    @Test void envelopeIsRandomizedBoundToCampaignAndAuthenticated() {
        var cipher=new SurpriseSecretCipher(Base64.getEncoder().encodeToString(new byte[32]),new SecureRandom());
        var secret=new SurpriseProof.Secret(List.of(0,1,2,3,4,5,6,7,8,9),"01".repeat(32));
        var first=cipher.encrypt("campaign-a",secret); var second=cipher.encrypt("campaign-a",secret);
        assertThat(first).isNotEqualTo(second).doesNotContain(secret.saltHex());
        assertThat(cipher.decrypt("campaign-a",first)).isEqualTo(secret);
        assertThatThrownBy(()->cipher.decrypt("campaign-b",first)).isInstanceOf(BusinessException.class);
        var bytes=Base64.getDecoder().decode(first); bytes[bytes.length-1]^=1; var corrupted=Base64.getEncoder().encodeToString(bytes);
        assertThatThrownBy(()->cipher.decrypt("campaign-a",corrupted)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->cipher.decrypt("campaign-a","AA==")).isInstanceOf(BusinessException.class);
    }
    @Test void rejectsInvalidMasterKeys() {
        assertThatThrownBy(()->new SurpriseSecretCipher("",new SecureRandom())).isInstanceOf(IllegalStateException.class);
    }
}
