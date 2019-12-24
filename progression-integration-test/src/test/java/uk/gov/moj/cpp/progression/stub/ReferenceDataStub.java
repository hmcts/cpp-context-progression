package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;


public class ReferenceDataStub {

    public static void stubQueryOffences(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject offences = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/offences";
        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("cjsoffencecode", matching(".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(offences.toString())));

        waitForStubToBeReady(urlPath + "?cjsoffencecode", "application/vnd.referencedata.query.offences+json");
    }

    public static void stubQueryLocalJusticeArea(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject jsonObject = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/local-justice-areas?.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(jsonObject.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.local-justice-areas+json");
    }

    public static void stubQueryOrganisation(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject judge = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/organisation-units/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(judge.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.organisation-unit.v2+json");
    }

    public static void stubQueryCourtsCodeData(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/local-justice-area-court-prosecutor-mapping/courts?.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.local-justice-area-court-prosecutor-mapping-courts.+json");
    }

    public static void stubQueryOrganisationUnitsData(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/organisationunits?.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.organisationunits+json");
    }

    public static void stubQueryDocumentTypeData(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/document-metadata/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.document+json");
    }

    public static void stubQueryAllDocumentsTypeData(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/documents-metadata/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.get-all-document-metadata+json");
    }


    public static void stubQueryReferralReasons(final String resourceName, final UUID referralReasonId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject referralReasonsJson = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String responseBody = referralReasonsJson.toString()
                .replace("RANDOM_REFERRAL_ID", referralReasonId.toString());

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/referral-reasons";
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responseBody)));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.referral-reasons+json");
    }

    public static void stubQueryJudiciaries(final String resourceName, final UUID judiciaryId) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject referralReasonsJson = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String responseBody = referralReasonsJson.toString();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/judiciaries";
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responseBody)));

        waitForStubToBeReady(urlPath, "application/vnd.reference-data.judiciaries+json");
    }

    public static void stubQueryEthinicityData(final String resourceName, final UUID id) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/ethnicities";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString().replace("ETHNICITYID", id.toString()))));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.ethnicities+json");
    }

    public static void stubQueryNationalityData(final String resourceName, final UUID id) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/country-nationality";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString().replace("NATIONALITYID", id.toString()))));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.country-nationality+json");
    }

    public static void stubQueryHearingTypeData(final String resourceName, final UUID id) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/hearing-types";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString().replace("HEARINGTYPEID", id.toString()))));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.country-nationality+json");
    }

    public static void stubQueryProsecutorData(final String resourceName, final UUID id) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", id.toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.prosecutor+json");
    }

    public static void stubQueryCourtOURoom(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject courtCentre = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/courtrooms/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(courtCentre.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.ou-courtrooms+json");
    }

    public static void stubEnforcementArea(final String resourceName) {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final JsonObject enforcementArea = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/enforcement-area?.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(enforcementArea.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.enforcement-area+json");
    }
}
