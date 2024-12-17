package uk.gov.moj.cpp.progression.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutionCaseUpdateCaseMarkersHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseUpdateCaseMarkersHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-case-markers+json";

    private static final String TEMPLATE_UPDATE_CASE_MARKERS_PAYLOAD = "progression.update-case-markers.json";
    private static final String TEMPLATE_REMOVE_CASE_MARKERS_PAYLOAD = "progression.remove-case-markers.json";

    private final String prosecutionCaseId;
    private String request;

    public ProsecutionCaseUpdateCaseMarkersHelper(String prosecutionCaseId) {
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public void updateCaseMarkers() {
        request = getPayload(TEMPLATE_UPDATE_CASE_MARKERS_PAYLOAD);
        makePostCall(getWriteUrl("/prosecutioncases/" + prosecutionCaseId), WRITE_MEDIA_TYPE, request);

    }

    public void removeCaseMarkers() {
        request = getPayload(TEMPLATE_REMOVE_CASE_MARKERS_PAYLOAD);
        makePostCall(getWriteUrl("/prosecutioncases/" + prosecutionCaseId), WRITE_MEDIA_TYPE, request);
    }

    public void verifyInMessagingQueueForCaseMarkersUpdated(final JmsMessageConsumerClient publicEventsCaseMarkersUpdated) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventsCaseMarkersUpdated);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.prosecutionCaseId", hasToString(Matchers.containsString(prosecutionCaseId)))));
    }


}
