package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.join;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.Cleaner.closeSilently;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_ENFORCEMENT_ACKNOWLEDGMENT_ERROR;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_NOW_REQUEST_IGNORED_WITH_ACCOUNT_NUMBER;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_NOW_REQUEST_WITH_ACCOUNT_NUMBER;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.concurrent.TimeUnit;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

public class NowsRequestHelper extends AbstractTestHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.add-now-document-request+json";

    public void makeNowsRequest(final String requestId, final String requestPayload) {
        makePostCall(getWriteUrl("/nows"), WRITE_MEDIA_TYPE, requestPayload);
        verifyNowDocumentRequestProcessed(requestId);
    }

    public void verifyAccountNumberAddedToRequest(final String accountNumber, final String requestId) {
        privateEventsConsumer = privateEvents.createPrivateConsumer(EVENT_NOW_REQUEST_WITH_ACCOUNT_NUMBER);
        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.getString("accountNumber"), Is.is(accountNumber));
        assertThat(jsonResponse.getString("requestId"), Is.is(requestId));
    }

    public void verifyAccountNumberIgnoredToRequest(final String accountNumber, final String requestId) {
        privateEventsConsumer = privateEvents.createPrivateConsumer(EVENT_NOW_REQUEST_IGNORED_WITH_ACCOUNT_NUMBER);
        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.getString("accountNumber"), Is.is(accountNumber));
        assertThat(jsonResponse.getString("requestId"), Is.is(requestId));
    }

    public void verifyErrorEventRaised(final String errorCode, final String errorMessage) {
        privateEventsConsumer = privateEvents.createPrivateConsumer(EVENT_ENFORCEMENT_ACKNOWLEDGMENT_ERROR);
        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
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
                .timeout(40, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    @Override
    public void close() {
        super.close();
        closeSilently(privateEventsConsumer);
    }
}
