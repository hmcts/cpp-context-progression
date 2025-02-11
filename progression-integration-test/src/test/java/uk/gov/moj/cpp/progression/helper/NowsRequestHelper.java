package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_ENFORCEMENT_ACKNOWLEDGMENT_ERROR;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

public class NowsRequestHelper extends AbstractTestHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.add-now-document-request+json";

    public void makeNowsRequestAndVerify(final String requestId, final String requestPayload) {
        makePostCall(getWriteUrl("/nows"), WRITE_MEDIA_TYPE, requestPayload);
        if (null != requestId) {
            verifyNowDocumentRequestProcessed(requestId);
        }
    }

    public void verifyErrorEventRaised(final String errorCode, final String errorMessage) {
        final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(EVENT_ENFORCEMENT_ACKNOWLEDGMENT_ERROR).getMessageConsumerClient();
        final JsonPath jsonResponse = retrieveMessageAsJsonPath(privateEventsConsumer);
        assertThat(jsonResponse.getString("errorCode"), Is.is(errorCode));
        assertThat(jsonResponse.getString("errorMessage"), Is.is(errorMessage));
    }

    private void verifyNowDocumentRequestProcessed(final String requestId) {
        verifyNowDocumentRequestProcessed(requestId, anyOf(
                withJsonPath("$.nowDocumentRequests[0].requestId", equalTo(requestId))
        ));
    }

    public static String verifyNowDocumentRequestProcessed(final String requestId, final Matcher... matchers) {
        return pollForResponse("/nows/request/" + requestId,
                "application/vnd.progression.query.now-document-requests-by-request-id+json",
                USER_ID.toString(),
                matchers);
    }

}
