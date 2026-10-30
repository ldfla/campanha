package la.meleva.fundraiser.campaign;

import la.meleva.fundraiser.campaign.dto.CreateCampaignRequest;
import la.meleva.fundraiser.campaign.dto.PrizeRequest;
import la.meleva.fundraiser.campaign.dto.UpdateCampaignRequest;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignPrizeRepository prizeRepository;

    public CampaignService(CampaignRepository campaignRepository,
                           CampaignPrizeRepository prizeRepository) {
        this.campaignRepository = campaignRepository;
        this.prizeRepository = prizeRepository;
    }

    @Transactional
    public Campaign create(CreateCampaignRequest request, Long organizationId) {
        if (campaignRepository.existsBySlug(request.slug())) {
            throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
        }
        var campaign = new Campaign(organizationId, request.title(), request.slug(), request.mode());
        if (request.description() != null) {
            campaign.setDescription(request.description());
        }
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign update(String publicId, UpdateCampaignRequest request) {
        var campaign = findByPublicId(publicId);
        if (request.title() != null) {
            campaign.setTitle(request.title());
        }
        if (request.slug() != null) {
            if (!campaign.getSlug().equals(request.slug()) && campaignRepository.existsBySlug(request.slug())) {
                throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
            }
            campaign.setSlug(request.slug());
        }
        if (request.description() != null) {
            campaign.setDescription(request.description());
        }
        if (request.startsAt() != null) {
            campaign.setStartsAt(request.startsAt());
        }
        if (request.endsAt() != null) {
            campaign.setEndsAt(request.endsAt());
        }
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign transition(String publicId, CampaignStatus targetStatus) {
        var campaign = findByPublicId(publicId);

        if (targetStatus == CampaignStatus.OPEN || targetStatus == CampaignStatus.SCHEDULED) {
            if (!campaign.getMode().isDemo()) {
                AdminGateMatrix.requireGatesForTransition(campaign.getMode(), targetStatus);
            }
        }

        campaign.transitionTo(targetStatus);
        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public Campaign findByPublicId(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
    }

    @Transactional(readOnly = true)
    public Campaign findBySlug(String slug) {
        return campaignRepository.findBySlug(slug)
                .orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
    }

    @Transactional(readOnly = true)
    public List<Campaign> findByOrganization(Long organizationId) {
        return campaignRepository.findByOrganizationId(organizationId);
    }

    @Transactional
    public CampaignPrize addPrize(String campaignPublicId, PrizeRequest request) {
        var campaign = findByPublicId(campaignPublicId);

        if (request.type() == CampaignPrize.PrizeType.CASH && !campaign.getMode().allowsCashPrizes()) {
            throw ErrorCode.PRIZE_CASH_NOT_ALLOWED.toException();
        }

        var prize = new CampaignPrize(
                campaign.getId(),
                request.type(),
                request.description(),
                request.valueCents(),
                request.quantity(),
                request.sortOrder()
        );
        return prizeRepository.save(prize);
    }

    @Transactional(readOnly = true)
    public List<CampaignPrize> listPrizes(String campaignPublicId) {
        var campaign = findByPublicId(campaignPublicId);
        return prizeRepository.findByCampaignIdOrderBySortOrder(campaign.getId());
    }

    @Transactional
    public CampaignPrize updatePrize(Long prizeId, PrizeRequest request) {
        var prize = prizeRepository.findById(prizeId)
                .orElseThrow(ErrorCode.PRIZE_NOT_FOUND::toException);
        prize.update(request.description(), request.valueCents(), request.quantity(), request.sortOrder());
        return prizeRepository.save(prize);
    }

    @Transactional
    public void deactivatePrize(Long prizeId) {
        var prize = prizeRepository.findById(prizeId)
                .orElseThrow(ErrorCode.PRIZE_NOT_FOUND::toException);
        prize.deactivate();
        prizeRepository.save(prize);
    }
}
