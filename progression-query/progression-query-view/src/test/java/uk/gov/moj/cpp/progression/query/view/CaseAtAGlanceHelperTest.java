package uk.gov.moj.cpp.progression.query.view;

import static org.mockito.ArgumentMatchers.any;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.CustodyTimeLimit.custodyTimeLimit;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.LegalEntityDefendant.legalEntityDefendant;
import static uk.gov.justice.core.courts.Marker.marker;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.Plea.plea;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.Verdict.verdict;
import static uk.gov.justice.progression.courts.Defendants.defendants;
import static uk.gov.justice.progression.courts.Hearings.hearings;
import static uk.gov.justice.progression.courts.Offences.offences;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_1;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_2;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_3;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_4;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.ADDRESS_5;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.POSTCODE;
import static uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper.YOUTH_MARKER_TYPE;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.MigrationSourceSystem;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.progression.courts.CaagDefendantOffences;
import uk.gov.justice.progression.courts.CaagDefendants;
import uk.gov.justice.progression.courts.CaagResults;
import uk.gov.justice.progression.courts.CaseDetails;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.ProsecutorDetails;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAtAGlanceHelperTest {
    private static final String CASE_URN = "CASEURN";
    private static final String PROSECUTION_AUTHORITY_CODE = "CPS";
    private static final String PROSECUTION_AUTHORITY_REFERENCE = "3603e667";
    private static final String PROSECUTOR_CODE = "CPS_CODE";
    private static final String PROSECUTOR_NAME = "CPS_NAME";
    private static final UUID PROSECUTOR_ID = randomUUID();
    private static final String INTERPRETER_LANGUAGE_NEEDS = "Spanish";
    private static final String NATIONALITY_DESCRIPTION = "British";
    private static final String ADD_NATIONALITY_DESCRIPTION = "Australian";
    private static final Address ADDRESS = Address.address().withAddress1("line1").withAddress2("line2").build();
    private static final String REMAND_STATUS = "Conditional bail";
    private static final String OFFENCE_CODE = "CJSCODEGAPS1";
    private static final String OTHER_OFFENCE_CODE = "CJSCODEGAPS2";
    private static final String OFFENCE_TITLE = "Offence Title";
    private static final String OFFENCE_TITLE_WELSH = "Offence Title Welsh";
    private static final String PLEA_GUILTY = "GUILTY";
    private static final String PLEA_NOT_GUILTY = "NOT_GUILTY";
    private static final String GUILTY = "Guilty";
    private static final String NOT_GUILTY = "NotGuilty";
    private static final String OFFENCE_LEGISLATION = "OffenceLegislation";
    private static final String LEGAL_REP_NAME = "Legal Rep name";
    private static final String OFFENCE_RESULT_LABEL_1 = "offence result label 1";
    private static final String OFFENCE_RESULT_LABEL_2 = "offence result label 2";
    private static final String CASE_RESULT_LABEL = "case result label";
    private static final String DEFENDANT_RESULT_LABEL = "defendant result label";
    private static final String ANOTHER_OFFENCE_RESULT_LABEL = "another offence result label";
    private static final String ANOTHER_CASE_RESULT_LABEL = "another case result label";
    private static final String ANOTHER_DEFENDANT_RESULT_LABEL = "another defendant result label";
    private static final String AMEND_REASON = "Amend reason";
    private static final String OTHER_AMEND_REASON = "Other amend reason";
    private static final String VALUE = "value";
    private static final String CASE_STATUS = "EJECTED";
    private static final String REMOVAL_REASON = "CPP cannot handle this case";
    private static final UUID RECENT_JUDICIAL_RESULT_ID = randomUUID();
    private static final String LEGAL_AID_STATUS = "Granted";
    private static final UUID JOHN_SMITH_ID = randomUUID();
    private static final UUID JOHN_SMITH_MASTER_ID = randomUUID();
    private static final UUID JOHN_RAMBO_ID = randomUUID();
    private static final UUID ALAN_SMITH_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID JUDICIAL_RESULT_ID = randomUUID();
    private static final UUID CASE_JUDICIAL_RESULT_ID = randomUUID();
    private static final String RESULT_WORDING = "Some result wording";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1990, 9, 29);
    private static final LocalDate PLEA_DATE = LocalDate.of(2020, 9, 29);
    private static final LocalDate INDICATED_PLEA_DATE = LocalDate.of(2020, 9, 30);
    private static final LocalDate VERDICT_DATE = LocalDate.of(2020, 9, 29);
    private static final String ORG_NAME = "Org name";

    private static final String CAAG_RESULT_TEXT = "code - Result Text For Caag";
    private static final String JUDICIAL_RESULT_TEXT = "code - Result Text For Judicial";
    private static final String DRIVER_NUMBER = "JUDDE101099NP9LN";
    private static final String PNC_ID = "pncId";
    private static final String ASN = "asn1234";
    private static final Gender DRIVER_GENDER = Gender.MALE;
    private static final String MIGRATION_SOURCE_SYSTEM_NAME = "LIBRA";
    private static final String MIGRATION_SOURCE_SYSTEM_CASE_IDENTIFIER = "LIBRA-359";

    private CaseAtAGlanceHelper caseAtAGlanceHelper;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private CivilFeeRepository civilFeeRepository;

    @Mock
    private RelatedReferenceRepository relatedReferenceRepository;

    @Test
    public void shouldReturnEmptyCaseDetailsWhenNoDataFound() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase().build(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final CaseDetails caseDetails = caseAtAGlanceHelper.getCaseDetails();

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getCaseMarkers(), nullValue());
        assertThat(caseDetails.getCaseURN(), nullValue());
        assertThat(caseDetails.getCaseStatus(), nullValue());
        assertThat(caseDetails.getInitiationCode(), nullValue());
        assertThat(caseDetails.getRemovalReason(), nullValue());
    }

    @Test
    public void shouldGetCaseDetailsFromProsecutionCase() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final CaseDetails caseDetails = caseAtAGlanceHelper.getCaseDetails();

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getCaseMarkers().size(), is(2));
        assertThat(caseDetails.getCaseURN(), is(CASE_URN));
        assertThat(caseDetails.getInitiationCode(), is(InitiationCode.J.toString()));
    }

    @Test
    public void shouldGetCaseDetailsWithCaseStatusAndReason() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final CaseDetails caseDetails = caseAtAGlanceHelper.getCaseDetails();

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getCaseStatus(), is(CASE_STATUS));
        assertThat(caseDetails.getRemovalReason(), is(REMOVAL_REASON));
    }

    @Test
    public void shouldGetEmptyProsecutorDetailsFromProsecutionCase() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase().build(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final ProsecutorDetails prosecutorDetails = caseAtAGlanceHelper.getProsecutorDetails();

        assertThat(prosecutorDetails, notNullValue());
        assertThat(prosecutorDetails.getProsecutionAuthorityCode(), nullValue());
        assertThat(prosecutorDetails.getProsecutionAuthorityReference(), nullValue());
    }

    @Test
    public void shouldGetProsecutorDetailsFromProsecutionCase() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        when(referenceDataService.getProsecutor(anyString())).thenReturn(getProsecutorFromReferenceData(randomUUID().toString()));

        final ProsecutorDetails prosecutorDetails = caseAtAGlanceHelper.getProsecutorDetails();

        assertThat(prosecutorDetails, notNullValue());
        assertThat(prosecutorDetails.getProsecutionAuthorityCode(), is(PROSECUTION_AUTHORITY_CODE));
        assertThat(prosecutorDetails.getProsecutionAuthorityReference(), is(PROSECUTION_AUTHORITY_REFERENCE));
        assertThat(prosecutorDetails.getProsecutionAuthorityId(), notNullValue());
        assertThat(prosecutorDetails.getAddress().getAddress1(), is("address1"));
        assertThat(prosecutorDetails.getAddress().getAddress2(), is("address2"));
        assertThat(prosecutorDetails.getAddress().getPostcode(), is("postcode"));
        assertThat(prosecutorDetails.getIsCpsOrgVerifyError(), is(true));
    }

    @Test
    public void shouldGetProsecutorDetailsFromProsecutionCaseWhenProsecutorIsNotNull() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithProsecutor(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        when(referenceDataService.getProsecutor(anyString())).thenReturn(getProsecutorFromReferenceData(randomUUID().toString()));

        final ProsecutorDetails prosecutorDetails = caseAtAGlanceHelper.getProsecutorDetails();

        assertThat(prosecutorDetails, notNullValue());
        assertThat(prosecutorDetails.getProsecutionAuthorityCode(), is(PROSECUTOR_CODE));
        assertThat(prosecutorDetails.getProsecutionAuthorityReference(), is(PROSECUTION_AUTHORITY_REFERENCE));
        assertThat(prosecutorDetails.getProsecutionAuthorityId(), is(PROSECUTOR_ID));
        assertThat(prosecutorDetails.getAddress().getAddress1(), is("address1"));
        assertThat(prosecutorDetails.getAddress().getAddress2(), is("address2"));
        assertThat(prosecutorDetails.getAddress().getPostcode(), is("postcode"));
        assertThat(prosecutorDetails.getIsCpsOrgVerifyError(), is(true));
    }

    @Test
    public void shouldGetDefendantPersonalDetails() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final Map<UUID, LocalDate> defendantUpdatedOn = new HashMap<>();
        defendantUpdatedOn.put(getProsecutionCaseWithCaseDetails().getDefendants().get(0).getMasterDefendantId(), LocalDate.now());
        defendantUpdatedOn.put(getProsecutionCaseWithCaseDetails().getDefendants().get(1).getMasterDefendantId(), LocalDate.now().minusDays(10));
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(defendantUpdatedOn);
        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(3));

        final CaagDefendants defendantSmith = defendants.get(0);
        assertThat(defendantSmith.getMasterDefendantId(), is(JOHN_SMITH_MASTER_ID));
        assertThat(defendantSmith.getFirstName(), is("John"));
        assertThat(defendantSmith.getLastName(), is("Smith"));
        assertThat(defendantSmith.getDateOfBirth(), is(DATE_OF_BIRTH));
        assertThat(defendantSmith.getAge(), notNullValue());
        assertThat(defendantSmith.getInterpreterLanguageNeeds(), is(INTERPRETER_LANGUAGE_NEEDS));
        assertThat(defendantSmith.getNationality(), is(NATIONALITY_DESCRIPTION));
        assertThat(defendantSmith.getAddress(), is(ADDRESS));
        assertThat(defendantSmith.getRemandStatus(), is(REMAND_STATUS));
        assertThat(defendantSmith.getDriverNumber(), is(DRIVER_NUMBER));
        assertThat(defendantSmith.getGender(), is(DRIVER_GENDER));
        assertThat(defendantSmith.getUpdatedOn(), is(LocalDate.now()));

        final CaagDefendants defendantRambo = defendants.get(1);
        assertThat(defendantRambo.getMasterDefendantId(), is(JOHN_RAMBO_ID));
        assertThat(defendantRambo.getFirstName(), is("John"));
        assertThat(defendantRambo.getLastName(), is("Rambo"));
        assertThat(defendantRambo.getDateOfBirth(), nullValue());
        assertThat(defendantRambo.getAge(), nullValue());
        assertThat(defendantRambo.getInterpreterLanguageNeeds(), nullValue());
        assertThat(defendantRambo.getNationality(), nullValue());
        assertThat(defendantRambo.getAddress(), nullValue());
        assertThat(defendantRambo.getRemandStatus(), nullValue());
        assertThat(defendantRambo.getDriverNumber(), nullValue());
        assertThat(defendantRambo.getGender(), nullValue());
        assertThat(defendantRambo.getUpdatedOn(), is(LocalDate.now().minusDays(10)));
    }

    @Test
    public void shouldGetDefendantPersonalDetailsWithMultipleNationalities() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final Map<UUID, LocalDate> defendantUpdatedOn = new HashMap<>();
        defendantUpdatedOn.put(getProsecutionCaseWithCaseDetails().getDefendants().get(2).getId(), LocalDate.now().minusDays(10));
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(defendantUpdatedOn);

        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(3));

        final CaagDefendants defendantSmith = defendants.get(2);
        assertThat(defendantSmith.getFirstName(), is("Alan"));
        assertThat(defendantSmith.getLastName(), is("Smith"));
        assertThat(defendantSmith.getNationality(), is(String.format("%s, %s", NATIONALITY_DESCRIPTION, ADD_NATIONALITY_DESCRIPTION)));
        assertThat(defendantSmith.getUpdatedOn(), is(LocalDate.now().minusDays(10)));
    }


    @Test
    public void shouldGetDefendantMarkerWhenDefendantYouth() {

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());

        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(3));

        final CaagDefendants defendantSmith = defendants.get(2);
        assertThat(defendantSmith.getFirstName(), is("Alan"));
        assertThat(defendantSmith.getLastName(), is("Smith"));
        assertTrue(defendantSmith.getDefendantMarkers().stream().anyMatch(m -> m.equals(YOUTH_MARKER_TYPE)));
    }

    @Test
    public void shouldGetDefendantOffenceDetails() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), getCaseHearings(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());

        final CaagDefendants defendantSmith = defendants.get(0);
        assertThat(defendantSmith.getCaagDefendantOffences().isEmpty(), is(false));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getOffenceCode(), is(OFFENCE_CODE));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getOffenceTitle(), is(OFFENCE_TITLE));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getOffenceTitleWelsh(), is(OFFENCE_TITLE_WELSH));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCount(), is(2));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getOrderIndex(), is(2));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getPlea().getPleaValue(), is(PLEA_GUILTY));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getPlea().getPleaDate(), is(PLEA_DATE));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getIndicatedPlea().getIndicatedPleaValue(), is(IndicatedPleaValue.INDICATED_GUILTY));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getIndicatedPlea().getIndicatedPleaDate(), is(INDICATED_PLEA_DATE));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getVerdict().getVerdictType().getCategory(), is(GUILTY));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getVerdict().getVerdictDate(), is(VERDICT_DATE));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getOffenceLegislation(), is(OFFENCE_LEGISLATION));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getStartDate(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getEndDate(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().isEmpty(), is(false));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(0).getLabel(), is(OFFENCE_RESULT_LABEL_1));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getLabel(), is(OFFENCE_RESULT_LABEL_2));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(0).getResultText(), is(CAAG_RESULT_TEXT));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(0).getUseResultText(), is(true));
        assertThat(defendantSmith.getDefendantCaseJudicialResults().get(0).getLabel(), is(CASE_RESULT_LABEL));
        assertThat(defendantSmith.getDefendantCaseJudicialResults().get(0).getJudicialResultId(), is(CASE_JUDICIAL_RESULT_ID));
        assertThat(defendantSmith.getDefendantCaseJudicialResults().get(0).getResultWording(), is(RESULT_WORDING));

        final CaagDefendants defendantRambo = defendants.get(1);
        assertThat(defendantRambo.getCaagDefendantOffences().isEmpty(), is(false));
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getId(), notNullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getOffenceCode(), is(OFFENCE_CODE));
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getOffenceTitleWelsh(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getCount(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getOrderIndex(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getPlea(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getIndicatedPlea(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getVerdict(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getOffenceLegislation(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getStartDate(), nullValue());
        assertThat(defendantRambo.getCaagDefendantOffences().get(0).getEndDate(), nullValue());
    }

    @Test
    public void shouldGetDefendantOffenceResultDetails() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), getCaseHearings(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());
        final CaagDefendants defendantSmith = defendants.get(0);

        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().isEmpty(), is(false));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getId(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getLabel(), is(OFFENCE_RESULT_LABEL_2));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getAmendmentReason(), is(AMEND_REASON));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getAmendedBy(), is("first last"));
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getAmendmentDate(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getLastSharedDateTime(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getOrderedDate(), notNullValue());
        assertThat(defendantSmith.getCaagDefendantOffences().get(0).getCaagResults().get(1).getCaagResultPrompts().isEmpty(), is(false));
    }

    @Test
    public void shouldGetDefendantOffenceResultDetailsInDescendingOrder() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), getCaseHearings(), referenceDataService, civilFeeRepository, relatedReferenceRepository);

        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());

        final CaagDefendants defendantSmith = defendants.get(0);
        final List<CaagResults> caagResults = defendantSmith.getCaagDefendantOffences().get(0).getCaagResults();
        assertThat(caagResults.isEmpty(), is(false));
        assertThat(caagResults.get(0).getAmendmentReason(), is(OTHER_AMEND_REASON));
        assertThat(caagResults.get(1).getAmendmentReason(), is(AMEND_REASON));
        assertThat(caagResults.get(1).getResultText(), is(CAAG_RESULT_TEXT));
        assertThat(caagResults.get(1).getUseResultText(), is(true));
        assertThat(caagResults.get(0).getId(), is(RECENT_JUDICIAL_RESULT_ID));
    }

    @Test
    public void shouldGetDefendantPncAndAsn() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), getCaseHearings(), referenceDataService, civilFeeRepository, relatedReferenceRepository);

        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());

        final CaagDefendants defendant1 = defendants.get(1);
        assertThat(defendant1.getPncId(), is(PNC_ID));
        assertThat(defendant1.getArrestSummonsNumber(), is(ASN));
    }

    @Test
    public void shouldGetDefendantWithLegalAidStatus() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());
        final CaagDefendants defendantSmith = defendants.get(0);
        assertThat(defendantSmith.getLegalAidStatus(), is(LEGAL_AID_STATUS));
    }

    @Test
    public void shouldGetDefendantLegalEntityOrganisationDetails() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithDefendantLegalEntity(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());

        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(1));
        final CaagDefendants legalEntityDefendant = defendants.get(0);
        assertThat(legalEntityDefendant.getLegalEntityDefendant().getName(), is(ORG_NAME));
        assertThat(legalEntityDefendant.getLegalEntityDefendant().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(legalEntityDefendant.getLegalEntityDefendant().getAddress().getAddress2(), is(ADDRESS_2));
        assertThat(legalEntityDefendant.getLegalEntityDefendant().getAddress().getPostcode(), is(POSTCODE));
    }

    @Test
    public void shouldGetCaagDefendantsListReturnThreeSetOfResults() {
        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCaseDetails(), getCaseHearings(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());

        final CaagDefendants firstDefendant = defendants.get(0);
        final CaagDefendants secondDefendant = defendants.get(1);
        final CaagDefendants thirdDefendant = defendants.get(2);

        assertThat(firstDefendant.getDefendantJudicialResults(), hasSize(1));
        assertThat(firstDefendant.getDefendantJudicialResults().get(0).getLabel(), is(DEFENDANT_RESULT_LABEL));
        assertThat(firstDefendant.getDefendantJudicialResults().get(0).getJudicialResultId(), is(JUDICIAL_RESULT_ID));
        assertThat(firstDefendant.getDefendantCaseJudicialResults(), hasSize(1));
        assertThat(firstDefendant.getDefendantCaseJudicialResults().get(0).getLabel(), is(CASE_RESULT_LABEL));

        final List<CaagDefendantOffences> firstDefendantOffences = firstDefendant.getCaagDefendantOffences();
        assertThat(firstDefendantOffences, hasSize(2));
        assertThat(firstDefendantOffences.get(0).getOffenceCode(), is(OFFENCE_CODE));
        assertThat(firstDefendantOffences.get(1).getOffenceCode(), is(OTHER_OFFENCE_CODE));

        final List<CaagResults> firstDefendantFirstOffenceJudicialResults = firstDefendantOffences.get(0).getCaagResults();
        assertThat(firstDefendantFirstOffenceJudicialResults, hasSize(2));
        assertThat(firstDefendantFirstOffenceJudicialResults.get(0).getLabel(), is(OFFENCE_RESULT_LABEL_1));
        assertThat(firstDefendantFirstOffenceJudicialResults.get(0).getAmendmentReason(), is(OTHER_AMEND_REASON));
        assertThat(firstDefendantFirstOffenceJudicialResults.get(1).getLabel(), is(OFFENCE_RESULT_LABEL_2));
        assertThat(firstDefendantFirstOffenceJudicialResults.get(1).getAmendmentReason(), is(AMEND_REASON));

        final List<CaagResults> firstDefendantSecondOffenceJudicialResults = firstDefendantOffences.get(1).getCaagResults();
        assertThat(firstDefendantSecondOffenceJudicialResults, is(empty()));

        final List<CaagDefendantOffences> secondDefendantOffences = secondDefendant.getCaagDefendantOffences();
        assertThat(secondDefendantOffences, hasSize(1));
        assertThat(secondDefendantOffences.get(0).getOffenceCode(), is(OFFENCE_CODE));
        final List<CaagResults> secondDefendantFirstOffenceJudicialResults = secondDefendantOffences.get(0).getCaagResults();
        assertThat(secondDefendantFirstOffenceJudicialResults, is(empty()));
        assertThat(secondDefendant.getDefendantJudicialResults(), nullValue());
        assertThat(secondDefendant.getDefendantCaseJudicialResults(), nullValue());

        final List<CaagDefendantOffences> thirdDefendantOffences = thirdDefendant.getCaagDefendantOffences();
        assertThat(thirdDefendantOffences, hasSize(1));
        assertThat(thirdDefendantOffences.get(0).getOffenceCode(), is(OFFENCE_CODE));
        final List<CaagResults> thirdCaagResults = thirdDefendantOffences.get(0).getCaagResults();
        assertThat(thirdCaagResults, is(empty()));
        assertThat(thirdDefendant.getDefendantJudicialResults(), nullValue());
        assertThat(thirdDefendant.getDefendantCaseJudicialResults(), nullValue());
    }

    @Test
    public void shouldNotReturnCtlExpiryDateWhenCustodyTimeLimitNotExistsInAllOffences() {
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withDefendants(asList(defendant()
                        .withId(randomUUID())
                        .withMasterDefendantId(randomUUID())
                        .withOffences(asList(offence()
                                .withId(randomUUID())
                                .build()))
                        .build()))
                .build();

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase, getCaseHearings(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());

        assertThat(defendants.get(0).getCtlExpiryDate(), nullValue());
        assertThat(defendants.get(0).getCtlExpiryCountDown(), nullValue());
    }

    @Test
    public void shouldReturnEarliestCtlExpiryDateWhenCustodyTimeLimitExistsInOffences() {
        final LocalDate timeLimit1 = LocalDate.now().plusDays(10);
        final LocalDate timeLimit2 = LocalDate.now().plusDays(11);
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withDefendants(asList(defendant()
                        .withId(randomUUID())
                        .withMasterDefendantId(randomUUID())
                        .withOffences(asList(
                                offence()
                                        .withId(randomUUID())
                                        .build(),
                                offence()
                                        .withId(randomUUID())
                                        .withCustodyTimeLimit(custodyTimeLimit()
                                                .withTimeLimit(timeLimit1)
                                                .build())
                                        .build(),
                                offence()
                                        .withId(randomUUID())
                                        .withCustodyTimeLimit(custodyTimeLimit()
                                                .withTimeLimit(timeLimit2)
                                                .build())
                                        .build()))
                        .build()))
                .build();

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase, getCaseHearings(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());

        assertThat(defendants.get(0).getCtlExpiryDate(), is(timeLimit1));
        assertThat(defendants.get(0).getCtlExpiryCountDown(), is((10)));
    }

    @Test
    public void shouldReturnEarliestCtlExpiryDateWhenCustodyTimeLimitExistsInOffencesAndBeforeTheCurrentDate() {
        final LocalDate timeLimit1 = LocalDate.now().minusDays(10);
        final LocalDate timeLimit2 = LocalDate.now().minusDays(9);
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withDefendants(asList(defendant()
                        .withId(randomUUID())
                        .withMasterDefendantId(randomUUID())
                        .withOffences(asList(
                                offence()
                                        .withId(randomUUID())
                                        .build(),
                                offence()
                                        .withId(randomUUID())
                                        .withCustodyTimeLimit(custodyTimeLimit()
                                                .withTimeLimit(timeLimit1)
                                                .build())
                                        .build(),
                                offence()
                                        .withId(randomUUID())
                                        .withCustodyTimeLimit(custodyTimeLimit()
                                                .withTimeLimit(timeLimit2)
                                                .build())
                                        .build()))
                        .build()))
                .build();

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase, getCaseHearings(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final List<CaagDefendants> defendants = caseAtAGlanceHelper.getCaagDefendantsList(new HashMap<>());

        assertThat(defendants.get(0).getCtlExpiryDate(), is(timeLimit1));
        assertThat(defendants.get(0).getCtlExpiryCountDown(), is((-10)));
    }

    @Test
    public void shouldGetCaseDetailsFromProsecutionCaseWithMigrationCaseDetails() {
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withValuesFrom(getProsecutionCaseWithCaseDetails())
                .withMigrationSourceSystem(MigrationSourceSystem.migrationSourceSystem()
                        .withMigrationSourceSystemName(MIGRATION_SOURCE_SYSTEM_NAME)
                        .withMigrationSourceSystemCaseIdentifier(MIGRATION_SOURCE_SYSTEM_CASE_IDENTIFIER)
                        .build())
                .build();

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase, new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);


        final CaseDetails caseDetails = caseAtAGlanceHelper.getCaseDetails();

        assertThat(caseDetails.getCaseMarkers().size(), is(2));
        assertThat(caseDetails.getCaseURN(), is(CASE_URN));
        assertThat(caseDetails.getInitiationCode(), is(InitiationCode.J.toString()));
        assertThat(caseDetails.getMigrationSourceSystem().getMigrationSourceSystemName(), is(MIGRATION_SOURCE_SYSTEM_NAME));
        assertThat(caseDetails.getMigrationSourceSystem().getMigrationSourceSystemCaseIdentifier(), is(MIGRATION_SOURCE_SYSTEM_CASE_IDENTIFIER));
    }

    private ProsecutionCase getProsecutionCaseWithProsecutor(){
        return prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(CASE_URN)
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                        .withProsecutionAuthorityReference(PROSECUTION_AUTHORITY_REFERENCE)
                        .build())
                .withProsecutor(Prosecutor.prosecutor()
                        .withProsecutorId(PROSECUTOR_ID)
                        .withProsecutorCode(PROSECUTOR_CODE)
                        .withProsecutorName(PROSECUTOR_NAME)
                        .build())
                .withIsCpsOrgVerifyError(true)
                .withCaseStatus(CASE_STATUS)
                .withRemovalReason(REMOVAL_REASON)
                .withInitiationCode(InitiationCode.J)
                .withCaseMarkers(asList(marker().withId(randomUUID()).withMarkerTypeDescription("Vulnerable or intimidated victim").build(),
                        marker().withId(randomUUID()).withMarkerTypeDescription("Prohibited Weapons").build()))
                .withDefendants(asList(defendant().withId(JOHN_SMITH_ID)
                                .withMasterDefendantId(JOHN_SMITH_MASTER_ID)
                                .withPersonDefendant(personDefendant()
                                        .withBailStatus(BailStatus.bailStatus().withDescription(REMAND_STATUS).build())
                                        .withPersonDetails(Person.person()
                                                .withFirstName("John").withLastName("Smith")
                                                .withNationalityDescription(NATIONALITY_DESCRIPTION)
                                                .withDateOfBirth(DATE_OF_BIRTH)
                                                .withAddress(ADDRESS)
                                                .withInterpreterLanguageNeeds(INTERPRETER_LANGUAGE_NEEDS).build())
                                        .build())
                                .withDefenceOrganisation(organisation().withName(LEGAL_REP_NAME).build())
                                .withOffences(asList(offence().withId(OFFENCE_ID)
                                                .withOffenceCode(OFFENCE_CODE)
                                                .withOffenceTitle(OFFENCE_TITLE)
                                                .withOffenceTitleWelsh(OFFENCE_TITLE_WELSH)
                                                .withCount(2)
                                                .withOrderIndex(2)
                                                .withPlea(plea().withPleaDate(PLEA_DATE).withPleaValue(PLEA_GUILTY).build())
                                                .withVerdict(verdict().withVerdictDate(VERDICT_DATE).withVerdictType(VerdictType.verdictType().withCategory(GUILTY).build()).build())
                                                .withOffenceLegislation(OFFENCE_LEGISLATION)
                                                .withStartDate(LocalDate.now())
                                                .withEndDate(LocalDate.now())
                                                .withJudicialResults(emptyList())
                                                .build(),
                                        offence().withId(randomUUID()).withOffenceCode(OTHER_OFFENCE_CODE).build()))
                                .withLegalAidStatus(LEGAL_AID_STATUS)
                                .build(),
                        defendant().withId(JOHN_RAMBO_ID)
                                .withMasterDefendantId(JOHN_RAMBO_ID)
                                .withPersonDefendant(personDefendant().withPersonDetails(Person.person().withFirstName("John").withLastName("Rambo").build()).build())
                                .withOffences(singletonList(offence().withId(randomUUID()).withOffenceCode(OFFENCE_CODE).build()))
                                .build(),
                        defendant().withId(ALAN_SMITH_ID)
                                .withMasterDefendantId(ALAN_SMITH_ID)
                                .withIsYouth(Boolean.TRUE)
                                .withPersonDefendant(personDefendant()
                                        .withPersonDetails(Person.person().withFirstName("Alan").withLastName("Smith")
                                                .withNationalityDescription(NATIONALITY_DESCRIPTION)
                                                .withAdditionalNationalityDescription(ADD_NATIONALITY_DESCRIPTION)
                                                .build()).build())
                                .withOffences(singletonList(offence().withId(randomUUID()).withOffenceCode(OFFENCE_CODE).build()))
                                .build()))
                .build();
    }

    private ProsecutionCase getProsecutionCaseWithCaseDetails() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(CASE_URN)
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                        .withProsecutionAuthorityReference(PROSECUTION_AUTHORITY_REFERENCE)
                        .build())
                .withIsCpsOrgVerifyError(true)
                .withCaseStatus(CASE_STATUS)
                .withRemovalReason(REMOVAL_REASON)
                .withInitiationCode(InitiationCode.J)
                .withCaseMarkers(asList(marker().withId(randomUUID()).withMarkerTypeDescription("Vulnerable or intimidated victim").build(),
                        marker().withId(randomUUID()).withMarkerTypeDescription("Prohibited Weapons").build()))
                .withDefendants(asList(defendant().withId(JOHN_SMITH_ID)
                                .withMasterDefendantId(JOHN_SMITH_MASTER_ID)
                                .withPersonDefendant(personDefendant()
                                        .withBailStatus(BailStatus.bailStatus().withDescription(REMAND_STATUS).build())
                                        .withPersonDetails(Person.person()
                                                .withFirstName("John").withLastName("Smith")
                                                .withGender(DRIVER_GENDER)
                                                .withNationalityDescription(NATIONALITY_DESCRIPTION)
                                                .withDateOfBirth(DATE_OF_BIRTH)
                                                .withAddress(ADDRESS)
                                                .withInterpreterLanguageNeeds(INTERPRETER_LANGUAGE_NEEDS).build())
                                        .withDriverNumber(DRIVER_NUMBER)
                                        .build())
                                .withDefenceOrganisation(organisation().withName(LEGAL_REP_NAME).build())
                                .withOffences(asList(offence().withId(OFFENCE_ID)
                                                .withOffenceCode(OFFENCE_CODE)
                                                .withOffenceTitle(OFFENCE_TITLE)
                                                .withOffenceTitleWelsh(OFFENCE_TITLE_WELSH)
                                                .withCount(2)
                                                .withOrderIndex(2)
                                                .withPlea(plea().withPleaDate(PLEA_DATE).withPleaValue(PLEA_GUILTY).build())
                                                .withVerdict(verdict().withVerdictDate(VERDICT_DATE).withVerdictType(VerdictType.verdictType().withCategory(GUILTY).build()).build())
                                                .withOffenceLegislation(OFFENCE_LEGISLATION)
                                                .withStartDate(LocalDate.now())
                                                .withEndDate(LocalDate.now())
                                                .withJudicialResults(emptyList())
                                                .build(),
                                        offence().withId(randomUUID()).withOffenceCode(OTHER_OFFENCE_CODE).build()))
                                .withLegalAidStatus(LEGAL_AID_STATUS)
                                .build(),
                        defendant().withId(JOHN_RAMBO_ID)
                                .withMasterDefendantId(JOHN_RAMBO_ID)
                                .withPncId(PNC_ID)
                                .withPersonDefendant(personDefendant().withPersonDetails(Person.person().withFirstName("John").withLastName("Rambo").build()).withArrestSummonsNumber(ASN).build())
                                .withOffences(singletonList(offence().withId(randomUUID()).withOffenceCode(OFFENCE_CODE).build()))
                                .build(),
                        defendant().withId(ALAN_SMITH_ID)
                                .withMasterDefendantId(ALAN_SMITH_ID)
                                .withIsYouth(Boolean.TRUE)
                                .withPersonDefendant(personDefendant()
                                        .withPersonDetails(Person.person().withFirstName("Alan").withLastName("Smith")
                                                .withNationalityDescription(NATIONALITY_DESCRIPTION)
                                                .withAdditionalNationalityDescription(ADD_NATIONALITY_DESCRIPTION)
                                                .build()).build())
                                .withOffences(singletonList(offence().withId(randomUUID()).withOffenceCode(OFFENCE_CODE).build()))
                                .build()))
                .build();
    }

    private Optional<JsonObject> getProsecutorFromReferenceData(final String prosecutorId) {
        return Optional.of(createObjectBuilder()
                .add("id", prosecutorId)
                .add("address", createObjectBuilder()
                        .add(ADDRESS_1, "address1")
                        .add(ADDRESS_2, "address2")
                        .add(ADDRESS_3, "address3")
                        .add(ADDRESS_4, "address4")
                        .add(ADDRESS_5, "address5")
                        .add(POSTCODE, "postcode")
                )
                .build());
    }

    private ProsecutionCase getProsecutionCaseWithDefendantLegalEntity() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(CASE_URN)
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                        .withProsecutionAuthorityReference(PROSECUTION_AUTHORITY_REFERENCE)
                        .build())
                .withCaseStatus(CASE_STATUS)
                .withRemovalReason(REMOVAL_REASON)
                .withCaseMarkers(asList(marker().withId(randomUUID()).withMarkerTypeDescription("Vulnerable or intimidated victim").build(),
                        marker().withId(randomUUID()).withMarkerTypeDescription("Prohibited Weapons").build()))
                .withDefendants(singletonList(defendant().withId(randomUUID())
                        .withLegalEntityDefendant(legalEntityDefendant().withOrganisation(organisation().withName(ORG_NAME)
                                .withAddress(address()
                                        .withAddress1(ADDRESS_1)
                                        .withAddress2(ADDRESS_2)
                                        .withAddress3(ADDRESS_3)
                                        .withAddress4(ADDRESS_4)
                                        .withPostcode(POSTCODE)
                                        .build())
                                .build()).build())
                        .withDefenceOrganisation(organisation().withName(LEGAL_REP_NAME).build())
                        .build()))
                .build();
    }

    private List<Hearings> getCaseHearings() {
        return asList(hearings()
                        .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                        .withDefendants(asList(defendants()
                                .withId(JOHN_SMITH_ID)
                                .withJudicialResults(asList(judicialResult().withLabel(CASE_RESULT_LABEL)
                                        .withJudicialResultId(CASE_JUDICIAL_RESULT_ID)
                                        .withResultText(JUDICIAL_RESULT_TEXT)
                                        .withResultWording(RESULT_WORDING).build()))
                                .withOffences(asList(offences()
                                        .withId(OFFENCE_ID)
                                        .withPleas(asList(
                                                plea().withPleaDate(PLEA_DATE).withPleaValue(PLEA_GUILTY).build(),
                                                plea().withPleaDate(PLEA_DATE.minusDays(1)).withPleaValue(PLEA_NOT_GUILTY).build(),
                                                plea().withPleaDate(PLEA_DATE.plusDays(1)).build())
                                        )
                                        .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                                                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                                                .withIndicatedPleaDate(INDICATED_PLEA_DATE)
                                                .build())
                                        .withVerdicts(asList(
                                                verdict().withVerdictDate(VERDICT_DATE.plusDays(1)).build(),
                                                verdict()
                                                        .withVerdictDate(VERDICT_DATE)
                                                        .withVerdictType(VerdictType.verdictType().withCategory(GUILTY).build()).build(),
                                                verdict()
                                                        .withVerdictDate(VERDICT_DATE.minusDays(1))
                                                        .withVerdictType(VerdictType.verdictType().withCategory(NOT_GUILTY).build()).build())
                                        )
                                        .withJudicialResults(asList(judicialResult().withIsDeleted(true).build(),
                                                judicialResult()
                                                        .withLabel(OFFENCE_RESULT_LABEL_1)
                                                        .withJudicialResultId(RECENT_JUDICIAL_RESULT_ID)
                                                        .withOrderedDate(LocalDate.now().plusDays(1))
                                                        .withAmendmentReason(OTHER_AMEND_REASON)
                                                        .withResultText(CAAG_RESULT_TEXT)
                                                        .build(),
                                                judicialResult()
                                                        .withJudicialResultId(randomUUID())
                                                        .withDelegatedPowers(DelegatedPowers.delegatedPowers().withFirstName("first").withLastName("last").build())
                                                        .withOrderedDate(LocalDate.now())
                                                        .withAmendmentDate(LocalDate.now())
                                                        .withAmendmentReason(AMEND_REASON)
                                                        .withLastSharedDateTime(LocalDate.now().toString())
                                                        .withLabel(OFFENCE_RESULT_LABEL_2)
                                                        .withResultText(CAAG_RESULT_TEXT)
                                                        .withJudicialResultPrompts(singletonList(JudicialResultPrompt.judicialResultPrompt().withLabel(OFFENCE_RESULT_LABEL_1).withValue(VALUE).build()))
                                                        .build()))
                                        .build()))
                                .build()))
                        .withDefendantJudicialResults(singletonList(DefendantJudicialResult.defendantJudicialResult()
                                .withJudicialResult(judicialResult()
                                        .withJudicialResultId(JUDICIAL_RESULT_ID)
                                        .withLabel(DEFENDANT_RESULT_LABEL).build())
                                .withMasterDefendantId(JOHN_SMITH_MASTER_ID)
                                .build()))
                        .build(),
                hearings()
                        .withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED)
                        .withDefendants(asList(defendants()
                                .withId(JOHN_SMITH_ID)
                                .withJudicialResults(asList(judicialResult().withLabel(ANOTHER_CASE_RESULT_LABEL)
                                        .withJudicialResultId(CASE_JUDICIAL_RESULT_ID)
                                        .withResultText(CAAG_RESULT_TEXT)
                                        .withResultWording(RESULT_WORDING).build()))
                                .withOffences(asList(offences()
                                        .withId(OFFENCE_ID)
                                        .withJudicialResults(asList(judicialResult().withIsDeleted(true).build(),
                                                judicialResult()
                                                        .withJudicialResultId(randomUUID())
                                                        .withDelegatedPowers(DelegatedPowers.delegatedPowers().withFirstName("first").withLastName("last").build())
                                                        .withOrderedDate(LocalDate.now())
                                                        .withAmendmentDate(LocalDate.now())
                                                        .withAmendmentReason(AMEND_REASON)
                                                        .withLastSharedDateTime(LocalDate.now().toString())
                                                        .withLabel(ANOTHER_OFFENCE_RESULT_LABEL)
                                                        .withResultText(CAAG_RESULT_TEXT)
                                                        .withJudicialResultPrompts(singletonList(JudicialResultPrompt.judicialResultPrompt().withLabel(OFFENCE_RESULT_LABEL_1).withValue(VALUE).build()))
                                                        .build()))
                                        .build()))
                                .build()))
                        .withDefendantJudicialResults(singletonList(DefendantJudicialResult.defendantJudicialResult()
                                .withJudicialResult(judicialResult()
                                        .withJudicialResultId(JUDICIAL_RESULT_ID)
                                        .withLabel(ANOTHER_DEFENDANT_RESULT_LABEL).build())
                                .withMasterDefendantId(JOHN_SMITH_MASTER_ID)
                                .build()))
                        .build());
    }

    @Test
    public void shouldGetCivilDetailsFromProsecutionCase() {
        final CivilFeeEntity civilFeeEntity = new CivilFeeEntity(UUID.randomUUID(), uk.gov.moj.cpp.progression.domain.constant.FeeType.INITIAL, uk.gov.moj.cpp.progression.domain.constant.FeeStatus.OUTSTANDING,"paymentRef");
        when(civilFeeRepository.findBy(any())).thenReturn(civilFeeEntity);

        caseAtAGlanceHelper = new CaseAtAGlanceHelper(getProsecutionCaseWithCivilCaseDetails(), new ArrayList<>(), referenceDataService, civilFeeRepository, relatedReferenceRepository);
        final CaseDetails caseDetails = caseAtAGlanceHelper.getCaseDetails();

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getCivilFees().size(), is(1));
        assertThat(caseDetails.getIsCivil(), is(true));
        assertThat(caseDetails.getIsGroupMaster(), is(true));
    }

    private ProsecutionCase getProsecutionCaseWithCivilCaseDetails() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(CASE_URN)
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                        .withProsecutionAuthorityReference(PROSECUTION_AUTHORITY_REFERENCE)
                        .build())
                .withIsCpsOrgVerifyError(true)
                .withCaseStatus(CASE_STATUS)
                .withRemovalReason(REMOVAL_REASON)
                .withInitiationCode(InitiationCode.J)
                .withCivilFees(asList(CivilFees.civilFees()
                        .withFeeId(UUID.randomUUID())
                        .withFeeStatus(FeeStatus.OUTSTANDING)
                        .withFeeType(FeeType.INITIAL)
                        .withPaymentReference("paymentReference001")
                        .build()))
                .withIsCivil(true)
                .withIsGroupMaster(true)
                .withIsGroupMember(true)
                .withGroupId(UUID.randomUUID())
                .withCaseMarkers(asList(marker().withId(randomUUID()).withMarkerTypeDescription("Vulnerable or intimidated victim").build(),
                        marker().withId(randomUUID()).withMarkerTypeDescription("Prohibited Weapons").build()))
                .withDefendants(asList(defendant().withId(JOHN_SMITH_ID)
                                .withMasterDefendantId(JOHN_SMITH_MASTER_ID)
                                .withPersonDefendant(personDefendant()
                                        .withBailStatus(BailStatus.bailStatus().withDescription(REMAND_STATUS).build())
                                        .withPersonDetails(Person.person()
                                                .withFirstName("John").withLastName("Smith")
                                                .withNationalityDescription(NATIONALITY_DESCRIPTION)
                                                .withDateOfBirth(DATE_OF_BIRTH)
                                                .withAddress(ADDRESS)
                                                .withInterpreterLanguageNeeds(INTERPRETER_LANGUAGE_NEEDS).build())
                                        .build())
                                .withDefenceOrganisation(organisation().withName(LEGAL_REP_NAME).build())
                                .withOffences(asList(offence().withId(OFFENCE_ID)
                                                .withOffenceCode(OFFENCE_CODE)
                                                .withOffenceTitle(OFFENCE_TITLE)
                                                .withOffenceTitleWelsh(OFFENCE_TITLE_WELSH)
                                                .withCount(2)
                                                .withOrderIndex(2)
                                                .withPlea(plea().withPleaDate(PLEA_DATE).withPleaValue(PLEA_GUILTY).build())
                                                .withVerdict(verdict().withVerdictDate(VERDICT_DATE).withVerdictType(VerdictType.verdictType().withCategory(GUILTY).build()).build())
                                                .withOffenceLegislation(OFFENCE_LEGISLATION)
                                                .withStartDate(LocalDate.now())
                                                .withEndDate(LocalDate.now())
                                                .withJudicialResults(emptyList())
                                                .build(),
                                        offence().withId(randomUUID()).withOffenceCode(OTHER_OFFENCE_CODE).build()))
                                .withLegalAidStatus(LEGAL_AID_STATUS)
                                .build(),
                        defendant().withId(JOHN_RAMBO_ID)
                                .withMasterDefendantId(JOHN_RAMBO_ID)
                                .withPersonDefendant(personDefendant().withPersonDetails(Person.person().withFirstName("John").withLastName("Rambo").build()).build())
                                .withOffences(singletonList(offence().withId(randomUUID()).withOffenceCode(OFFENCE_CODE).build()))
                                .build(),
                        defendant().withId(ALAN_SMITH_ID)
                                .withMasterDefendantId(ALAN_SMITH_ID)
                                .withIsYouth(Boolean.TRUE)
                                .withPersonDefendant(personDefendant()
                                        .withPersonDetails(Person.person().withFirstName("Alan").withLastName("Smith")
                                                .withNationalityDescription(NATIONALITY_DESCRIPTION)
                                                .withAdditionalNationalityDescription(ADD_NATIONALITY_DESCRIPTION)
                                                .build()).build())
                                .withOffences(singletonList(offence().withId(randomUUID()).withOffenceCode(OFFENCE_CODE).build()))
                                .build()))
                .build();
    }

}
