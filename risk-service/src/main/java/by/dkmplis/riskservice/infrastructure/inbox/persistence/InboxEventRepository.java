package by.dkmplis.riskservice.infrastructure.inbox.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface InboxEventRepository
        extends JpaRepository<InboxEvent, UUID> {
    @Modifying
    @Query(
            value = """
                    INSERT INTO inbox_events (
                        event_id,
                        event_type,
                        event_version,
                        aggregate_id,
                        payload,
                        received_at
                    )
                    VALUES (
                        :eventId,
                        :eventType,
                        :eventVersion,
                        :aggregateId,
                        :payload,
                        now()
                    )
                    ON CONFLICT (event_id)
                    DO NOTHING
                    """,
            nativeQuery = true
    )
    int register(
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("eventVersion") int eventVersion,
            @Param("aggregateId") UUID aggregateId,
            @Param("payload") String payload
    );

    @Modifying
    @Query("""
            update InboxEvent e
            set e.processedAt = :processedAt
            where e.eventId = :eventId
              and e.processedAt is null
            """)
    int markProcessed(
            @Param("eventId") UUID eventId,
            @Param("processedAt") Instant processedAt
    );
}
