package uk.gov.moj.cpp.progression.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;


public class AdjournHearingHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdjournHearingHelper.class);
    private static final String PUBLIC_HEARING_HEARING_ADJOURNED = "public.hearing.adjourned";
    private static final MessageProducer PUBLIC_MESSAGE_PRODUCER = publicEvents.createProducer();

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-offences-for-prosecution-case+json";

    private static final String TEMPLATE_UPDATE_OFFENCES_PAYLOAD = "progression.update-offences-for-prosecution-case.json";
    private final MessageConsumer publicEventsConsumerForOffencesUpdated =
            QueueUtil.publicEvents.createConsumer(
                    "public.progression.defendant-offences-changed");

    private String adjournHearingRequest;

    private final String caseId;

    private final String defendantId;

    private final String offenceId;

    public AdjournHearingHelper(final String caseId, final String defendantId, final String offenceId) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.offenceId = offenceId;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer("listing.command.send-case-for-listing");
    }

    public void adjournHearing() {
        final Metadata metadata = generateMetadata(PUBLIC_HEARING_HEARING_ADJOURNED);
        JsonObject adjournHearingPayload = generateAdjournHearingPayload(caseId, defendantId, offenceId, PUBLIC_HEARING_HEARING_ADJOURNED);
        adjournHearingRequest = adjournHearingPayload.toString();
        sendMessage(PUBLIC_MESSAGE_PRODUCER, PUBLIC_HEARING_HEARING_ADJOURNED, adjournHearingPayload, metadata);
    }

    private Metadata generateMetadata(String eventName) {
        return metadataBuilder()
                .withId(UUID.randomUUID())
                .withName(eventName)
                .build();
    }

    private JsonObject generateAdjournHearingPayload(String caseId, String defendantId, String offenceId, String eventName) {
        String payloadStr = getStringFromResource(eventName + ".json")
                .replaceAll("PROSECUTION_CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("OFFENCE_ID", offenceId);
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
    public void verifyInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(adjournHearingRequest);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("id"), is(jsRequest.getString("id")));
    }

    public void verifyInMessagingQueue() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForOffencesUpdated);
        assertTrue(message.isPresent());
    }


}
