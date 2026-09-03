package by.dkmplis.transfer_service.infrastructure.client.ledger;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "clients.ledger")
public record LedgerClientProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
