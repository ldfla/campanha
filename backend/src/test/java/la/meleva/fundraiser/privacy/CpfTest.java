package la.meleva.fundraiser.privacy;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    @Test void validatesCheckDigitsAndMasksForDisplay() {
        assertThat(new Cpf("111.444.777-35").digits()).isEqualTo("11144477735");
        assertThat(new Cpf("11144477735").masked()).isEqualTo("***.***.777-35");
        assertThat(new Cpf("52998224725").masked()).isEqualTo("***.***.247-25");
    }

    @Test void rejectsInvalidValues() {
        assertThatThrownBy(() -> new Cpf("111.444.777-00")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cpf("11111111111")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cpf("123")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cpf(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
