package uk.gov.moj.cpp.progression.helper;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class OptionalPresent<T> {

    private final T value;

    private OptionalPresent(T value) {
        this.value = value;
    }

    public T orElse(Supplier<T> supplier) {
        if (value == null) {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new IllegalStateException("orElse resulted in an exception: " + e.getMessage(), e);
            }
        } else {
            return value;
        }
    }

    public static <X, Y> OptionalPresent<Y> ifPresent(Optional<X> opt, Function<? super X, Y> function) {
        if (opt.isPresent()) {
            return new OptionalPresent(function.apply(opt.get()));
        }
        return new OptionalPresent(null);
    }
}