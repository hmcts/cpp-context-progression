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

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Test;

import com.google.common.io.Resources;
import com.jayway.awaitility.Awaitility;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.PeoplePersonStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;
import uk.gov.moj.cpp.progression.stub.StructureCaseDetailsStub;

public class InitiateHearingIT {

    private static final String REF_DATA_QUERY_JUDGE_PAYLOAD = "/restResource/ref-data-get-judge.json";
    private static final String REF_DATA_QUERY_COURT_CENTRE_PAYLOAD = "/restResource/ref-data-get-court-centre.json";
    private static final String REF_DATA_QUERY_CJSCODE_PAYLOAD = "/restResource/ref-data-cjscode.json";
    private static final String STRUCTURE_CASE_QUERY_PAYLOAD =
                    "/restResource/structure.query.case.json";
    private static final String PEOPLE_PERSONS_QUERY_PAYLOAD =
                    "/restResource/people.query.persons.json";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final MessageProducer messageProducerClientPrivate = privateEvents.createProducer();
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();

    public static void init(){
        createMockEndpoints();
        HearingStub.stubInitiateHearing();
        ReferenceDataStub.stubQueryJudge(REF_DATA_QUERY_JUDGE_PAYLOAD);
        ReferenceDataStub.stubQueryCourtCentre(REF_DATA_QUERY_COURT_CENTRE_PAYLOAD);
        ReferenceDataStub.stubQueryOffences(REF_DATA_QUERY_CJSCODE_PAYLOAD);
        StructureCaseDetailsStub.stubQueryCaseDetails(STRUCTURE_CASE_QUERY_PAYLOAD);
        PeoplePersonStub.stubQueryPersons(PEOPLE_PERSONS_QUERY_PAYLOAD);
    }

    @Test
    public void shouldInitiateHearing() throws Exception {

        String commandName = "public.hearing-confirmed";
        final String userId = UUID.randomUUID().toString();
        final String hearingId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String defendantId = randomUUID().toString();
        //add defendant with defendantId
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            addDefendantHelper.addFullDefendant(defendantId);
            addDefendantHelper.verifyInActiveMQ();
        }

        init();

        Metadata metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), commandName)
                .withUserId(userId)
                .build();
        //message should trigger hearingConfirmed process via  public event
        sendMessage(messageProducerClientPublic,
                commandName, getHearingJsonObject("public.hearing-confirmed.json",  caseId, hearingId, defendantId), metadata);
        //intiate hearing command should called by initiateHearing delegate
        Awaitility.await().atMost(20, TimeUnit.SECONDS).until(this::initiateHearingEndpointsCalled);
        // hearing initiated message so that workflow ends
        commandName = "public.hearing.initiated";
        metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), commandName)
                .withUserId(userId)
                .build();
        //message should nudge workflow from 'recieveHearingInitiatedConfirmation' state
        sendMessage(messageProducerClientPublic, commandName, getJsonObject("public.message.hearing.initiated.json", "HEARING_ID", hearingId), metadata);

    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                    final String defendantId) {
        return stringToJsonObjectConverter.convert(getPayloadForCreatingRequest(path).replaceAll("CASE_ID",caseId).replaceAll("HEARING_ID", hearingId).replaceAll("DEFENDANT_ID", defendantId));
    }


    private JsonObject getJsonObject(final String path, final String key, final String value) {
        return stringToJsonObjectConverter.convert(getPayloadForCreatingRequest(path).replaceAll(key, value));
    }

    private boolean initiateHearingEndpointsCalled() {
        return (findAll(postRequestedFor(urlPathMatching(HearingStub.HEARING_COMMAND))).size() == 1);
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPrivate.close();
        messageProducerClientPublic.close();
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }


}

