package by.dkmplis.transfer_service.application.exception;

public class LedgerCallUncertainException
        extends RuntimeException {

    public LedgerCallUncertainException(String message) {
        super(message);
    }

    public LedgerCallUncertainException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
