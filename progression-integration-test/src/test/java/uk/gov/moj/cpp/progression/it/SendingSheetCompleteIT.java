package uk.gov.moj.cpp.progression.it;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub.stubSetStatusForCapability;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCaseProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.givenCaseProgressionDetail;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

public class SendingSheetCompleteIT {

    private static final String PROGRESSION_COMMAND_COMPLETE_SENDING_SHEET = "progression.command.complete-sending-sheet";
    private static final MessageConsumerClient publicEventSendingSheetCompletedConsumer = new MessageConsumerClient();
    private static final MessageConsumerClient publicEventSendingSheetPreviouslyCompletedConsumer = new MessageConsumerClient();
    private static final MessageConsumerClient publicEventCompleteSendingSheetInvalidatedConsumer = new MessageConsumerClient();

    public static final String PUBLIC_ACTIVE_MQ_TOPIC = "public.event";
    public static final String PUBLIC_SENDING_SHEET_COMPLETED = "public.progression.events.sending-sheet-completed";
    public static final String PUBLIC_SENDING_SHEET_PREVIOUSLY_COMPLETED = "public.progression.events.sending-sheet-previously-completed";
    public static final String PUBLIC_SENDING_SHEET_INVALIDATED = "public.progression.events.sending-sheet-invalidated";
    public static final String COMPLETE_SENDING_SHEET_JSON = "progression.command.complete-sending-sheet.json";
    private static final String REF_DATA_QUERY_CJSCODE_PAYLOAD = "/restResource/ref-data-cjscode.json";

    private String caseId;
    private String request;

    enum RANDOM_DATA {
        CASE_ID,
        DEFENDANT_ID,
        OFFENCE_ID
    }

    private Set<RANDOM_DATA> randomData;


    public static void init() {
        createMockEndpoints();
        ListingStub.stubSendCaseForListing();
        ReferenceDataStub.stubQueryOffences(REF_DATA_QUERY_CJSCODE_PAYLOAD);
        publicEventSendingSheetCompletedConsumer.startConsumer(PUBLIC_SENDING_SHEET_COMPLETED, PUBLIC_ACTIVE_MQ_TOPIC);
        publicEventSendingSheetPreviouslyCompletedConsumer.startConsumer(PUBLIC_SENDING_SHEET_PREVIOUSLY_COMPLETED, PUBLIC_ACTIVE_MQ_TOPIC);
        publicEventCompleteSendingSheetInvalidatedConsumer.startConsumer(PUBLIC_SENDING_SHEET_INVALIDATED, PUBLIC_ACTIVE_MQ_TOPIC);
    }

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        request = "";
        randomData = Sets.newHashSet();
    }

    @Test
    public void shouldCompleteSendingSheet() throws Exception {
        request = addDefendant(caseId);
        addCaseToCrownCourt(caseId);
        givenCaseProgressionDetail(caseId);
        init();


        final Response writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.complete-sending-sheet+json",
                getJsonBodyStr(COMPLETE_SENDING_SHEET_JSON));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifySendingSheetCompletedPublicEvent();
        final String response = getCaseProgressionFor(caseId);
        final JsonObject jsonObject = getJsonObject(response);
        assertThat(jsonObject.getString("status"), equalTo("READY_FOR_REVIEW"));
    }


    @Test
    public void shouldRejectPreviouslyCompleteSendingSheet() throws Exception {
        request = addDefendant(caseId);
        addCaseToCrownCourt(caseId);
        givenCaseProgressionDetail(caseId);
        init();

        Response writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.complete-sending-sheet+json",
                getJsonBodyStr(COMPLETE_SENDING_SHEET_JSON));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.complete-sending-sheet+json",
                getJsonBodyStr(COMPLETE_SENDING_SHEET_JSON));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifySendingSheetPreviouslyCompletedPublicEvent();
    }

    @Test
    public void shouldInvalidateCompleteSendingSheet() throws Exception {
        request = addDefendant(caseId);
        addCaseToCrownCourt(caseId);
        givenCaseProgressionDetail(caseId);
        // and
        randomData.add(RANDOM_DATA.DEFENDANT_ID);
        // and
        init();

        final Response writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.complete-sending-sheet+json",
                getJsonBodyStr(COMPLETE_SENDING_SHEET_JSON));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifyCompleteSendingSheetInvalidatedPublicEvent();
    }

    private void givenAddSentenceHearingDateCapabilityDisabled() {
        stubSetStatusForCapability(PROGRESSION_COMMAND_COMPLETE_SENDING_SHEET, false);
    }


    private String getJsonBodyStr(final String fileName) throws IOException {
        String fileContent = Resources.toString(Resources.getResource(fileName), Charset.defaultCharset());
        final JSONObject jObj = new JSONObject(request);
        final String defendantId = jObj.getString("defendantId");
        final JSONObject offence = (JSONObject) jObj.getJSONArray("offences").get(0);
        final String offenceId = offence.getString("id");
        if (!randomData.contains(RANDOM_DATA.CASE_ID)) {
            fileContent = fileContent.replace("RANDOM_CASE_ID", caseId);
        }
        if (!randomData.contains(RANDOM_DATA.DEFENDANT_ID)) {
            fileContent = fileContent.replace("0baecac5-222b-402d-9047-84803679edac", defendantId);
        }
        if (!randomData.contains(RANDOM_DATA.OFFENCE_ID)) {
            fileContent = fileContent.replace("0baecac5-222b-402d-9047-84803679edad", offenceId);
        }
        fileContent = fileContent.replace("TODAY", LocalDate.now().toString());
        return fileContent;
    }

    private void verifySendingSheetCompletedPublicEvent() {
        final String sendingSheetCompletedEvent = publicEventSendingSheetCompletedConsumer.retrieveMessage().orElse(null);

        assertThat(sendingSheetCompletedEvent, notNullValue());

        with(sendingSheetCompletedEvent)
                .assertThat("$.hearing.caseId", is(caseId));
    }

    private void verifySendingSheetPreviouslyCompletedPublicEvent() {
        final String sendingSheetPreviouslyCompletedEvent = publicEventSendingSheetPreviouslyCompletedConsumer.retrieveMessage().orElse(null);

        assertThat(sendingSheetPreviouslyCompletedEvent, notNullValue());

        with(sendingSheetPreviouslyCompletedEvent)
                .assertThat("$.caseId", is(caseId));
    }

    private void verifyCompleteSendingSheetInvalidatedPublicEvent() {
        final String completeSendingSheetPreviouslyCompletedEvent = publicEventCompleteSendingSheetInvalidatedConsumer.retrieveMessage().orElse(null);

        assertThat(completeSendingSheetPreviouslyCompletedEvent, notNullValue());

        with(completeSendingSheetPreviouslyCompletedEvent)
                .assertThat("$.caseId", is(caseId));
    }

    @AfterClass
    public static void close() throws Exception {
        publicEventSendingSheetCompletedConsumer.close();
        publicEventSendingSheetPreviouslyCompletedConsumer.close();
        publicEventCompleteSendingSheetInvalidatedConsumer.close();
    }
}
