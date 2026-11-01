package la.meleva.fundraiser.compliance;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de autorização vigente (ADR-0007). Só uma autorização **verificada** e **dentro da
 * vigência** libera cobrança/prêmio em dinheiro no título de capitalização premiável.
 */
@Service
public class AuthorizationService {

    private final CampaignAuthorizationRepository authorizations;
    private final Clock clock;

    @Autowired
    public AuthorizationService(CampaignAuthorizationRepository authorizations) {
        this(authorizations, Clock.systemUTC());
    }

    AuthorizationService(CampaignAuthorizationRepository authorizations, Clock clock) {
        this.authorizations = authorizations;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean hasEffectiveAuthorization(Long campaignId) {
        Instant now = Instant.now(clock);
        return authorizations.findByCampaignId(campaignId).stream().anyMatch(authorization -> authorization.isEffectiveAt(now));
    }
}
