package by.dkmplis.ledgerservice.infrastructure.persistence.repositories;

import by.dkmplis.ledgerservice.domain.model.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerTransactionRepository
        extends JpaRepository<LedgerTransaction, UUID> {

    Optional<LedgerTransaction> findByExternalOperationId(
            UUID externalOperationId
    );
}
