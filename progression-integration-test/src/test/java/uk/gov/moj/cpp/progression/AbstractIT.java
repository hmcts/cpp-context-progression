package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.progression.helper.NotifyStub.stubLetterNotifications;
import static uk.gov.moj.cpp.progression.helper.NotifyStub.stubNotifications;
import static uk.gov.moj.cpp.progression.helper.RestHelper.HOST;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.stub.CourtOrderStub.setupCourtOrdersStub;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.stubMaterialUploadFile;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataOffenceStub.stubReferenceDataOffencesGetOffenceById;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataOffenceStub.stubReferenceDataOffencesGetOffenceByOffenceCode;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubEnforcementArea;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryAllResultDefinitions;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCourtOURoom;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCourtsCodeData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryEthinicityData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryHearingTypeData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryJudiciaries;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryLocalJusticeArea;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryNationalityData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryOrganisation;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryOrganisationUnitsData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryPrisonSuites;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryReferralReasons;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatch;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.mockMaterialUpload;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsSystemUser;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupHearingQueryStub;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Header;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(JmsResourceManagementExtension.class)
public class AbstractIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIT.class);

    protected static final UUID USER_ID_VALUE = randomUUID();
    public static final Header CPP_UID_HEADER = new Header(USER_ID, USER_ID_VALUE.toString());
    protected static final UUID USER_ID_VALUE_AS_ADMIN = randomUUID();
    protected static final String APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON = "application/vnd.progression.query.courtdocuments+json";
    protected static final String HEARING_ID_TYPE_TRIAL = randomUUID().toString();
    protected static final String HEARING_ID_TYPE_TRIAL_OF_ISSUE = randomUUID().toString();
    protected static final String HEARING_ID_TYPE_NON_TRIAL = randomUUID().toString();
    protected static final String REST_RESOURCE_REF_DATA_GET_ORGANISATION_JSON = "/restResource/ref-data-get-organisation.json";
    protected static final String REST_RESOURCE_REF_DATA_GET_ORGANISATION_WITHOUT_POSTCODE_JSON = "/restResource/ref-data-get-organisation-without-postcode.json";
    protected static ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil = null;
    protected static ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;
    protected static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    protected static final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    protected static final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    protected static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();


    /**
     * NOTE: this approach is employed to enabled massive savings in test execution test.
     * All tests will need to extend AbstractIT thus ensuring the static initialisation block is fired just once before any test runs
     * Mock reset and stub for all reference data happens once per VM.  If parallel test run is considered, this approach will be tweaked.
     */

    static {
        try {
            configureFor(HOST, 8080);
            reset(); // will need to be removed when things are being run in parallel
            defaultStubs();
            setUpElasticSearch();
        } catch (final Throwable e) {
            LOGGER.error("Failure during set up of integration test", e);
            throw e;
        }
    }

    private static void setUpElasticSearch() {
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);
        elasticSearchIndexRemoverUtil = new ElasticSearchIndexRemoverUtil();
        deleteAndCreateIndex();
    }

    protected static void deleteAndCreateIndex() {
        try {
            elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();
        } catch (final IOException e) {
            LOGGER.error("Error while creating index ", e);
        }
    }


    protected static void defaultStubs() {
        setupAsAuthorisedUser(USER_ID_VALUE);
        setupAsSystemUser(USER_ID_VALUE_AS_ADMIN);
        mockMaterialUpload();
        setupUsersGroupQueryStub();
        stubQueryLocalJusticeArea("/restResource/referencedata.query.local-justice-areas.json");
        stubQueryCourtsCodeData("/restResource/referencedata.query.local-justice-area-court-prosecutor-mapping-courts.json");
        stubQueryOrganisationUnitsData("/restResource/referencedata.query.organisationunits.json");
        stubQueryAllResultDefinitions("/restResource/referencedata.get-all-result-definitions.json");
        stubListCourtHearing();
        stubReferenceDataOffencesGetOffenceById("/restResource/referencedataoffences.get-offences-by-id.json");
        stubReferenceDataOffencesGetOffenceByOffenceCode("/restResource/referencedataoffences.get-offences-by-offence-code.json");
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubQueryNationalityData("/restResource/ref-data-nationalities.json", randomUUID());
        stubQueryHearingTypeData("/restResource/ref-data-hearing-types.json", randomUUID());
        stubQueryReferralReasons("/restResource/referencedata.query.referral-reasons.json", randomUUID());
        stubQueryJudiciaries("/restResource/referencedata.query.judiciaries.json", randomUUID());
        stubQueryPrisonSuites("/restResource/ref-data.prisons-custody-suites.json");
        stubEnforcementArea("/restResource/referencedata.query.enforcement-area.json");
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
        stubQueryCourtOURoom();
        stubQueryOrganisation(REST_RESOURCE_REF_DATA_GET_ORGANISATION_JSON);
        stubNotifications();
        stubLetterNotifications();
        stubMaterialUploadFile();
        stubQueryEthinicityData("/restResource/ref-data-ethnicities.json", randomUUID());
        setupHearingQueryStub(fromString(HEARING_ID_TYPE_TRIAL), "stub-data/hearing.get-hearing-of-type-trial.json");
        setupHearingQueryStub(fromString(HEARING_ID_TYPE_TRIAL_OF_ISSUE), "stub-data/hearing.get-hearing-of-type-trial-of-issue.json");
        setupHearingQueryStub(fromString(HEARING_ID_TYPE_NON_TRIAL), "stub-data/hearing.get-hearing-of-type-non-trial.json");
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatch(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), "2099/1234567L", "1234567");
        setupCourtOrdersStub();
        IdMapperStub.setUp();
    }



}
