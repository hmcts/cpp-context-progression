package uk.gov.moj.cpp.progression.helper;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import java.util.Optional;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.getCaseById;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.getDefendantsByCaseId;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_ALLOCATION_DECISION_UPDATED_FOR_DEFENDANT;
import static uk.gov.moj.cpp.progression.helper.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;

public class UpdateAllocationDecisionForDefendantHelper extends AbstractTestHelper {


    private static final Logger LOGGER = LoggerFactory.getLogger(AddDefendantHelper.class);

    private static final String WRITE_MEDIA_TYPE =
                    "application/vnd.progression.command.update-allocation-decision-for-defendant+json";

    private static final String TEMPLATE_PAYLOAD =
                    "raml/json/progression.command.update-allocation-decision-for-defendant.json";

    private MessageConsumer publicEventsConsumerForOffenceDoesNotHaveModeOfTrial =
            QueueUtil.publicEvents.createConsumer(
                    "public.progression.events.defendant-offences-does-not-have-required-modeoftrial");

    private MessageConsumer privateEventsConsumerForAllocationDecisionRemoved =
            QueueUtil.privateEvents.createConsumer(
                    "progression.events.defendant-allocation-decision-removed");

    private String caseId;
    private String request;

    private String defendantId;

    public UpdateAllocationDecisionForDefendantHelper(String caseId, String defendantId) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_ALLOCATION_DECISION_UPDATED_FOR_DEFENDANT);
    }

    public String getDefendantId() {
        return defendantId;
    }

    public void updateAllocationDecision() {
        String jsonString = getPayload(TEMPLATE_PAYLOAD);
        JSONObject jsonObject = new JSONObject(jsonString);
        request = jsonObject.toString();

        String writeUrl = "/cases/CASEID/defendants/DEFENDANTID"
                        .replace("CASEID", caseId)
                        .replace("DEFENDANTID", defendantId);
        makePostCall(getWriteUrl(writeUrl), WRITE_MEDIA_TYPE, request);
    }


    /**
     * Retrieve message from queue and do additional verifications
     */
    public void verifyInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);

        assertThat(jsonResponse.get("allocationDecision"),
                        equalTo(jsRequest.get("allocationDecision")));
    }

    public void verifyAllocationDecisionUpdated() {
        JsonPath jsRequest = new JsonPath(request);

        poll(getDefendantsByCaseId(caseId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.defendants", hasSize(1)),
                                withJsonPath("$.defendants[0].allocationDecision", is(jsRequest.getString("allocationDecision")))
                        ))
                );

    }

    public void verifyAllocationDecisionRemoved() {
        JsonPath jsRequest = new JsonPath(request);

        poll(getDefendantsByCaseId(caseId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.defendants", hasSize(1)),
                                withoutJsonPath("$.defendants[0].allocationDecision")
                        ))
                );

    }

    public  void verifyInMessagingQueueAllocationDecisionRemoved(){
        Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(privateEventsConsumerForAllocationDecisionRemoved);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.caseId", Matchers.hasToString(
                Matchers.containsString(caseId)))));
    }

    public  void verifyInMessagingQueueOffenceDoesNotHaveModeOfTrial(){
        Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForOffenceDoesNotHaveModeOfTrial);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.caseId", Matchers.hasToString(
                Matchers.containsString(caseId)))));
    }
}
