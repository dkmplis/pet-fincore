package by.dkmplis.ledgerservice.repositories;

import by.dkmplis.ledgerservice.entities.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
}
