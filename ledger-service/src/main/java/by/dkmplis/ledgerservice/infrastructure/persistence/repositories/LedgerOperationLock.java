package by.dkmplis.ledgerservice.infrastructure.persistence.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class LedgerOperationLock {
    @PersistenceContext
    private EntityManager entityManager;

    public void lock(UUID operationId) {
        long lockKey = operationId.getMostSignificantBits()
                ^ operationId.getLeastSignificantBits();
        entityManager.createNativeQuery(
                "SELECT pg_advisory_xact_lock(:lockKey)"
        )
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }
}
