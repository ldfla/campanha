package la.meleva.fundraiser.ranking.dto;

import jakarta.validation.constraints.NotNull;
import la.meleva.fundraiser.ranking.RankingScope;
import java.time.Instant;

public record CreateSnapshotRequest(
        @NotNull RankingScope scope,
        Instant cutoffAt) {}
