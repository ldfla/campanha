package la.meleva.fundraiser.participation;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import la.meleva.fundraiser.shared.PhoneNumbers;

/**
 * PII do comprador de um número, isolada em tabela própria (1:1). Guarda o snapshot de auditoria
 * e um pseudônimo (iniciais + 4 últimos dígitos) para exibição sem expor identidade integral.
 * Telefone sempre em E.164.
 */
@Entity
@Table(name = "campaign_number_buyers")
public class CampaignNumberBuyer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "number_id", nullable = false, updatable = false, unique = true) private Long numberId;
    @Column(name = "campaign_id", nullable = false, updatable = false) private Long campaignId;
    @Column(name = "buyer_user_id", nullable = false, updatable = false) private Long buyerUserId;
    @Column(name = "buyer_phone", nullable = false, length = 20) private String buyerPhone;
    @Column(name = "buyer_first_name", nullable = false, length = 100) private String buyerFirstName;
    @Column(name = "buyer_surname", nullable = false, length = 100) private String buyerSurname;
    @Column(name = "buyer_pseudonym", nullable = false, length = 40) private String buyerPseudonym;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected CampaignNumberBuyer() {}

    public CampaignNumberBuyer(Long numberId, Long campaignId, Long buyerUserId, String phone,
                               String firstName, String surname) {
        this.numberId = Objects.requireNonNull(numberId, "numberId");
        this.campaignId = Objects.requireNonNull(campaignId, "campaignId");
        this.buyerUserId = Objects.requireNonNull(buyerUserId, "buyerUserId");
        this.buyerPhone = PhoneNumbers.toE164(Objects.requireNonNull(phone, "phone"));
        this.buyerFirstName = Objects.requireNonNull(firstName, "firstName").strip();
        this.buyerSurname = Objects.requireNonNull(surname, "surname").strip();
        this.buyerPseudonym = pseudonym(this.buyerFirstName, this.buyerSurname, this.buyerPhone);
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getNumberId() { return numberId; }
    public Long getCampaignId() { return campaignId; }
    public Long getBuyerUserId() { return buyerUserId; }
    public String getBuyerPhone() { return buyerPhone; }
    public String getBuyerFirstName() { return buyerFirstName; }
    public String getBuyerSurname() { return buyerSurname; }
    public String getBuyerPseudonym() { return buyerPseudonym; }

    static String pseudonym(String firstName, String surname, String phone) {
        return initial(firstName) + initial(surname) + " " + lastFour(phone);
    }

    private static String initial(String value) {
        if (value == null || value.isBlank()) return "?.";
        int codePoint = value.strip().codePointAt(0);
        return new String(Character.toChars(codePoint)).toUpperCase(Locale.ROOT) + ".";
    }

    private static String lastFour(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() >= 4 ? digits.substring(digits.length() - 4) : "----";
    }
}
