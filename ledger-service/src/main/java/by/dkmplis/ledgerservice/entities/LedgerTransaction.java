package by.dkmplis.ledgerservice.entities;

import by.dkmplis.ledgerservice.enums.LedgerTransactionType;
import by.dkmplis.ledgerservice.enums.TransactionState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_transactions")
@Getter
@NoArgsConstructor
public class LedgerTransaction {
    @Id
    private UUID id;
    @Column(name = "external_operation_id", nullable = false, updatable = false)
    private UUID externalOperationId;
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false,updatable = false)
    private LedgerTransactionType transactionType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionState state;
    @Column(name = "request_fingerprint", nullable = false, length = 64, updatable = false)
    private String requestFingerprint;
    @Column(name = "reverses_transaction_id", updatable = false)
    private UUID reversesTransactionId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;


    public LedgerTransaction(
            UUID id,
            UUID externalOperationId,
            String currency,
            LedgerTransactionType transactionType,
            String requestFingerprint,
            UUID reversesTransactionId
    ) {
        this.id = id;
        this.externalOperationId = externalOperationId;
        this.currency = currency;
        this.transactionType = transactionType;
        this.state = TransactionState.DRAFT;
        this.requestFingerprint = requestFingerprint;
        this.reversesTransactionId = reversesTransactionId;
        this.createdAt = Instant.now();
    }

    public void post() {
        if (state != TransactionState.DRAFT) {
            throw new IllegalStateException("Размещать можно только DRAFT транзакций.");
        }
        this.state = TransactionState.POSTED;
    }
}



