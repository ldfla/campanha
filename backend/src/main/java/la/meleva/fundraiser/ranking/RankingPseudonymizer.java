package la.meleva.fundraiser.ranking;

import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Pseudonimiza participantes para exibição pública: iniciais do nome e sobrenome
 * mais os quatro últimos dígitos do telefone. Nunca expõe nome completo ou telefone íntegro.
 */
@Component
public class RankingPseudonymizer {

    public String pseudonym(String displayName, String surname, String phoneNumber) {
        return initial(displayName) + initial(surname) + " " + lastFour(phoneNumber);
    }

    private String initial(String value) {
        if (value == null || value.isBlank()) {
            return "?.";
        }
        int codePoint = value.strip().codePointAt(0);
        return new String(Character.toChars(codePoint)).toUpperCase(Locale.ROOT) + ".";
    }

    private String lastFour(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "----";
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        return digits.length() >= 4 ? digits.substring(digits.length() - 4) : "----";
    }
}
