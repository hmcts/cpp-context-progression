package uk.gov.moj.cpp.progression.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import io.restassured.path.json.JsonPath;

public class CaseProsecutorUpdateHelper extends AbstractTestHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-cps-prosecutor-details+json";

    private static final String TEMPLATE_UPDATE_CASE_PROSECUTOR_PAYLOAD = "progression.update-cps-prosecutor-details.json";

    private static final String NEW_PROSECUTION_AUTH_CODE = "CPS-EM";
    private final String prosecutionCaseId;
    private String request;

    public CaseProsecutorUpdateHelper(String prosecutionCaseId) {
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public void updateCaseProsecutor() {
        request = getPayload(TEMPLATE_UPDATE_CASE_PROSECUTOR_PAYLOAD);
        makePostCall(getWriteUrl("/prosecutioncases/" + prosecutionCaseId), WRITE_MEDIA_TYPE, request);
    }

    public void verifyInMessagingQueueForProsecutorUpdated(int hearingsCount, final JmsMessageConsumerClient publicEventsCaseProsecutorUpdated) {
        JsonPath jsonResponse = retrieveMessageAsJsonPath(publicEventsCaseProsecutorUpdated, isJson(allOf(
                withJsonPath("$.prosecutionCaseId", is(prosecutionCaseId)),
                withoutJsonPath("$.oldCpsProsecutor"),
                withJsonPath("$.prosecutionAuthorityCode", is(NEW_PROSECUTION_AUTH_CODE)),
                withJsonPath("$.hearingIds", hasSize(hearingsCount)
                ))));
        assertNotNull(jsonResponse);
    }
}
