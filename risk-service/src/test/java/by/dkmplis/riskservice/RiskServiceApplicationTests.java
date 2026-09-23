package by.dkmplis.riskservice;

import by.dkmplis.riskservice.support.AbstractRiskIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "outbox.publisher.enabled=false",
                "spring.kafka.listener.auto-startup=false"
        }
)
class RiskServiceApplicationTests extends AbstractRiskIntegrationTest {

    @Test
    void contextLoads() {
    }

}
