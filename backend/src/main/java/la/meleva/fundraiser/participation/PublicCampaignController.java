package la.meleva.fundraiser.participation;

import java.time.Clock;
import java.time.Instant;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.campaign.CapabilityPolicy.Capability;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Estado público de compra (P06-08): o servidor decide se o CTA fica habilitado; o frontend só exibe.
 * Sem dados pessoais e sem contagens por comprador.
 */
@RestController
@RequestMapping("/campaigns/public")
public class PublicCampaignController {
    private final CampaignRepository campaigns;
    private final ParticipationService participations;
    private final ReservationCheckoutService checkout;
    private final PurchaseOfferRepository offers;
    private final Clock clock;

    @Autowired
    PublicCampaignController(CampaignRepository campaigns, ParticipationService participations,
                             ReservationCheckoutService checkout, PurchaseOfferRepository offers) {
        this(campaigns, participations, checkout, offers, Clock.systemUTC());
    }

    PublicCampaignController(CampaignRepository campaigns, ParticipationService participations,
                             ReservationCheckoutService checkout, PurchaseOfferRepository offers, Clock clock) {
        this.campaigns = campaigns;
        this.participations = participations;
        this.checkout = checkout;
        this.offers = offers;
        this.clock = clock;
    }

    @GetMapping("/{campaignPublicId}")
    @Transactional(readOnly = true)
    ResponseEntity<PublicCampaignView> state(@PathVariable String campaignPublicId) {
        Campaign campaign = campaigns.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        long available = participations.availableCount(campaignPublicId);
        CtaState cta = ctaState(campaign, available, Instant.now(clock));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new PublicCampaignView(campaign.getPublicId(), campaign.getTitle(), campaign.getMode(),
                        campaign.getMode().isDemo(), cta, cta == CtaState.OPEN, available, campaign.getEndsAt()));
    }

    private CtaState ctaState(Campaign campaign, long available, Instant now) {
        CampaignStatus status = campaign.getStatus();
        if (status == CampaignStatus.SUSPENDED || status == CampaignStatus.CANCELLED) return CtaState.SUSPENDED;
        if (!campaign.capabilities().can(Capability.ACCEPT_PARTICIPATIONS) || !checkout.payable(campaign)) {
            return CtaState.BLOCKED;
        }
        // A compra pela interface só existe via oferta publicada pelo servidor (preço nunca no cliente).
        if (status.ordinal() < CampaignStatus.OPEN.ordinal()
                || (campaign.getStartsAt() != null && now.isBefore(campaign.getStartsAt()))
                || !offers.existsByCampaignIdAndActiveTrue(campaign.getId())) {
            return CtaState.NOT_STARTED;
        }
        if (!status.acceptsParticipations() || (campaign.getEndsAt() != null && now.isAfter(campaign.getEndsAt()))) {
            return CtaState.CLOSED;
        }
        return available > 0 ? CtaState.OPEN : CtaState.SOLD_OUT;
    }

    public enum CtaState { OPEN, NOT_STARTED, SOLD_OUT, CLOSED, SUSPENDED, BLOCKED }

    public record PublicCampaignView(String id, String title, CampaignMode mode, boolean simulation, CtaState ctaState,
                              boolean purchaseEnabled, long availableNumbers, Instant endsAt) { }
}
