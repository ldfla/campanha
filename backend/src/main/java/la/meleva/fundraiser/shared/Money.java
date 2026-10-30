package la.meleva.fundraiser.shared;

import java.util.Objects;

public record Money(long amountCents) {

    public static final String CURRENCY = "BRL";

    public Money {
        if (amountCents < 0) {
            throw new IllegalArgumentException("Valor negativo não permitido");
        }
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public static Money ofReais(long reais) {
        return new Money(Math.multiplyExact(reais, 100));
    }

    public static Money zero() {
        return new Money(0);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other);
        return new Money(Math.addExact(amountCents, other.amountCents));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other);
        long result = Math.subtractExact(amountCents, other.amountCents);
        if (result < 0) {
            throw new ArithmeticException("Subtração resultaria em valor negativo");
        }
        return new Money(result);
    }

    public Money multiply(int factor) {
        return new Money(Math.multiplyExact(amountCents, factor));
    }

    public boolean isZero() {
        return amountCents == 0;
    }

    public boolean isGreaterThan(Money other) {
        return amountCents > other.amountCents;
    }

    public String toDisplay() {
        long reais = amountCents / 100;
        long centavos = amountCents % 100;
        return String.format("R$ %d,%02d", reais, centavos);
    }
}
