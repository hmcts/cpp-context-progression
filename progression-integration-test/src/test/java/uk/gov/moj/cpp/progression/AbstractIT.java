package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.progression.helper.NotifyStub.stubNotifications;
import static uk.gov.moj.cpp.progression.helper.RestHelper.HOST;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.stubMaterialUploadFile;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataOffenceStub.stubReferenceDataOffencesGetOffenceById;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataOffenceStub.stubReferenceDataOffencesGetOffenceByOffenceCode;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubEnforcementArea;
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
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryReferralReasons;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.mockMaterialUpload;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsSystemUser;

import java.util.UUID;

import com.jayway.restassured.response.Header;
import org.junit.Before;

public class AbstractIT {

    protected static final UUID USER_ID_VALUE = randomUUID();
    public static final Header CPP_UID_HEADER = new Header(USER_ID, USER_ID_VALUE.toString());
    protected static final UUID USER_ID_VALUE_AS_ADMIN = randomUUID();
    protected static final String APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON = "application/vnd.progression.query.courtdocuments+json";


    /**
     * NOTE: this approach is employed to enabled massive savings in test execution test.
     * All tests will need to extend AbstractIT thus ensuring the static initialisation block is fired just once before any test runs
     * Mock reset and stub for all reference data happens once per VM.  If parallel test run is considered, this approach will be tweaked.
     */

    static {
        configureFor(HOST, 8080);
        reset(); // will need to be removed when things are being run in parallel
        defaultStubs();
    }

    @Before
    public void setUp() {
        setupAsAuthorisedUser(USER_ID_VALUE);
        setupAsSystemUser(USER_ID_VALUE_AS_ADMIN);
        mockMaterialUpload();
    }

    private static void defaultStubs() {
        setupUsersGroupQueryStub();
        stubEnableAllCapabilities();
        stubQueryLocalJusticeArea("/restResource/referencedata.query.local-justice-areas.json");
        stubQueryCourtsCodeData("/restResource/referencedata.query.local-justice-area-court-prosecutor-mapping-courts.json");
        stubQueryOrganisationUnitsData("/restResource/referencedata.query.organisationunits.json");
        stubListCourtHearing();
        stubReferenceDataOffencesGetOffenceById("/restResource/referencedataoffences.get-offences-by-id.json");
        stubReferenceDataOffencesGetOffenceByOffenceCode("/restResource/referencedataoffences.get-offences-by-offence-code.json");
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubQueryNationalityData("/restResource/ref-data-nationalities.json", randomUUID());
        stubQueryHearingTypeData("/restResource/ref-data-hearing-types.json", randomUUID());
        stubQueryReferralReasons("/restResource/referencedata.query.referral-reasons.json", randomUUID());
        stubQueryJudiciaries("/restResource/referencedata.query.judiciaries.json", randomUUID());
        stubEnforcementArea("/restResource/referencedata.query.enforcement-area.json");
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
        stubQueryCourtOURoom("/restResource/referencedata.ou-courtroom.json");
        stubQueryOrganisation("/restResource/ref-data-get-organisation.json");
        stubNotifications();
        stubMaterialUploadFile();
        stubQueryEthinicityData("/restResource/ref-data-ethnicities.json", randomUUID());
    }

}
