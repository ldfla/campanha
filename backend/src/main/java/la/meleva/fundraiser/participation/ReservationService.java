package la.meleva.fundraiser.participation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.notification.OutboxEvent;
import la.meleva.fundraiser.notification.OutboxEventRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.participation.beneficiary.BeneficiarySelection;
import la.meleva.fundraiser.participation.beneficiary.BeneficiarySelectionRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reserva interna para ensaios DEMO; não cria cobrança nem confirma pagamento. */
@Service
public class ReservationService {
    private static final Duration TTL = Duration.ofMinutes(15);
    private final CampaignRepository campaigns;
    private final CampaignNumberRepository numbers;
    private final ParticipationRequestRepository requests;
    private final ParticipationEntryRepository entries;
    private final ReservationLeaseRepository leases;
    private final UserRepository users;
    private final OutboxEventRepository outbox;
    private final EntityManager entityManager;
    private final Clock clock;
    private final BeneficiaryRepository beneficiaries;
    private final BeneficiarySelectionRepository selections;

    @Autowired
    public ReservationService(CampaignRepository campaigns, CampaignNumberRepository numbers,
                              ParticipationRequestRepository requests, ParticipationEntryRepository entries,
                              ReservationLeaseRepository leases, UserRepository users,
                              OutboxEventRepository outbox, EntityManager entityManager,
                              BeneficiaryRepository beneficiaries, BeneficiarySelectionRepository selections) {
        this(campaigns, numbers, requests, entries, leases, users, outbox, entityManager, beneficiaries, selections, Clock.systemUTC());
    }

    ReservationService(CampaignRepository campaigns, CampaignNumberRepository numbers,
                       ParticipationRequestRepository requests, ParticipationEntryRepository entries,
                       ReservationLeaseRepository leases, UserRepository users,
                       OutboxEventRepository outbox, EntityManager entityManager,
                       BeneficiaryRepository beneficiaries, BeneficiarySelectionRepository selections, Clock clock) {
        this.campaigns = campaigns; this.numbers = numbers; this.requests = requests; this.entries = entries;
        this.leases = leases; this.users = users; this.outbox = outbox; this.entityManager = entityManager;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.beneficiaries = Objects.requireNonNull(beneficiaries, "beneficiaries");
        this.selections = Objects.requireNonNull(selections, "selections");
    }

    @Transactional
    public ReservationView reserveDemo(Long userId, String campaignPublicId, String key, int count) {
        return reserveDemo(userId, campaignPublicId, key, count, null);
    }

