package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.awaitility.Awaitility.await;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;

/**
 * Created by satishkumar on 10/12/2018.
 */
public class NotificationServiceStub {
    public static final String NOTIFICATION_NOTIFY_ENDPOINT = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/*";
    public static final String NOTIFICATION_NOTIFY_CONTENT_TYPE = "application/vnd.notificationnotify.letter+json";
    public static final String NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON = "application/vnd.notificationnotify.send-email-notification+json";


    public static void setUp() {
        stubPingFor("notificationnotify-service");
        stubFor(post(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(NOTIFICATION_NOTIFY_CONTENT_TYPE))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString()))
        );
        stubFor(post(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString()))
        );
    }


    public static void verifyEmailNotificationIsRaisedWithoutAttachment(final List<String> expectedValues) {
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
            expectedValues.forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            requestPatternBuilder.withRequestBody(notMatching("materialUrl"));
            verify(requestPatternBuilder);
        });
    }

    public static void verifyEmailNotificationIsRaisedWithAttachment(final List<String> expectedValues, final UUID materialId) {
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
            expectedValues.forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            requestPatternBuilder.withRequestBody(containing("materialUrl"));
            requestPatternBuilder.withRequestBody(containing(materialId.toString()));
            verify(requestPatternBuilder);
        });
    }

    public static void verifyCreateLetterRequested(final List<String> expectedValues) {
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> {
            RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
            expectedValues.forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            verify(requestPatternBuilder);
        });
    }

    public static void verifyNoLetterRequested(final List<String> notExpectedValues) {
        await().atMost(30, SECONDS).pollDelay(5, SECONDS).pollInterval(5, SECONDS).until(() -> {
            RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
            notExpectedValues.forEach(
                    notExpectedValue -> requestPatternBuilder.withRequestBody(containing(notExpectedValue))
            );
            requestPatternBuilder.withRequestBody(containing("letterUrl"));
            verify(0, requestPatternBuilder);
        });
    }

    public static void verifyNoEmailNotificationIsRaised(final List<String> notExpectedValues) {
        await().atMost(30, SECONDS).pollDelay(5, SECONDS).pollInterval(5, SECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
            notExpectedValues.forEach(
                    notExpectedValue -> requestPatternBuilder.withRequestBody(containing(notExpectedValue))
            );
            requestPatternBuilder.withRequestBody(containing("sendToAddress"));
            requestPatternBuilder.withRequestBody(containing("templateId"));
            verify(0, requestPatternBuilder);
        });
    }
}
