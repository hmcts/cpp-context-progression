package uk.gov.moj.cpp.progression.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutionCaseUpdateCaseMarkersHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseUpdateCaseMarkersHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-case-markers+json";

    private static final String TEMPLATE_UPDATE_CASE_MARKERS_PAYLOAD = "progression.update-case-markers.json";
    private static final String TEMPLATE_REMOVE_CASE_MARKERS_PAYLOAD = "progression.remove-case-markers.json";

    private final MessageConsumer publicEventsCaseMarkersUpdated =
            QueueUtil.publicEvents
                    .createPublicConsumer("public.progression.case-markers-updated");

    private final String prosecutionCaseId;
    private String request;

    public ProsecutionCaseUpdateCaseMarkersHelper(String prosecutionCaseId) {
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public void updateCaseMarkers() {
        request = getPayload(TEMPLATE_UPDATE_CASE_MARKERS_PAYLOAD);
        makePostCall(getWriteUrl("/prosecutioncases/" + prosecutionCaseId), WRITE_MEDIA_TYPE, request);
        privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumer("progression.event.case-markers-updated");

    }

    public void removeCaseMarkers() {
        request = getPayload(TEMPLATE_REMOVE_CASE_MARKERS_PAYLOAD);
        makePostCall(getWriteUrl("/prosecutioncases/" + prosecutionCaseId), WRITE_MEDIA_TYPE, request);
        privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumer("progression.event.case-markers-updated");

    }

    public void verifyInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("id"), is(jsRequest.getString("id")));
    }

    public void verifyInMessagingQueueForCaseMarkersUpdated() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventsCaseMarkersUpdated);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.prosecutionCaseId", hasToString(Matchers.containsString(prosecutionCaseId)))));
    }
}
