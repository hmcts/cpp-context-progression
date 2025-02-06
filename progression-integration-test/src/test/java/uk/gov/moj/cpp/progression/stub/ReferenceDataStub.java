package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.helper.FileHelper;
import uk.gov.moj.cpp.progression.util.Pair;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.collect.Lists;


public class ReferenceDataStub {

    public static final String WELSH_COURT_ID = "f8254db1-1683-483e-afb3-b87fde5a0a26";
    public static final String ENGLISH_COURT_ID = "e3114db1-1683-483e-afb3-b87fde5a7777";
    private static final String REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/documents-type-access/" + LocalDate.now();
    private static final String REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_MEDIA_TYPE = "application/vnd.referencedata.get-all-document-type-access+json";
    private static final String COUNTRY_BY_POSTCODE_CONTENT_TYPE = "application/vnd.referencedata.query.country-by-postcode+json";
    private static final String COUNTRY_BY_POSTCODE_ENDPOINT = "/referencedata-service/query/api/rest/referencedata/country-by-postcode";

    private static final List<Pair<String, String>> COURT_ID_LIST = Lists.newArrayList(Pair.p(".*", "/restResource/referencedata.ou-courtroom.json"), Pair.p(ENGLISH_COURT_ID, "/restResource/referencedata.ou-courtroom-english.json"));

