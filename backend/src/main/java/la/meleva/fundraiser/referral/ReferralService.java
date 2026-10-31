package la.meleva.fundraiser.referral;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Optional;
import la.meleva.fundraiser.participation.ParticipationEntry;
import la.meleva.fundraiser.participation.ParticipationEntryRepository;
import la.meleva.fundraiser.participation.ParticipationStatus;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralService {
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private final SecureRandom random = new SecureRandom();
    private final ReferralLinkRepository linkRepository;
    private final ReferralClickRepository clickRepository;
    private final ReferralAttributionRepository attributionRepository;
    private final ParticipationEntryRepository entryRepository;

    ReferralService(ReferralLinkRepository linkRepository, ReferralClickRepository clickRepository,
                    ReferralAttributionRepository attributionRepository, ParticipationEntryRepository entryRepository) {
        this.linkRepository = linkRepository; this.clickRepository = clickRepository;
        this.attributionRepository = attributionRepository; this.entryRepository = entryRepository;
    }

    @Transactional
    public ReferralLink link(Long campaignId, Long promoterUserId) {
        return linkRepository.findByCampaignIdAndPromoterUserId(campaignId, promoterUserId)
                .orElseGet(() -> linkRepository.save(new ReferralLink(campaignId, promoterUserId, code())));
    }

    @Transactional
    public void click(Long campaignId, String code, String anonymousId) {
        ReferralLink link = active(code);
        if (!link.getCampaignId().equals(campaignId)) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        if (anonymousId == null || anonymousId.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException();
        clickRepository.save(new ReferralClick(link.getId(), hash(anonymousId)));
    }

    @Transactional
    public ReferralAttribution attribute(Long campaignId, String code, Long entryId) {
        ReferralLink link = active(code);
        return attributeKnownLink(campaignId, link, entryId);
    }

    @Transactional
    public Optional<ReferralAttribution> attributeIfValid(Long campaignId, String code, Long entryId) {
        Objects.requireNonNull(campaignId, "campaignId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(entryId, "entryId");
        return linkRepository.findByCode(code).filter(ReferralLink::isActive)
                .filter(link -> link.getCampaignId().equals(campaignId))
                .map(link -> attributeKnownLink(campaignId, link, entryId));
    }

    @Transactional(readOnly = true)
    public Optional<String> validCodeForUser(Long campaignId, String code, Long userId) {
        Objects.requireNonNull(campaignId, "campaignId"); Objects.requireNonNull(code, "code"); Objects.requireNonNull(userId, "userId");
        var link = linkRepository.findByCode(code).filter(ReferralLink::isActive)
                .filter(candidate -> candidate.getCampaignId().equals(campaignId));
        if (link.isPresent() && link.get().getPromoterUserId().equals(userId)) throw ErrorCode.REFERRAL_SELF_REFERRAL.toException();
        return link.map(ReferralLink::getCode);
    }

    private ReferralAttribution attributeKnownLink(Long campaignId, ReferralLink link, Long entryId) {
        if (!link.getCampaignId().equals(campaignId)) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        ParticipationEntry entry = entryRepository.findById(entryId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!entry.getCampaignId().equals(link.getCampaignId()) || entry.getStatus() != ParticipationStatus.ASSIGNED) throw ErrorCode.PARTICIPATION_NOT_ELIGIBLE.toException();
        if (entry.getUserId().equals(link.getPromoterUserId())) throw ErrorCode.REFERRAL_SELF_REFERRAL.toException();
        if (attributionRepository.findByCampaignIdAndEntryId(entry.getCampaignId(), entryId).isPresent()) throw ErrorCode.REFERRAL_ALREADY_ATTRIBUTED.toException();
        return attributionRepository.save(new ReferralAttribution(entry.getCampaignId(), link.getId(), entry.getUserId(), entryId));
    }

    @Transactional(readOnly = true)
    public ReferralMetrics metrics(Long campaignId, Long promoterUserId) {
        ReferralLink link = linkRepository.findByCampaignIdAndPromoterUserId(campaignId, promoterUserId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        return new ReferralMetrics(link.getCode(), clickRepository.countByReferralLinkId(link.getId()), attributionRepository.countByReferralLinkId(link.getId()));
    }

    private ReferralLink active(String code) { return linkRepository.findByCode(code).filter(ReferralLink::isActive).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException); }
    private String code() { StringBuilder value = new StringBuilder(12); for (int i=0;i<12;i++) value.append(BASE62[random.nextInt(BASE62.length)]); return value.toString(); }
    private String hash(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception ex) { throw new IllegalStateException(ex); } }
    public record ReferralMetrics(String code, long clicks, long participations) {}
}
