package la.meleva.fundraiser.payment;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class FinancialExpenseReversalService {
    private final FinancialExpenseService access;
    private final FinancialExpenseRepository expenses;
    private final FinancialExpenseReversalRepository reversals;
    private final AuditEventRepository audits;
    private final Clock clock;
    @Autowired
    FinancialExpenseReversalService(FinancialExpenseService access, FinancialExpenseRepository expenses,
                                    FinancialExpenseReversalRepository reversals, AuditEventRepository audits) {
        this(access, expenses, reversals, audits, Clock.systemUTC());
    }
    FinancialExpenseReversalService(FinancialExpenseService access, FinancialExpenseRepository expenses,
                                    FinancialExpenseReversalRepository reversals, AuditEventRepository audits, Clock clock) {
        this.access = access;
        this.expenses = expenses;
        this.reversals = reversals;
        this.audits = audits;
        this.clock = clock;
    }

    @Transactional
    View reverse(Long actor, String campaignPublicId, String expensePublicId, String key, String reason) {
        var campaign = access.authorizedCampaign(actor, campaignPublicId);
        String canonicalKey;
        try {
            canonicalKey = UUID.fromString(key).toString();
            if (!canonicalKey.equalsIgnoreCase(key)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        if (reason == null || reason.isBlank() || reason.strip().length() > 200)
            throw ErrorCode.VALIDATION_ERROR.toException();
        String normalizedReason = reason.strip();
        var expense = expenses.findByCampaignIdAndPublicId(campaign.getId(), expensePublicId)
                .orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var existing = reversals.findByCampaignIdAndIdempotencyKey(campaign.getId(), canonicalKey);
        if (existing.isPresent()) {
            if (!existing.get().matches(expense.id(), actor, canonicalKey, normalizedReason))
                throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
            return view(existing.get(), expensePublicId);
        }
        if (reversals.findByExpenseId(expense.id()).isPresent())
            throw ErrorCode.IDEMPOTENCY_CONFLICT.toException();
        var reversal = reversals.saveAndFlush(new FinancialExpenseReversal(campaign.getId(), expense.id(), actor,
                canonicalKey, normalizedReason, clock.instant().truncatedTo(ChronoUnit.MICROS)));
        audits.save(new AuditEvent(actor, "FINANCIAL_EXPENSE_REVERSED", "financial_expense_reversal", reversal.id(),
                reversal.auditHash()));
        return view(reversal, expensePublicId);
    }
    @Transactional
    View get(Long actor, String campaignPublicId, String expensePublicId) {
        var campaign = access.authorizedCampaign(actor, campaignPublicId);
        var expense = expenses.findByCampaignIdAndPublicId(campaign.getId(), expensePublicId)
                .orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var reversal = reversals.findByExpenseId(expense.id()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        return view(reversal, expensePublicId);
    }
    private static View view(FinancialExpenseReversal reversal, String expenseId) {
        return new View(reversal.publicId(), expenseId, reversal.reason(), reversal.createdAt(), "ANULADA",
                "Correção declarada; não movimenta recursos nem comprova pagamento.");
    }
    record View(String id, String expenseId, String reason, Instant createdAt, String status, String notice) { }
}
