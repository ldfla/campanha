package la.meleva.fundraiser.payment;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.HashSet;
import java.util.UUID;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class FinancialExpenseService {
    static final long MAX_AMOUNT_CENTS = 9_007_199_254_740_991L;
    private static final Instant MIN_OCCURRED_AT = Instant.parse("1970-01-01T00:00:01Z");
    private static final Instant MAX_OCCURRED_AT = Instant.parse("2038-01-19T03:14:07.999999Z");
    private static final List<UserRole> AUTHORIZED_ROLES = List.of(UserRole.FINANCE, UserRole.SYSTEM_ADMIN);
    private final CampaignRepository campaigns;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final FinancialExpenseRepository expenses;
    private final FinancialExpenseReversalRepository reversals;
    private final AuditEventRepository audits;
    private final Clock clock;

    @Autowired
    FinancialExpenseService(CampaignRepository campaigns, UserRepository users, MembershipRepository memberships,
                            FinancialExpenseRepository expenses, FinancialExpenseReversalRepository reversals,
                            AuditEventRepository audits) {
        this(campaigns, users, memberships, expenses, reversals, audits, Clock.systemUTC());
    }

    FinancialExpenseService(CampaignRepository campaigns, UserRepository users, MembershipRepository memberships,
                            FinancialExpenseRepository expenses, FinancialExpenseReversalRepository reversals,
                            AuditEventRepository audits, Clock clock) {
        this.campaigns = campaigns;
        this.users = users;
        this.memberships = memberships;
        this.expenses = expenses;
        this.reversals = reversals;
        this.audits = audits;
        this.clock = clock;
    }

    @Transactional
    View create(Long actorId, String campaignPublicId, String key, FinancialSimulation.CostCategory category,
                long cents, String description, Instant occurredAt) {
        Campaign campaign = authorizedCampaign(actorId, campaignPublicId);
        String normalizedKey;
        try {
            normalizedKey = UUID.fromString(key).toString();
            if (!normalizedKey.equalsIgnoreCase(key)) throw new IllegalArgumentException("UUID inválido");
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        if (category == null || description == null || occurredAt == null || cents <= 0 || cents > MAX_AMOUNT_CENTS)
            throw ErrorCode.VALIDATION_ERROR.toException();
        Instant timestamp = occurredAt.truncatedTo(ChronoUnit.MICROS);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        if (description.isBlank() || description.strip().length() > 200 || timestamp.isAfter(now)
                || timestamp.isBefore(MIN_OCCURRED_AT) || timestamp.isAfter(MAX_OCCURRED_AT))
            throw ErrorCode.VALIDATION_ERROR.toException();
        var existing = expenses.findByCampaignIdAndIdempotencyKey(campaign.getId(), normalizedKey);
        if (existing.isPresent()) {
            if (!existing.get().matches(actorId, category, cents, description, timestamp))
                throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
            return view(existing.get(), reversals.findByExpenseId(existing.get().id()).isPresent());
        }
        var entry = expenses.saveAndFlush(new FinancialExpense(campaign.getId(), actorId, normalizedKey,
                category, cents, description, timestamp, now));
        audits.save(new AuditEvent(actorId, "FINANCIAL_EXPENSE_DECLARED", "financial_expense", entry.id(),
                entry.auditHash()));
        return view(entry, false);
    }

    @Transactional
    List<View> list(Long actorId, String campaignPublicId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw ErrorCode.VALIDATION_ERROR.toException();
        Campaign campaign = authorizedCampaign(actorId, campaignPublicId);
        var pageContent = expenses.findAllByCampaignId(campaign.getId(), PageRequest.of(page, size, Sort.by("id")))
                .getContent();
        if (pageContent.isEmpty()) return List.of();
        var reversedIds = new HashSet<>(reversals.findReversedExpenseIds(pageContent.stream().map(FinancialExpense::id).toList()));
        return pageContent.stream().map(expense -> view(expense, reversedIds.contains(expense.id()))).toList();
    }

    Campaign authorizedCampaign(Long actorId, String campaignPublicId) {
        if (actorId == null || campaignPublicId == null || campaignPublicId.isBlank())
            throw ErrorCode.VALIDATION_ERROR.toException();
        Campaign campaign = campaigns.findLockedByPublicId(campaignPublicId)
                .orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        var actor = users.lockForAuthorization(actorId).filter(User::isActive)
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        if (memberships.lockActiveForAuthorization(actor.getId(), campaign.getOrganizationId(), AUTHORIZED_ROLES,
                Membership.MembershipStatus.ACTIVE, User.UserStatus.ACTIVE).isEmpty())
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        return campaign;
    }

    private View view(FinancialExpense expense, boolean reversed) {
        return new View(expense.publicId(), expense.category(), expense.amountCents(), "BRL",
                expense.description(), expense.occurredAt(), expense.createdAt(), reversed ? "ANULADA" : "DECLARADA",
                "Valor informado por operador; não comprova pagamento nem conciliação.");
    }

    record View(String id, FinancialSimulation.CostCategory category, long amountCents, String currency,
                String description, Instant occurredAt, Instant createdAt, String status, String notice) { }
}
