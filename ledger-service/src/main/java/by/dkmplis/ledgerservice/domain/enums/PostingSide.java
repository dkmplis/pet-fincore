package by.dkmplis.ledgerservice.domain.enums;

public enum PostingSide {
    DEBIT,
    CREDIT;

    public PostingSide opposite() {
        return this == DEBIT
                ? CREDIT
                : DEBIT;
    }
}
