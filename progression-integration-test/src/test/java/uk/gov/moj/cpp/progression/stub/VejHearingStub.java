package uk.gov.moj.cpp.progression.stub;

import java.time.Duration;
import java.util.List;

import com.github.tomakehurst.wiremock.client.VerificationException;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import uk.gov.justice.services.test.utils.core.http.FibonacciPollWithStartAndMax;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INITIAL_INTERVAL_IN_MILLISECONDS;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INTERVAL_IN_MILLISECONDS;

public class VejHearingStub {

    public static final String VEJ_HEARING_COMMAND = "/vep/api/v1/hearing/details";
    public static final String VEJ_HEARING_DELETED_COMMAND = "/vep/api/v1/hearing/deleted";

    public static void stubVejHearing() {

        stubFor(post(urlMatching(VEJ_HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED))
        );

    }

    public static void stubVejHearingDeleted() {
        stubFor(post(urlPathEqualTo(VEJ_HEARING_DELETED_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED))
        );
    }

    public static void verifyHearingCommandInvoked(final List<String> expectedValues) {
        verifyVejHearingStubCommandInvoked(VEJ_HEARING_COMMAND, expectedValues);
    }

    public static void verifyHearingDeletedCommandInvoked(final List<String> expectedValues) {
        verifyVejHearingStubCommandInvoked(VEJ_HEARING_DELETED_COMMAND, expectedValues);
    }

    public static void verifyVejHearingStubCommandInvoked(final String commandEndPoint, final List<String> expectedValues) {
        await().atMost(30, SECONDS).pollInterval(new FibonacciPollWithStartAndMax(Duration.ofMillis(INITIAL_INTERVAL_IN_MILLISECONDS), Duration.ofMillis(INTERVAL_IN_MILLISECONDS))).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(commandEndPoint));
            expectedValues.forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );

            try {
                verify(requestPatternBuilder);
            } catch (VerificationException e) {
                return false;
            }
            return true;
        });
    }
}
