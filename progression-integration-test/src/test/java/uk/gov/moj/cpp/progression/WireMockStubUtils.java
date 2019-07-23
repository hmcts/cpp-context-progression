package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.FileUtil.getPayload;
import org.apache.http.HttpStatus;

import javax.json.JsonObject;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;

/**
 * Utility class for setting stubs.
 */
public class WireMockStubUtils {
///nowsmaterial/.*
    public static final String MATERIAL_STATUS_UPLOAD_COMMAND =
                    "/results-service/command/api/rest/results/hearings/.*";
                                                           ////hearings/{hearingId}/nowsmaterial/{materialId}

   public static final String MATERIAL_STATUS_UPLOAD_COMMAND_EXACT = "/results-service/command/api/rest/results/hearings/%s/nowsmaterial/%s";

    public static final String MATERIAL_UPLOAD_COMMAND =
                    "/material-service/command/api/rest/material/material";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String CONTENT_TYPE_QUERY_GROUPS = "application/vnd.usersgroups.groups+json";
    private static final String CONTENT_TYPE_QUERY_PROGRESSION_CASE_DETAILS = "application/vnd.progression.query.caseprogressiondetail+json";
    public static final String BASE_URI = "http://" + HOST + ":8080";

    static {
        configureFor(HOST, 8080);
        reset();
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

    public static void mockProgressionCaseDetails(final UUID caseId, final String caseUrn) {
        stubPingFor("progression-service");

        stubFor(get(urlPathEqualTo(format("/progression-service/query/api/rest/progression/cases/{0}", caseId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_QUERY_PROGRESSION_CASE_DETAILS)
                        .withBody(getProgressionCaseJson(caseId, caseUrn).toString())));

        waitForStubToBeReady(format("/progression-service/query/api/rest/progression/cases/{0}", caseId), CONTENT_TYPE_QUERY_PROGRESSION_CASE_DETAILS);
    }

    public static final void mockMaterialUpload() {
        stubPingFor("material-service");

        stubFor(post(urlMatching(MATERIAL_UPLOAD_COMMAND))
                        .willReturn(aResponse().withStatus(HttpStatus.SC_ACCEPTED)
                                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                        .withBody("")));
    }

    public static final void mockUpdateResultsMaterialStatus() {
        stubPingFor("results-service");
        stubFor(post(urlMatching(
                        MATERIAL_STATUS_UPLOAD_COMMAND))
                                        .willReturn(aResponse().withStatus(HttpStatus.SC_ACCEPTED)
                                                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                                        .withBody("")));

        System.out.println("stubbing " + MATERIAL_STATUS_UPLOAD_COMMAND);
    }

    public static final void mockUpdateResultsMaterialStatus(UUID hearingId, UUID materialId) {
        stubPingFor("results-service");
        String url = String.format(MATERIAL_STATUS_UPLOAD_COMMAND_EXACT, hearingId.toString(), materialId.toString());
        stubFor(post(urlPathEqualTo(url))
                .willReturn(aResponse().withStatus(HttpStatus.SC_ACCEPTED)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("")));

        System.out.println("stubbing " + url);
    }

    static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Status.OK);
    }

    private static void waitForStubToBeReady(final String resource, final String mediaType, final Status expectedStatus) {
        poll(requestParams(format("{0}/{1}", getBaseUri(), resource), mediaType).build())
                        .until(status().is(expectedStatus));
    }

    private static JsonObject getProgressionCaseJson(final UUID caseId, final String caseUrn) {
        return createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("caseUrn", caseUrn)
                .build();
    }

}
