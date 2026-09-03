package by.dkmplis.transfer_service.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TransferOperationLockRepository {

    private final EntityManager entityManager;

    public void lock(UUID operationId) {
        long lockKey =
                operationId.getMostSignificantBits()
                        ^ operationId.getLeastSignificantBits();

        String QUERY = "SELECT pg_advisory_xact_lock(:lockKey)";
        entityManager.createNativeQuery(
                        QUERY
                ).setParameter("lockKey", lockKey)
                .getSingleResult();
    }
}
