package uk.gov.moj.cpp.progression.processor.utils;

import static java.util.Objects.nonNull;

import java.util.concurrent.TimeUnit;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryHelper {

    private Integer retryTimes;
    private IntSupplier supplier;
    private long retryInterval;
    private Supplier<RuntimeException> exceptionSupplier;
    private IntPredicate predicate;
    private String apimUrl;
    private String payload;
    private String ampPcrNotificationUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryHelper.class.getName());

    public RetryHelper(Builder builder) {
        this.supplier = builder.supplier;
        this.retryTimes = builder.retryTimes;
        this.retryInterval = builder.retryInterval;
        this.exceptionSupplier = builder.exceptionSupplier;
        this.predicate = builder.predicate;
        this.apimUrl = builder.apimUrl;
        this.payload = builder.payload;
        this.ampPcrNotificationUrl = builder.ampPcrNotificationUrl;
    }

    public static RetryHelper.Builder retryHelper() {
        return new RetryHelper.Builder();
    }

    public void postWithRetry() throws InterruptedException {

        int tryCount = 1;

        do {
            final int statusCode = supplier.getAsInt();

            if(nonNull(ampPcrNotificationUrl)) {
                LOGGER.info("Try - {} : Crime Hearing Case Event Pcr Notification URL invoked with Request: {} Received response status: {}", tryCount, ampPcrNotificationUrl, payload, statusCode);
            } else {
                LOGGER.info("Try - {} : Azure Function {} invoked with Request: {} Received response status: {}", tryCount, apimUrl, payload, statusCode);
            }

            final boolean pollSucceeded = predicate.test(statusCode);

            if (!pollSucceeded) {
                return;
            }
            if (tryCount != retryTimes) {
                TimeUnit.MILLISECONDS.sleep(retryInterval);
            }
            tryCount++;

        } while (tryCount <= retryTimes);

        if (exceptionSupplier == null) {
            return;
        }
        throw exceptionSupplier.get();
    }

    public static class Builder {
        private Integer retryTimes = 0;
        private IntSupplier supplier;
        private long retryInterval = 0;
        private Supplier<RuntimeException> exceptionSupplier;
        private IntPredicate predicate;
        private String apimUrl;
        private String payload;
        private String ampPcrNotificationUrl;

        public Builder withSupplier(IntSupplier supplier) {
            this.supplier = supplier;
            return this;
        }

        public Builder withRetryInterval(long retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        public Builder withRetryTimes(Integer retryTimes) {
            this.retryTimes = retryTimes;
            return this;
        }

        public Builder withExceptionSupplier(Supplier<RuntimeException> exceptionSupplier) {
            this.exceptionSupplier = exceptionSupplier;
            return this;
        }

        public Builder withPredicate(IntPredicate predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder withApimUrl(String apimUrl) {
            this.apimUrl = apimUrl;
            return this;
        }

        public Builder withPayload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder withAmpPcrNotificationUrl(String ampPcrNotificationUrl) {
            this.ampPcrNotificationUrl = ampPcrNotificationUrl;
            return this;
        }

        public RetryHelper build() {
            return new RetryHelper(this);
        }
    }
}
