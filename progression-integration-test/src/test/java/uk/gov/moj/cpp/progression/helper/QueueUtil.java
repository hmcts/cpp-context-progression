package uk.gov.moj.cpp.progression.helper;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.OptionalPresent.ifPresent;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.io.StringReader;
import java.util.Optional;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueueUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueUtil.class);

    private static final String EVENT_SELECTOR_TEMPLATE = "CPPNAME IN ('%s')";

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String QUEUE_URI = System.getProperty("queueUri", "tcp://" + HOST + ":61616");

    private static final long RETRIEVE_TIMEOUT = 90000;
    private static final long MESSAGE_RETRIEVE_TRIAL_TIMEOUT = 10000;

    private  Session session;

    private  Topic topic;

    private  Connection connection;

    private final String topicName;

    public static  QueueUtil privateEvents= new QueueUtil("progression.event");

    public static  QueueUtil publicEvents = new QueueUtil("public.event");

    private QueueUtil(final String topicName) {
        this.topicName = topicName;
        initialize(topicName);
    }

    private void initialize(String topicName) {
        try {
            LOGGER.info("Artemis URI: {}", QUEUE_URI);
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = new ActiveMQTopic(topicName);
        } catch (final JMSException e) {
            LOGGER.error("Fatal error initialising Artemis", e);
            throw new RuntimeException(e);
        }
    }

    public static boolean isAlive(Connection connection) {
        try {
            return (connection != null && connection.getMetaData() != null);
        } catch (JMSException ex) {
            LOGGER.error("Failed on isAlive",ex);
            return false;
        }
    }

    public MessageConsumer createPrivateConsumer(final String eventSelector) {
        try {
            if(!isAlive(connection)){
                initialize("progression.event");
            }
            return session.createConsumer(topic, String.format(EVENT_SELECTOR_TEMPLATE, eventSelector));
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createPublicConsumer(final String eventSelector) {
        try {
            if(!isAlive(connection)){
                initialize("public.event");
            }
            return session.createConsumer(topic, String.format(EVENT_SELECTOR_TEMPLATE, eventSelector));
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageProducer createPublicProducer() {
        try {
            if(!isAlive(this.connection)){
                initialize("public.event");
            }
            return session.createProducer(topic);
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageProducer createPrivateProducer() {
        try {
            if(!isAlive(this.connection)){
                initialize("progression.event");
            }
            return session.createProducer(topic);
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createPrivateConsumerForMultipleSelectors(final String... eventSelectors) {
        final StringBuffer str = new StringBuffer("CPPNAME IN (");
        for (int i = 0; i < eventSelectors.length; i++) {
            if (i != 0) {
                str.append(", ");
            }

            str.append("'").append(eventSelectors[i]).append("'");
        }
        str.append(")");

        try {
            if(!isAlive(connection)){
                this.initialize("progression.event");
            }
            return session.createConsumer(topic, str.toString());
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createPublicConsumerForMultipleSelectors(final String... eventSelectors) {
        final StringBuffer str = new StringBuffer("CPPNAME IN (");
        for (int i = 0; i < eventSelectors.length; i++) {
            if (i != 0) {
                str.append(", ");
            }

            str.append("'").append(eventSelectors[i]).append("'");
        }
        str.append(")");

        try {
            if(!isAlive(connection)){
                this.initialize("public.event");
            }
            return session.createConsumer(topic, str.toString());
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonPath retrieveMessage(final MessageConsumer consumer, final Matcher matchers) {
        final long startTime = System.currentTimeMillis();
        JsonPath message;
        do {
            message = retrieveMessage(consumer, RETRIEVE_TIMEOUT).orElse(null);
            if (ofNullable(message).isPresent()) {
                if(matchers.matches(message.prettify())){
                    return message;
                }
            }
        } while (MESSAGE_RETRIEVE_TRIAL_TIMEOUT > (System.currentTimeMillis() - startTime));
        return null;
    }

    public static JsonPath retrieveMessage(final MessageConsumer consumer) {
        return retrieveMessage(consumer, RETRIEVE_TIMEOUT).orElse(null);
    }

    public static Optional<JsonPath> retrieveMessage(final MessageConsumer consumer, final long customTimeOutInMillis) {
        return ifPresent(retrieveMessageAsString(consumer, customTimeOutInMillis),
                (x) -> Optional.of(new JsonPath(x))
        ).orElse(Optional::empty);
    }

    public static Optional<JsonObject> retrieveMessageAsJsonObject(final MessageConsumer consumer) {
        return ifPresent(retrieveMessageAsString(consumer, RETRIEVE_TIMEOUT),
                (x) -> Optional.of(Json.createReader(new StringReader(x)).readObject())
        ).orElse(Optional::empty);
    }

    public static Optional<JsonObject> pollMessageAsJsonObjectNotSent(final MessageConsumer consumer) {
        return ifPresent(retrieveMessageAsString(consumer, MESSAGE_RETRIEVE_TRIAL_TIMEOUT),
                (x) -> Optional.of(Json.createReader(new StringReader(x)).readObject())
        ).orElse(Optional::empty);
    }

    public static JsonEnvelope retrieveMessageAsEnvelope(final MessageConsumer consumer) {
        return retrieveMessageAsString(consumer).map(new DefaultJsonObjectEnvelopeConverter()::asEnvelope).orElse(null);
    }

    public static Optional<String> retrieveMessageAsString(final MessageConsumer consumer) {
        return retrieveMessageAsString(consumer, RETRIEVE_TIMEOUT);
    }

    public static Optional<String> retrieveMessageAsString(final MessageConsumer consumer, final long customTimeOutInMillis) {
        try {
            final TextMessage message = (TextMessage) consumer.receive(customTimeOutInMillis);
            if (message == null) {
                LOGGER.error("No message retrieved using consumer with selector {}", consumer.getMessageSelector());
                return Optional.empty();
            }
            return Optional.of(message.getText());
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public  Optional<String> retrievePrivateMessageAsString(final MessageConsumer consumer, final long customTimeOutInMillis) {
        try {
            if(!isAlive(this.connection)){
                this.initialize("progression.event");
            }
            final TextMessage message = (TextMessage) consumer.receive(customTimeOutInMillis);
            if (message == null) {
                LOGGER.error("No message retrieved using consumer with selector {}", consumer.getMessageSelector());
                return Optional.empty();
            }
            return Optional.of(message.getText());
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessage(final MessageProducer messageProducer, final String eventName, final JsonObject payload, final Metadata metadata) {

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        final String json = jsonEnvelope.toDebugStringPrettyPrint();
        try {
            final TextMessage message = new ActiveMQTextMessage();

            message.setText(json);
            message.setStringProperty("CPPNAME", eventName);

            messageProducer.send(message);
        } catch (final JMSException e) {
            throw new RuntimeException("Failed to send message. commandName: '" + eventName + "', json: " + json, e);
        }
    }

}
