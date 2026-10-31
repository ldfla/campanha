package la.meleva.fundraiser.ranking;

/**
 * Métrica de pontuação do ranking. Cada métrica só pode ser usada no escopo correspondente,
 * impedindo que um placar de participantes seja pontuado por indicações de terceiros e vice-versa.
 */
public enum RankingMetric {
    REFERRAL_CONFIRMED_PARTICIPATIONS(RankingScope.PROMOTER, "Indicações com participação confirmada"),
    CONFIRMED_PARTICIPATIONS(RankingScope.PARTICIPANT, "Participações confirmadas");

    private final RankingScope scope;
    private final String label;

    RankingMetric(RankingScope scope, String label) {
        this.scope = scope;
        this.label = label;
    }

    public RankingScope scope() {
        return scope;
    }

    public String label() {
        return label;
    }

    public boolean appliesTo(RankingScope scope) {
        return this.scope == scope;
    }
}
