package uk.gov.moj.cpp.progression.helper;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.getCaseByUrn;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.getOffencesForDefendantId;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_OFFENCES_FOR_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.helper.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;

public class UpdateOffencesForDefendantHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOffencesForDefendantHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.command.update-offences-for-defendant+json";

    private static final String TEMPLATE_ADD_OFFENCE_FOR_DEFENDANT_PAYLOAD = "raml/json/progression.command.update-offences-for-defendant.json";
    private static final String TEMPLATE_ADD_MULTIPLE_OFFENCE_FOR_DEFENDANT_PAYLOAD = "raml/json/progression.command.update-offences-for-defendant-multiple-offence.json";

    public static final String OFFENCE_CODE = "PS123FG";



    private MessageConsumer publicEventsConsumerForOffencesForDefendantUpdated =
            QueueUtil.publicEvents.createConsumer(
                    "public.progression.events.offences-for-defendant-updated");

    private String request;

    private final String defendantId ;

    private final String caseId;

    private final String offenceId = UUID.randomUUID().toString();

    public UpdateOffencesForDefendantHelper(String caseId, String defendantId) {
        this.defendantId = defendantId;
        this.caseId = caseId;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_OFFENCES_FOR_DEFENDANT_UPDATED);
    }

    public void updateOffencesForDefendant() {
        updateOffencesForDefendant("EWAY");
    }

    public void updateOffencesForDefendant(String modeOfTrial) {
        final String jsonString = getPayload(TEMPLATE_ADD_OFFENCE_FOR_DEFENDANT_PAYLOAD);
        JSONObject jsonObjectPayload = new JSONObject(jsonString);

        populateOffence(jsonObjectPayload, 0, defendantId, offenceId, "add offence to defendant test", 1, 1);
        request = jsonObjectPayload.toString();

        makePostCall(getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateMultipleOffencesForDefendant() {
        final String jsonString = getPayload(TEMPLATE_ADD_MULTIPLE_OFFENCE_FOR_DEFENDANT_PAYLOAD);
        JSONObject jsonObjectPayload = new JSONObject(jsonString);
        populateOffence(jsonObjectPayload, 0, defendantId, UUID.randomUUID().toString(), "3", 3, 1);

        populateOffence(jsonObjectPayload, 1, defendantId, UUID.randomUUID().toString(), "1", 1, 2);

        populateOffence(jsonObjectPayload, 2, defendantId, UUID.randomUUID().toString(), "2", 2, 3);

        request = jsonObjectPayload.toString();



        makePostCall(getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    private void populateOffence(JSONObject jsonObjectPayload, int index, String defendantId, String offenceId, String wording, int orderIndex, int count) {
        JSONObject jsonObject0 = jsonObjectPayload.getJSONArray("offences").getJSONObject(index);
        jsonObject0.put("defendantId", defendantId);
        jsonObject0.put("id", offenceId);
        jsonObject0.put("offenceCode", OFFENCE_CODE);
        jsonObject0.put("wording", wording);
        jsonObject0.put("indicatedPlea", getIndicatedPlea());
        jsonObject0.put("section", "Section 51");
        jsonObject0.put("startDate", "2010-08-01");
        jsonObject0.put("endDate", "2011-08-01");
        jsonObject0.put("orderIndex", orderIndex);
        jsonObject0.put("count", count);
    }


    private JSONObject getIndicatedPlea() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", UUID.randomUUID().toString());
        jsonObject.put("value", "INDICATED_GUILTY");

        return jsonObject;
    }

    public void verifyOffencesPleasForDefendantUpdated() {
        JsonPath jsRequest = new JsonPath(request);

        poll(getOffencesForDefendantId(caseId,defendantId))
                .until(
                        status().is(OK),
                        payload()
                                .isJson(allOf(
                                        withJsonPath("$.offences[0].indicatedPlea.value", is("INDICATED_GUILTY"))
                                        )
                                ));
    }




    /**
     * Retrieve message from queue and do additional verifications
     */
    public void verifyInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

            assertThat(jsonResponse.getString("id"), is(jsRequest.getString("id")));
    }

    public void verifyOffencesForDefendantUpdated() {
        JsonPath jsRequest = new JsonPath(request);

        poll(getOffencesForDefendantId(caseId,defendantId))
                .until(
                        status().is(OK),
                        payload()
                                .isJson(allOf(
                                        withJsonPath("$.offences[0].offenceCode", is(OFFENCE_CODE)),
                                        withJsonPath("$.offences[0].count", is(1))
                                        )
                                ));
    }

    public void verifyOffencesForDefendantUpdatedWithOffenceOrdering(String caseUrn) {
        JsonPath jsRequest = new JsonPath(request);

        poll(getOffencesForDefendantId(caseId,defendantId))
                .until(
                        status().is(OK),
                        payload()
                                .isJson(
                                        allOf(
                                                withJsonPath("$.offences[0].wording", is("1")),
                                                withJsonPath("$.offences[1].wording", is("2")),
                                                withJsonPath("$.offences[2].wording", is("3"))
                                        )
                                ));
        poll(getCaseByUrn(caseUrn))
                .until(
                        status().is(OK),
                        payload()
                                .isJson(
                                        allOf(
                                                withJsonPath("$.defendants[0].offences[0].wording", is("1")),
                                                withJsonPath("$.defendants[0].offences[1].wording", is("2")),
                                                withJsonPath("$.defendants[0].offences[2].wording", is("3"))
                                        )
                                ));
    }

    public String getDefendantId() {
        return defendantId;
    }

    public  void verifyInMessagingQueueOffencesForDefendentUpdated(){
        Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForOffencesForDefendantUpdated);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.caseId", Matchers.hasToString(
                Matchers.containsString(caseId)))));
    }

}
