package by.dkmplis.transfer_service.support;

import by.dkmplis.transfer_service.infrastructure.persistence.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
@Import(TransferTestcontainersConfiguration.class)
public class AbstractTransferIntegrationTest {

    @Autowired
    protected TransferRepository transferRepository;

    @BeforeEach
    void cleanDatabase() {
        transferRepository.deleteAll();
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
