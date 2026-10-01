package by.dkmplis.transfer_service;

import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "outbox.publisher.enabled=false",
                "spring.kafka.listener.auto-startup=false"
        }
)
class TransferServiceApplicationTests extends AbstractTransferIntegrationTest {

    @Test
    void contextLoads() {
    }

}
