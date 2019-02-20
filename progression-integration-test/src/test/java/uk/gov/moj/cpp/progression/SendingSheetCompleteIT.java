package uk.gov.moj.cpp.progression;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCaseProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.givenCaseProgressionDetail;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.StubUtil;
import uk.gov.moj.cpp.progression.helper.UpdateDefendantHelper;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;


public class SendingSheetCompleteIT {

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
    private final static UUID materialId = randomUUID();

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
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        ReferenceDataStub.stubQueryAllDocumentsTypeData("/restResource/ref-data-all-documents-type.json");
        StubUtil.setupMaterialStub(materialId.toString());
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
        String response = getCaseProgressionFor(caseId);
        final JsonObject jsonObject = getJsonObject(response);
        assertThat(jsonObject.getString("status"), equalTo("READY_FOR_REVIEW"));

        // verify prosecution case created in view store
        response = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutioncaseJsonObject = getJsonObject(response);
        assertThat(prosecutioncaseJsonObject.getJsonObject("prosecutionCase").getString("id"), equalTo(caseId.toString()));
        assertThat(prosecutioncaseJsonObject.getJsonObject("prosecutionCase").getString("initiationCode"), equalTo("C"));
        assertThat(prosecutioncaseJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getString("prosecutionCaseId"), equalTo(caseId.toString()));
        assertThat(prosecutioncaseJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonObject("personDetails").getString("firstName"), equalTo("David"));

    }

    /**
     * Adds  and update a Defendant Bail status and complete sending sheet then verifies by reading a case to determine
     * that the defendant has been updated.
     */
    @Test
    public void updateDefendantBailStatusAndVerify() throws Exception {

        request = addDefendant(caseId);
        final JSONObject jObj = new JSONObject(request);
        final String defendantId = jObj.getString("defendantId");
        final String personId = jObj.getJSONObject("person").getString("id");
        addCaseToCrownCourt(caseId);
        givenCaseProgressionDetail(caseId);


        final UpdateDefendantHelper updateDefendantHelper = new UpdateDefendantHelper(caseId, defendantId, personId);
        updateDefendantHelper.updateDefendantBailStatus(materialId.toString());
        updateDefendantHelper.verifyInActiveMQ();
        updateDefendantHelper.verifyDefendantBailStatusUpdated();
        updateDefendantHelper.verifyInMessagingQueueForDefendentUpdated();
        init();
        final Response writeResponse = postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.complete-sending-sheet+json",
                getJsonBodyStr(COMPLETE_SENDING_SHEET_JSON));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifySendingSheetCompletedPublicEvent();
        String response = getCaseProgressionFor(caseId);
        final JsonObject jsonObject = getJsonObject(response);
        assertThat(jsonObject.getString("status"), equalTo("READY_FOR_REVIEW"));

        // verify prosecution case created in view store
        response = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutioncaseJsonObject = getJsonObject(response);
        assertThat(prosecutioncaseJsonObject.getJsonObject("prosecutionCase").getString("id"), equalTo(caseId.toString()));
        assertThat(prosecutioncaseJsonObject.getJsonObject("prosecutionCase").getString("initiationCode"), equalTo("C"));
        assertThat(prosecutioncaseJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getString("prosecutionCaseId"), equalTo(caseId.toString()));
        assertThat(prosecutioncaseJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonObject("personDetails").getString("firstName"), equalTo("David"));

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
