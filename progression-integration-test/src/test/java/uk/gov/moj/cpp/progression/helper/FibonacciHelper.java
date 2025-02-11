package uk.gov.moj.cpp.progression.helper;

import static java.lang.System.currentTimeMillis;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.IterativePollInterval.iterative;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.awaitility.pollinterval.IterativePollInterval;

public class FibonacciHelper {

    public static void main(String[] args) {
        final long startTime = currentTimeMillis();
        final IterativePollInterval pollInterval = iterative(duration -> duration.plusMillis(50), Duration.ofMillis(10));
        await().pollInterval(pollInterval).timeout(20, TimeUnit.SECONDS).until(() -> {
            System.out.println(currentTimeMillis() - startTime);
            return false;
        });
    }
}
