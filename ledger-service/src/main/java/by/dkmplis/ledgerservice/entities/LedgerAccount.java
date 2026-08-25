package by.dkmplis.ledgerservice.entities;

import by.dkmplis.ledgerservice.enums.AccountClass;
import by.dkmplis.ledgerservice.enums.AccountPurpose;
import by.dkmplis.ledgerservice.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_accounts")
@Getter
@NoArgsConstructor
public class LedgerAccount {
    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_class", nullable = false)
    private AccountClass accountClass;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountPurpose purpose;
    @Column(nullable = false, length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;
    @Column(name = "allow_negative", nullable = false)
    private boolean allowNegative;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Version
    private long version;

    public LedgerAccount(
            UUID id,
            AccountClass accountClass,
            AccountPurpose purpose,
            String currency,
            AccountStatus status,
            boolean allowNegative
    ) {
        this.id = id;
        this.accountClass = accountClass;
        this.purpose = purpose;
        this.currency = currency;
        this.status = status;
        this.allowNegative = allowNegative;
        this.createdAt = Instant.now();
    }



}
