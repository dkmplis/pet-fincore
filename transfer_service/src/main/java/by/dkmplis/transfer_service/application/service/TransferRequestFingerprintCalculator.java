package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class TransferRequestFingerprintCalculator {

    public String calculate(CreateTransferCommand command) {
        String canonical = String.join(
                "|",
                command.fromAccountId().toString(),
                command.toAccountId().toString(),
                command.currency(),
                Long.toString(command.amountMinor())
        );

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    canonical.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

}
