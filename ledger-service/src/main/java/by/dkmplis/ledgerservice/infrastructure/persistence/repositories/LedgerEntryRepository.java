package by.dkmplis.ledgerservice.infrastructure.persistence.repositories;

import by.dkmplis.ledgerservice.domain.enums.PostingSide;
import by.dkmplis.ledgerservice.domain.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, UUID> {

    @Query("""
        select coalesce(sum(e.amountMinor), 0)
        from LedgerEntry  e
        where e.accountId = :accountId
        and e.side = :side
    """)
    long sumAmount(
            @Param("accountId") UUID accountId,
            @Param("side")PostingSide side
    );

    List<LedgerEntry> findAllByTransactionId(UUID transactionalId);
}
