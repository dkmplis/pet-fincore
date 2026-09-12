package by.dkmplis.riskservice.domain.model;

import by.dkmplis.riskservice.domain.enums.RiskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
@Getter
@NoArgsConstructor
public class RiskAssessment {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false, unique = true)
    private UUID transferId;

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
    private RiskStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RiskAssessment(
            UUID id,
            UUID transferId,
            UUID fromAccountId,
            UUID toAccountId,
            String currency,
            long amountMinor
    ) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                    "Risk assessment accounts must be different"
            );
        }

        if (amountMinor <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be positive"
            );
        }

        this.id = id;
        this.transferId = transferId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.currency = currency;
        this.amountMinor = amountMinor;
        this.status = RiskStatus.PENDING;

        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
