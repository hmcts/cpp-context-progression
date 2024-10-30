package uk.gov.moj.cpp.progression.util;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

public class Utilities {

    public static class EventListener {

        private final JmsMessageConsumerClient messageConsumer;
        private Matcher<?> matcher;
        private final long timeout;

        private EventListener(final JmsMessageConsumerClient messageConsumer) {
            this(messageConsumer, 10000);
        }

        private EventListener(final JmsMessageConsumerClient messageConsumer, long timeout) {
            this.messageConsumer = messageConsumer;
            this.timeout = timeout;
        }

        public JsonPath waitFor() {
            Optional<JsonPath> message = retrieveMessageAsJsonPath(messageConsumer, timeout);
            StringDescription description = new StringDescription();

            while (message.isPresent() && !this.matcher.matches(message.get().prettify())) {
                description = new StringDescription();
                description.appendText("Expected ");
                this.matcher.describeTo(description);
                description.appendText(" but ");
                this.matcher.describeMismatch(message.get().prettify(), description);

                message = ofNullable(retrieveMessageAsJsonPath(messageConsumer));
            }

            if (!message.isPresent()) {
                fail("Expected message to emit on the private.event topic: " + description);
            }
            return message.orElse(null);
        }

        public EventListener withFilter(Matcher<?> matcher) {
            this.matcher = matcher;
            return this;
        }
    }

    public static EventListener listenForPrivateEvent(final JmsMessageConsumerClient messageConsumer) {
        return new EventListener(messageConsumer);
    }

    public static class JsonUtil {
        public static String toJsonString(final Object o) throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return mapper.writeValueAsString(o);
        }
    }

}
