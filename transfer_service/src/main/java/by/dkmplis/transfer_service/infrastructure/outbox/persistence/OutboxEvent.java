package by.dkmplis.transfer_service.infrastructure.outbox.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 64
    )
    private String aggregateType;

    @Column(
            name = "aggregate_id",
            nullable = false
    )
    private UUID aggregateId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 128
    )
    private String eventType;

    @Column(
            name = "event_version",
            nullable = false
    )
    private int eventVersion;

    @Column(
            nullable = false,
            length = 128
    )
    private String topic;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claim_token")
    private UUID claimToken;

    public OutboxEvent(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            int eventVersion,
            String topic,
            String payload,
            Instant createdAt
    ) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.topic = topic;
        this.payload = payload;
        this.createdAt = createdAt;
        this.attempts = 0;
    }

    public void claim(
            UUID claimToken,
            Instant claimedAt
    ) {
        if (publishedAt != null) {
            throw new IllegalStateException(
                    "Published outbox event cannot be claimed"
            );
        }

        this.claimToken = claimToken;
        this.claimedAt = claimedAt;
        this.attempts++;
    }
}
