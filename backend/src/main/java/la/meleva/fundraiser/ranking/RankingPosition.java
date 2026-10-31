package la.meleva.fundraiser.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "ranking_positions")
public class RankingPosition {

    @EmbeddedId
    private PositionId id;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private long score;

    @Column(name = "tiebreak_value", length = 100)
    private String tiebreakValue;

    protected RankingPosition() {}

    public RankingPosition(Long snapshotId, Long userId, int position, long score, String tiebreakValue) {
        this.id = new PositionId(snapshotId, userId);
        this.position = position;
        this.score = score;
        this.tiebreakValue = tiebreakValue;
    }

    public PositionId getId() { return id; }

    public int getPosition() { return position; }

    public long getScore() { return score; }

    public String getTiebreakValue() { return tiebreakValue; }

    @Embeddable
    public record PositionId(
            @Column(name = "snapshot_id") Long snapshotId,
            @Column(name = "user_id") Long userId) implements Serializable {}
}
