package by.dkmplis.transfer_service.application.exception;

public class LedgerTransferRejectedException
        extends RuntimeException {

    public LedgerTransferRejectedException(String message) {
        super(message);
    }
}
