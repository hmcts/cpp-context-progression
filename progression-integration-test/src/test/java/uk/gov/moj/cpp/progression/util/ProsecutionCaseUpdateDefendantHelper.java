package uk.gov.moj.cpp.progression.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProsecutionCaseUpdateDefendantHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseUpdateDefendantHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-defendant-for-prosecution-case+json";

    private static final String TEMPLATE_UPDATE_DEFENDANT_PAYLOAD = "progression.update-defendant-for-prosecution-case.json";
    private static final String TEMPLATE_UNCHANGED_DEFENDANT_PAYLOAD = "progression.update-unchanged-defendant-for-prosecution-case.json";
    private static final String TEMPLATE_UPDATE_YOUTH_FLAG_PAYLOAD = "progression.update-youth-flag-for-defendant.json";

    private final MessageConsumer publicEventsCaseDefendantChanged =
            QueueUtil.publicEvents
                    .createConsumer("public.progression.case-defendant-changed");

    private String request;

    private final String defendantId;

    private final String caseId;

    public ProsecutionCaseUpdateDefendantHelper(final String caseId, final String defendantId) {
        this.defendantId = defendantId;
        this.caseId = caseId;

        privateEventsConsumer = QueueUtil.privateEvents.createConsumer("progression.event.prosecution-case-defendant-updated");
    }

    public void updateDefendant() {
        final String jsonString = getPayload(TEMPLATE_UPDATE_DEFENDANT_PAYLOAD);
        updateDefendant(jsonString);
    }

    public void updateDefendant(String jsonString) {
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendant").put("id", defendantId);
        jsonObjectPayload.getJSONObject("defendant").put("prosecutionCaseId", caseId);

        request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    public void updateYouthFlagForDefendant() {
        final String jsonString = getPayload(TEMPLATE_UPDATE_YOUTH_FLAG_PAYLOAD);
        updateDefendant(jsonString);
    }

    public void updateSameDefendant() {
        final String jsonString = getPayload(TEMPLATE_UNCHANGED_DEFENDANT_PAYLOAD);
        updateDefendant(jsonString);
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

    public void verifyInMessagingQueueForDefendentChanged() {
        final Optional<JsonObject> message =
                QueueUtil.retrieveMessageAsJsonObject(publicEventsCaseDefendantChanged);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.defendant.prosecutionCaseId",
                Matchers.hasToString(Matchers.containsString(caseId)))));
    }

    public void closePrivateEventConsumer() throws JMSException {
        privateEventsConsumer.close();
    }
}
