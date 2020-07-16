package uk.gov.moj.cpp.progression.helper;

import static org.hamcrest.CoreMatchers.is;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;

public class AwaitUtil {
    public static Optional<JsonObject> awaitAndRetrieveMessageAsJsonObject(MessageConsumer messageConsumer) {
        return awaitAndRetrieveMessageAsJsonObject(messageConsumer, Duration.TEN_SECONDS);
    }

    public static Optional<JsonObject> awaitAndRetrieveMessageAsJsonObject(MessageConsumer messageConsumer, Duration awaitTime) {
        final AtomicReference<Optional<JsonObject>> message = new AtomicReference<>();
        Awaitility
                .await()
                .atMost(awaitTime)
                .until(() -> {
            message.set(QueueUtil.retrieveMessageAsJsonObject(messageConsumer));
            return message.get().isPresent();
        }, is(true));
        return message.get();
    }
}
