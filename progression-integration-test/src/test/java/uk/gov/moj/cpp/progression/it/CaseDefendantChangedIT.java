package uk.gov.moj.cpp.progression.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.Cleaner.closeSilently;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubListCourtHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.helper.UpdateDefendantHelper;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.time.LocalDate;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({"squid:S1607"})
public class CaseDefendantChangedIT extends AbstractIT {


    private static final String COMPLETE_SENDING_SHEET_JSON =
            "progression.command.complete-sending-sheet.json";
    private static final String REF_DATA_QUERY_CJSCODE_PAYLOAD =
            "/restResource/ref-data-cjscode.json";
    private AddDefendantHelper addDefendantHelper;
    private UpdateDefendantHelper updateDefendantHelper;
    private String caseId;
    private String request;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        addDefendantHelper = new AddDefendantHelper(caseId);
        request = addDefendantHelper.addMinimalDefendant();
        addDefendantHelper.verifyInActiveMQ();
        addDefendantHelper.verifyInPublicTopic();
        addDefendantHelper.verifyMinimalDefendantAdded();
    }

    @Test
    public void shouldPublishCaseDefendantChanged() throws Exception {
        // Set wiremocks required for activity workflow only after add defendant is done
        init();
        updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
        updateDefendantHelper.updateDefendantBailStatus();
        updateDefendantHelper.verifyInActiveMQ();
        updateDefendantHelper.verifyDefendantBailStatusUpdated();
        updateDefendantHelper.verifyInMessagingQueueForDefendentUpdated();
    }

    @Test
    public void shouldPublishCaseDefendantCannotChanged() throws Exception {
        // Set wiremocks required for activity workflow only after add defendant is done
        init();
        completeSendingSheet();
        updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
        updateDefendantHelper.updateDefendantBailStatus();
        updateDefendantHelper.verifySendingSheetPreviouslyCompletedPublicEvent();
    }

    @Test
    public void shouldNotPublishCaseDefendantChangedNoRealChangeInPerson() throws Exception {
        init();
        completeSendingSheet();
        updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
        updateDefendantHelper.updateDefendantPerson();
        updateDefendantHelper.verifySendingSheetPreviouslyCompletedPublicEvent();
    }

    @Test
    public void shouldNotPublishCaseDefendantChangedSendingSheetNotCompleted() throws Exception {
        init();
        updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
        updateDefendantHelper.updateDefendantBailStatus();
        updateDefendantHelper.verifyInActiveMQ();
        updateDefendantHelper.verifyDefendantBailStatusUpdated();
        updateDefendantHelper.verifyInMessagingQueueForDefendentUpdated();
    }

    private void completeSendingSheet() throws IOException {
        addCaseToCrownCourt(caseId);
        pollCaseProgressionFor(caseId);

        final Response writeResponse = postCommand(getWriteUrl("/cases/" + caseId),
                "application/vnd.progression.command.complete-sending-sheet+json",
                getJsonBodyStr(COMPLETE_SENDING_SHEET_JSON));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    private String getJsonBodyStr(final String fileName) {
        String fileContent = getPayload(fileName);
        final JSONObject jObj = new JSONObject(request);
        final String defendantId = jObj.getString("defendantId");
        final JSONObject offence = (JSONObject) jObj.getJSONArray("offences").get(0);
        final String offenceId = offence.getString("id");
        fileContent = fileContent.replace("RANDOM_CASE_ID", caseId);

        fileContent = fileContent.replace("0baecac5-222b-402d-9047-84803679edac", defendantId);

        fileContent = fileContent.replace("0baecac5-222b-402d-9047-84803679edad", offenceId);
        fileContent = fileContent.replace("TODAY", LocalDate.now().toString());
        return fileContent;
    }

    @After
    public void tearDown() {
        closeSilently(updateDefendantHelper);
    }

    private static void init() {
        stubListCourtHearing();
        ReferenceDataStub.stubQueryOffences(REF_DATA_QUERY_CJSCODE_PAYLOAD);
    }

}
