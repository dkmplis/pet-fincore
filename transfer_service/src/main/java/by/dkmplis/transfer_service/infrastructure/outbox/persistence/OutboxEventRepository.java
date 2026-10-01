package by.dkmplis.transfer_service.infrastructure.outbox.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE published_at IS NULL
              AND (
                  claimed_at IS NULL
                  OR claimed_at < :claimBefore
              )
            ORDER BY created_at, id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """,
            nativeQuery = true
    )
    List<OutboxEvent> findClaimableForUpdate(
            @Param("claimBefore")
            Instant claimBefore,

            @Param("limit")
            int limit
    );

    List<OutboxEvent> findAllByAggregateIdOrderByCreatedAtAsc(UUID uuid);

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            update OutboxEvent e
            set e.publishedAt = :publishedAt,
                e.claimedAt = null,
                e.claimToken = null,
                e.lastError = null
            where e.id = :eventId
              and e.claimToken = :claimToken
              and e.publishedAt is null
            """)
    int markPublished(
            @Param("eventId")
            UUID eventId,

            @Param("claimToken")
            UUID claimToken,

            @Param("publishedAt")
            Instant publishedAt
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            update OutboxEvent e
            set e.claimedAt = null,
                e.claimToken = null,
                e.lastError = :error
            where e.id = :eventId
              and e.claimToken = :claimToken
              and e.publishedAt is null
            """)
    int markFailed(
            @Param("eventId")
            UUID eventId,

            @Param("claimToken")
            UUID claimToken,

            @Param("error")
            String error
    );


}

