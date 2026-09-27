package by.dkmplis.transfer_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "outbox.publisher.enabled=false"
        }
)
class TransferServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
