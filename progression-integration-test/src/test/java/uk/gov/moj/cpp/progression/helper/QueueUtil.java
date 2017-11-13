package uk.gov.moj.cpp.progression.helper;

import com.jayway.restassured.path.json.JsonPath;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.util.Optional;

import static uk.gov.moj.cpp.progression.helper.OptionalPresent.ifPresent;


public class QueueUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueUtil.class);

    private static final String EVENT_SELECTOR_TEMPLATE = "CPPNAME IN ('%s')";

    private static final String QUEUE_URI = System.getProperty("queueUri", "tcp://localhost:61616");

    private static final long RETRIEVE_TIMEOUT = 20000;

    private Connection connection;

    private Session session;

    private Topic topic;

    public static final QueueUtil privateEvents = new QueueUtil("progression.event");

    public static final QueueUtil publicEvents = new QueueUtil("public.event");

    private QueueUtil(final String topicName) {
        try {
            LOGGER.info("Artemis URI: {}", QUEUE_URI);
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = new ActiveMQTopic(topicName);
        } catch (JMSException e) {
            LOGGER.error("Fatal error initialising Artemis", e);
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createConsumer(final String eventSelector) {
        try {
            return session.createConsumer(topic, String.format(EVENT_SELECTOR_TEMPLATE, eventSelector));
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createConsumerForMultipleSelectors(final String... eventSelectors) {
        StringBuffer str = new StringBuffer("CPPNAME IN (");
        for (int i = 0; i < eventSelectors.length; i++) {
            if (i != 0) {
                str.append(", ");
            }

            str.append("'" + eventSelectors[i] + "'");
        }
        str.append(")");

        try {
            return session.createConsumer(topic, str.toString());
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonPath retrieveMessage(final MessageConsumer consumer) {
        return retrieveMessage(consumer, RETRIEVE_TIMEOUT).orElse(null);
    }

    public static Optional<JsonPath> retrieveMessage(final MessageConsumer consumer, long customTimeOutInMillis) {
        return ifPresent(retrieveMessageAsString(consumer, customTimeOutInMillis),
                (x) -> Optional.of(new JsonPath(x))
        ).orElse(Optional::<JsonPath>empty);
    }

    public static Optional<JsonObject> retrieveMessageAsJsonObject(final MessageConsumer consumer) {
        return ifPresent(retrieveMessageAsString(consumer, RETRIEVE_TIMEOUT),
                (x) -> Optional.of(Json.createReader(new StringReader(x)).readObject())
        ).orElse(Optional::<JsonObject>empty);
    }

    public static Optional<String> retrieveMessageAsString(final MessageConsumer consumer, long customTimeOutInMillis) {
        try {
            TextMessage message = (TextMessage) consumer.receive(customTimeOutInMillis);
            if (message == null) {
                LOGGER.error("No message retrieved using consumer with selector {}", consumer.getMessageSelector());
                return Optional.empty();
            }
            return Optional.of(message.getText());
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
