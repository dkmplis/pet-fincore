package by.dkmplis.ledgerservice.repositories;

import by.dkmplis.ledgerservice.entities.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
}
