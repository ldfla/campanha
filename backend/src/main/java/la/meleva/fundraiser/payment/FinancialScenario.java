package la.meleva.fundraiser.payment;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "financial_scenarios")
class FinancialScenario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, length = 36) private String publicId;
    @Column(name = "campaign_id", nullable = false) private Long campaignId;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "price_cents", nullable = false) private long priceCents;
    @Column(name = "unit_cost_cents", nullable = false) private long unitCostCents;
    @Column(nullable = false) private int capacity;
    @Column(nullable = false) private int quantity;
    @Column(name = "fee_basis_points", nullable = false) private int feeBasisPoints;
    @Column(name = "tax_basis_points", nullable = false) private int taxBasisPoints;
    @Column(name = "target_profit_cents", nullable = false) private long targetProfitCents;
    @Version @Column(nullable = false) private long version;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "financial_scenario_costs", joinColumns = @JoinColumn(name = "scenario_id"))
    private List<Cost> costs = new ArrayList<>();

    protected FinancialScenario() { }

    FinancialScenario(Long campaignId, String name, FinancialSimulation.Scenario scenario) {
        this.publicId = UUID.randomUUID().toString();
        this.campaignId = java.util.Objects.requireNonNull(campaignId);
        update(name, scenario);
    }

    void update(String name, FinancialSimulation.Scenario scenario) {
        String validatedName = java.util.Objects.requireNonNull(name).strip();
        java.util.Objects.requireNonNull(scenario);
        if (validatedName.isEmpty() || validatedName.length() > 120) throw new IllegalArgumentException("Nome inválido");
        this.name = validatedName;
        this.priceCents = scenario.priceCents();
        this.unitCostCents = scenario.unitCostCents();
        this.capacity = scenario.capacity();
        this.quantity = scenario.quantity();
        this.feeBasisPoints = scenario.feeBasisPoints();
        this.taxBasisPoints = scenario.taxBasisPoints();
        this.targetProfitCents = scenario.targetProfitCents();
        this.costs.clear();
        scenario.fixedCosts().forEach(cost -> this.costs.add(new Cost(cost.category(), cost.cents())));
    }

    String publicId() { return publicId; }
    String name() { return name; }
    long version() { return version; }
    FinancialSimulation.Scenario scenario() {
        return new FinancialSimulation.Scenario(priceCents, unitCostCents, capacity, quantity,
                feeBasisPoints, taxBasisPoints, targetProfitCents,
                costs.stream().map(c -> new FinancialSimulation.FixedCost(c.category, c.cents)).toList());
    }

    @Embeddable
    static class Cost {
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
        private FinancialSimulation.CostCategory category;
        @Column(nullable = false) private long cents;
        protected Cost() { }
        Cost(FinancialSimulation.CostCategory category, long cents) { this.category = category; this.cents = cents; }
    }
}
