package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.awaitility.Awaitility.await;

import java.util.List;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProbationCaseworkerStub {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProbationCaseworkerStub.class);

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
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(commandEndPoint));
            expectedValues.forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            try {
                verify(requestPatternBuilder);
            } catch (VerificationException e) {
                LOGGER.error(e.getMessage());
                return false;
            }
            return true;
        });
    }
}
