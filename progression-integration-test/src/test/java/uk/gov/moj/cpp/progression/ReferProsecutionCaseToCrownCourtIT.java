package uk.gov.moj.cpp.progression;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_CASE_AT_A_GLANCE_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper
        .addProsecutionCaseToCrownCourtWithMinimumAttributes;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addRemoveCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutionCaseAtAGlanceFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpointsWithEmpty;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getQueryUri;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertcourtDocuments;

@SuppressWarnings("squid:S1607")
public class ReferProsecutionCaseToCrownCourtIT {
    static final String REFER_PROSECUTION_CASES_TO_COURT_REJECTED = "public.progression.refer-prosecution-cases-to-court-rejected";

    private static final MessageConsumer consumerForReferToCourtRejected = publicEvents.createConsumer(REFER_PROSECUTION_CASES_TO_COURT_REJECTED);



    private String caseId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referraReasonId;


    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        materialIdActive = UUID.randomUUID().toString();
        materialIdDeleted = UUID.randomUUID().toString();
        courtDocumentId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        referraReasonId = UUID.randomUUID().toString();
    }

    @Test
    public void shouldGetProsecutionCaseWithDocumentsAndGetConfirmation() throws Exception {
        createMockEndpoints();
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referraReasonId);
        // when
        final String response = getProsecutioncasesProgressionFor(caseId);
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);

        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertThat(prosecutioncasesJsonObject.getJsonArray("courtDocuments").getJsonObject(0).getJsonArray("materials").size(),equalTo(2));
        assertcourtDocuments(prosecutioncasesJsonObject.getJsonArray("courtDocuments").getJsonObject(0), caseId, courtDocumentId, materialIdActive);
    }

    @Test
    public void shouldGetProsecutionCaseWithDocumentsAndReferralRejected() throws Exception {
        createMockEndpointsWithEmpty();
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referraReasonId);
        // when
        verifyInMessagingQueueForReferToCourtsRejcted();
    }


    @Test
    public void shouldGetProsecutionCaseWithoutDocuments() throws Exception {
        createMockEndpoints();
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when


        final String response = getProsecutioncasesProgressionFor(caseId);
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);

        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertThat(prosecutioncasesJsonObject.getJsonArray("courtDocuments").size(), equalTo(0));

    }

    @Test
    public void shouldGetProsecutionCaseWithMinimumMandatoryAttributes() throws Exception {
        createMockEndpoints();
        // given
        addProsecutionCaseToCrownCourtWithMinimumAttributes(caseId, defendantId);
        // when
        final String response = getProsecutioncasesProgressionFor(caseId);
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);

        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getString("id"), equalTo(caseId));
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getString("initiationCode"), equalTo("J"));

    }

    @Test
    public void shouldRemoveAndAddDocuments() throws Exception {
        createMockEndpoints();
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referraReasonId);
        // when
         String response = getProsecutioncasesProgressionFor(caseId);
        // then
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);

        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertThat(prosecutioncasesJsonObject.getJsonArray("courtDocuments").getJsonObject(0).getJsonArray("materials").size(),equalTo(2));
        assertcourtDocuments(prosecutioncasesJsonObject.getJsonArray("courtDocuments").getJsonObject(0), caseId, courtDocumentId, materialIdActive);
        //Remove document
        addRemoveCourtDocument(courtDocumentId, materialIdActive,true);

        //read document
        response = getProsecutioncasesProgressionFor(caseId);
        prosecutioncasesJsonObject = getJsonObject(response);
        assertThat(prosecutioncasesJsonObject.getJsonArray("courtDocuments").size(),equalTo(0));
       //undo remove
        addRemoveCourtDocument(courtDocumentId, materialIdActive,false);
        response = getProsecutioncasesProgressionFor(caseId);
        prosecutioncasesJsonObject = getJsonObject(response);
        assertThat(prosecutioncasesJsonObject.getJsonArray("courtDocuments").size(),equalTo(1));
    }



    @Test
    public void shouldGetProsecutionCaseAtAGlance() throws Exception {
        createMockEndpoints();
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getProsecutioncasesProgressionFor(caseId);
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);

        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertThat(prosecutioncasesJsonObject.getJsonArray("courtDocuments").size(), equalTo(0));

        getProsecutionCaseAtAGlanceFor(caseId);

        poll(requestParams(getQueryUri("/prosecutioncases/"+ caseId), PROGRESSION_QUERY_CASE_AT_A_GLANCE_JSON).withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id",equalTo(caseId))
                        )));
    }



    public static void verifyInMessagingQueueForReferToCourtsRejcted() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForReferToCourtRejected);
        assertTrue(message.isPresent());
    }


}

