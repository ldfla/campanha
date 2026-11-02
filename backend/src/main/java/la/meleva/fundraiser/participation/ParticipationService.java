package la.meleva.fundraiser.participation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.notification.OutboxEvent;
import la.meleva.fundraiser.notification.OutboxEventRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.participation.beneficiary.BeneficiarySelection;
import la.meleva.fundraiser.participation.beneficiary.BeneficiarySelectionRepository;
import org.jspecify.annotations.Nullable;
import la.meleva.fundraiser.referral.ReferralService;
import la.meleva.fundraiser.shared.PhoneNumbers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipationService {
    private static final int MAX_NUMBERS_PER_REQUEST = 100;
    private final CampaignRepository campaignRepository;
    private final CampaignNumberRepository numberRepository;
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationEntryRepository entryRepository;
    private final EntryNumberAssignmentRepository assignmentRepository;
    private final EligibilityEventRepository eligibilityEventRepository;
    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final OutboxEventRepository outbox;
    private final BeneficiaryRepository beneficiaries;
    private final BeneficiarySelectionRepository selections;
    private final ReferralService referrals;
    private final CampaignNumberBuyerRepository buyers;
    private final NumberAllocator numberAllocator;

    ParticipationService(CampaignRepository campaignRepository, CampaignNumberRepository numberRepository,
                         ParticipationRequestRepository requestRepository, ParticipationEntryRepository entryRepository,
                         EntryNumberAssignmentRepository assignmentRepository, EligibilityEventRepository eligibilityEventRepository,
                         UserRepository userRepository, AuditEventRepository auditEventRepository,
                         OutboxEventRepository outbox, BeneficiaryRepository beneficiaries,
                         BeneficiarySelectionRepository selections, ReferralService referrals,
                         CampaignNumberBuyerRepository buyers, NumberAllocator numberAllocator) {
        this.campaignRepository = campaignRepository;
        this.numberRepository = numberRepository;
        this.requestRepository = requestRepository;
        this.entryRepository = entryRepository;
        this.assignmentRepository = assignmentRepository;
        this.eligibilityEventRepository = eligibilityEventRepository;
        this.userRepository = userRepository;
        this.auditEventRepository = auditEventRepository;
        this.outbox = outbox;
        this.beneficiaries = beneficiaries;
        this.selections = selections;
        this.referrals = referrals;
        this.buyers = buyers;
        this.numberAllocator = numberAllocator;
    }

    @Transactional
    public int initializeSeries(String campaignPublicId) {
        Campaign campaign = lockedCampaign(campaignPublicId);
        if (numberRepository.countByCampaignId(campaign.getId()) > 0) return 0;
        List<CampaignNumber> numbers = new ArrayList<>(campaign.totalNumbers());
        for (int value = campaign.getNumberMin(); value <= campaign.getNumberMax(); value++) {
            numbers.add(new CampaignNumber(campaign.getId(), "A", value));
        }
        numberRepository.saveAll(numbers);
        return numbers.size();
    }

    @Transactional
    public ParticipationAllocation request(Long userId, String campaignPublicId, String idempotencyKey, int count) {
        return request(userId, campaignPublicId, idempotencyKey, count, null);
    }

    @Transactional
    public ParticipationAllocation request(Long userId, String campaignPublicId, String idempotencyKey, int count,
                                           @Nullable String entityId) {
        return request(userId, campaignPublicId, idempotencyKey, count, entityId, null);
    }

    @Transactional
    public ParticipationAllocation request(Long userId, String campaignPublicId, String idempotencyKey, int count,
                                           @Nullable String entityId, @Nullable String referralCode) {
        if (userId == null || count < 1 || count > MAX_NUMBERS_PER_REQUEST || idempotencyKey == null
                || !idempotencyKey.matches("^[0-9a-fA-F-]{36}$")) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        Campaign campaign = lockedCampaign(campaignPublicId);
        // P20-F04: a participação sem cobrança é exclusiva de DEMO; modos pagos exigem checkout conciliado.
        if (!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_LIVE_OPERATIONS_DISABLED.toException();
        if (!campaign.getStatus().acceptsParticipations()) throw ErrorCode.PARTICIPATION_CAMPAIGN_CLOSED.toException();
        if (!userRepository.findById(userId).filter(la.meleva.fundraiser.identity.User::isActive).isPresent()) {
            throw ErrorCode.PARTICIPATION_NOT_ELIGIBLE.toException();
        }
        var now = java.time.Instant.now();
        if ((campaign.getStartsAt() != null && now.isBefore(campaign.getStartsAt()))
                || (campaign.getEndsAt() != null && now.isAfter(campaign.getEndsAt()))) {
            throw ErrorCode.PARTICIPATION_CAMPAIGN_CLOSED.toException();
        }
        var existing = requestRepository.findByCampaignIdAndUserIdAndIdempotencyKey(campaign.getId(), userId, idempotencyKey);
        if (existing.isPresent()) {
            var prior = existing.get();
            var priorEntity = selections.findByRequestId(prior.getId())
                    .flatMap(selection -> beneficiaries.findById(selection.getBeneficiaryId()))
                    .map(BeneficiaryEntity::getPublicId).orElse(null);
            if (prior.getRequestedCount() != count || !java.util.Objects.equals(priorEntity, entityId)) {
                throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
            }
            return allocationFor(prior);
        }
        if (entityId == null || entityId.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException();
        var beneficiary = beneficiaries.findByPublicIdAndCampaignIdAndActiveTrue(entityId, campaign.getId())
                .orElseThrow(ErrorCode.VALIDATION_ERROR::toException);
        var buyer = userRepository.findById(userId).orElseThrow(ErrorCode.PARTICIPATION_NOT_ELIGIBLE::toException);
        String referrerPhone = referralCode == null || referralCode.isBlank() ? null
                : referrals.promoterUserIdForActiveCode(campaign.getId(), referralCode)
                        .filter(promoterId -> !promoterId.equals(userId))
                        .flatMap(userRepository::findById)
                        .map(la.meleva.fundraiser.identity.User::getPhoneNumber)
                        .map(ParticipationService::e164OrNull).orElse(null);
        if (entryRepository.countByCampaignIdAndUserIdAndStatusNotIn(campaign.getId(), userId,
                List.of(ParticipationStatus.REJECTED, ParticipationStatus.INVALIDATED)) + count > campaign.getMaxEntriesPerUser()) {
            throw ErrorCode.PARTICIPATION_LIMIT_EXCEEDED.toException();
        }
        ParticipationRequest request;
        try {
            request = requestRepository.saveAndFlush(new ParticipationRequest(campaign.getId(), userId, idempotencyKey, count));
        } catch (DataIntegrityViolationException exception) {
            return allocationFor(requestRepository.findByCampaignIdAndUserIdAndIdempotencyKey(campaign.getId(), userId, idempotencyKey)
                    .orElseThrow(ErrorCode.IDEMPOTENCY_CONFLICT::toException));
        }
        List<CampaignNumber> available = numberAllocator.lockAvailable(campaign, count);
        if (available.size() != count) throw ErrorCode.NUMBER_POOL_EXHAUSTED.toException();
        String modality = modality(count);
        List<ParticipationAllocation.AssignedNumber> assigned = new ArrayList<>(count);
        for (CampaignNumber number : available) {
            ParticipationEntry entry = entryRepository.save(new ParticipationEntry(campaign.getId(), userId, request.getId(), "DEMO_SIMULATION"));
            entry.markAssigned();
            number.assignTo(entry.getId());
            number.recordSale(now, modality, request.getPublicId(), beneficiary.getName(), referrerPhone);
            buyers.save(new CampaignNumberBuyer(number.getId(), campaign.getId(), userId,
                    e164OrFail(buyer.getPhoneNumber()), buyer.getDisplayName(), buyer.getSurname()));
            assignmentRepository.save(new EntryNumberAssignment(entry.getId(), number.getId()));
            eligibilityEventRepository.save(new EligibilityEvent(entry.getId(), 1, "ELIGIBLE", "DEMO_SIMULATION"));
            if (referralCode != null && !referralCode.isBlank()) {
                referrals.attributeIfValid(campaign.getId(), referralCode, entry.getId());
            }
            assigned.add(new ParticipationAllocation.AssignedNumber(entry.getPublicId(), number.getSeriesCode(), number.getNumberValue(), format(number.getNumberValue())));
        }
        request.markAssigned();
        if (campaign.getMode().isDemo()) {
            outbox.save(new OutboxEvent("PARTICIPATION_REQUEST", request.getId(),
                    "NOTIFICATION_PARTICIPATION_VALID_V1", "{}"));
        }
        selections.save(new BeneficiarySelection(request.getId(),
                java.util.Objects.requireNonNull(beneficiary.getId(), "beneficiary.id")));
        return new ParticipationAllocation(request.getPublicId(), assigned);
    }

    private static String e164OrFail(String phone) {
        try { return PhoneNumbers.toE164(phone); }
        catch (RuntimeException exception) { throw ErrorCode.VALIDATION_ERROR.toException(); }
    }

    private static String e164OrNull(String phone) {
        try { return PhoneNumbers.optionalE164(phone); }
        catch (RuntimeException exception) { return null; }
    }

    /** Modalidade de compra derivada da quantidade, para auditoria. */
    static String modality(int count) {
        return switch (count) {
            case 1 -> "UNIT";
            case 10 -> "COMBO_10";
            case 100 -> "COMBO_100";
            case 1000 -> "COMBO_1000";
            case 5000 -> "COMBO_5000";
            default -> "CUSTOM";
        };
    }

    @Transactional(readOnly = true)
    public long availableCount(String campaignPublicId) {
        return numberRepository.countByCampaignIdAndStatus(campaign(campaignPublicId).getId(), NumberStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public Page<NumberAvailability> numbers(String campaignPublicId, Pageable pageable) {
        return numberRepository.findByCampaignId(campaign(campaignPublicId).getId(), pageable)
                .map(number -> new NumberAvailability(number.getSeriesCode(), number.getNumberValue(),
                        format(number.getNumberValue()), number.getStatus()));
    }

    @Transactional
    public int freezeAssignedNumbers(String campaignPublicId) {
        Campaign campaign = lockedCampaign(campaignPublicId);
        if (campaign.getStatus() != CampaignStatus.LOCKED && campaign.getStatus() != CampaignStatus.DRAW_PENDING) {
            throw ErrorCode.NUMBER_FROZEN.toException();
        }
        List<CampaignNumber> numbers = numberRepository.findByCampaignIdAndStatus(campaign.getId(), NumberStatus.ASSIGNED);
        List<ParticipationEntry> entries = entryRepository.findByCampaignIdAndStatus(campaign.getId(), ParticipationStatus.ASSIGNED);
        numbers.forEach(CampaignNumber::freeze);
        entries.forEach(ParticipationEntry::freeze);
        return numbers.size();
    }

    @Transactional
    public void invalidateFrozenEntry(Long actorId, String campaignPublicId, Long entryId, String reason) {
        if (reason == null || reason.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException();
        Campaign campaign = lockedCampaign(campaignPublicId);
        ParticipationEntry entry = entryRepository.findById(entryId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!campaign.getId().equals(entry.getCampaignId()) || entry.getStatus() != ParticipationStatus.FROZEN) {
            throw ErrorCode.NUMBER_FROZEN.toException();
        }
        entry.invalidate();
        numberRepository.findByEntryId(entryId).forEach(CampaignNumber::invalidate);
        auditEventRepository.save(new AuditEvent(actorId, "PARTICIPATION_INVALIDATED", "participation_entry", entryId,
                sha256(campaignPublicId + ":" + entryId + ":" + reason)));
    }

    private ParticipationAllocation allocationFor(ParticipationRequest request) {
        List<ParticipationAllocation.AssignedNumber> numbers = entryRepository.findByRequestId(request.getId()).stream()
                .map(entry -> assignmentRepository.findByIdEntryId(entry.getId())
                        .map(assignment -> numberRepository.findById(assignment.getId().campaignNumberId()).orElseThrow(ErrorCode.NUMBER_NOT_AVAILABLE::toException))
                        .map(number -> new ParticipationAllocation.AssignedNumber(entry.getPublicId(), number.getSeriesCode(), number.getNumberValue(), format(number.getNumberValue())))
                        .orElseThrow(ErrorCode.NUMBER_NOT_AVAILABLE::toException))
                .toList();
        return new ParticipationAllocation(request.getPublicId(), numbers);
    }

    private Campaign campaign(String publicId) {
        return campaignRepository.findByPublicId(publicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
    }

    private Campaign lockedCampaign(String publicId) {
        return campaignRepository.findLockedByPublicId(publicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
    }

    private String format(int number) { return "%05d".formatted(number); }

    private String sha256(String input) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    public record NumberAvailability(String seriesCode, int numberValue, String formattedNumber, NumberStatus status) {}
}
