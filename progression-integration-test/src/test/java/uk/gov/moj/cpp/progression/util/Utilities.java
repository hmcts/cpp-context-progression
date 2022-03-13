package uk.gov.moj.cpp.progression.util;

import static com.jayway.restassured.RestAssured.given;
import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.progression.AbstractIT.CPP_UID_HEADER;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;

import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utilities {

    public static class EventListener {

        private final MessageConsumer messageConsumer;
        private final String eventType;
        private Matcher<?> matcher;
        private final long timeout;

        private EventListener(final MessageConsumer messageConsumer, final String eventType) {
            this(messageConsumer, eventType, 10000);
        }

        private EventListener(final MessageConsumer messageConsumer, final String eventType, long timeout) {
            this.eventType = eventType;
            this.messageConsumer = messageConsumer;
            this.timeout = timeout;
        }

        public JsonPath waitFor() {
            Optional<JsonPath> message = retrieveMessage(messageConsumer, timeout);
            StringDescription description = new StringDescription();

            while (message.isPresent() && !this.matcher.matches(message.get().prettify())) {
                description = new StringDescription();
                description.appendText("Expected ");
                this.matcher.describeTo(description);
                description.appendText(" but ");
                this.matcher.describeMismatch(message.get().prettify(), description);

                message = ofNullable(retrieveMessage(messageConsumer));
            }

            if (!message.isPresent()) {
                fail("Expected '" + eventType + "' message to emit on the public.event topic: " + description.toString());
            }
            return message.orElse(null);
        }

        public EventListener withFilter(Matcher<?> matcher) {
            this.matcher = matcher;
            return this;
        }

        public void close() throws JMSException {
            this.messageConsumer.close();
        }
    }

    public static EventListener listenFor(String mediaType) {
        return new EventListener(publicEvents.createPublicConsumer(mediaType), mediaType);
    }

    public static EventListener listenFor(String mediaType, long timeout) {
        return new EventListener(publicEvents.createPublicConsumer(mediaType), mediaType, timeout);
    }

    public static EventListener listenForPrivateEvent(String mediaType) {
        return new EventListener(privateEvents.createPrivateConsumer(mediaType), mediaType);
    }

    public static EventListener listenForPrivateEvent(String mediaType, long timeout) {
        return new EventListener(privateEvents.createPrivateConsumer(mediaType), mediaType, timeout);
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

    public static class CommandBuilder {
        private static final Logger LOGGER = LoggerFactory.getLogger(CommandBuilder.class);

        private final RequestSpecification requestSpec;
        private final String endpoint;
        private String type;
        private String payloadAsString;

        public CommandBuilder(RequestSpecification requestSpec, String endpoint) {
            this.requestSpec = requestSpec;
            this.endpoint = endpoint;
        }

        public CommandBuilder ofType(final String type) {
            this.type = type;
            return this;
        }


        public CommandBuilder withPayload(final Object payload) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                mapper.registerModule(new Jdk8Module());

                this.payloadAsString = mapper.writeValueAsString(payload);

                LOGGER.info("Command Payload: {}", payloadAsString);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return this;
        }

        public void executeSuccessfully() {

            String url = getWriteUrl(endpoint);
            LOGGER.info("Command Url: {}", url);

            Response writeResponse = given().spec(requestSpec).and()
                    .contentType(type)
                    .accept(type)
                    .body(ofNullable(this.payloadAsString).orElse(""))
                    .header(CPP_UID_HEADER).when()
                    .post(url)
                    .then().extract().response();
            assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        }
    }

    public static CommandBuilder makeCommand(RequestSpecification requestSpec, String endpoint) {
        return new CommandBuilder(requestSpec, endpoint);
    }


}
