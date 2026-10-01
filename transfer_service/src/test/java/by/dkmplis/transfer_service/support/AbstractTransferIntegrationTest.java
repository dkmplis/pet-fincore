package by.dkmplis.transfer_service.support;

import by.dkmplis.transfer_service.infrastructure.outbox.persistence.OutboxEventRepository;
import by.dkmplis.transfer_service.infrastructure.persistence.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.*;

@SpringBootTest(
        properties = {
                "outbox.publisher.enabled=false",
                "spring.kafka.listener.auto-startup=false"
        }
)
@Import(TransferTestcontainersConfiguration.class)
public class AbstractTransferIntegrationTest {

    @Autowired
    protected TransferRepository transferRepository;

    @Autowired
    protected OutboxEventRepository outboxEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    inbox_events,
                    outbox_events,
                    transfers
                CASCADE
                """);
    }

    protected <T> List<T> runConcurrently(
            Callable<T> first,
            Callable<T> second
    ) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<T> wrapFirst = () -> {
            ready.countDown();
            start.await();
            return first.call();
        };

        Callable<T> wrapSecond = () -> {
            ready.countDown();
            start.await();
            return second.call();
        };

        try {
            Future<T> firstFuture =
                    executor.submit(wrapFirst);

            Future<T> secondFuture =
                    executor.submit(wrapSecond);

            ready.await();
            start.countDown();

            return List.of(
                    firstFuture.get(),
                    secondFuture.get()
            );

        } finally {
            executor.shutdownNow();
        }
    }
}
