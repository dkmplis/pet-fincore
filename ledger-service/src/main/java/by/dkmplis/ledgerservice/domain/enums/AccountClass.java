package by.dkmplis.ledgerservice.domain.enums;

import lombok.Getter;

@Getter
public enum AccountClass {
    ASSET(PostingSide.DEBIT),
    LIABILITY(PostingSide.CREDIT),
    EQUITY(PostingSide.CREDIT),
    REVENUE(PostingSide.CREDIT),
    EXPENSE(PostingSide.DEBIT);

    private final PostingSide normalSide;

    AccountClass(PostingSide normalSide) {
        this.normalSide = normalSide;
    }

}
