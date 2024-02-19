package uk.gov.moj.cpp.progression.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpStatus;

public class WireMockStubUtils {

    public static final String MATERIAL_UPLOAD_COMMAND =
            "/material-service/command/api/rest/material/material";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    public static final String BASE_URI = "http://" + HOST + ":8080";
    private static final String CONTENT_TYPE_QUERY_GROUPS = "application/vnd.usersgroups.groups+json";
    private static final String CONTENT_TYPE_QUERY_HEARING = "application/vnd.hearing.get.hearing+json";
    private static final String CONTENT_TYPE_QUERY_PERMISSION = "application/vnd.usersgroups.permissions+json";
    private static final String CONTENT_TYPE_QUERY_USER_ORGANISATION = "application/vnd.usersgroups.get-organisation-details-for-user+json";
    private static final String CONTENT_TYPE_QUERY_DEFENCE_SERVICE_USER_ROLE_IN_CASE = "application/vnd.advocate.query.role-in-case-by-caseid+json";
    private static final String CONTENT_TYPE_QUERY_HEARING_EVENT_LOG_CDES_DOCUMENT = "application/hearing.get-hearing-event-log-for-documents+json";


    static {
        configureFor(HOST, 8080);
    }

    public static void setupHearingQueryStub(final UUID hearingId, String resource) {
        stubPingFor("hearing-service");

        stubFor(get(urlPathEqualTo(format("/hearing-service/query/api/rest/hearing/hearings/{0}", hearingId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resource))));

        waitForStubToBeReady(format("/hearing-service/query/api/rest/hearing/hearings/{0}", hearingId), CONTENT_TYPE_QUERY_HEARING);
    }


    public static void setupAsAuthorisedUser(final UUID userId, final String responsePayLoad) {
        stubFor(get(urlMatching(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload(responsePayLoad))));
    }

    public static void setupAsAuthorisedUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user.json"))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), CONTENT_TYPE_QUERY_GROUPS);
    }

    public static void setupAsSystemUser(final UUID userId) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/usersgroups.get-systemuser-groups-by-user.json"))));

        waitForStubToBeReady(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId), CONTENT_TYPE_QUERY_GROUPS);
    }

    public static void stubUserGroupDefenceClientPermission(final String defendantId, final String responsePayLoad) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/permissions")))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_QUERY_PERMISSION)
                        .withBody(responsePayLoad)));

        waitForStubToBeReady(format("usersgroups-service/query/api/rest/usersgroups/permissions"), CONTENT_TYPE_QUERY_PERMISSION);
    }

    public static void stubAdvocateRoleInCaseByCaseId(final String caseId, final String responsePayLoad) {
        stubPingFor("defence-service");

        stubFor(get(urlPathEqualTo(format("/defence-service/query/api/rest/defence/cases/{0}", caseId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_QUERY_DEFENCE_SERVICE_USER_ROLE_IN_CASE)
                        .withBody(responsePayLoad)));

        waitForStubToBeReady(format("/defence-service/query/api/rest/defence/cases/{0}", caseId), CONTENT_TYPE_QUERY_DEFENCE_SERVICE_USER_ROLE_IN_CASE);
    }


    public static void stubUserGroupOrganisation(final String userId, final String responsePayLoad) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlPathEqualTo(format("/users/{0}/organisation", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayLoad)));

        waitForStubToBeReady(format("/users/{0}/organisation", userId), CONTENT_TYPE_QUERY_USER_ORGANISATION);
    }

    public static void stubUserGroupOrganisation(final String responsePayLoad) {
        stubPingFor("usersgroups-service");

        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*/organisation"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayLoad)));

    }

    public static void stubHearingEventLogs(final String caseId, final String responsePayLoad) {
        stubPingFor("hearing-service");

        stubFor(get(urlPathEqualTo(format("/hearing-service/query/api/rest/hearing/hearings/event-log?caseId={0}", caseId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayLoad)));

        waitForStubToBeReady(format("/hearing-service/query/api/rest/hearing/hearings/event-log?caseId={0}", caseId), CONTENT_TYPE_QUERY_HEARING_EVENT_LOG_CDES_DOCUMENT);


    }

    public static void stubAaagHearingEventLogs(final String applicationId, final String responsePayLoad) {
        stubPingFor("hearing-service");

        stubFor(get(urlPathEqualTo(format("/hearing-service/query/api/rest/hearing/hearings/event-log?applicationId={0}", applicationId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayLoad)));

        waitForStubToBeReady(format("/hearing-service/query/api/rest/hearing/hearings/event-log?applicationId={0}", applicationId), CONTENT_TYPE_QUERY_HEARING_EVENT_LOG_CDES_DOCUMENT);


    }

    public static void mockMaterialUpload() {
        stubPingFor("material-service");

        stubFor(post(urlMatching(MATERIAL_UPLOAD_COMMAND))
                .willReturn(aResponse().withStatus(HttpStatus.SC_ACCEPTED)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("")));
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Status.OK);
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType, final Status expectedStatus) {
        poll(requestParams(format("{0}/{1}", getBaseUri(), resource), mediaType).build())
                .until(status().is(expectedStatus));
    }

}
