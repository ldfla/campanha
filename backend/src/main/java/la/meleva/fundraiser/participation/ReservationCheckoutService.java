package la.meleva.fundraiser.participation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.compliance.AuthorizationService;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferRepository;
import la.meleva.fundraiser.participation.purchase.ReservationPaymentLink;
import la.meleva.fundraiser.participation.purchase.ReservationPaymentLinkRepository;
import la.meleva.fundraiser.payment.OrderRepository;
import la.meleva.fundraiser.payment.OrderService;
import la.meleva.fundraiser.payment.OrderStatus;
import la.meleva.fundraiser.payment.PaymentProvider;
import la.meleva.fundraiser.payment.PaymentRepository;
import la.meleva.fundraiser.payment.PaymentStatus;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.referral.ReferralService;
import la.meleva.fundraiser.shared.PhoneNumbers;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Checkout apenas simulado; a oferta e o preço são escolhidos no servidor. */
@Service
public class ReservationCheckoutService {
    private final ReservationService reservations;
    private final CampaignRepository campaigns;
    private final PurchaseOfferRepository offers;
    private final ParticipationRequestRepository requests;
    private final ReservationPaymentLinkRepository links;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final PaymentRepository payments;
    private final PaymentProvider provider;
    private final ReservationLeaseRepository leases;
    private final CampaignNumberRepository numbers;
    private final ParticipationEntryRepository entries;
    private final EntityManager entityManager;
    private final Clock clock;
    private final UserRepository users;
    private final ReferralService referrals;
    private final JdbcTemplate jdbc;
    private final AuthorizationService authorizations;

    @Autowired
    public ReservationCheckoutService(ReservationService reservations, CampaignRepository campaigns,
            PurchaseOfferRepository offers, ParticipationRequestRepository requests, ReservationPaymentLinkRepository links,
            OrderRepository orders, OrderService orderService, PaymentRepository payments, PaymentProvider provider,
            ReservationLeaseRepository leases, CampaignNumberRepository numbers, ParticipationEntryRepository entries,
            EntityManager entityManager,
            UserRepository users, ReferralService referrals, JdbcTemplate jdbc,
            AuthorizationService authorizations) {
        this.reservations=reservations; this.campaigns=campaigns; this.offers=offers; this.requests=requests;
        this.links=links; this.orders=orders; this.orderService=orderService; this.payments=payments; this.provider=provider;
        this.leases=leases; this.numbers=numbers; this.entries=entries;
        this.entityManager=entityManager; this.clock=Clock.systemUTC();
        this.users=Objects.requireNonNull(users, "users"); this.referrals=Objects.requireNonNull(referrals, "referrals");
        this.jdbc=Objects.requireNonNull(jdbc, "jdbc");
        this.authorizations=Objects.requireNonNull(authorizations, "authorizations");
    }

    @Transactional
    public CheckoutView start(Long userId, String campaignPublicId, String key, String offerPublicId) {
        return start(userId, campaignPublicId, key, offerPublicId, null, null);
    }

