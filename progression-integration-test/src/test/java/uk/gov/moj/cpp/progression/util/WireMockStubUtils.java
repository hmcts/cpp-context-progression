package uk.gov.moj.cpp.progression.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import java.util.UUID;

import org.apache.http.HttpStatus;

public class WireMockStubUtils {

    public static final String MATERIAL_UPLOAD_COMMAND =
            "/material-service/command/api/rest/material/material";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    public static final String BASE_URI = "http://" + HOST + ":8080";
    private static final String CONTENT_TYPE_QUERY_PERMISSION = "application/vnd.usersgroups.permissions+json";
    private static final String CONTENT_TYPE_QUERY_DEFENCE_SERVICE_USER_ROLE_IN_CASE = "application/vnd.advocate.query.role-in-case-by-caseid+json";


    static {
        configureFor(HOST, 8080);
    }

    public static void setupHearingQueryStub(final UUID hearingId, String resource) {
        stubFor(get(urlPathEqualTo(format("/hearing-service/query/api/rest/hearing/hearings/{0}", hearingId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(resource))));
    }


    public static void setupAsAuthorisedUser(final UUID userId, final String responsePayLoad) {
        stubFor(get(urlMatching(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload(responsePayLoad))));
    }

    public static void setupAsAuthorisedUser(final UUID userId) {
        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user.json"))));
    }

    public static void setupAsSystemUser(final UUID userId) {
        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/users/{0}/groups", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload("stub-data/usersgroups.get-systemuser-groups-by-user.json"))));
    }

    public static void stubUserGroupDefenceClientPermission(final String responsePayLoad) {
        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/permissions")))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_QUERY_PERMISSION)
                        .withBody(responsePayLoad)));
    }

    public static void stubAdvocateRoleInCaseByCaseId(final String caseId, final String responsePayLoad) {
        stubFor(get(urlPathEqualTo(format("/defence-service/query/api/rest/defence/cases/{0}", caseId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_QUERY_DEFENCE_SERVICE_USER_ROLE_IN_CASE)
                        .withBody(responsePayLoad)));
    }


    public static void stubUserGroupOrganisation(final String userId, final String responsePayLoad) {
        stubFor(get(urlPathEqualTo(format("/users/{0}/organisation", userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayLoad)));
    }

    public static void stubGetUserOrganisation(final String organisationId, final String responsePayLoad) {

        stubFor(get(urlPathEqualTo(format("/usersgroups-service/query/api/rest/usersgroups/organisations/{0}", organisationId)))
                        .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayLoad)));
    }


    public static void stubUserGroupOrganisation(final String responsePayLoad) {
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*/organisation"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayLoad)));

    }

    public static void stubHearingEventLogs(final String caseId, final String responsePayLoad) {
        stubFor(get(urlPathMatching("/hearing-service/query/api/rest/hearing/hearings/event-log"))
                .withQueryParam("caseId", matching(caseId))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayLoad)));
    }

    public static void stubAaagHearingEventLogs(final String applicationId, final String responsePayLoad) {
        stubFor(get(urlPathMatching("/hearing-service/query/api/rest/hearing/hearings/event-log"))
                .withQueryParam("applicationId", matching(applicationId))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayLoad)));
    }

    public static void mockMaterialUpload() {
        stubFor(post(urlMatching(MATERIAL_UPLOAD_COMMAND))
                .willReturn(aResponse().withStatus(HttpStatus.SC_ACCEPTED)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("")));
    }

}
