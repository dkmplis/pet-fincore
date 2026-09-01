package by.dkmplis.ledgerservice.infrastructure.persistence.repositories;

import by.dkmplis.ledgerservice.domain.model.LedgerTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LedgerTransactionRepository
        extends JpaRepository<LedgerTransaction, UUID> {

    Optional<LedgerTransaction> findByExternalOperationId(
            UUID externalOperationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select t
        from LedgerTransaction t
        where t.id = :id
    """)
    Optional<LedgerTransaction> findByIdForUpdate(@Param("id") UUID id);

    Optional<LedgerTransaction> findByReversesTransactionId(UUID reversesTransactionId);
}
