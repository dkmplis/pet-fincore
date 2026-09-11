package by.dkmplis.transfer_service.infrastructure.outbox.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "outbox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisherScheduler {

    private final KafkaOutboxPublisher publisher;

    @Scheduled(
            fixedDelayString =
                    "${outbox.publisher.fixed-delay-ms:1000}"
    )
    public void publish() {
        publisher.publishBatch();
    }
}
