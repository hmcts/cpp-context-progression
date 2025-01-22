package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.join;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_ENFORCEMENT_ACKNOWLEDGMENT_ERROR;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_NOW_REQUEST_IGNORED_WITH_ACCOUNT_NUMBER;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_NOW_REQUEST_WITH_ACCOUNT_NUMBER;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.TIMEOUT;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.concurrent.TimeUnit;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

public class NowsRequestHelper extends AbstractTestHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.add-now-document-request+json";

    public void makeNowsRequest(final String requestId, final String requestPayload) {
        makePostCall(getWriteUrl("/nows"), WRITE_MEDIA_TYPE, requestPayload);
        verifyNowDocumentRequestProcessed(requestId);
    }

    public void verifyAccountNumberAddedToRequest(final String accountNumber, final String requestId) {
        final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(EVENT_NOW_REQUEST_WITH_ACCOUNT_NUMBER).getMessageConsumerClient();
        final JsonPath jsonResponse = retrieveMessageAsJsonPath(privateEventsConsumer);
        assertThat(jsonResponse.getString("accountNumber"), Is.is(accountNumber));
        assertThat(jsonResponse.getString("requestId"), Is.is(requestId));
    }

    public void verifyAccountNumberIgnoredToRequest(final String accountNumber, final String requestId) {
        final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(EVENT_NOW_REQUEST_IGNORED_WITH_ACCOUNT_NUMBER).getMessageConsumerClient();
        final JsonPath jsonResponse = retrieveMessageAsJsonPath(privateEventsConsumer);
        assertThat(jsonResponse.getString("accountNumber"), Is.is(accountNumber));
        assertThat(jsonResponse.getString("requestId"), Is.is(requestId));
    }

    public void verifyErrorEventRaised(final String errorCode, final String errorMessage) {
        final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(EVENT_ENFORCEMENT_ACKNOWLEDGMENT_ERROR).getMessageConsumerClient();
        final JsonPath jsonResponse = retrieveMessageAsJsonPath(privateEventsConsumer);
        assertThat(jsonResponse.getString("errorCode"), Is.is(errorCode));
        assertThat(jsonResponse.getString("errorMessage"), Is.is(errorMessage));
    }

    private void verifyNowDocumentRequestProcessed(final String requestId) {
        getNowDocumentRequestsFor(requestId, anyOf(
                withJsonPath("$.nowDocumentRequests[0].requestId", equalTo(requestId))
        ));
    }

    public static String getNowDocumentRequestsFor(final String requestId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(join("", "/nows/request/", requestId)), "application/vnd.progression.query.now-document-requests-by-request-id+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

}
