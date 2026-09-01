package by.dkmplis.transfer_service.domain.model;

import by.dkmplis.transfer_service.domain.enums.TransferState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "transfers")
@NoArgsConstructor
@Getter
public class Transfer {
    @Id
    private UUID id;
    @Column(
            name = "external_operation_id",
            nullable = false,
            unique = true
    )
    private UUID externalOperationId;
    @Column(
            name = "request_fingerprint",
            nullable = false,
            length = 64
    )
    private String requestFingerprint;
    @Column(
            name = "ledger_operation_id",
            nullable = false,
            unique = true
    )
    private UUID ledgerOperationId;
    @Column(name = "ledger_transaction_id")
    private UUID ledgerTransactionId;
    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;
    @Column(name = "to_account_id", nullable = false)
    private UUID toAccountId;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferState state;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Transfer(
            UUID id,
            UUID externalOperationId,
            String requestFingerprint,
            UUID ledgerOperationId,
            UUID fromAccountId,
            UUID toAccountId,
            String currency,
            long amountMinor
    ) {
        this.id = id;
        this.externalOperationId = externalOperationId;
        this.requestFingerprint = requestFingerprint;
        this.ledgerOperationId = ledgerOperationId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.currency = currency;

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                    "Transfer accounts must be different"
            );
        }

        if (amountMinor <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be positive"
            );
        }

        this.amountMinor = amountMinor;
        this.state = TransferState.PENDING;

        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void complete(UUID ledgerTransactionId) {
        if (state != TransferState.PENDING) {
            throw new IllegalStateException(
                    "Only pending transfer can be completed"
            );
        }

        this.ledgerTransactionId =
                Objects.requireNonNull(ledgerTransactionId);

        this.state = TransferState.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void reject() {
        if (state != TransferState.PENDING) {
            throw new IllegalStateException(
                    "Only pending transfer can be rejected"
            );
        }

        this.state = TransferState.REJECTED;
        this.updatedAt = Instant.now();
    }


}
