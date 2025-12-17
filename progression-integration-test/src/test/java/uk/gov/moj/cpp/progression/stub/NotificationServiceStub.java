package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class NotificationServiceStub {
    public static final String NOTIFICATION_NOTIFY_ENDPOINT = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/.*";
    public static final String NOTIFICATION_NOTIFY_CONTENT_TYPE = "application/vnd.notificationnotify.letter+json";
    public static final String NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON = "application/vnd.notificationnotify.email+json";

    static final String NOTIFY_CMS_TRANSFORM_AND_SEND = "/notification-cms/v1/transformAndSendCms";
    static final String COTR_FORM_SERVED = "cotr-form-served";

    public static void stubPostCallsNotificationNotify() {
        stubFor(post(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(NOTIFICATION_NOTIFY_CONTENT_TYPE))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, randomUUID().toString()))
        );
        stubFor(post(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, randomUUID().toString()))
        );

        stubFor(post(urlPathEqualTo(NOTIFY_CMS_TRANSFORM_AND_SEND))
                .withRequestBody(equalToJson("{\"businessEventType\":\"defence-requested-to-notify-cps-of-material\",\"subjectBusinessObjectId\":\"7325fcd3-fb0a-4dbb-a876-848f6893aa09\",\"subjectDetails\":{\"material\":\"5e1cc18c-76dc-47dd-99c1-d6f87385edf1\",\"materialContentType\":\"pdf\",\"materialType\":\"SJP Notice\",\"prosecutionCaseSubject\":{\"caseUrn\":\"3cdbf809\",\"defendantSubject\":{\"asn\":\"arrest123\",\"prosecutorDefendantId\":\"TFL12345-ABC\"},\"prosecutingAuthority\":\"GB10056\"}}}"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("Ocp-Apim-Subscription-Key", "dummyValue")
                        .withHeader("Ocp-Apim-Trace", "true")));

        stubFor(post(urlPathEqualTo("/CPS/v1/notification/bcm-notification"))
                .withRequestBody(equalToJson("{\"notificationDate\":\"2022-06-27T14:52:36.101Z\",\"notificationType\":\"bcm-form-updated\",\"bcmNotification\":{\"prosecutionCaseSubject\":{\"caseURN\":\"caseUrn123\",\"prosecutingAuthority\":\"ouCode123\"},\"defendantSubject\":{\"cpsDefendantId\":\"41725716-97ee-4a2b-acb8-52c1d12363ad\",\"asn\":\"arrestSummonsNo1\"},\"pleas\":[{\"cjsOffenceCode\":\"\",\"offenceSequenceNo\":1,\"offenceTitle\":\"Offence Title 1\",\"pleaValue\":\"Guilty\"},{\"cjsOffenceCode\":\"\",\"offenceSequenceNo\":2,\"offenceTitle\":\"Offence Title 2\",\"pleaValue\":\"\"},{\"cjsOffenceCode\":\"\",\"offenceSequenceNo\":3,\"offenceTitle\":\"Offence Title 3\",\"pleaValue\":\"Not Guilty\"},{\"cjsOffenceCode\":\"\",\"offenceSequenceNo\":4,\"offenceTitle\":\"Offence Title 4\",\"pleaValue\":\"If there is a lim\"}],\"realIssuesInCase\":\"realIssue1\",\"evidenceNeededForEffectivePTPH\":\"otherEvidencePriorPtph1\",\"otherInformation\":\"anyOther1\"}}"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("Ocp-Apim-Subscription-Key", "dummyValue")
                        .withHeader("Ocp-Apim-Trace", "true")));
    }

    public static void verifyEmailNotificationIsRaisedWithoutAttachment(final List<String> expectedValues, CountMatchingStrategy expectedCount) {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
            expectedValues.forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            requestPatternBuilder.withRequestBody(notMatching("materialUrl"));
            try {
                verify(expectedCount, requestPatternBuilder);
            } catch (VerificationException e) {
                return false;
            }
            return true;
        });
    }

    public static void verifyEmailNotificationIsRaisedWithoutAttachment(final List<String> expectedValues, int count) {
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedValues, exactly(count));
    }

    public static void verifyEmailNotificationIsRaisedWithoutAttachment(final List<String> expectedValues) {
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedValues, moreThanOrExactly(1));
    }

    public static void verifyEmailNotificationIsRaisedWithAttachment(final List<String> expectedValues) {
        verifyEmailNotificationIsRaisedWithAttachment(expectedValues, Optional.empty());
    }

    public static void verifyEmailNotificationIsRaisedWithAttachment(final List<String> expectedValues, Optional<UUID> materialId) {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
            expectedValues.forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            requestPatternBuilder.withRequestBody(containing("materialUrl"));
            materialId.ifPresent(m -> requestPatternBuilder.withRequestBody(containing(m.toString())));
            try {
                verify(requestPatternBuilder);
            } catch (VerificationException e) {
                return false;
            }
            return true;
        });
    }

    public static void verifyCreateLetterRequested(final List<String> expectedValues) {
        await().atMost(30, SECONDS).pollInterval(500, MILLISECONDS).until(() -> {
            RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
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

    public static void stubCotrFormServedNotificationCms() {
        stubFor(post(urlPathEqualTo(NOTIFY_CMS_TRANSFORM_AND_SEND))
                .withRequestBody(containing(COTR_FORM_SERVED))
                .willReturn(aResponse().withStatus(SC_OK)));
    }
}
