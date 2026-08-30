package by.dkmplis.ledgerservice.application.service;

import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionResult;
import by.dkmplis.ledgerservice.application.command.Posting;
import by.dkmplis.ledgerservice.application.command.ReverseLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.exception.LedgerTransactionAlreadyReversedException;
import by.dkmplis.ledgerservice.application.exception.LedgerTransactionNotFoundException;
import by.dkmplis.ledgerservice.domain.enums.LedgerTransactionType;
import by.dkmplis.ledgerservice.domain.enums.TransactionState;
import by.dkmplis.ledgerservice.domain.model.LedgerEntry;
import by.dkmplis.ledgerservice.domain.model.LedgerTransaction;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerEntryRepository;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerReversalService {

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerPostingService postingService;


    @Transactional
    public PostLedgerTransactionResult reverse(
            ReverseLedgerTransactionCommand command
    ) {
        LedgerTransaction original = transactionRepository.findByIdForUpdate(
                command.originalTransactionId()
        ).orElseThrow(() ->
                new LedgerTransactionNotFoundException(command.originalTransactionId())
        );

        validateOriginal(original);

        List<LedgerEntry> originalEntries =
                entryRepository.findAllByTransactionId(original.getId());

        if (originalEntries.size() < 2) {
            throw new IllegalStateException(
                    "Posted ledger transaction has invalid journal: %s"
                            .formatted(original.getId())
            );
        }

        List<Posting> reversedPosting = originalEntries.stream()
                .map(entry ->
                        new Posting(
                                entry.getAccountId(),
                                entry.getSide().opposite(),
                                entry.getAmountMinor()
                        )
                )
                .toList();
        PostLedgerTransactionCommand reversalCommand = new PostLedgerTransactionCommand(
                command.externalOperationId(),
                LedgerTransactionType.REVERSAL,
                original.getCurrency(),
                original.getId(),
                reversedPosting
        );
        transactionRepository.findByReversesTransactionId(original.getId())
                .ifPresent((existingReversal) -> {
                    if (!existingReversal.getExternalOperationId()
                            .equals(command.externalOperationId())) {
                        throw new LedgerTransactionAlreadyReversedException(original.getId());
                    }
                });
        return postingService.post(reversalCommand);
    }


    private void validateOriginal(LedgerTransaction original) {
        if (original.getState() != TransactionState.POSTED) {
            throw new IllegalStateException(
                    "Only POSTED transaction can be reversed: %s"
                            .formatted(original.getId())
            );
        }
        if (original.getTransactionType() == LedgerTransactionType.REVERSAL) {
            throw new IllegalArgumentException(
                    "Reversal transaction cannot be reversed: %s"
                            .formatted(original.getId())
            );
        }
    }
}
