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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INITIAL_INTERVAL_IN_MILLISECONDS;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INTERVAL_IN_MILLISECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.test.utils.core.http.FibonacciPollWithStartAndMax;

public class ProbationCaseworkerStub {
    public static final String PROBATION_HEARING_COMMAND = "/probation/api/v1/hearing/details";
    public static final String PROBATION_HEARING_DELETED_COMMAND = "/probation/api/v1/hearing/deleted";

    public static void stubProbationHearing() {
        stubFor(post(urlMatching(PROBATION_HEARING_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED))
        );
    }

    public static void stubProbationHearingDeleted() {
        stubFor(post(urlPathEqualTo(PROBATION_HEARING_DELETED_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED))
        );
    }

    public static void verifyProbationHearingCommandInvoked(final List<String> expectedValues) {
        verifyProbationHearingStubCommandInvoked(PROBATION_HEARING_COMMAND, expectedValues);
    }

    public static void verifyProbationHearingDeletedCommandInvoked(final List<String> expectedValues) {
        verifyProbationHearingStubCommandInvoked(PROBATION_HEARING_DELETED_COMMAND, expectedValues);
    }

    private static void verifyProbationHearingStubCommandInvoked(final String commandEndPoint, final List<String> expectedValues) {
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
