package la.meleva.fundraiser.drawing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Operações puras da prova; persistência e autorização da revelação são responsabilidades do serviço.
 *
 * <p>Dois tiers de prêmios-surpresa: 10 números de R$ 100 e 100 números de R$ 10 (110 no total),
 * conforme a parametrização da campanha de referência.</p>
 */
public final class SurpriseProof {
    public static final String DEMO_RELEASE_POLICY = "DEMO_AFTER_DRAWN_DUAL_REVIEW_V1";
    public static final int TIER1_COUNT = 10;
    public static final int TIER2_COUNT = 100;
    public static final long TIER1_PRIZE_CENTS = 10_000L;
    public static final long TIER2_PRIZE_CENTS = 1_000L;
    public static final int TOTAL_COUNT = TIER1_COUNT + TIER2_COUNT;
    private static final int MIN_RANGE = TOTAL_COUNT - 1;

    private final SecureRandom random;

    public SurpriseProof(SecureRandom random) { this.random = Objects.requireNonNull(random); }

    public Secret generate(int min, int max) {
        if (min < 0 || max > 99999 || max - min < MIN_RANGE) {
            throw new IllegalArgumentException("Intervalo insuficiente para %d números".formatted(TOTAL_COUNT));
        }
        var pool = new TreeSet<Integer>();
        while (pool.size() < TOTAL_COUNT) pool.add(min + random.nextInt(max - min + 1));
        var drawn = new ArrayList<>(pool);
        // A divisão entre os tiers é sorteada, não determinada pelo valor do número.
        for (int index = drawn.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            var temporary = drawn.get(index);
            drawn.set(index, drawn.get(swap));
            drawn.set(swap, temporary);
        }
        byte[] salt = new byte[32];
        random.nextBytes(salt);
        var tier1 = sortedCopy(drawn.subList(0, TIER1_COUNT));
        var tier2 = sortedCopy(drawn.subList(TIER1_COUNT, TOTAL_COUNT));
        return new Secret(tier1, tier2, HexFormat.of().formatHex(salt));
    }

    public static String commitment(Secret secret, Context context) {
        Objects.requireNonNull(context, "context");
        byte[] salt;
        try { salt = HexFormat.of().parseHex(secret.saltHex()); } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Sal inválido", ex); }
        validate(secret, context);
        String canonical = "{\"campaignId\":\"" + context.campaignId()
                + "\",\"committedAt\":\"" + context.committedAt()
                + "\",\"numberRange\":{\"max\":" + context.numberMax() + ",\"min\":" + context.numberMin() + "}"
                + ",\"randomSaltHex\":\"" + secret.saltHex() + "\""
                + ",\"regulationVersion\":" + context.regulationVersion()
                + ",\"releasePolicy\":\"" + DEMO_RELEASE_POLICY + "\""
                + ",\"ruleVersion\":" + context.ruleVersion()
                + ",\"surpriseTiers\":[{\"prizeCents\":" + TIER1_PRIZE_CENTS + ",\"numbers\":[" + join(secret.tier1()) + "]}"
                + ",{\"prizeCents\":" + TIER2_PRIZE_CENTS + ",\"numbers\":[" + join(secret.tier2()) + "]}]}";
        try { var digest = MessageDigest.getInstance("SHA-256"); digest.update(salt); return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    public static boolean verify(Secret secret, Context context, String expected) {
        if (expected == null) return false;
        try {
            return MessageDigest.isEqual(commitment(secret, context).getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static void validate(Secret secret, Context context) {
        if (secret.tier1().size() != TIER1_COUNT || secret.tier2().size() != TIER2_COUNT) {
            throw new IllegalArgumentException("Tiers inválidos");
        }
        var all = new TreeSet<Integer>();
        for (int value : concat(secret.tier1(), secret.tier2())) {
            if (value < context.numberMin() || value > context.numberMax()) throw new IllegalArgumentException("Número fora do intervalo");
            if (!all.add(value)) throw new IllegalArgumentException("Número duplicado na surpresa");
        }
        if (all.size() != TOTAL_COUNT) throw new IllegalArgumentException("Quantidade de números inválida");
    }

    private static int[] concat(List<Integer> first, List<Integer> second) {
        int[] values = new int[first.size() + second.size()];
        for (int index = 0; index < first.size(); index++) values[index] = first.get(index);
        for (int index = 0; index < second.size(); index++) values[first.size() + index] = second.get(index);
        return values;
    }

    private static List<Integer> sortedCopy(List<Integer> values) {
        return values.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private static String join(List<Integer> values) {
        return values.stream().sorted(Comparator.naturalOrder()).map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    public record Context(String campaignId, int ruleVersion, int regulationVersion, int numberMin, int numberMax, java.time.Instant committedAt) {
        public Context {
            Objects.requireNonNull(campaignId, "campaignId");
            Objects.requireNonNull(committedAt, "committedAt");
            if (!java.util.UUID.fromString(campaignId).toString().equals(campaignId) || ruleVersion < 1 || regulationVersion < 0
                    || numberMin < 0 || numberMax > 99999 || numberMax - numberMin < MIN_RANGE
                    || !committedAt.equals(committedAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS))) {
                throw new IllegalArgumentException("surprise.context.invalid");
            }
        }
    }

    public record Secret(List<Integer> tier1, List<Integer> tier2, String saltHex) {
        public Secret {
            tier1 = List.copyOf(tier1);
            tier2 = List.copyOf(tier2);
            Objects.requireNonNull(saltHex, "saltHex");
        }

        /** Todos os números (tiers combinados), ordenados — conveniência de consulta e teste. */
        public List<Integer> numbers() {
            var combined = new ArrayList<>(tier1);
            combined.addAll(tier2);
            return sortedCopy(combined);
        }

        public long prizeCents(int number) {
            return tier1.contains(number) ? TIER1_PRIZE_CENTS : TIER2_PRIZE_CENTS;
        }

        @Override public String toString() { return "Secret[REDACTED]"; }
    }
}
