package uk.gov.moj.cpp.progression.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.cpp.progression.domain.notification.Subscription;

import java.util.List;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class NotifyStub {

    public static final String COMMAND_URL = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/";
    public static final String COMMAND_MEDIA_TYPE = "application/vnd.notificationnotify.email+json";

    public static void stubNotifications() {
        InternalEndpointMockUtils.stubPingFor("notificationnotify-service");

        stubFor(post(urlPathMatching(COMMAND_URL + ".*"))
                .withHeader(CONTENT_TYPE, equalTo(COMMAND_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void verifyNotification(final Subscription subscription, List<String> expectedValues) {

        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(COMMAND_URL + ".*"))
                .withRequestBody(containing(subscription.getDestination()));
        expectedValues.forEach(
                expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
        );
        verify(requestPatternBuilder);
    }
}
