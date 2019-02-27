package uk.gov.moj.cpp.progression;

import static com.jayway.restassured.RestAssured.given;
import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.progression.AbstractIT.CPP_UID_HEADER;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
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

import javax.jms.MessageConsumer;
import java.text.MessageFormat;
import java.util.Optional;

public class Utilities {

    public static class EventListener {

        private MessageConsumer messageConsumer;
        private String eventType;
        private Matcher<?> matcher;
        private long timeout;

        public EventListener(final String eventType) {
            this(eventType, 10000);
        }

        public EventListener(final String eventType, long timeout) {
            this.eventType = eventType;
            this.messageConsumer = publicEvents.createConsumer(eventType);
            this.timeout=timeout;
        }

        public void expectNoneWithin(long timeout) {

            Optional<JsonPath> message = retrieveMessage(messageConsumer, timeout);

            while (message != null && message.isPresent() && !this.matcher.matches(message.get().prettify())) {
                message = ofNullable(retrieveMessage(messageConsumer));
            }
            if (message != null) {
                fail("expected no messages");
            }
        }

        public JsonPath waitFor() {
            Optional<JsonPath> message = retrieveMessage(messageConsumer, timeout);
            StringDescription description = new StringDescription();

            while (message != null && message.isPresent() && !this.matcher.matches(message.get().prettify())) {
                description = new StringDescription();
                description.appendText("Expected ");
                this.matcher.describeTo(description);
                description.appendText(" but ");
                this.matcher.describeMismatch(message.get().prettify(), description);

                message = ofNullable(retrieveMessage(messageConsumer));
            }

            if (message == null) {
                fail("Expected '" + eventType + "' message to emit on the public.event topic: " + description.toString());
            }
            return message.orElse(null);
        }

        public EventListener withFilter(Matcher<?> matcher) {
            this.matcher = matcher;
            return this;
        }
    }

    public static EventListener listenFor(String mediaType) {
        return new EventListener(mediaType);
    }

    public static EventListener listenFor(String mediaType, long timeout) {
        return new EventListener(mediaType, timeout);
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
        private RequestSpecification requestSpec;
        private String endpoint;
        private String type;
        private String payloadAsString;
        private Object[] arguments = new Object[0];

        public CommandBuilder(RequestSpecification requestSpec, String endpoint) {
            this.requestSpec = requestSpec;
            this.endpoint = endpoint;
        }

        public CommandBuilder withArgs(Object... args) {
            this.arguments = args;
            return this;
        }

        public CommandBuilder ofType(final String type) {
            this.type = type;
            return this;
        }

        public CommandBuilder withPayload(final String payload) {
            this.payloadAsString = payload;
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
                        //JsonUtil.toJsonString(payload);

                System.out.println("Command Payload: ");
                System.out.println(this.payloadAsString);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return this;
        }

        public void executeSuccessfully() {

            String url = MessageFormat.format(AbstractIT.ENDPOINT_PROPERTIES.getProperty(endpoint), arguments);
            System.out.println("Command Url: ");
            System.out.println(url);

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
