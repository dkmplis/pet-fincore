package by.dkmplis.ledgerservice.infrastructure.persistence.repositories;

import by.dkmplis.ledgerservice.domain.model.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
}
