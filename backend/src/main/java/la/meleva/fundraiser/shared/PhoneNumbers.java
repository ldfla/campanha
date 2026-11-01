package la.meleva.fundraiser.shared;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Normalização e validação de telefone no formato E.164 (ITU-T E.164):
 * {@code +} seguido do código do país e do número, no máximo 15 dígitos.
 */
public final class PhoneNumbers {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final String DEFAULT_COUNTRY_CODE = "+55";

    private PhoneNumbers() {}

    /** Normaliza para E.164 e valida; lança {@link IllegalArgumentException} se inválido. */
    public static String toE164(String phone) {
        Objects.requireNonNull(phone, "phone");
        String digits = phone.replaceAll("[^\\d+]", "");
        if (!digits.startsWith("+")) {
            // 12–13 dígitos iniciados por 55 já trazem o código do país (DDD + 8/9 dígitos = 10–11).
            boolean hasCountryCode = digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13);
            digits = hasCountryCode ? "+" + digits : DEFAULT_COUNTRY_CODE + digits;
        }
        if (!E164.matcher(digits).matches()) throw new IllegalArgumentException("telefone.fora_do_padrao_e164");
        return digits;
    }

    /** Igual a {@link #toE164} para valores opcionais; devolve {@code null} quando ausente/em branco. */
    public static String optionalE164(String phone) {
        if (phone == null || phone.isBlank()) return null;
        return toE164(phone);
    }

    public static boolean isE164(String phone) {
        return phone != null && E164.matcher(phone).matches();
    }
}
