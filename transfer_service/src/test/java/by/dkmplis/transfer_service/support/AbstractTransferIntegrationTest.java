package by.dkmplis.transfer_service.support;

import by.dkmplis.transfer_service.infrastructure.persistence.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TransferTestcontainersConfiguration.class)
public class AbstractTransferIntegrationTest {

    @Autowired
    protected TransferRepository transferRepository;

    @BeforeEach
    void cleanDatabase() {
        transferRepository.deleteAll();
    }
}
