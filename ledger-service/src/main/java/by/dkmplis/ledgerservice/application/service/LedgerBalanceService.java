package by.dkmplis.ledgerservice.application.service;

import by.dkmplis.ledgerservice.domain.enums.PostingSide;
import by.dkmplis.ledgerservice.domain.model.LedgerAccount;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LedgerBalanceService {

    private final LedgerEntryRepository entryRepository;

    public long getBalanceMinor(
            LedgerAccount account
    ) {
        long debit = entryRepository.sumAmount(
                account.getId(),
                PostingSide.DEBIT
        );
        long credit = entryRepository.sumAmount(
                account.getId(),
                PostingSide.CREDIT
        );

        return account.getAccountClass().getNormalSide()
                == PostingSide.DEBIT
                ? Math.subtractExact(debit, credit)
                : Math.subtractExact(credit, debit);
    }
}
