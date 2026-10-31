package la.meleva.fundraiser.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' AND available_at <= :now ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> lockAvailableBatch(@Param("now") Instant now, @Param("limit") int limit);

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' AND event_type = :eventType AND available_at <= :now ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> lockAvailableBatchByEventType(@Param("eventType") String eventType,
                                                     @Param("now") Instant now, @Param("limit") int limit);
}
