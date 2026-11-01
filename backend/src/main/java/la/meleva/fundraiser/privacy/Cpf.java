package la.meleva.fundraiser.privacy;

/**
 * CPF do contemplado — usado **somente** para recebimento do prêmio. Validado por dígitos
 * verificadores; nunca exposto em claro na API (a leitura devolve a máscara).
 */
public record Cpf(String digits) {

    public Cpf {
        String normalized = digits == null ? "" : digits.replaceAll("\\D", "");
        if (!isValid(normalized)) throw new IllegalArgumentException("cpf.invalid");
        digits = normalized;
    }

    public String masked() {
        return "***.***." + digits.substring(6, 9) + "-" + digits.substring(9, 11);
    }

    static boolean isValid(String value) {
        if (value.length() != 11 || value.chars().distinct().count() == 1) return false;
        return checkDigit(value, 9) == value.charAt(9) - '0' && checkDigit(value, 10) == value.charAt(10) - '0';
    }

    private static int checkDigit(String value, int length) {
        int sum = 0;
        int weight = length + 1;
        for (int index = 0; index < length; index++) {
            sum += (value.charAt(index) - '0') * (weight--);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
