package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
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

import java.util.List;

import javax.json.Json;

import org.apache.http.HttpHeaders;

public class UsersAndGroupsStub {

    public static final String BASE_QUERY = "/usersgroups-service/query/api/rest/usersgroups";

    public static final String GROUPS = "/users/{0}/groups";
    public static final String GROUPS_BY_LOGGEDIN_USER = "/users/logged-in-user/groups";
    public static final String GET_GROUPS_QUERY = BASE_QUERY + GROUPS;
    public static final String GET_GROUPS_BY_LOGGEDIN_USER_QUERY = BASE_QUERY + GROUPS_BY_LOGGEDIN_USER;

    public static final String ORGANISATION = "/users/{0}/organisation";
    public static final String GET_ORGANISATION_QUERY = BASE_QUERY + ORGANISATION;
    public static final String GET_ORGANISATION_QUERY_MEDIA_TYPE = "application/vnd.usersgroups.get-organisation-name-for-user+json";

    public static final String ORGANISATION_DETAIL = "/organisations/{0}";
    public static final String GET_ORGANISATION_DETAIL_QUERY = BASE_QUERY + ORGANISATION_DETAIL;
    private static final String GROUPS_FOR_LOGGED_IN_USER_MEDIA_TYPE =
            "application/vnd.usersgroups.get-logged-in-user-groups+json";
    private static final String GET_ORGANISATION_DETAIL_FOR_USER_QUERY = BASE_QUERY + "/users/{0}/organisation";


    public static final String USERS_GROUPS_SERVICE_NAME = "usergroups-service";
    public static final String GET_ORGANISATIONS_DETAILS_FORIDS_QUERY = BASE_QUERY + "/organisations.*";
    public static final String GET_ORGANISATIONS_DETAILS_FOR_TYPES_QUERY = BASE_QUERY + "/organisationlist";


    public static void stubGetOrganisationQuery(final String userId, final String organisationId, final String organisationName) {
        String body = getPayload("stub-data/usersgroups.get-organisation-details-by-user.json");
        body = body.replaceAll("%ORGANISATION_ID%", organisationId);
        body = body.replaceAll("%ORGANISATION_NAME%", organisationName);

        stubFor(get(urlPathEqualTo(format(GET_ORGANISATION_QUERY, userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void stubGetGroupsForLoggedInQuery(final String userId) {
        stubEndpoint(USERS_GROUPS_SERVICE_NAME,
                GET_GROUPS_BY_LOGGEDIN_USER_QUERY,
                GROUPS_FOR_LOGGED_IN_USER_MEDIA_TYPE,
                userId,
                "stub-data/usersGroups.get-Groups-by-loggedIn-user.json");
    }
    public static void stubGetUsersAndGroupsQuery() {
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-non-defence-groups-by-user.json"))));
    }

    public static void stubGetUsersAndGroupsUserDetailsQuery(final String userId) {
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/".concat(userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.usersgroups.user-details+json")
                        .withBody(getPayload("stub-data/usersgroups.user-details.json").replaceAll("%USER_ID%", userId))));
    }

    public static void stubGetOrganisationDetailsForUser(final String userId, final String organisationId, final String organisationName) {

        String body = getPayload("stub-data/usersgroups.get-organisation-details-by-user.json");
        body = body.replaceAll("%ORGANISATION_ID%", organisationId);
        body = body.replaceAll("%ORGANISATION_NAME%", organisationName);

        stubFor(get(urlPathEqualTo(format(GET_ORGANISATION_DETAIL_FOR_USER_QUERY, userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void stubGetUsersAndGroupsQueryForSystemUsers(final String userId) {
        stubEndpoint(USERS_GROUPS_SERVICE_NAME,
                GET_GROUPS_QUERY,
                GET_ORGANISATION_QUERY_MEDIA_TYPE,
                userId,
                "stub-data/usersgroups.get-systemuser-groups-by-user.json");
    }

    public static void stubGetOrganisationDetails(final String organisationId, final String organisationName) {

        String body = getPayload("stub-data/usersgroups.get-organisation-details-by-user.json");
        body = body.replaceAll("%ORGANISATION_ID%", organisationId);
        body = body.replaceAll("%ORGANISATION_NAME%", organisationName);

        stubFor(get(urlPathEqualTo(format(GET_ORGANISATION_DETAIL_QUERY, organisationId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void stubGetOrganisationDetailForLAAContractNumber(final String laaContractNumber, final String organisationId, final String organisationName) {
        String body = getPayload("stub-data/usersGroups.get-organisation-details-by-laaContractNumber.json");
        body = body.replaceAll("%LAA_CONTRACT_NUMBER%", laaContractNumber);
        body = body.replaceAll("%ORGANISATION_ID%", organisationId);
        body = body.replaceAll("%ORGANISATION_NAME%", organisationName);

        stubFor(get(urlPathEqualTo(format(GET_ORGANISATION_DETAIL_QUERY, laaContractNumber)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }


    public static void stubGetOrganisationDetailForLAAContractNumberAsEmpty(final String laaContractNumber) {
        String body = Json.createObjectBuilder().build().toString();

        stubFor(get(urlPathEqualTo(format(GET_ORGANISATION_DETAIL_QUERY, laaContractNumber)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void stubEndpoint(final String serviceName, final String query,
                                    String queryMediaType,
                                    final String userId,
                                    final String responseBodyPath) {
        stubFor(get(urlPathEqualTo(format(query, userId)))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getPayload(responseBodyPath))));
    }

    public static void stubGetOrganisationDetailForIds(final String resourceName, final List<String> organisationIds ,final String userId ) {
        String body = getPayload(resourceName);
        stubFor(get(urlMatching(GET_ORGANISATIONS_DETAILS_FORIDS_QUERY))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                .withHeader(ID,randomUUID().toString())
                .withHeader(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON)
                .withBody(body)));
    }

    public static void stubGetOrganisationDetailForTypes(final String resourceName,final String userId ) {
        String body = getPayload(resourceName);
        stubFor(get(urlPathMatching(GET_ORGANISATIONS_DETAILS_FOR_TYPES_QUERY))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID,randomUUID().toString())
                        .withHeader(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON)
                        .withBody(body)));
    }

    public static void stubUserWithPermission(final String userId, final String body) {
        removeStub(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions")));

        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, userId)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(body)));
    }

}
