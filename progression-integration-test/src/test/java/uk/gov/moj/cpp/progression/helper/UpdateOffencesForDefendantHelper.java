package uk.gov.moj.cpp.progression.helper;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.getDefendantsByCaseId;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.getOffencesForDefendantId;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_OFFENCES_FOR_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.helper.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;

public class UpdateOffencesForDefendantHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOffencesForDefendantHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.command.update-offences-for-defendant+json";

    private static final String TEMPLATE_ADD_OFFENCE_FOR_DEFENDANT_PAYLOAD = "raml/json/progression.command.update-offences-for-defendant.json";

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
        JSONObject jsonObject = jsonObjectPayload.getJSONArray("offences").getJSONObject(0);
        jsonObject.put("defendantId", defendantId);
        jsonObject.put("id", offenceId);
        jsonObject.put("offenceCode", OFFENCE_CODE);
        jsonObject.put("modeOfTrial", modeOfTrial);
        jsonObject.put("wording", "add offence to defendant test");
        jsonObject.put("indicatedPlea", "GUILTY");
        jsonObject.put("section","Section 51");
        jsonObject.put("startDate","2010-08-01");
        jsonObject.put("endDate", "2011-08-01");
        jsonObject.put("orderIndex", 1);
        jsonObject.put("count", 1);
        request = jsonObjectPayload.toString();

        makePostCall(getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
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
            assertThat(jsonResponse.getString("indicatedPlea"), is(jsRequest.getString("indicatedPlea")));
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
