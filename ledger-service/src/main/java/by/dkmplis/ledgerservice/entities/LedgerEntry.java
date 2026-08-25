package by.dkmplis.ledgerservice.entities;

import by.dkmplis.ledgerservice.enums.PostingSide;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor
public class LedgerEntry {
    @Id
    private UUID id;
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostingSide side;
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public LedgerEntry(
            UUID id,
            UUID transactionId,
            UUID accountId,
            PostingSide side,
            long amountMinor
    ) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException(
                    "Отправляемая сумма должна быть положительной"
            );
        }
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.side = side;
        this.amountMinor = amountMinor;
        this.createdAt = Instant.now();
    }
}
