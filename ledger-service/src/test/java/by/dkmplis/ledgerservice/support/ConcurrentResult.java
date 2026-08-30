package by.dkmplis.ledgerservice.support;

public record ConcurrentResult<T>(
        T value,
        Throwable error
) {
    static <T> ConcurrentResult<T> success(T value) {
        return new ConcurrentResult<>(value, null);
    }

    static <T> ConcurrentResult<T> failure(Throwable error) {
        return new ConcurrentResult<>(null, error);
    }

    public boolean succeeded() {
        return error == null;
    }

    public boolean failed() {
        return error != null;
    }
}
