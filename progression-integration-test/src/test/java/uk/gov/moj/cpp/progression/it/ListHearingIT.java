package uk.gov.moj.cpp.progression.it;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ListHearingIT {

    private static final String REF_DATA_QUERY_CJSCODE_PAYLOAD = "/restResource/ref-data-cjscode.json";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final MessageProducer messageProducerClientPrivate = privateEvents.createProducer();
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();



    public static void init() throws Exception {
        createMockEndpoints();
        ListingStub.stubSendCaseForListing();
        ReferenceDataStub.stubQueryOffences(REF_DATA_QUERY_CJSCODE_PAYLOAD);
    }


    @Test
    public void shouldBookHearingForCaseViaListing() throws Exception {
        init();
        final String caseId = UUID.randomUUID().toString();
        String commandName = "progression.events.sending-sheet-completed";
        final String userId = UUID.randomUUID().toString();
        Metadata metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), commandName)
                .withUserId(userId)
                .build();
        //message should trigger listHearing process via listing
        sendMessage(messageProducerClientPrivate,
                commandName, getJsonObject("progression.command.complete-sending-sheet.json", "RANDOM_CASE_ID", caseId), metadata);

        //case send for listing command should called by sendCaseForListing delegate
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(this::caseSentForListingEndpointsCalled);
        // Send a case sent for listing message so that workflow ends
        commandName = "public.listing.case-sent-for-listing";
        metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), commandName)
                .withUserId(userId)
                .build();
        //message should nudge workflow from 'recieveListingCreatedConfirmation' state
        sendMessage(messageProducerClientPublic, commandName, getJsonObject("listing.public.message.case-sent-for-listing.json", "CASE_ID", caseId), metadata);

    }

    @Test
    public void shouldReListHearing() throws Exception {
        final String caseId = UUID.randomUUID().toString();
        final String defendantId = randomUUID().toString();
        String offenceId;
        //add defendant with defendantId
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            addDefendantHelper.addFullDefendant(defendantId);
            addDefendantHelper.verifyInActiveMQ();
            offenceId= addDefendantHelper.getOffenceId();

        }
        init();
        String commandName = "public.hearing.adjourned";
        final String userId = UUID.randomUUID().toString();
        Metadata metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), commandName)
                .withUserId(userId)
                .build();

        //message should trigger listHearing process via listing
        sendMessage(messageProducerClientPublic,
                commandName, getHearingAdjournedJsonObject("public.hearing.adjourned.json",  caseId,defendantId,offenceId), metadata);

        //case send for listing command should called by sendCaseForListing delegate
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(this::caseSentForListingEndpointsCalled);
        // Send a case sent for listing message so that workflow ends
        commandName = "public.listing.case-sent-for-listing";
        metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), commandName)
                .withUserId(userId)
                .build();
        //message should nudge workflow from 'recieveListingCreatedConfirmation' state
        sendMessage(messageProducerClientPublic, commandName, getJsonObject("listing.public.message.case-sent-for-listing.json", "CASE_ID", caseId), metadata);

    }

    private JsonObject getHearingAdjournedJsonObject(String path, String caseId, String defendantId, String offenceId) {
        return stringToJsonObjectConverter.convert(getPayloadForCreatingRequest(path).replaceAll("RANDOM_CASE_ID", caseId).replaceAll("DEFENDANT_ID",defendantId).replaceAll("OFFENCE_ID",offenceId));
    }

    private JsonObject getJsonObject(final String path, final String key, final String value) {
        return stringToJsonObjectConverter.convert(getPayloadForCreatingRequest(path).replaceAll(key, value));
    }

    private boolean caseSentForListingEndpointsCalled() {
        return (findAll(postRequestedFor(urlPathMatching(ListingStub.LISTING_COMMAND))).size() == 1);
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPrivate.close();
        messageProducerClientPublic.close();
    }

    private static String getPayloadForCreatingRequest(final String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }


}

