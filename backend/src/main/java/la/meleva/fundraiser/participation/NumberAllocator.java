package la.meleva.fundraiser.participation;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import la.meleva.fundraiser.campaign.Campaign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Seleção **aleatória uniforme** de números disponíveis (AGENTS §5.2): cada número disponível tem a
 * mesma chance de ser escolhido, em vez de varrer sempre a partir do menor.
 *
 * <p>A exclusividade (não alocar o mesmo número duas vezes) é garantida pela **trava pessimista da
 * campanha** em {@code ReservationService}/{@code ParticipationService} — a "fila" que serializa as
 * reservas — somada ao {@code FOR UPDATE} sobre os números selecionados. Esta classe apenas sorteia
 * candidatos distintos e os trava; se um candidato já tiver sido tomado, sorteia outro.
 */
@Component
class NumberAllocator {

    /** Tentativas de bloco aleatório antes do fallback de varredura (só ocorre perto do esgotamento). */
    private static final int MAX_BLOCKS = 8;

    private final CampaignNumberRepository numbers;
    private final SecureRandom random;

    @Autowired
    NumberAllocator(CampaignNumberRepository numbers) {
        this(numbers, new SecureRandom());
    }

    NumberAllocator(CampaignNumberRepository numbers, SecureRandom random) {
        this.numbers = Objects.requireNonNull(numbers, "numbers");
        this.random = Objects.requireNonNull(random, "random");
    }

    List<CampaignNumber> lockAvailable(Campaign campaign, int count) {
        if (count <= 0) return List.of();
        int min = campaign.getNumberMin();
        int max = campaign.getNumberMax();
        int span = max - min + 1;
        if (span <= 0) return List.of();

        var selected = new ArrayList<CampaignNumber>(count);
        var drawn = new LinkedHashSet<Integer>();
        for (int block = 0; block < MAX_BLOCKS && selected.size() < count; block++) {
            Set<Integer> candidates = distinctCandidates(random, min, span, count - selected.size(), drawn);
            drawn.addAll(candidates);
            if (!candidates.isEmpty()) {
                selected.addAll(numbers.lockAvailableByValues(campaign.getId(), candidates));
            }
        }
        if (selected.size() < count) {
            selected.addAll(fallback(campaign, count - selected.size(), drawn));
        }
        return selected;
    }

    /**
     * Sorteia até {@code need} valores distintos em {@code [min, min + span)}, ignorando
     * {@code excluded}. Pura e determinística dado o {@link Random}; exposta para teste.
     */
    static Set<Integer> distinctCandidates(Random random, int min, int span, int need, Set<Integer> excluded) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(excluded, "excluded");
        var picked = new LinkedHashSet<Integer>();
        if (need <= 0 || span <= 0) return picked;
        int guard = 0;
        int limit = need * 16 + 64;
        while (picked.size() < need && guard++ < limit) {
            int candidate = min + random.nextInt(span);
            if (!excluded.contains(candidate)) picked.add(candidate);
        }
        return picked;
    }

    /**
     * Fallback de varredura: usado quando o sorteio aleatório não preenche a quantidade (pool quase
     * esgotado). Lê os disponíveis e escolhe aleatoriamente entre eles, sem repetir {@code excluded}.
     */
    private List<CampaignNumber> fallback(Campaign campaign, int need, Collection<Integer> excluded) {
        var available = numbers.lockAvailableByCampaignId(campaign.getId(),
                PageRequest.of(0, campaign.getNumberMax() - campaign.getNumberMin() + 1));
        var pool = new ArrayList<CampaignNumber>(available.size());
        for (var number : available) {
            if (!excluded.contains(number.getNumberValue())) pool.add(number);
        }
        Collections.shuffle(pool, random);
        return pool.subList(0, Math.min(need, pool.size()));
    }
}
