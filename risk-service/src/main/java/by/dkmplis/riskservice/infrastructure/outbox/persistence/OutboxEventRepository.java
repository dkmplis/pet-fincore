package by.dkmplis.riskservice.infrastructure.outbox.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findAllByAggregateIdOrderByCreatedAtAsc(
            UUID aggregateId
    );
}
