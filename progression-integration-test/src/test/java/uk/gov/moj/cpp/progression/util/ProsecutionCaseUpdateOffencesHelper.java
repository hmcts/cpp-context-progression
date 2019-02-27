package uk.gov.moj.cpp.progression.util;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import java.util.Optional;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;


public class ProsecutionCaseUpdateOffencesHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseUpdateOffencesHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-offences-for-prosecution-case+json";

    private static final String TEMPLATE_UPDATE_OFFENCES_PAYLOAD = "progression.update-offences-for-prosecution-case.json";
    private final MessageConsumer publicEventsConsumerForOffencesUpdated =
            QueueUtil.publicEvents.createConsumer(
                    "public.progression.defendant-offences-changed");

    private String request;

    private final String defendantId;

    private final String caseId;

    private final String offenceId;

    public ProsecutionCaseUpdateOffencesHelper(final String caseId, final String defendantId, final String offenceId) {
        this.defendantId = defendantId;
        this.caseId = caseId;
        this.offenceId = offenceId;

        privateEventsConsumer = QueueUtil.privateEvents.createConsumer("progression.event.prosecution-case-offences-updated");
    }

    public void updateOffences() {
        updateOffences(this.offenceId);
    }

    public void updateOffences(final String offenceId) {
        final String jsonString = getPayload(TEMPLATE_UPDATE_OFFENCES_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").put("defendantId", defendantId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").put("prosecutionCaseId", caseId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").getJSONArray("offences").getJSONObject(0).put("id", offenceId);

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }


    /**
     * Retrieve message from queue and do additional verifications
     */
    public void verifyInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("id"), is(jsRequest.getString("id")));
    }

    public void verifyInMessagingQueueForOffencesUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForOffencesUpdated);
        assertTrue(message.isPresent());
    }


}
