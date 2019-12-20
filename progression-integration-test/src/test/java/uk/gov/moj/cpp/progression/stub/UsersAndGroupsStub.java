package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import org.apache.http.HttpHeaders;

public class UsersAndGroupsStub {

    public static final String BASE_QUERY = "/usersgroups-service/query/api/rest/usersgroups";

    public static final String GROUPS = "/users/{0}/groups";
    public static final String GET_GROUPS_QUERY = BASE_QUERY + GROUPS;

    public static final String ORGANISATION = "/users/{0}/organisation";
    public static final String GET_ORGANISATION_QUERY = BASE_QUERY + ORGANISATION;
    public static final String GET_ORGANISATION_QUERY_MEDIA_TYPE = "application/vnd.usersgroups.get-organisation-name-for-user+json";

    public static final String ORGANISATION_DETAIL = "/organisations/{0}";
    public static final String GET_ORGANISATION_DETAIL_QUERY = BASE_QUERY + ORGANISATION_DETAIL;
    public static final String GET_ORGANISATION_DETAIL_QUERY_MEDIA_TYPE = "application/vnd.usersgroups.get-organisation-details+json";

    public static final String USERS_GROUPS_SERVICE_NAME = "usergroups-service";


    public static void stubGetOrganisationQuery(final String userId, final String organisationId, final String organisationName) {
        InternalEndpointMockUtils.stubPingFor(USERS_GROUPS_SERVICE_NAME);

        String body = getPayload("stub-data/usersgroups.get-organisation-details-by-user.json");
        body = body.replaceAll("%ORGANISATION_ID%", organisationId);
        body = body.replaceAll("%ORGANISATION_NAME%", organisationName);

        stubFor(get(urlPathEqualTo(format(GET_ORGANISATION_QUERY, userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));

        waitForStubToBeReady(format(GET_ORGANISATION_QUERY, userId), GET_ORGANISATION_QUERY_MEDIA_TYPE);
    }

    public static void stubGetUsersAndGroupsQueryForDefenceUsers(final String userId) {
        stubEndpoint(USERS_GROUPS_SERVICE_NAME,
                GET_GROUPS_QUERY,
                GET_ORGANISATION_QUERY_MEDIA_TYPE,
                userId,
                "stub-data/usersgroups.get-defenceuser-groups-by-user.json");
    }

    public static void stubGetUsersAndGroupsQueryForHMCTSUsers(final String userId) {
        stubEndpoint(USERS_GROUPS_SERVICE_NAME,
                GET_GROUPS_QUERY,
                GET_ORGANISATION_QUERY_MEDIA_TYPE,
                userId,
                "stub-data/usersgroups.get-hmcts-groups-by-user.json");
    }

    public static void stubGetUsersAndGroupsQueryForSystemUsers(final String userId) {
        stubEndpoint(USERS_GROUPS_SERVICE_NAME,
                GET_GROUPS_QUERY,
                GET_ORGANISATION_QUERY_MEDIA_TYPE,
                userId,
                "stub-data/usersgroups.get-systemuser-groups-by-user.json");
    }

    public static void stubGetOrganisationDetails(final String organisationId, final String organisationName) {

        InternalEndpointMockUtils.stubPingFor(USERS_GROUPS_SERVICE_NAME);

        String body = getPayload("stub-data/usersgroups.get-organisation-details-by-user.json");
        body = body.replaceAll("%ORGANISATION_ID%", organisationId);
        body = body.replaceAll("%ORGANISATION_NAME%", organisationName);

        stubFor(get(urlPathEqualTo(format(GET_ORGANISATION_DETAIL_QUERY, organisationId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));

        waitForStubToBeReady(format(GET_ORGANISATION_DETAIL_QUERY, organisationId), GET_ORGANISATION_DETAIL_QUERY_MEDIA_TYPE);
    }


    public static void stubEndpoint(final String serviceName, final String query,
                                    String queryMediaType,
                                    final String userId,
                                    final String responseBodyPath) {
        InternalEndpointMockUtils.stubPingFor(serviceName);
        stubFor(get(urlPathEqualTo(format(query, userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(responseBodyPath))));
        waitForStubToBeReady(format(query, userId), queryMediaType);
    }
}
