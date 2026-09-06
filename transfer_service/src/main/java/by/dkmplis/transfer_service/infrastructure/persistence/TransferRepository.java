package by.dkmplis.transfer_service.infrastructure.persistence;

import by.dkmplis.transfer_service.domain.model.Transfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository
        extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByExternalOperationId(
            UUID externalOperationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from Transfer t
            where t.id = :id
            """)
    Optional<Transfer> findByIdForUpdate(
            @Param("id")UUID id
    );
}
