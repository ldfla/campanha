package la.meleva.fundraiser.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
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
    private static final List<UserRole> AUTHORIZED_ROLES = List.of(UserRole.FINANCE, UserRole.SYSTEM_ADMIN);
    private final CampaignRepository campaigns;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final FinancialExpenseRepository expenses;
    private final AuditEventRepository audits;
    private final Clock clock;

    @Autowired
    FinancialExpenseService(CampaignRepository campaigns, UserRepository users, MembershipRepository memberships,
                            FinancialExpenseRepository expenses, AuditEventRepository audits) {
        this(campaigns, users, memberships, expenses, audits, Clock.systemUTC());
    }

    FinancialExpenseService(CampaignRepository campaigns, UserRepository users, MembershipRepository memberships,
                            FinancialExpenseRepository expenses, AuditEventRepository audits, Clock clock) {
        this.campaigns = campaigns;
        this.users = users;
        this.memberships = memberships;
        this.expenses = expenses;
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
        if (category == null || description == null || occurredAt == null || cents <= 0)
            throw ErrorCode.VALIDATION_ERROR.toException();
        Instant timestamp = occurredAt.truncatedTo(ChronoUnit.MICROS);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        if (description.isBlank() || description.strip().length() > 200 || timestamp.isAfter(now))
            throw ErrorCode.VALIDATION_ERROR.toException();
        var existing = expenses.findByCampaignIdAndIdempotencyKey(campaign.getId(), normalizedKey);
        if (existing.isPresent()) {
            if (!existing.get().matches(actorId, category, cents, description, timestamp))
                throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
            return view(existing.get());
        }
        var entry = expenses.saveAndFlush(new FinancialExpense(campaign.getId(), actorId, normalizedKey,
                category, cents, description, timestamp, now));
        audits.save(new AuditEvent(actorId, "FINANCIAL_EXPENSE_DECLARED", "financial_expense", entry.id(),
                sha256(entry.publicId() + ":" + category + ":" + cents + ":" + timestamp)));
        return view(entry);
    }

    @Transactional
    List<View> list(Long actorId, String campaignPublicId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw ErrorCode.VALIDATION_ERROR.toException();
        Campaign campaign = authorizedCampaign(actorId, campaignPublicId);
        return expenses.findAllByCampaignId(campaign.getId(), PageRequest.of(page, size, Sort.by("id")))
                .map(this::view).getContent();
    }

    private Campaign authorizedCampaign(Long actorId, String campaignPublicId) {
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

    private View view(FinancialExpense expense) {
        return new View(expense.publicId(), expense.category(), expense.amountCents(), "BRL",
                expense.description(), expense.occurredAt(), expense.createdAt(), "DECLARADA",
                "Valor informado por operador; não comprova pagamento nem conciliação.");
    }

    private static String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    record View(String id, FinancialSimulation.CostCategory category, long amountCents, String currency,
                String description, Instant occurredAt, Instant createdAt, String status, String notice) { }
}
