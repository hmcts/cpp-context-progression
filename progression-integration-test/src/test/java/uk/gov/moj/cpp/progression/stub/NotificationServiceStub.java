package uk.gov.moj.cpp.progression.stub;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import java.util.List;
import java.util.UUID;

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


    public static void verifyEmailCreate(List<String> expectedValues) {

        RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT ));
        expectedValues.forEach(
                expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
        );
        verify(requestPatternBuilder);
    }
}
