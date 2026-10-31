package la.meleva.fundraiser.ranking;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RankingPseudonymizerTest {

    private final RankingPseudonymizer pseudonymizer = new RankingPseudonymizer();

    @Test
    void buildsInitialsAndLastFourDigitsOnly() {
        assertThat(pseudonymizer.pseudonym("Ana", "Lima", "+55 (11) 99999-1234")).isEqualTo("A.L. 1234");
        assertThat(pseudonymizer.pseudonym("joão", "silva", "11999905678")).isEqualTo("J.S. 5678");
    }

    @Test
    void neverLeaksFullNameOrPhoneWhenDataIsMissing() {
        assertThat(pseudonymizer.pseudonym("Ana", "Lima", null)).isEqualTo("A.L. ----");
        assertThat(pseudonymizer.pseudonym("Ana", "Lima", "123")).isEqualTo("A.L. ----");
        assertThat(pseudonymizer.pseudonym(null, " ", "+5511999991234")).isEqualTo("?.?. 1234");
    }

    @Test
    void doesNotContainSurnameOrCompleteNumber() {
        String pseudonym = pseudonymizer.pseudonym("Ana", "Lima", "+5511999991234");
        assertThat(pseudonym).doesNotContain("Lima").doesNotContain("+5511999991234").contains("1234");
    }
}
