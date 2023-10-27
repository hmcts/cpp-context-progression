package uk.gov.moj.cpp.progression.helper;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonassert.impl.matcher.IsMapContainingKey.hasKey;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsString;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S2925")
public class QueueReaderUtil {

    private final Logger LOG = LoggerFactory.getLogger(QueueReaderUtil.class.getName());

    private final Map<String, ConcurrentLinkedDeque<String>> MESSAGE_CACHE_BY_CONSUMER = new HashMap<>();
    private final List<MessageConsumer> messageConsumers = new ArrayList<>();

    public void startListeningToPrivateEvents(final String... queueNames) throws JMSException {
        for (final String queueName : queueNames) {
            MESSAGE_CACHE_BY_CONSUMER.put(queueName, new ConcurrentLinkedDeque<>());
            final MessageConsumer messageConsumer = privateEvents.createPrivateConsumer(queueName);
            messageConsumers.add(messageConsumer);

            (new Thread(() -> {
                while (true) {
                    try {
                        final Optional<String> optionalMessage = retrieveMessageAsString(messageConsumer);
                        optionalMessage.ifPresent(message -> MESSAGE_CACHE_BY_CONSUMER.get(queueName).add(message));
                    } catch (final Exception e) {
                        logAndWaitBeforeNextPolling(messageConsumer, e);
                    }
                }
            }, format("%s-queue-listener", messageConsumer.getMessageSelector()))).start();
        }
    }

    public Optional<JsonObject> retrieveMessageAsJsonObject(final String queueName, final String... matchingValues) {
        assertThat(MESSAGE_CACHE_BY_CONSUMER, hasKey(queueName));

        final ConcurrentLinkedDeque<String> matchingQueue = MESSAGE_CACHE_BY_CONSUMER.get(queueName);

        try {
            final String actualMessage = await().with().timeout(30, SECONDS)
                    .until(() -> {
                        final Optional<String> matchingMessage = matchingQueue.stream()
                                .filter(message -> Arrays.stream(matchingValues).allMatch(message::contains))
                                .findFirst();

                        if (matchingMessage.isPresent()) {
                            matchingQueue.remove(matchingMessage.get());
                            return matchingMessage.get();
                        } else {
                            LOG.info("Looking for message matching values {} from queue {}", matchingValues, queueName);
                        }
                        return null;
                    }, is(notNullValue()));

            return Optional.of(createReader(new StringReader(actualMessage)).readObject());
        } catch (final ConditionTimeoutException e) {
            LOG.error("Could not find message matching values {} from queue {}", matchingValues, queueName);
            return empty();
        }
    }

    public void closeQueues() throws JMSException {
        for (final MessageConsumer messageConsumer : messageConsumers) {
            messageConsumer.close();
        }
    }

    private void logAndWaitBeforeNextPolling(final MessageConsumer messageConsumer, final Exception e) {
        try {
            LOG.warn(format("Error while listening for messages from queue %s", messageConsumer.getMessageSelector()), e);
        } catch (final JMSException ex) {
            //do nothing
        }
        try {
            Thread.sleep(1000L);
        } catch (final InterruptedException exception) {
            //do nothing
        }
    }
}
