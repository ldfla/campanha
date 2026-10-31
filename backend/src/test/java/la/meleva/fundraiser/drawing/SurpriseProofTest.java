package la.meleva.fundraiser.drawing;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SurpriseProofTest {
    private final SurpriseProof.Context context = new SurpriseProof.Context("9d6b0b76-7cdf-4249-ae55-58b6698d0938",1,0,0,9,Instant.parse("2026-01-01T00:00:00Z"));
    @Test void generatesTenUniqueNumbersAndDetectsTampering() {
        var proof = new SurpriseProof(new SecureRandom());
        var secret = proof.generate(0, 9);
        assertThat(secret.numbers()).containsExactly(0,1,2,3,4,5,6,7,8,9);
        assertThat(secret.saltHex()).hasSize(64);
        assertThat(secret.toString()).doesNotContain(secret.saltHex()).doesNotContain(secret.numbers().toString());
        var hash = SurpriseProof.commitment(secret,context);
        var reordered = new ArrayList<>(secret.numbers()); Collections.reverse(reordered);
        assertThat(SurpriseProof.verify(new SurpriseProof.Secret(reordered,secret.saltHex()),context,hash)).isTrue();
        assertThat(SurpriseProof.verify(new SurpriseProof.Secret(secret.numbers(),"00".repeat(32)),context,hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,context,"00".repeat(32))).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context("ebc6a12a-a0b6-41d2-b08f-63543636912f",1,0,0,9,context.committedAt()),hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context(context.campaignId(),2,0,0,9,context.committedAt()),hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context(context.campaignId(),1,1,0,9,context.committedAt()),hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context(context.campaignId(),1,0,0,10,context.committedAt()),hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context(context.campaignId(),1,0,0,9,context.committedAt().plusSeconds(1)),hash)).isFalse();
    }
    @Test void rejectsDuplicateNumbersAndInsufficientRanges() {
        var proof = new SurpriseProof(new SecureRandom());
        assertThatThrownBy(() -> proof.generate(0,8)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SurpriseProof.commitment(new SurpriseProof.Secret(java.util.Collections.nCopies(10,1),"00".repeat(32)),context)).isInstanceOf(IllegalArgumentException.class);
    }
}
