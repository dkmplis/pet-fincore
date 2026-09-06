package by.dkmplis.transfer_service.support;

public record ConcurrentResult<T>(
        T value,
        Throwable error
) {
}
