package la.meleva.fundraiser.privacy;

import java.util.Optional;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CPF do contemplado — coletado **apenas para recebimento do prêmio**. Cifrado em repouso
 * (AES-GCM) com índice HMAC para busca exata/duplicidade; a leitura devolve somente a máscara.
 */
@Service
public class CpfService {

    private final UserRepository users;
    private final CpfCipher cipher;
    private final AuditEventRepository audits;

    CpfService(UserRepository users, CpfCipher cipher, AuditEventRepository audits) {
        this.users = users;
        this.cipher = cipher;
        this.audits = audits;
    }

    @Transactional
    public void register(Long userId, String rawCpf) {
        Cpf cpf;
        try {
            cpf = new Cpf(rawCpf);
        } catch (IllegalArgumentException exception) {
            throw ErrorCode.CPF_INVALID.toException();
        }
        var user = users.findLockedById(userId).orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        if (!user.isActive()) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        String hmac = cipher.hmac(cpf.digits());
        users.findByCpfHmac(hmac).filter(owner -> !owner.getId().equals(userId)).ifPresent(owner -> {
            throw ErrorCode.CPF_ALREADY_REGISTERED.toException();
        });
        user.setCpf(cipher.encrypt(cpf.digits()), hmac);
        users.saveAndFlush(user);
        audits.save(new AuditEvent(userId, "CPF_REGISTERED", "user", userId, hmac));
    }

    @Transactional(readOnly = true)
    public Optional<String> masked(Long userId) {
        return users.findById(userId)
                .filter(user -> user.getCpfEncrypted() != null)
                .map(user -> new Cpf(cipher.decrypt(user.getCpfEncrypted())).masked());
    }
}
