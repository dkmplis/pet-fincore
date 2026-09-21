package by.dkmplis.riskservice.infrastructure.outbox.persistence;

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
            name = "event_id",
            nullable = false,
            unique = true
    )
    private UUID eventId;

    @Column(
            nullable = false,
            length = 128
    )
    private String topic;

    @Column(
            name = "event_key",
            nullable = false,
            length = 128
    )
    private String eventKey;

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
            name = "aggregate_id",
            nullable = false
    )
    private UUID aggregateId;

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

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(
            name = "last_error",
            columnDefinition = "TEXT"
    )
    private String lastError;

    public OutboxEvent(
            UUID id,
            UUID eventId,
            String topic,
            String eventKey,
            String eventType,
            int eventVersion,
            UUID aggregateId,
            String payload,
            Instant createdAt
    ) {
        this.id = id;
        this.eventId = eventId;
        this.topic = topic;
        this.eventKey = eventKey;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.createdAt = createdAt;
        this.attempts = 0;
    }
}