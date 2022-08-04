package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;

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

    public static void verifyHearingCommandInvoked() {
        verifyVejHearingStubCommandInvoked(VEJ_HEARING_COMMAND);
    }

    public static void verifyHearingDeletedCommandInvoked() {
        verifyVejHearingStubCommandInvoked(VEJ_HEARING_DELETED_COMMAND);
    }

    public static void verifyVejHearingStubCommandInvoked(final String commandEndPoint) {
        await().atMost(30, SECONDS).pollInterval(10, SECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlMatching(commandEndPoint));
            verify(1, requestPatternBuilder);
        });
    }
}
