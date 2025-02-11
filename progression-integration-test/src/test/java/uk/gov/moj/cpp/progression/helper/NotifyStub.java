package uk.gov.moj.cpp.progression.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;

public class NotifyStub {

    private static final String COMMAND_URL = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/";

    private static final String COMMAND_MEDIA_TYPE = "application/vnd.notificationnotify.email+json";
    private static final String COMMAND_PRINT_MEDIA_TYPE = "application/vnd.notificationnotify.letter+json";

    public static void stubNotifications() {
        stubFor(post(urlPathMatching(COMMAND_URL + ".*"))
                .withHeader(CONTENT_TYPE, equalTo(COMMAND_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubLetterNotifications() {
        stubFor(post(urlPathMatching(COMMAND_URL + ".*"))
                .withHeader(CONTENT_TYPE, equalTo(COMMAND_PRINT_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }
}
