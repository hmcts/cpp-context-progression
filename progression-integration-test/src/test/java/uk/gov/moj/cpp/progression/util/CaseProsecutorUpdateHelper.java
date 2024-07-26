package uk.gov.moj.cpp.progression.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;

public class CaseProsecutorUpdateHelper extends AbstractTestHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-cps-prosecutor-details+json";

    private static final String TEMPLATE_UPDATE_CASE_PROSECUTOR_PAYLOAD = "progression.update-cps-prosecutor-details.json";

    private static final String NEW_PROSECUTION_AUTH_CODE = "TFL-CM";

    private final MessageConsumer publicEventsCaseProsecutorUpdated = publicEvents.createPublicConsumer("public.progression.events.cps-prosecutor-updated");

    private final String prosecutionCaseId;
    private String request;

    private MessageConsumer caseProsecutorUpdatedPrivateEventsConsumer;

    public CaseProsecutorUpdateHelper(String prosecutionCaseId) {
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public void updateCaseProsecutor() {
        request = getPayload(TEMPLATE_UPDATE_CASE_PROSECUTOR_PAYLOAD);
        privateEventsConsumer = privateEvents.createPrivateConsumer("progression.event.cps-prosecutor-updated");
        caseProsecutorUpdatedPrivateEventsConsumer = privateEvents.createPrivateConsumer("progression.event.case-cps-prosecutor-updated");
        makePostCall(getWriteUrl("/prosecutioncases/" + prosecutionCaseId), WRITE_MEDIA_TYPE, request);
    }

    public void verifyInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.getString("prosecutionAuthorityCode"), is(jsRequest.getString("prosecutionAuthorityCode")));
        assertThat(jsonResponse.getString("oldCpsProsecutor"), is(jsRequest.getString("oldCpsProsecutor")));

        jsonResponse = retrieveMessage(caseProsecutorUpdatedPrivateEventsConsumer);
        assertThat(jsonResponse.getString("prosecutionAuthorityCode"), is(jsRequest.getString("prosecutionAuthorityCode")));
        assertThat(jsonResponse.getString("oldCpsProsecutor"), is(jsRequest.getString("oldCpsProsecutor")));
    }

    public void verifyInMessagingQueueForProsecutorUpdated(int hearingsCount) {
        retrieveMessage(publicEventsCaseProsecutorUpdated, isJson(allOf(
                withJsonPath("$.prosecutionCaseId", is(prosecutionCaseId)),
                withoutJsonPath("$.oldCpsProsecutor"),
                withJsonPath("$.prosecutionAuthorityCode", is(NEW_PROSECUTION_AUTH_CODE)),
                withJsonPath("$.hearingIds", hasSize(hearingsCount)
        ))));
    }
}