    public static void stubPleaTypes() {
        final String payload = getPayload("restResource/referencedata.query.plea-types.json");

        stubFor(get(urlPathMatching("/referencedata-service/query/api/rest/referencedata/plea-types"))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.referencedata.plea-types+json")
                        .withBody(payload)));
    }

    public static void stubQueryLocalJusticeArea(final String resourceName) {
        final JsonObject jsonObject = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/local-justice-areas?.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(jsonObject.toString())));
    }

    public static void stubQueryOrganisation(final String resourceName) {
        final JsonObject judge = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/organisation-units/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(judge.toString())));
    }

    public static void stubQueryProsecutorsByOucode(final String resourceName, final String prosecutionAuthorityId, final String ouCode) {
        final String responsePayload = getPayload(resourceName)
                .replace("[PROSECUTION_AUTHORITY_ID]", prosecutionAuthorityId)
                .replace("[OU_CODE]", ouCode);

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors";
        stubFor(get(urlPathMatching(urlPath))
                .withQueryParam("oucode", matching(ouCode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responsePayload)));

    }

    public static void stubQueryCourtsCodeData(final String resourceName) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/local-justice-area-court-prosecutor-mapping/courts?.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));
    }

    public static void stubQueryOrganisationUnitsData(final String resourceName) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/organisationunits?.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));
    }

    public static void stubQueryAllResultDefinitions(final String resourceName) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/result-definitions";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));
    }

    public static void stubQueryDocumentTypeData(final String resourceName) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/document-type-access/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));
    }

    public static void stubQueryDocumentTypeAccessQueryData(final String resourceName) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/document-type-access/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));
    }

    public static void stubQueryDocumentTypeData(final String resourceName, final String documentTypeId) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = format("/referencedata-service/query/api/rest/referencedata/document-type-access/%s", documentTypeId);
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));
    }


    public static void stubGetDocumentsTypeAccess(final String filePath) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(filePath))
                .readObject();

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_MEDIA_TYPE)
                        .withBody(documentType.toString())));
    }

    public static void stubCourtApplicationTypes(final String resourceName) {
        final JsonObject applicationTypesResponse = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/application-types";

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(applicationTypesResponse.toString())));

    }

    public static void stubQueryReferralReasons(final String resourceName, final UUID referralReasonId) {
        final JsonObject referralReasonsJson = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String responseBody = referralReasonsJson.toString()
                .replace("RANDOM_REFERRAL_ID", referralReasonId.toString());

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/referral-reasons/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responseBody)));
    }

    public static void stubQueryJudiciaries(final String resourceName) {
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
    }

    public static void stubQueryPrisonSuites(final String resourceName) {
        final JsonObject referralReasonsJson = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String responseBody = referralReasonsJson.toString();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prisons-custody-suites";
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responseBody)));
    }

    public static void stubQueryEthinicityData(final String resourceName, final UUID id) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/ethnicities";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString().replace("ETHNICITYID", id.toString()))));
    }

    public static void stubQueryNationalityData(final String resourceName, final UUID id) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/country-nationality";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString().replace("NATIONALITYID", id.toString()))));
    }

    public static void stubQueryHearingTypeData(final String resourceName, final UUID id) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/hearing-types";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString().replace("HEARINGTYPEID", id.toString()))));
    }

    public static void stubQueryProsecutorDataForGivenProsecutionAuthorityId(final String resourceName, final String prosecutionAuthorityId, final String ouCode) {
        final String responsePayload = getPayload(resourceName)
                .replace("[PROSECUTION_AUTHORITY_ID]", prosecutionAuthorityId)
                .replace("[OU_CODE]", ouCode);

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors/" + prosecutionAuthorityId;
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responsePayload)));
    }

    public static void stubQueryProsecutorData(final String resourceName, final UUID id) {
        final JsonObject responsePayload = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", id.toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responsePayload.toString())));
    }

    public static void stubQueryProsecutorData(final JsonObject payload, final UUID prosecutorId, final UUID id) {
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors/" + prosecutorId.toString();
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", id.toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(payload.toString())));
    }

    public static void stubQueryCpsProsecutorData(final String resourceName, final UUID id, int returnStatus) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors.*oucode.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(returnStatus)
                        .withHeader("CPPID", id.toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));
    }


    public static void stubQueryPetFormData(final String resourceName, final UUID id, int returnStatus) {
        final JsonObject documentType = Json.createReader(ReferenceDataStub.class
                .getResourceAsStream(resourceName)).readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/latest-pet-form";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(returnStatus)
                        .withHeader("CPPID", id.toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(documentType.toString())));
    }


    public static void stubQueryCourtOURoom() {
        COURT_ID_LIST.forEach(cid -> {
            final JsonObject courtCentre = Json.createReader(ReferenceDataStub.class
                            .getResourceAsStream(cid.getV()))
                    .readObject();

            final String urlPath = "/referencedata-service/query/api/rest/referencedata/courtrooms/" + cid.getK();
            stubFor(get(urlMatching(urlPath))
                    .willReturn(aResponse().withStatus(SC_OK)
                            .withHeader("CPPID", randomUUID().toString())
                            .withHeader("Content-Type", APPLICATION_JSON)
                            .withBody(courtCentre.toString())));
        });

    }

    public static void stubEnforcementArea(final String resourceName) {
        final JsonObject enforcementArea = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/enforcement-area?.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(enforcementArea.toString())));
    }

    public static void stubLegalStatus(final String resourceName, final String statusCode) {
        final JsonObject legalStatuses = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/legal-statuses";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(legalStatuses.toString().replace("STATUS_CODE", statusCode))));
    }

    public static void stubLegalStatusWithStatusDescription(final String resourceName, final String statusCode, final String statusDescription) {
        final JsonObject legalStatuses = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/legal-statuses";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(legalStatuses.toString().replace("LAA_STATUS_CODE", statusCode).replace("LAA_STATUS_DESC", statusDescription))));
    }


    public static void stubGetOrganisationById(final String resourceName) {
        final JsonObject judge = Json.createReader(ReferenceDataStub.class
                        .getResourceAsStream(resourceName))
                .readObject();

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/organisation-units/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(judge.toString())));
    }

    public static void stubCotrReviewNotes() {
        final String payload = getPayload("restResource/referencedata.query.cotr-review-notes.json");

        stubFor(get(urlPathMatching("/referencedata-service/query/api/rest/referencedata/cotr-review-notes"))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.referencedata.query.cotr-review-notes+json")
                        .withBody(payload)));
    }

    public static void stubGetCountryByPostCode(final String... postCode) {
        Stream.of(postCode)
                .forEach(
                         postcode -> stubFor(get(urlMatching(COUNTRY_BY_POSTCODE_ENDPOINT + ".*")) // we'd expect it to work without the ".*" but it doesn't
                        .willReturn(aResponse()
                                .withStatus(OK.getStatusCode())
                                .withHeader(ID, UUID.randomUUID().toString())
                                .withHeader(CONTENT_TYPE, COUNTRY_BY_POSTCODE_CONTENT_TYPE)
                                .withBody(FileHelper.read(format("stub-data/referencedata-country-post-code-%s.json", postcode))))));
    }

    public static void stubQueryCourtRoomById(final String courtCentreId) {
            final String payload = getPayload("restResource/referencedata-query-court-room-by-court-centre-id.json");

            final String urlPath = "/referencedata-service/query/api/rest/referencedata/courtrooms/" + courtCentreId;
            stubFor(get(urlMatching(urlPath))
                    .willReturn(aResponse().withStatus(SC_OK)
                            .withHeader("CPPID", randomUUID().toString())
                            .withHeader("Content-Type", APPLICATION_JSON)
                            .withBody(payload)));
    }
}
