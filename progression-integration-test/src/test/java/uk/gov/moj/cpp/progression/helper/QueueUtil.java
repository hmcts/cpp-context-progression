package uk.gov.moj.cpp.progression.helper;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import org.hamcrest.Matcher;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;


public class QueueUtil {

    private static final long RETRIEVE_TIMEOUT = 15000;
    private static final long MESSAGE_RETRIEVE_TRIAL_TIMEOUT = 20000;


    public static JsonPath retrieveMessageAsJsonPath(final JmsMessageConsumerClient consumer, final Matcher matchers) {
        return retrieveMessageAsJsonPath(consumer, matchers, RETRIEVE_TIMEOUT);
    }

    public static JsonPath retrieveMessageAsJsonPath(final JmsMessageConsumerClient consumer, final Matcher matchers, final long retrieveTimeOut) {
        final long startTime = System.currentTimeMillis();
        JsonPath message;
        do {
            message = consumer.retrieveMessageAsJsonPath(retrieveTimeOut).orElse(null);
            if (ofNullable(message).isPresent()) {
                if (matchers.matches(message.prettify())) {
                    return message;
                }
            }
        } while (MESSAGE_RETRIEVE_TRIAL_TIMEOUT > (System.currentTimeMillis() - startTime));
        return null;
    }


    public static Optional<JsonObject> retrieveMessageBody(final JmsMessageConsumerClient consumer) {
        return consumer.retrieveMessageAsJsonEnvelope(RETRIEVE_TIMEOUT).map(JsonEnvelope::payloadAsJsonObject);
    }

    public static Optional<JsonEnvelope> retrieveMessage(final JmsMessageConsumerClient consumer) {
        return consumer.retrieveMessageAsJsonEnvelope(RETRIEVE_TIMEOUT);
    }

    public static JsonPath retrieveMessageAsJsonPath(final JmsMessageConsumerClient consumer) {
        return consumer.retrieveMessageAsJsonPath(RETRIEVE_TIMEOUT).orElse(null);
    }

    public static Optional<JsonPath> retrieveMessageAsJsonPath(final JmsMessageConsumerClient consumer, final long retrieveTimeout) {
        return consumer.retrieveMessageAsJsonPath(retrieveTimeout);
    }

    public static Metadata buildMetadata(final String publicListingHearingConfirmed, final UUID userId) {
        return buildMetadata(publicListingHearingConfirmed, userId.toString());
    }

    public static Metadata buildMetadata(final String publicListingHearingConfirmed, final String userId) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(publicListingHearingConfirmed)
                .withUserId(userId)
                .build();
    }

    public static void sendPublicEvent(final String eventName,  final JsonObject jsonObject, final Metadata metadata) {
        final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        publicMessageProducerClient.sendMessage(eventName, envelopeFrom(metadata, jsonObject));
    }
}
