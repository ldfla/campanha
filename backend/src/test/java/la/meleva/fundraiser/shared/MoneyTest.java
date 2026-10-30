package la.meleva.fundraiser.shared;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void ofCentsCreatesCorrectAmount() {
        var money = Money.ofCents(1500);
        assertEquals(1500, money.amountCents());
    }

    @Test
    void ofReaisConvertsToCorrectCents() {
        var money = Money.ofReais(15);
        assertEquals(1500, money.amountCents());
    }

    @Test
    void addSumsCorrectly() {
        var a = Money.ofCents(1000);
        var b = Money.ofCents(500);
        assertEquals(1500, a.add(b).amountCents());
    }

    @Test
    void subtractWorksCorrectly() {
        var a = Money.ofCents(1000);
        var b = Money.ofCents(300);
        assertEquals(700, a.subtract(b).amountCents());
    }

    @Test
    void negativeAmountThrows() {
        assertThrows(IllegalArgumentException.class, () -> Money.ofCents(-1));
    }

    @Test
    void subtractBelowZeroThrows() {
        var a = Money.ofCents(100);
        var b = Money.ofCents(200);
        assertThrows(ArithmeticException.class, () -> a.subtract(b));
    }

    @Test
    void displayFormatsCorrectly() {
        assertEquals("R$ 0,00", Money.zero().toDisplay());
        assertEquals("R$ 15,50", Money.ofCents(1550).toDisplay());
        assertEquals("R$ 100,00", Money.ofReais(100).toDisplay());
    }

    @Test
    void zeroIsZero() {
        assertTrue(Money.zero().isZero());
        assertFalse(Money.ofCents(1).isZero());
    }

    @Test
    void greaterThanComparesCorrectly() {
        assertTrue(Money.ofCents(200).isGreaterThan(Money.ofCents(100)));
        assertFalse(Money.ofCents(100).isGreaterThan(Money.ofCents(200)));
    }
}