    @Transactional
    public CheckoutView start(Long userId, String campaignPublicId, String key, String offerPublicId,
                              @Nullable String entityId, @Nullable String referralCode) {
        Objects.requireNonNull(userId, "userId"); Objects.requireNonNull(campaignPublicId, "campaignPublicId");
        Objects.requireNonNull(key, "key"); Objects.requireNonNull(offerPublicId, "offerPublicId");
        if (!"MOCK".equals(provider.name())) throw ErrorCode.PAYMENT_NOT_ALLOWED.toException();
        var campaign = campaigns.findByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if (!payable(campaign)) throw ErrorCode.CAMPAIGN_OPERATION_BLOCKED.toException();
        var offer = offers.findByPublicIdAndCampaignIdAndActiveTrue(offerPublicId, campaign.getId())
                .orElseThrow(ErrorCode.VALIDATION_ERROR::toException);
        var validReferral = referralCode == null || referralCode.isBlank() ? java.util.Optional.<String>empty()
                : referrals.validCodeForUser(campaign.getId(), referralCode, userId);
        var reservation = reservations.reserveDemo(userId, campaignPublicId, key, offer.getQuantity(), entityId);
        var request = requests.findByPublicId(reservation.requestPublicId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var existing = links.findByRequestId(request.getId());
        if (existing.isPresent()) {
            if (!existing.get().getOfferId().equals(offer.getId()) || existing.get().getTotalCents()!=offer.getTotalCents()
                    || !existing.get().referralCode().equals(validReferral)) {
                throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
            }
            var order = orders.findById(existing.get().getOrderId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
            return new CheckoutView(reservation, order.getPublicId(), offer.getTotalCents(), "BRL", true);
        }
        var receipt = orderService.createOrder(campaign.getId(), userId, offer.getTotalCents(), key);
        var order = orders.findByPublicId(receipt.orderPublicId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (!order.getCampaignId().equals(campaign.getId()) || !order.getUserId().equals(userId)
                || order.getTotalCents()!=offer.getTotalCents()) throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
        links.save(new ReservationPaymentLink(request.getId(), order.getId(), offer.getId(), offer.getTotalCents(), validReferral.orElse(null)));
        return new CheckoutView(reservation, order.getPublicId(), offer.getTotalCents(), "BRL", true);
    }

    @Transactional
    public void confirm(Long userId, String campaignPublicId, String requestPublicId) {
        Objects.requireNonNull(userId, "userId"); Objects.requireNonNull(campaignPublicId, "campaignPublicId");
        Objects.requireNonNull(requestPublicId, "requestPublicId");
        if (!"MOCK".equals(provider.name())) throw ErrorCode.PAYMENT_NOT_ALLOWED.toException();
        if (users.findById(userId).filter(user -> user.isActive()).isEmpty()) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        var campaign = campaigns.findLockedByPublicId(campaignPublicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if (!payable(campaign)) throw ErrorCode.CAMPAIGN_OPERATION_BLOCKED.toException();
        var request = requests.findByPublicId(requestPublicId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        entityManager.refresh(request, LockModeType.PESSIMISTIC_WRITE);
        if (!request.getCampaignId().equals(campaign.getId()) || !request.getUserId().equals(userId)) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
        var link = links.findByRequestId(request.getId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var payment = payments.findLockedByOrderId(link.getOrderId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var order = orders.findById(link.getOrderId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var charge = provider.getCharge(payment.getProviderChargeId());
        // P20-F17: inconsistência real de dados do provedor => 502; ainda não conciliado => 409.
        if (!"MOCK".equals(payment.getProvider()) || !order.getUserId().equals(userId)
                || !order.getCampaignId().equals(campaign.getId()) || order.getTotalCents()!=link.getTotalCents()
                || payment.getAmountCents()!=link.getTotalCents() || charge.amountCents()!=link.getTotalCents()
                || !Objects.equals(charge.id(), payment.getProviderChargeId())
                || !Objects.equals(charge.txid(), payment.getTxid())) throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
        if (payment.getStatus()!=PaymentStatus.CONFIRMED || order.getStatus()!=OrderStatus.RECONCILED
                || charge.state()!=PaymentProvider.ChargeState.PAID) throw ErrorCode.PAYMENT_NOT_RECONCILED.toException();
        if (request.getStatus()==ParticipationStatus.ASSIGNED) return;
        if (request.getStatus()!=ParticipationStatus.REQUESTED) throw ErrorCode.PAYMENT_ORDER_EXPIRED.toException();
        var active = leases.findByRequestIdAndStatusOrderByIdAsc(request.getId(), "ACTIVE");
        if (active.size()!=request.getRequestedCount()) throw new IllegalStateException("reservation.lease_count_invalid");
        Instant now = Instant.now(clock);
        if (active.stream().anyMatch(lease -> lease.isExpiredAt(now))) throw ErrorCode.PAYMENT_ORDER_EXPIRED.toException();
        String charity = reservations.charityForRequest(request.getId());
        var buyer = users.findById(userId).orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        String referrerPhone = link.referralCode()
                .flatMap(code -> referrals.promoterUserIdForActiveCode(campaign.getId(), code))
                .filter(promoterId -> !promoterId.equals(userId))
                .flatMap(users::findById)
                .map(User::getPhoneNumber)
                .map(ReservationCheckoutService::e164OrNull).orElse(null);
        String modality = ParticipationService.modality(request.getRequestedCount());
        var numbersById = numbers.findAllById(active.stream().map(ReservationLease::getNumberId).toList()).stream()
                .collect(Collectors.toMap(CampaignNumber::getId, Function.identity()));
        var entriesById = entries.findAllById(numbersById.values().stream()
                        .map(number -> Objects.requireNonNull(number.getEntryId(), "reservation.entryId")).toList()).stream()
                .collect(Collectors.toMap(ParticipationEntry::getId, Function.identity()));
        for (var lease : active) {
            var number = numbersById.get(lease.getNumberId());
            if (number == null) throw ErrorCode.NUMBER_NOT_AVAILABLE.toException();
            Long entryId = Objects.requireNonNull(number.getEntryId(), "reservation.entryId");
            var entry = entriesById.get(entryId);
            if (entry == null) throw ErrorCode.RESOURCE_NOT_FOUND.toException();
            if (!request.getId().equals(entry.getRequestId()) || !campaign.getId().equals(number.getCampaignId())
                    || entry.getStatus()!=ParticipationStatus.REQUESTED) throw new IllegalStateException("reservation.entry_mismatch");
            number.assignReservation(entryId); entry.markAssigned(); lease.consume(now);
            number.recordSale(now, modality, request.getPublicId(), charity, referrerPhone);
        }
        recordAssignmentsInBulk(request.getId(), campaign.getId(), userId, buyer, now);
        link.referralCode().ifPresent(code -> referrals.attributeAllIfValid(campaign.getId(), code, request.getId(),
                List.copyOf(entriesById.values()), now));
        request.markAssigned();
    }

    /**
     * Um comando por tabela, derivado das reservas do pedido: inserir número a número deixaria a campanha travada
     * por milhares de idas ao banco nos combos grandes. As chaves únicas continuam impedindo duplicidade.
     */
    private void recordAssignmentsInBulk(Long requestId, Long campaignId, Long userId, User buyer, Instant now) {
        var identity = new CampaignNumberBuyer(0L, campaignId, userId, e164OrFail(buyer.getPhoneNumber()),
                buyer.getDisplayName(), buyer.getSurname());
        // Mesmo tratamento do Hibernate (jdbc.time_zone=UTC): o valor vai em UTC, sem depender do fuso da JVM.
        LocalDateTime at = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO campaign_number_buyers (number_id, campaign_id, buyer_user_id, buyer_phone,
                    buyer_first_name, buyer_surname, buyer_pseudonym, created_at)
                SELECT l.number_id, l.campaign_id, ?, ?, ?, ?, ?, ? FROM reservation_leases l WHERE l.request_id = ?""",
                userId, identity.getBuyerPhone(), identity.getBuyerFirstName(), identity.getBuyerSurname(),
                identity.getBuyerPseudonym(), at, requestId);
        jdbc.update("""
                INSERT INTO entry_number_assignments (entry_id, campaign_number_id, assigned_at)
                SELECT n.entry_id, n.id, ? FROM reservation_leases l JOIN campaign_numbers n ON n.id = l.number_id
                WHERE l.request_id = ?""", at, requestId);
        jdbc.update("""
                INSERT INTO eligibility_events (entry_id, rule_version, result, reason_code, checked_at)
                SELECT n.entry_id, 1, 'ELIGIBLE', 'DEMO_PAYMENT_CONFIRMED', ?
                FROM reservation_leases l JOIN campaign_numbers n ON n.id = l.number_id
                WHERE l.request_id = ?""", at, requestId);
    }

    /** ADR-0007: capitalização premiável só opera (sandbox) com autorização verificada e vigente. */
    public boolean payable(Campaign campaign) {
        var mode = campaign.getMode();
        if (mode.isDemo()) return true;
        return mode == CampaignMode.CAPITALIZATION_PRIZE_TITLE && authorizations.hasEffectiveAuthorization(campaign.getId());
    }

    private static String e164OrFail(String phone) {
        try { return PhoneNumbers.toE164(phone); }
        catch (RuntimeException exception) { throw ErrorCode.VALIDATION_ERROR.toException(); }
    }

    private static String e164OrNull(String phone) {
        try { return PhoneNumbers.optionalE164(phone); }
        catch (RuntimeException exception) { return null; }
    }

    public record CheckoutView(ReservationService.ReservationView reservation, String orderPublicId,
                               long amountCents, String currency, boolean simulation) { }
}
