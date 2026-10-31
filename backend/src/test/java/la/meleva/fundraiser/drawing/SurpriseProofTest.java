package la.meleva.fundraiser.drawing;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SurpriseProofTest {
    private final SurpriseProof.Context context = new SurpriseProof.Context("9d6b0b76-7cdf-4249-ae55-58b6698d0938",1,0,0,99999,Instant.parse("2026-01-01T00:00:00Z"));

    @Test void generatesTwoTiersOfUniqueNumbersAndDetectsTampering() {
        var proof = new SurpriseProof(new SecureRandom());
        var secret = proof.generate(0, 99999);
        assertThat(secret.tier1()).hasSize(SurpriseProof.TIER1_COUNT).doesNotHaveDuplicates();
        assertThat(secret.tier2()).hasSize(SurpriseProof.TIER2_COUNT).doesNotHaveDuplicates();
        assertThat(secret.numbers()).hasSize(SurpriseProof.TOTAL_COUNT).doesNotHaveDuplicates();
        assertThat(secret.saltHex()).hasSize(64);
        assertThat(secret.prizeCents(secret.tier1().getFirst())).isEqualTo(10_000L);
        assertThat(secret.prizeCents(secret.tier2().getFirst())).isEqualTo(1_000L);
        assertThat(secret.toString()).doesNotContain(secret.saltHex()).doesNotContain(secret.numbers().toString());
        var hash = SurpriseProof.commitment(secret,context);
        // Ordenação interna dos tiers não altera o compromisso.
        var reordered = new ArrayList<>(secret.tier2()); Collections.reverse(reordered);
        assertThat(SurpriseProof.verify(new SurpriseProof.Secret(secret.tier1(),reordered,secret.saltHex()),context,hash)).isTrue();
        assertThat(SurpriseProof.verify(new SurpriseProof.Secret(secret.tier1(),secret.tier2(),"00".repeat(32)),context,hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,context,"00".repeat(32))).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context("ebc6a12a-a0b6-41d2-b08f-63543636912f",1,0,0,99999,context.committedAt()),hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context(context.campaignId(),2,0,0,99999,context.committedAt()),hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context(context.campaignId(),1,1,0,99999,context.committedAt()),hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context(context.campaignId(),1,0,0,99998,context.committedAt()),hash)).isFalse();
        assertThat(SurpriseProof.verify(secret,new SurpriseProof.Context(context.campaignId(),1,0,0,99999,context.committedAt().plusSeconds(1)),hash)).isFalse();
        // Trocar a divisão de tiers invalida a prova.
        var swapped = new SurpriseProof.Secret(secret.tier2().subList(0,10), concat(secret.tier1(), secret.tier2().subList(10,100)), secret.saltHex());
        assertThat(SurpriseProof.verify(swapped,context,hash)).isFalse();
    }

    @Test void rejectsDuplicateNumbersAndInsufficientRanges() {
        var proof = new SurpriseProof(new SecureRandom());
        assertThatThrownBy(() -> proof.generate(0, SurpriseProof.TOTAL_COUNT - 2)).isInstanceOf(IllegalArgumentException.class);
        var duplicated = new SurpriseProof.Secret(java.util.Collections.nCopies(10,1), java.util.Collections.nCopies(100,1), "00".repeat(32));
        assertThatThrownBy(() -> SurpriseProof.commitment(duplicated,context)).isInstanceOf(IllegalArgumentException.class);
        var wrongSize = new SurpriseProof.Secret(java.util.List.of(1,2,3), java.util.List.of(4), "00".repeat(32));
        assertThatThrownBy(() -> SurpriseProof.commitment(wrongSize,context)).isInstanceOf(IllegalArgumentException.class);
    }

    private static java.util.List<Integer> concat(java.util.List<Integer> first, java.util.List<Integer> second) {
        var combined = new ArrayList<>(first);
        combined.addAll(second);
        return combined;
    }
}
