package la.meleva.fundraiser.drawing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;

/** Operações puras da prova; persistência e autorização da revelação são responsabilidades do serviço. */
public final class SurpriseProof {
    public static final String DEMO_RELEASE_POLICY = "DEMO_AFTER_DRAWN_DUAL_REVIEW_V1";
    private final SecureRandom random;

    public SurpriseProof(SecureRandom random) { this.random = java.util.Objects.requireNonNull(random); }

    public Secret generate(int min, int max) {
        if (min < 0 || max > 99999 || max - min < 9) throw new IllegalArgumentException("Intervalo insuficiente para dez números");
        var values = new TreeSet<Integer>();
        while (values.size() < 10) values.add(min + random.nextInt(max - min + 1));
        byte[] salt = new byte[32]; random.nextBytes(salt);
        return new Secret(List.copyOf(values), HexFormat.of().formatHex(salt));
    }

    public static String commitment(Secret secret, Context context) {
        java.util.Objects.requireNonNull(context, "context");
        byte[] salt;
        try { salt = HexFormat.of().parseHex(secret.saltHex()); } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Sal inválido", ex); }
        if (salt.length != 32 || secret.numbers().size() != 10 || new TreeSet<>(secret.numbers()).size() != 10 || secret.numbers().stream().anyMatch(n -> n < context.numberMin() || n > context.numberMax())) throw new IllegalArgumentException("Segredo inválido");
        var sorted = new ArrayList<>(secret.numbers()); sorted.sort(Integer::compareTo);
        String canonical = "{\"campaignId\":\""+context.campaignId()+"\",\"committedAt\":\""+context.committedAt()+"\",\"numberRange\":{\"max\":"+context.numberMax()+",\"min\":"+context.numberMin()+"},\"randomSaltHex\":\""+secret.saltHex()+"\",\"regulationVersion\":"+context.regulationVersion()+",\"releasePolicy\":\""+DEMO_RELEASE_POLICY+"\",\"ruleVersion\":"+context.ruleVersion()+",\"surpriseNumbersSorted\":[" + sorted.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "]}";
        try { var digest = MessageDigest.getInstance("SHA-256"); digest.update(salt); return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    public static boolean verify(Secret secret, Context context, String expected) {
        if (expected == null) return false;
        return MessageDigest.isEqual(commitment(secret,context).getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII));
    }

    public record Context(String campaignId,int ruleVersion,int regulationVersion,int numberMin,int numberMax,java.time.Instant committedAt) {
        public Context {
            java.util.Objects.requireNonNull(campaignId,"campaignId"); java.util.Objects.requireNonNull(committedAt,"committedAt");
            if (!java.util.UUID.fromString(campaignId).toString().equals(campaignId) || ruleVersion<1 || regulationVersion<0 || numberMin<0 || numberMax>99999 || numberMax-numberMin<9 || !committedAt.equals(committedAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS))) throw new IllegalArgumentException("surprise.context.invalid");
        }
    }

    public record Secret(List<Integer> numbers, String saltHex) {
        public Secret { numbers = List.copyOf(numbers); java.util.Objects.requireNonNull(saltHex); }
        @Override public String toString() { return "Secret[REDACTED]"; }
    }
}
