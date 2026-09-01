package by.dkmplis.ledgerservice.infrastructure.persistence.repositories;

import by.dkmplis.ledgerservice.domain.model.LedgerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LedgerAccountRepository
        extends JpaRepository<LedgerAccount, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<LedgerAccount> findAllByIdInOrderById(
            Collection<UUID> id
    );
}
