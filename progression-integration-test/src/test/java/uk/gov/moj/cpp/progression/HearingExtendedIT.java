package uk.gov.moj.cpp.progression;

import com.google.common.io.Resources;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getPayloadForCreatingRequest;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

@Ignore
public class HearingExtendedIT {

    //TODO: to unblock build failure in pipeline. This test runs in local Successfully.
    private static final String PROGRESSION_EXTEND_HEARING_JSON = "application/vnd.progression.extend-hearing+json";
    private static final String PROGRESSION_EVENT_HEARING_EXTENDED_FILE = "progression.event.hearing-extended.json";
    private static final String PROGRESSION_QUERY_APPLICATION_JSON = "application/vnd.progression.query.application+json";
    private static final String APPLICATION_REFERRED_AND_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed.json";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON = "progression.command.create-court-application.json";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String caseId;
    private String defendantId;
    private String hearingId;
    private String courtCentreId;
    private String courtApplicationId;

    @Before
    public void setUp() {
        createMockEndpoints();
        HearingStub.stubInitiateHearing();

        userId = randomUUID().toString();
        hearingId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();
        courtApplicationId = UUID.randomUUID().toString();

        System.out.println("CASE_ID: " + caseId);
        System.out.println("HEARING_ID: " + hearingId);
        System.out.println("COURT_APPLICATION_ID: " + courtApplicationId);
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldHearingExtendedWhenNewApplicationAddedToListedHearing() throws Exception {

        //New case added, referred tocourt and verified
        doReferCaseToCourtAndVerify();

        //New hearing added with HEARING_INITIALISED status and verified
        doHearingConfirmedAndVerify();

        //New court application added with DRAFT status and verified
        doAddCourtApplicationAndVerify();

        //CourtApplication content from file with replaced hearingId, courtApplicationId
        final String extendedHearingString = getExtendedHearingJsonBody(hearingId, courtApplicationId,
                PROGRESSION_EVENT_HEARING_EXTENDED_FILE);

        //progression.extend-hearing command triggered
        postCommand(getCommandUri("/referapplicationtocourt"),
                PROGRESSION_EXTEND_HEARING_JSON,
                extendedHearingString);

        //Verifying the public.progression.events.hearing-extended event in the MQ topic
        verifyPostHearingExtendedEvent(hearingId);

        //Verifying the CourtApplication status modified as LISTED
        verifyPostUpdateCourtApplicationStatusCommand(courtApplicationId);

    }

    private void doReferCaseToCourtAndVerify() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);

        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutionCasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);

        assertProsecutionCase(getJsonObject(getProsecutioncasesProgressionFor(caseId)).getJsonObject
                ("prosecutionCase"), caseId, defendantId);
    }

    private void doHearingConfirmedAndVerify() {
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                        caseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        final String hearingIdQueryResult = pollForResponse("/hearingSearch/"+ hearingId, PROGRESSION_QUERY_HEARING_JSON);
        final JsonObject hearingJsonObject = getJsonObject(hearingIdQueryResult);
        assertThat(hearingJsonObject, is(notNullValue()));
        assertThat(hearingJsonObject.getJsonObject("hearing").getString("id"), equalTo(hearingId));
    }

    private void doAddCourtApplicationAndVerify() throws Exception {
        addCourtApplication(caseId, courtApplicationId, PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON);
        final String caseResponse = getApplicationFor(courtApplicationId);
        assertThat(caseResponse, is(notNullValue()));
    }

    private void verifyPostHearingExtendedEvent(final String hearingId) {
        final MessageConsumer hearingExtendedConsumer = publicEvents.createConsumer(APPLICATION_REFERRED_AND_HEARING_EXTENDED);
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(hearingExtendedConsumer);
        assertTrue(message.isPresent());
        assertThat(message.get().getString("hearingId"), equalTo(hearingId));
    }

    private void verifyPostUpdateCourtApplicationStatusCommand(final String id) {
        final String QueryResult = pollForResponse("/applications/"+ id, PROGRESSION_QUERY_APPLICATION_JSON);

        final JsonObject courtApplicationJsonObject = getJsonObject(QueryResult);

        assertThat(courtApplicationJsonObject, is(notNullValue()));
        assertThat(courtApplicationJsonObject.getJsonObject("courtApplication").getString("id"), equalTo(id));
        assertThat(courtApplicationJsonObject.getJsonObject("courtApplication").getString("applicationStatus"), equalTo(ApplicationStatus.LISTED.toString()));
    }

    private String getExtendedHearingJsonBody(final String hearingId, final String courtApplicationId, final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_HEARING_ID", hearingId)
                .replace("RANDOM_APPLICATION_ID", courtApplicationId);
    }

    private JsonObject getJsonObject(final String jsonAsString) {
        final JsonObject payload;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonAsString))) {
            payload = jsonReader.readObject();
        }
        return payload;
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        System.out.println(strPayload);
        System.out.println("COURT_CENTRE_ID==" + courtCentreId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

}
