package uk.gov.moj.cpp.progression.it;

import static java.lang.String.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Resources;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;

public class ConvictionDateIT {

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();

    private AddDefendantHelper addDefendantHelper;
    private String caseId;
    private String offenceId;
    private String userId = "";

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        addDefendantHelper = new AddDefendantHelper(caseId);
        addDefendantHelper.addMinimalDefendant();
        offenceId = addDefendantHelper.getOffenceId();
        userId = UUID.randomUUID().toString();
    }

    @Test
    public void convictionDateChangedAndRemoved() throws Exception {

        String messageName = "public.hearing.offence-conviction-date-changed";

        Metadata metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), messageName).withUserId(userId).build();

        // Conviction Date Change

        sendMessage(messageProducerClientPublic, messageName,
                getHearingJsonObject("public.hearing.offence-conviction-date-changed.json", caseId, offenceId),
                metadata);

        String queryCaseProgressionResponse = pollForResponse(join("", "/cases/", caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");

        JsonObject caseProgressionDetailJsonObject = getJsonObject(queryCaseProgressionResponse);

        assertThat(caseProgressionDetailJsonObject.getJsonArray("defendants").getJsonObject(0).getJsonArray("offences")
                .getJsonObject(0).getJsonString("convictionDate").getString(), equalTo("2017-02-02"));

        // Conviction Date Removed

        messageName = "public.hearing.offence-conviction-date-removed";

        metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), messageName).withUserId(userId).build();

        sendMessage(messageProducerClientPublic, messageName,
                getHearingJsonObject("public.hearing.offence-conviction-date-removed.json", caseId, offenceId),
                metadata);

        queryCaseProgressionResponse = pollForResponse(join("", "/cases/", caseId),
                "application/vnd.progression.query.caseprogressiondetail+json");

        caseProgressionDetailJsonObject = getJsonObject(queryCaseProgressionResponse);

        assertEquals(JsonValue.NULL, caseProgressionDetailJsonObject.getJsonArray("defendants").getJsonObject(0)
                        .getJsonArray("offences").getJsonObject(0).getOrDefault("convictionDate", JsonValue.NULL));

    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String offenceId) {
        return stringToJsonObjectConverter.convert(
                getPayloadForCreatingRequest(path).replaceAll("CASE_ID", caseId).replaceAll("OFFENCE_ID", offenceId));
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(ramlPath), Charset.defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

}
