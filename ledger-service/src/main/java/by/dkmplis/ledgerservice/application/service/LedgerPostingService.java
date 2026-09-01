package by.dkmplis.ledgerservice.application.service;

import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionResult;
import by.dkmplis.ledgerservice.application.command.Posting;
import by.dkmplis.ledgerservice.application.exception.*;
import by.dkmplis.ledgerservice.domain.enums.AccountStatus;
import by.dkmplis.ledgerservice.domain.enums.TransactionState;
import by.dkmplis.ledgerservice.domain.model.LedgerAccount;
import by.dkmplis.ledgerservice.domain.model.LedgerEntry;
import by.dkmplis.ledgerservice.domain.model.LedgerTransaction;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerAccountRepository;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerEntryRepository;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerOperationLock;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LedgerPostingService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerBalanceService balanceService;
    private final RequestFingerprintCalculator fingerprintCalculator;
    private final LedgerEntryRepository entryRepository;
    private final LedgerOperationLock operationLock;

    @Transactional
    public PostLedgerTransactionResult post(
            PostLedgerTransactionCommand postLedgerTransactionCommand
    ) {
        String fingerprint = fingerprintCalculator.calculate(
                postLedgerTransactionCommand
        );

        operationLock.lock(postLedgerTransactionCommand.externalOperationId());

        Optional<LedgerTransaction> existing =
                transactionRepository.findByExternalOperationId(
                    postLedgerTransactionCommand.externalOperationId()
                );

        if (existing.isPresent()) {
            return resolveExisting(existing.get(), fingerprint);
        }

        List<UUID> accountIds = postLedgerTransactionCommand.postings()
                .stream()
                .map(Posting::accountId)
                .distinct()
                .sorted()
                .toList();

        List<LedgerAccount> lockedAccounts =
                accountRepository.findAllByIdInOrderById(accountIds);

        validateAllAccountsExisting(accountIds, lockedAccounts);

        Map<UUID, LedgerAccount> accounts = lockedAccounts.stream()
                .collect(Collectors.toMap(
                        LedgerAccount::getId,
                        Function.identity()
                ));

        validateAccounts(postLedgerTransactionCommand, accounts);
        validateResultingBalances(postLedgerTransactionCommand, accounts);


        UUID transactionId = UUID.randomUUID();

        LedgerTransaction transaction = new LedgerTransaction(
            transactionId,
            postLedgerTransactionCommand.externalOperationId(),
                postLedgerTransactionCommand.currency(),
                postLedgerTransactionCommand.transactionType(),
                fingerprint,
                postLedgerTransactionCommand.reversesTransactionId()
        );

        transaction = transactionRepository.saveAndFlush(transaction);

        List<LedgerEntry> entries = postLedgerTransactionCommand.postings().stream()
                .map(posting ->
                        new LedgerEntry(
                                UUID.randomUUID(),
                                transactionId,
                                posting.accountId(),
                                posting.side(),
                                posting.amountMinor()
                        )
                )
                .toList();
        entryRepository.saveAllAndFlush(entries);
        transaction.post();
        transactionRepository.flush();
        return new PostLedgerTransactionResult(
                transactionId,
                false
        );
    }

    private PostLedgerTransactionResult resolveExisting(
            LedgerTransaction existing,
            String fingerprint
    ) {
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(
                    existing.getExternalOperationId()
            );
        }

        if (existing.getState() != TransactionState.POSTED) {
            throw new IllegalStateException(
                    "Existing ledger transaction is not POSTED: %s"
                            .formatted(existing.getId())
            );
        }
        return new PostLedgerTransactionResult(
                existing.getId(),
                true
        );
    }

    private void validateAllAccountsExisting(
            List<UUID> expectedIds,
            List<LedgerAccount> accounts
    ) {
        Set<UUID> actualIds = accounts.stream()
                .map(LedgerAccount::getId)
                .collect(Collectors.toSet());

        expectedIds.stream()
                .filter(id -> !actualIds.contains(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new LedgerAccountNotFoundException(id);
                });
    }

    private void validateAccounts(
        PostLedgerTransactionCommand command,
        Map<UUID, LedgerAccount> accounts
    ) {
        for (LedgerAccount account: accounts.values()) {
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new LedgerAccountInactiveException(account.getId());
            }
            if (!account.getCurrency().equals(command.currency())) {
                throw new LedgerCurrencyMismatchException(
                        account.getId(),
                        command.currency(),
                        account.getCurrency()
                );
            }
        }
    }

    private void validateResultingBalances(
            PostLedgerTransactionCommand command,
            Map<UUID, LedgerAccount> accounts
    ) {
        Map<UUID, Long> deltas =
                calculateBalanceDeltas(command, accounts);

        for (var entry : deltas.entrySet()) {
            LedgerAccount account = accounts.get(entry.getKey());
            if (account.isAllowNegative()) {
                continue;
            }
            long current = balanceService.getBalanceMinor(
                    account
            );
            long resulting = Math.addExact(
                    current,
                    entry.getValue()
            );
            if (resulting < 0) {
                throw new InsufficientFundsException(
                        account.getId(),
                        current,
                        resulting
                );
            }
        }
    }

    private Map<UUID, Long> calculateBalanceDeltas(
        PostLedgerTransactionCommand command,
        Map<UUID, LedgerAccount> accounts
    ) {
        Map<UUID, Long> deltas = new HashMap<>();
        for (Posting posting : command.postings()) {
            LedgerAccount account = accounts.get(posting.accountId());

            long delta = posting.side() == account.getAccountClass().getNormalSide()
                    ? posting.amountMinor()
                    : Math.negateExact(posting.amountMinor());

            deltas.merge(posting.accountId(), delta, Math::addExact);
        }
        return deltas;
    }
}