    @Transactional
    public ReservationView reserveDemo(Long userId, String campaignPublicId, String key, int count, @Nullable String entityId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(campaignPublicId, "campaignPublicId");
        Objects.requireNonNull(key, "key");
        if (count < 1 || count > 100 || !key.matches("^[0-9a-fA-F-]{36}$")) throw ErrorCode.VALIDATION_ERROR.toException();
        var campaign = campaigns.findLockedByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if (!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_OPERATION_BLOCKED.toException();
        if (!campaign.getStatus().acceptsParticipations()) throw ErrorCode.PARTICIPATION_CAMPAIGN_CLOSED.toException();
        if (users.findById(userId).filter(user -> user.isActive()).isEmpty()) throw ErrorCode.PARTICIPATION_NOT_ELIGIBLE.toException();
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        if ((campaign.getStartsAt() != null && now.isBefore(campaign.getStartsAt()))
                || (campaign.getEndsAt() != null && now.isAfter(campaign.getEndsAt()))) throw ErrorCode.PARTICIPATION_CAMPAIGN_CLOSED.toException();
        var prior = requests.findByCampaignIdAndUserIdAndIdempotencyKey(campaign.getId(), userId, key);
        if (prior.isPresent()) {
            var priorEntity = selections.findByRequestId(prior.get().getId())
                    .flatMap(selection -> beneficiaries.findById(selection.getBeneficiaryId()))
                    .map(BeneficiaryEntity::getPublicId).orElse(null);
            if (prior.get().getRequestedCount() != count || prior.get().getStatus() != ParticipationStatus.REQUESTED) {
                throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
            }
            if (!Objects.equals(priorEntity, entityId)) throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
            return view(prior.get());
        }
        if (entityId == null || entityId.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException();
        var beneficiary = beneficiaries.findByPublicIdAndCampaignIdAndActiveTrue(entityId, campaign.getId())
                .orElseThrow(ErrorCode.VALIDATION_ERROR::toException);
        long current = entries.countByCampaignIdAndUserIdAndStatusNotIn(campaign.getId(), userId,
                List.of(ParticipationStatus.REJECTED, ParticipationStatus.INVALIDATED));
        if (current + count > campaign.getMaxEntriesPerUser()) throw ErrorCode.PARTICIPATION_LIMIT_EXCEEDED.toException();
        var available = numbers.lockAvailableByCampaignId(campaign.getId(), PageRequest.of(0, count));
        if (available.size() != count) throw ErrorCode.NUMBER_POOL_EXHAUSTED.toException();
        var request = requests.saveAndFlush(new ParticipationRequest(campaign.getId(), userId, key, count));
        selections.save(new BeneficiarySelection(request.getId(), beneficiary.getId()));
        Instant expiresAt = now.plus(TTL);
        for (var number : available) {
            var entry = entries.save(new ParticipationEntry(campaign.getId(), userId, request.getId(), "DEMO_RESERVATION"));
            number.reserveFor(entry.getId());
            leases.save(new ReservationLease(campaign.getId(), number.getId(), request.getId(), expiresAt));
        }
        outbox.save(new OutboxEvent("RESERVATION_REQUEST", request.getId(), "RESERVATION_EXPIRY_REQUESTED", "{}", expiresAt));
        return view(request);
    }

    @Transactional
    public boolean expireByRequestId(Long requestId, Instant now) {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(now, "now");
        var request = requests.findById(requestId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        campaigns.findLockedById(request.getCampaignId()).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        entityManager.refresh(request, LockModeType.PESSIMISTIC_WRITE);
        if (request.getStatus() != ParticipationStatus.REQUESTED) return true;
        var active = leases.findByRequestIdAndStatusOrderByIdAsc(requestId, "ACTIVE");
        if (active.size() != request.getRequestedCount()) throw new IllegalStateException("reservation.lease_count_invalid");
        if (active.stream().anyMatch(lease -> !lease.isExpiredAt(now))) return false;
        for (var lease : active) {
            var number = numbers.findById(lease.getNumberId()).orElseThrow(ErrorCode.NUMBER_NOT_AVAILABLE::toException);
            Long entryId = Objects.requireNonNull(number.getEntryId(), "reservation.entryId");
            var entry = entries.findById(entryId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
            if (!requestId.equals(entry.getRequestId()) || !request.getCampaignId().equals(number.getCampaignId())) {
                throw new IllegalStateException("reservation.entry_mismatch");
            }
            number.releaseReservation(entryId);
            entry.rejectReservation();
            lease.expire(now);
        }
        request.rejectReservation();
        return true;
    }

    @Transactional(readOnly = true)
    public String charityForRequest(Long requestId) {
        return selections.findByRequestId(requestId)
                .flatMap(selection -> beneficiaries.findById(selection.getBeneficiaryId()))
                .map(BeneficiaryEntity::getName)
                .orElseThrow(ErrorCode.VALIDATION_ERROR::toException);
    }

    private ReservationView view(ParticipationRequest request) {
        var active = leases.findByRequestIdAndStatusOrderByIdAsc(request.getId(), "ACTIVE");
        if (active.size() != request.getRequestedCount()) throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
        Instant expiresAt = active.getFirst().getExpiresAt();
        if (!Instant.now(clock).isBefore(expiresAt)) throw ErrorCode.PAYMENT_ORDER_EXPIRED.toException();
        var values = active.stream().map(lease -> numbers.findById(lease.getNumberId())
                .orElseThrow(ErrorCode.NUMBER_NOT_AVAILABLE::toException))
                .map(number -> "%05d".formatted(number.getNumberValue())).toList();
        return new ReservationView(request.getPublicId(), expiresAt, values);
    }

    public record ReservationView(String requestPublicId, Instant expiresAt, List<String> numbers) {
        public ReservationView { numbers = List.copyOf(numbers); }
    }
}
