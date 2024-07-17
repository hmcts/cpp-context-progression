package uk.gov.moj.cpp.progression.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.nio.charset.Charset;
import java.util.UUID;

import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConvictionDateHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvictionDateHelper.class);
    private static final String PUBLIC_HEARING_CONVICTION_DATE_CHANGED = "public.hearing.offence-conviction-date-changed";
    private static final String PUBLIC_HEARING_CONVICTION_DATE_REMOVED = "public.hearing.offence-conviction-date-removed";
    private static final MessageProducer PUBLIC_MESSAGE_PRODUCER = publicEvents.createPublicProducer();

    private String addConvictionDateRequest;

    private String removeConvictionDateRequest;

    private final String caseId;

    private final String offenceId;

    private final String courtApplicationId;

    public ConvictionDateHelper(final String caseId, final String offenceId, final String courtApplicationId) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.courtApplicationId = courtApplicationId;

        privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumerForMultipleSelectors("progression.event.conviction-date-added", "progression.event.conviction-date-removed");
    }

    public void addConvictionDate() {
        final Metadata metadata = generateMetadata(PUBLIC_HEARING_CONVICTION_DATE_CHANGED);
        JsonObject convictionDateChangedPayload = generateConvictionDatePayload(caseId, offenceId, PUBLIC_HEARING_CONVICTION_DATE_CHANGED, courtApplicationId);
        addConvictionDateRequest = convictionDateChangedPayload.toString();
        sendMessage(PUBLIC_MESSAGE_PRODUCER, PUBLIC_HEARING_CONVICTION_DATE_CHANGED, convictionDateChangedPayload, metadata);
    }

    public void removeConvictionDate() {
        final Metadata metadata = generateMetadata(PUBLIC_HEARING_CONVICTION_DATE_REMOVED);
        JsonObject convictionDateRemovedPayload = generateConvictionDatePayload(caseId, offenceId, PUBLIC_HEARING_CONVICTION_DATE_REMOVED, courtApplicationId);
        removeConvictionDateRequest = convictionDateRemovedPayload.toString();
        sendMessage(PUBLIC_MESSAGE_PRODUCER, PUBLIC_HEARING_CONVICTION_DATE_REMOVED, convictionDateRemovedPayload, metadata);
    }

    private Metadata generateMetadata(String eventName) {
        return metadataBuilder()
                .withId(UUID.randomUUID())
                .withName(eventName)
                .build();
    }

    private JsonObject generateConvictionDatePayload(String caseId, String offenceId, String eventName, final String courtApplicationId) {
        String payloadStr = getStringFromResource(eventName + ".json");
        if(caseId == null){
            payloadStr = payloadStr.replace("\"caseId\":\"CASE_ID\",", "");
        }
        if(courtApplicationId == null){
            payloadStr = payloadStr.replace("\"courtApplicationId\" : \"APPLICATION_ID\",", "");
        }
        if(offenceId == null){
            payloadStr = payloadStr.replace("\"offenceId\":\"OFFENCE_ID\",", "");
            payloadStr = payloadStr.replace(",\"offenceId\":\"OFFENCE_ID\"", "");
        }
        payloadStr = payloadStr.replaceAll("CASE_ID", caseId)
                .replaceAll("OFFENCE_ID", offenceId)
                .replaceAll("APPLICATION_ID", courtApplicationId);
        return new StringToJsonObjectConverter().convert(payloadStr);
    }

    private static String getStringFromResource(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), Charset.defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    /**
     * Retrieve message from queue and do additional verifications
     */
    public void verifyInActiveMQForConvictionDateChanged() {
        final JsonPath jsRequest = new JsonPath(addConvictionDateRequest);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("id"), is(jsRequest.getString("id")));
    }

    public void verifyInActiveMQForConvictionDateRemoved() {
        final JsonPath jsRequest = new JsonPath(removeConvictionDateRequest);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("id"), is(jsRequest.getString("id")));
    }
}
