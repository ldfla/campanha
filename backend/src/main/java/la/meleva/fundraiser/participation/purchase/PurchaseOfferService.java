package la.meleva.fundraiser.participation.purchase;

import java.util.List;
import java.util.Objects;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ofertas de compra por campanha. O preço é sempre definido no servidor; o cliente apenas
 * escolhe uma oferta publicada. Nenhuma oferta é criada ou reutilizada fora do modo DEMO.
 */
@Service
public class PurchaseOfferService {

    private static final int MAX_OFFERS = 100;

    private final CampaignRepository campaigns;
    private final PurchaseOfferRepository offers;
    private final MembershipRepository memberships;
    private final UserRepository users;

    PurchaseOfferService(CampaignRepository campaigns, PurchaseOfferRepository offers,
                         MembershipRepository memberships, UserRepository users) {
        this.campaigns = Objects.requireNonNull(campaigns, "campaigns");
        this.offers = Objects.requireNonNull(offers, "offers");
        this.memberships = Objects.requireNonNull(memberships, "memberships");
        this.users = Objects.requireNonNull(users, "users");
    }

    @Transactional(readOnly = true)
    public List<OfferView> list(String campaignPublicId) {
        Objects.requireNonNull(campaignPublicId, "campaignPublicId");
        var campaign = campaigns.findByPublicId(campaignPublicId)
                .orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        return offers.findByCampaignIdAndActiveTrueOrderByIdAsc(campaign.getId(), PageRequest.of(0, MAX_OFFERS))
                .stream().map(PurchaseOfferService::view).toList();
    }

    @Transactional
    public OfferView create(Long actorId, String campaignPublicId, int quantity, long totalCents) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(campaignPublicId, "campaignPublicId");
        if (quantity < 1 || quantity > 100 || totalCents < 1) throw ErrorCode.VALIDATION_ERROR.toException();
        var campaign = campaigns.findLockedByPublicId(campaignPublicId)
                .orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        var actor = users.findById(actorId).filter(user -> user.isActive())
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        boolean allowed = memberships.findActiveByUserAndOrganization(actor.getId(), campaign.getOrganizationId()).stream()
                .anyMatch(membership -> membership.getRole() == UserRole.CAMPAIGN_MANAGER
                        || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        if (!campaign.getStatus().isEditable()) throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
        return view(offers.save(new PurchaseOffer(campaign.getId(), quantity, totalCents)));
    }

    private static OfferView view(PurchaseOffer offer) {
        return new OfferView(offer.getPublicId(), offer.getQuantity(), offer.getTotalCents(), "BRL");
    }

    public record OfferView(String id, int quantity, long totalCents, String currency) { }
}
