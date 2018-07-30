package uk.gov.moj.cpp.progression.util;


import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.test.utils.core.http.RequestParams;

import javax.ws.rs.core.Response.Status;


/**
 * Provides helper methods for tests to interact with Wiremock instance
 */
public class WiremockTestHelper {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String BASE_URI = "http://" + HOST + ":8080";

    public static void waitForStubToBeReady(String resource, String mediaType) {
        waitForStubToBeReady(resource, mediaType, Status.OK);
    }

    public static void waitForStubToBeReady(String resource, String mediaType, Status expectedStatus, String headerName, String headerValue) {
        RequestParams requestParams = requestParams(BASE_URI + resource, mediaType)
                .withHeader(headerName, headerValue)
                .build();
        poll(requestParams)
                .until(
                        status().is(expectedStatus)
                );
    }

    public static void waitForStubToBeReady(String resource, String mediaType, Status expectedStatus) {
        RequestParams requestParams = requestParams(BASE_URI + resource, mediaType).build();

        poll(requestParams)
                .until(
                        status().is(expectedStatus)
                );
    }
}
