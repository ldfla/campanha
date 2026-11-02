package la.meleva.fundraiser.participation;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import la.meleva.fundraiser.campaign.Campaign;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Seleção **aleatória/equitativa** de números disponíveis (AGENTS §5.2 / P20-F11).
 *
 * <p>Em vez de varrer sempre a partir do menor número, sorteia um início com {@link SecureRandom} e
 * faz varredura **circular** sob lock pessimista (sem {@code SKIP LOCKED}); se faltar, continua a
 * partir do começo. Preserva a possibilidade de sortear {@code 00000}.
 */
@Component
class NumberAllocator {

    private final CampaignNumberRepository numbers;
    private final SecureRandom random = new SecureRandom();

    NumberAllocator(CampaignNumberRepository numbers) {
        this.numbers = numbers;
    }

    List<CampaignNumber> lockAvailable(Campaign campaign, int count) {
        int min = campaign.getNumberMin();
        int max = campaign.getNumberMax();
        int span = max - min + 1;
        int start = span <= 0 ? min : min + random.nextInt(span);
        var selected = new ArrayList<CampaignNumber>(count);
        selected.addAll(numbers.lockAvailableFrom(campaign.getId(), start, PageRequest.of(0, count)));
        int remaining = count - selected.size();
        if (remaining > 0) {
            selected.addAll(numbers.lockAvailableBelow(campaign.getId(), start, PageRequest.of(0, remaining)));
        }
        return selected;
    }
}
