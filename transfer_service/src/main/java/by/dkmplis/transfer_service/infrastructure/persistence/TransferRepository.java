package by.dkmplis.transfer_service.infrastructure.persistence;

import by.dkmplis.transfer_service.domain.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository
        extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByExternalOperationId(
            UUID externalOperationId
    );
}
