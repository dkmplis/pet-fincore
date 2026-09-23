package by.dkmplis.riskservice.infrastructure.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "outbox.publisher")
public record OutboxPublisherProperties(
        int batchSize,
        Duration claimTimeout,
        Duration sendTimeout
) {
}
