package by.dkmplis.ledgerservice.application.service;

import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.command.Posting;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Collectors;

@Component
public class RequestFingerprintCalculator {

    public String calculate(
            PostLedgerTransactionCommand command
    ) {
        String postings = command.postings().stream()
                .sorted(
                        Comparator.comparing(Posting::accountId)
                                .thenComparing(p -> p.side().name())
                                .thenComparing(Posting::amountMinor)
                )
                .map(
                        p -> "%s:%s:%d".formatted(
                                p.accountId(),
                                p.side(),
                                p.amountMinor()
                        )
                )
                .collect(Collectors.joining(";"));
        String canonical = "%s|%s|%s|%s".formatted(
                command.transactionType(),
                command.currency(),
                command.reversesTransactionId() == null
                    ? "-"
                    : command.reversesTransactionId(),
                postings
        );
        return sha256(canonical);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    e
            );
        }
    }
}
