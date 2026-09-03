package by.dkmplis.transfer_service.application.port;

public interface LedgerClient {
    LedgerTransferResult postTransfer(
            LedgerTransferCommand command
    );
}
