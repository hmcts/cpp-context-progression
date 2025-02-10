package uk.gov.moj.cpp.progression.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

/**
 * Class to set up stub.
 */
public class StubUtil {

    protected static final String DEFAULT_JSON_CONTENT_TYPE = "application/json";

    private static final int HTTP_STATUS_OK = 200;
    private static final int HTTP_STATUS_ACCEPTED = 202;
    private static final String MATERIAL_QUERY_URL = "/material-service/query/api/rest/material";

    public static void setupUsersGroupQueryStub() {
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user.json"))));

    }

    public static void setupHmctsUsersGroupQueryStub(final String payload) {
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(payload)));

    }

    public static void setupLoggedInUsersPermissionQueryStub() {
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.user-permissions.json"))));

    }


    public static void setupListingQueryStub() {
        stubFor(get(urlPathEqualTo("/listing-service/query/api/rest/listing/courtlistpayload"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/listing.courtlistpayload.json"))));

    }

    public static void setupForUshersMagistrateListQueryStub() {
        stubFor(get(urlPathEqualTo("/listing-service/query/api/rest/listing/courtlistpayload"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/listing.ushersmagistratepayload.json"))));

    }

    public static void setupForUshersCrownListQueryStub() {
        stubFor(get(urlPathEqualTo("/listing-service/query/api/rest/listing/courtlistpayload"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/listing.usherscrownpayload.json"))));

    }

    public static void setupStagingPubHubCommandStub() {
        stubFor(post(urlPathEqualTo("/stagingpubhub-service/command/api/rest/stagingpubhub/pubhub"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")));

    }

    public static void setupReferenceDataQueryCourtCenterDataByCourtNameStub() {
        stubFor(get(urlPathEqualTo("/referencedata-service/query/api/rest/referencedata/courtrooms"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/referencedata.get.ou.courtrooms.ou-courtroom-name.json"))));

    }

    public static void setupMaterialStub(String materialId) {
        stubFor(get(urlMatching("/material-service/query/api/rest/material/material/" + materialId + "/metadata"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/material.query.material-metadata.json").replace("MATERIAL_ID", materialId))));
    }

    public static void stubMaterialContent(final UUID materialId, final String materialContent, final String mimeType) {
        stubFor(get(urlPathEqualTo(MATERIAL_QUERY_URL + "/material/" + materialId))
                .withQueryParam("stream", equalTo("true"))
                .withQueryParam("requestPdf", equalTo("false"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, mimeType)
                        .withBody(materialContent)));
    }

    public static void setupMaterialStructuredPetQuery(final String structuredFormId) {
        stubFor(get(urlMatching(MATERIAL_QUERY_URL + "/structured-form/" + structuredFormId))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/material.query.structured-form.json").replace("STRUCTURED_FORM_ID", structuredFormId))));
    }

    public static void setupMaterialStructuredPetQueryForCotr(final String structuredFormId) {
        stubFor(get(urlMatching(MATERIAL_QUERY_URL + "/structured-form/" + structuredFormId))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/material.query.structured-form-for-cotr.json").replace("STRUCTURED_FORM_ID", structuredFormId))));
    }
}