package uk.gov.justice.api.resource.utils;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.ApplicantCounsel;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CompanyRepresentative;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.Jurors;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.RespondentCounsel;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.core.courts.YouthCourt;
import uk.gov.justice.progression.courts.CourtApplications;
import uk.gov.justice.progression.courts.CustodialEstablishment;
import uk.gov.justice.progression.courts.DefenceOrganisation;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.Respondents;
import uk.gov.justice.progression.courts.exract.AttendanceDayAndType;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtExtractTransformerTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String PROSECUTION_AUTHORITY_CODE = "TFL";
    private static final String GUILTY = "Guilty";
    private static final String PLEA_GUILTY_DESCRIPTION = "plea guilty description";
    private static final String COUNSELS_STATUS = "counsels status";
    private static final String FULL_NAME = "Jack Denial";
    private static final String ORGANISATION_NAME = "Liverpool defence";
    private static final String DESCRIPTION = "Dodged TFL tickets with passion";
    private static final String APPLICATION_TYPE = "Apil";
    private static final String SYNONYM = "synonym";
    private static final String HEARING1 = "HEARING-1";
    private static final String HEARING2 = "HEARING-2";
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID DEFENDANT_ID_2ND = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID HEARING_ID_2 = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final LocalDate DOB = LocalDate.of(LocalDate.now().getYear() - 30, 01, 01);

    private static final String HEARING_DATE_1 = "2018-06-01T10:00:00.000Z";
    private static final String HEARING_DATE_2 = "2018-06-04T10:00:00.000Z";
    private static final String HEARING_DATE_3 = "2018-07-01T10:00:00.000Z";

    private static final String CUSTODY_TYPE = "Prison";
    private static final String CUSTODY_ESTABLISHMENT_NAME = "HMP Belmarsh";
    private static final UUID CUSTODY_ESTABLISHMENT_UUID = randomUUID();

    private static final String DEFENDANT_NAME = "Harry JackKane Junior";
    private static final String DEFENDANT_NAME_2ND = "Kahul Vinchi";
    private static final String COURT_NAME = "liver pool";
    private static final String HEARING_TYPE = "Sentence";
    private static final String ADDRESS_1 = "22";
    private static final String ADDRESS_2 = "Acacia Avenue";
    private static final String ADDRESS_3 = "Acacia Town";
    private static final String POST_CODE = "CR7 0AA";
    private static final String PAR = "6225bd77";
    private static final String LABEL = "Fine";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Denial";
    private static final String FIRST_NAME_2 = "First name 2";
    private static final String LAST_NAME_2 = " Last name 2";
    private static final String PROMPT_VALUE = "10";
    private static final String COURT_EXTRACT = "Y";
    private static final String NO_COURT_EXTRACT = "N";
    private static final String PLEA_GUILTY = "GUILTY";
    private static final LocalDate CONVICTION_DATE = LocalDate.of(2018, 04, 04);
    private static final LocalDate PLEA_DATE = LocalDate.of(2018, 01, 01);
    private static final String DEFENDANT_AGE = "30";
    private static final LocalDate OUTCOME_DATE = LocalDate.of(2019, 07, 20);
    private static final LocalDate RESPONSE_DATE = LocalDate.of(2019, 07, 25);
    public static final String LEGACY_COMPENSATION = "Legacy Compensation";
    public static final String ORDERING = "Ordering";
    public static final String COMPENSATION = "Compensation";
    public static final String RESTRAINING_ORDER = "Restraining order";
    public static final String LEGACY_NAME = "Legacy Name";
    public static final String MINOR_CREDITOR_FIRST_NAME = "Minor creditor first name";
    public static final String AMOUNT_OF_COMPENSATION = "Amount of compensation";
    public static final String PROTECTED_PERSON_S_NAME = "Protected person's name";
    public static final String PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1 = "Protected person's address address line 1";
    public static final String THIS_ORDER_IS_MADE_ON = "this order is made on";
    public static final String LEGACY_RESULT = "Legacy Result";
    public static final String RESTRAINING_ORDER_DEFENDANT_LEVEL = "Restraining order Defendant Level";
    public static final String COMPENSATION_DEFENDANT_LEVEL = "Compensation Defendant Level";
    public static final String LEGACY_COMPENSATION_DEFENDANT_LEVEL = "Legacy Compensation Defendant Level";
    public static final String ORDERING_DEFENDANT_LEVEL = "Ordering Defendant Level";

    private final ProsecutionCase prosecutionCase = createProsecutionCase();
    private CourtExtractTransformer target;

    @InjectMocks
    private TransformationHelper transformationHelper;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private RequestedNameMapper requestedNameMapper;


    @Before
    public void init() {
        target = new CourtExtractTransformer();
        setField(this.target, "transformationHelper", transformationHelper);
        when(referenceDataService.getProsecutor(argThat(any(JsonEnvelope.class)), argThat(any(ProsecutionCaseIdentifier.class)))).thenReturn(new uk.gov.justice.progression.courts.exract.ProsecutingAuthority(null, null, null));
        when(requestedNameMapper.getRequestedJudgeName(argThat(any(JsonObject.class)))).thenReturn("Denial");
        when(referenceDataService.getJudiciary(argThat(any(UUID.class)))).thenReturn(Optional.ofNullable(createJudiciaryJsonObject()));
    }

    @Test
    public void testTransformToCourtExtract_shouldUseDefendantAssociatedDefenceOrganisation_whenPresent() {
        final String extractType = "CrownCourtExtract";
        final GetHearingsAtAGlance hearingsAtAGlance = createCaseAtAGlance();

        final List<String> selectedHearingIds = asList(HEARING_ID.toString(), HEARING_ID_2.toString());
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);
        final CourtExtractRequested courtExtract2ndDefendant = target.getCourtExtractRequested(hearingsAtAGlance, DEFENDANT_ID_2ND.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);
        final List<AttendanceDayAndType> attendanceDays = courtExtractRequested.getDefendant().getAttendanceDays();
        final List<JudicialResult> defendant2ndResults = courtExtract2ndDefendant.getDefendant().getResults();

        assertValues(courtExtractRequested, extractType, HEARING_DATE_2, HEARING_DATE_3, 4, "resultWording", "Fine");
        assertThat(attendanceDays.size(), is((2)));
        attendanceDays.forEach(ad -> anyOf(is(CourtExtractTransformer.PRESENT_BY_POLICE_VIDEO_LINK),
                is(CourtExtractTransformer.PRESENT_BY_PRISON_VIDEO_LINK),
                is(CourtExtractTransformer.PRESENT_BY_VIDEO_DEFAULT),
                is(CourtExtractTransformer.PRESENT_IN_PERSON)));
        assertThat(courtExtractRequested.getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getCompanyRepresentatives().size(), is(2));
        assertThat(courtExtractRequested.getProsecutionCounsels().size(), is(2));
        assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceCounsels().size(), is(2));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(2));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(2));
        assertThat(courtExtractRequested.getDefendant().getOffences().size(), is(2));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().size(), is(1));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(1).getResults().size(), is(1));
        assertThat(defendant2ndResults.size(), is(2));
    }

    @Test
    public void testTransformToCourtExtract_shouldUseSelectedHearing_whenExtractTypeIsCrownCourtExtractAndOneHearingsSelected() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = Collections.singletonList(HEARING_ID.toString());
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(createCaseAtAGlance(), DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

//        assertGetCourtExtractRequested(courtExtractRequested, extractType, 2);
    }

    @Test
    public void testTransformToCourtExtract_whenExtractTypeIsCrownCourtWithaPendingApplication_expectAppealPendingFlagAsTrue() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = asList(HEARING_ID.toString());
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(createCaseAtAGlanceWithCourtApplicationParty(), DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

  //      assertGetCourtExtractRequested(courtExtractRequested, extractType, 1);
    }

    @Test
    public void shouldRetrieveMasterDefendantBasedJudicialResultsWhenGetCourtExtractRequestedInvoked() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = asList(HEARING_ID.toString());
        final List<Hearings> judicialResultsByRandomId = createHearingsWithJudicialResults(randomUUID());
        final List<Hearings> judicialResultsByMasterDefendantId = createHearingsWithJudicialResults(MASTER_DEFENDANT_ID);

        final CourtExtractRequested extractRequestedByRandomId = target.getCourtExtractRequested(createCaseAtAGlance(judicialResultsByRandomId), DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);
        final CourtExtractRequested extractRequestedByMasterDefendantId = target.getCourtExtractRequested(createCaseAtAGlance(judicialResultsByMasterDefendantId), DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertGetCourtExtractRequested(extractRequestedByRandomId, extractType, 1);
        assertGetCourtExtractRequested(extractRequestedByMasterDefendantId, extractType, 2);

    }

    @Test
    public void testEjectCaseWithUnResultedCase_shouldExtractAllResultedAndFutureHearings() {
        final String extractType = "CrownCourtExtract";
        final GetHearingsAtAGlance hearingsAtAGlance = createCaseAtAGlanceWithoutHearings();
        final CourtExtractRequested courtExtractRequested = target.ejectCase(prosecutionCase, hearingsAtAGlance, DEFENDANT_ID.toString(), randomUUID());

        //then
        assertValues(courtExtractRequested, extractType, null, null, 0, null, null);
        assertThat(courtExtractRequested.getIsAppealPending(), is((true)));
        assertThat(courtExtractRequested.getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(1));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(0));

    }

    @Test
    public void testEjectCaseWithUnResultedCase_ForLegalEntity() {
        final String extractType = "CrownCourtExtract";
        final GetHearingsAtAGlance hearingsAtAGlance = createCaseAtAGlanceWithoutHearings();
        final CourtExtractRequested courtExtractRequested = target.ejectCase(getProsecutionCaseForLegalEntityDefendant(), hearingsAtAGlance, DEFENDANT_ID.toString(), randomUUID());

        assertNotNull(courtExtractRequested.getProsecutingAuthority());
        assertThat(courtExtractRequested.getCaseReference(), is(PAR));
        assertThat(courtExtractRequested.getExtractType(), is(extractType));
        assertThat(courtExtractRequested.getIsAppealPending(), is((true)));
        assertThat(courtExtractRequested.getDefendant().getName(), is("ABC LTD"));
        assertThat(courtExtractRequested.getDefendant().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(courtExtractRequested.getDefendant().getAddress().getAddress2(), is(ADDRESS_2));
        assertThat(courtExtractRequested.getDefendant().getAddress().getAddress3(), is(ADDRESS_3));
        assertThat(courtExtractRequested.getDefendant().getAddress().getPostcode(), is(POST_CODE));
        assertThat(courtExtractRequested.getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(1));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(0));
    }

    @Test
    public void testEjectCase_shouldExtractAllResultedAndFutureHearings() {
        final String extractType = "CrownCourtExtract";
        final GetHearingsAtAGlance hearingsAtAGlance = createCaseAtAGlance();
        final CourtExtractRequested courtExtractRequested = target.ejectCase(prosecutionCase, hearingsAtAGlance, DEFENDANT_ID.toString(), randomUUID());

        //then
        // GPE-15039 Commented temporarily
       /* assertThat(courtExtractRequested.getIsAppealPending(), is((true)));
        assertValues(courtExtractRequested, extractType, HEARING_DATE_2, HEARING_DATE_3, 3, "resultWording", "Fine");
        assertThat(courtExtractRequested.getDefendant().getAttendanceDays().size(), is((2)));
        assertThat(courtExtractRequested.getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getCompanyRepresentatives().size(), is(2));
        assertThat(courtExtractRequested.getProsecutionCounsels().size(), is(2));
        assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceCounsels().size(), is(2));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(2));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(2));
*/
    }

    @Test
    public void testTransformToCourtExtract_whenDefendantIsHeardInYouthCourt_expectYouthCourtNameInPublishingCourtAndCourtCenter() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = asList(HEARING_ID.toString(), HEARING_ID_2.toString());
        final GetHearingsAtAGlance hearingsAtAGlance = createCaseAtAGlanceWithDefendantHeardInYouthCourt();
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);
        assertThat(courtExtractRequested.getPublishingCourt().getName(), is("Youth Court Name"));
        assertThat(courtExtractRequested.getPublishingCourt().getWelshName(), is("Welsh Youth Court Name"));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtCentre().getName(), is("Youth Court Name"));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtCentre().getWelshName(), is("Welsh Youth Court Name"));


    }


    @Test
    public void shouldGenerateACourtExtractAfterFilteringOutResultDefinitionsThatAreNotForTheCourtExtract() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = asList(HEARING_ID.toString());
        final List<Hearings> judicialResultsByMasterDefendantId = createHearingsWithJudicialResultsWithCourtExtract(MASTER_DEFENDANT_ID);

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(createCaseAtAGlance(judicialResultsByMasterDefendantId), DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);
        verifyDefendantLevelResults(courtExtractRequested);
        verifyCaseLevelResult(courtExtractRequested);
        verifyOffenceLevelResult(courtExtractRequested);

    }

    private void verifyOffenceLevelResult(final CourtExtractRequested courtExtractRequested) {
        //Case Level Does Not Exist
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().stream().noneMatch(r -> r.getLabel().equals(LEGACY_COMPENSATION)), is(true));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().stream().noneMatch(r -> r.getLabel().equals(ORDERING)), is(true));

        //Defendant Level Exist
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().stream().anyMatch(r -> r.getLabel().equals(COMPENSATION)), is(true));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().stream().anyMatch(r -> r.getLabel().equals(RESTRAINING_ORDER)), is(true));


        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().stream()
                .filter(r -> r.getLabel().equals(COMPENSATION))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(AMOUNT_OF_COMPENSATION))), is(true));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().stream()
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(THIS_ORDER_IS_MADE_ON))), is(true));

        // Prompt Should not exist
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().stream()
                .filter(r -> r.getLabel().equals(COMPENSATION))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(MINOR_CREDITOR_FIRST_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().stream()
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().stream()
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1))), is(true));
    }

    private void verifyCaseLevelResult(final CourtExtractRequested courtExtractRequested) {
        //Case Level Does Not Exist
        assertThat(courtExtractRequested.getDefendant().getResults().stream().noneMatch(r -> r.getLabel().equals(LEGACY_COMPENSATION)), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().stream().noneMatch(r -> r.getLabel().equals(ORDERING)), is(true));

        //Defendant Level Exist
        assertThat(courtExtractRequested.getDefendant().getResults().stream().anyMatch(r -> r.getLabel().equals(COMPENSATION)), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().stream().anyMatch(r -> r.getLabel().equals(RESTRAINING_ORDER)), is(true));


        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(COMPENSATION))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(AMOUNT_OF_COMPENSATION))), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(THIS_ORDER_IS_MADE_ON))), is(true));

        // Prompt Should not exist
        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(COMPENSATION))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(MINOR_CREDITOR_FIRST_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1))), is(true));
    }

    private void verifyDefendantLevelResults(final CourtExtractRequested courtExtractRequested) {
        //Defendant Level Not Exist
        assertThat(courtExtractRequested.getDefendant().getResults().stream().noneMatch(r -> r.getLabel().equals(LEGACY_COMPENSATION_DEFENDANT_LEVEL)), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().stream().noneMatch(r -> r.getLabel().equals(ORDERING_DEFENDANT_LEVEL)), is(true));

        //Defendant Level Exist
        assertThat(courtExtractRequested.getDefendant().getResults().stream().anyMatch(r -> r.getLabel().equals(COMPENSATION_DEFENDANT_LEVEL)), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().stream().anyMatch(r -> r.getLabel().equals(RESTRAINING_ORDER_DEFENDANT_LEVEL)), is(true));

        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(COMPENSATION_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(AMOUNT_OF_COMPENSATION))), is(true));


        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(THIS_ORDER_IS_MADE_ON))), is(true));

        // Prompt Should not exist
        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(COMPENSATION_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(MINOR_CREDITOR_FIRST_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getResults().stream()
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1))), is(true));
    }

    private void assertGetCourtExtractRequested(final CourtExtractRequested courtExtractRequested, String extractType, int resultsCount) {
        assertThat(courtExtractRequested.getIsAppealPending(), is((true)));
        assertValues(courtExtractRequested, extractType, HEARING_DATE_2, HEARING_DATE_3, resultsCount, "resultWording", "Fine");
        assertThat(courtExtractRequested.getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getCompanyRepresentatives().size(), is(1));
        assertThat(courtExtractRequested.getProsecutionCounsels().size(), is(1));
        assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceCounsels().size(), is(1));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(2));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(1));
    }

    private void assertValues(final CourtExtractRequested courtExtractRequested, final String extractType, final String hearingDate, final String hearingDate2, int resultsCount, final String resultWording, final String label) {
        assertNotNull(courtExtractRequested.getProsecutingAuthority());
        assertThat(courtExtractRequested.getCaseReference(), is(PAR));
        assertThat(courtExtractRequested.getExtractType(), is(extractType));

        //Defendant
        assertThat(courtExtractRequested.getDefendant().getName(), is(DEFENDANT_NAME));
        assertThat(courtExtractRequested.getDefendant().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(courtExtractRequested.getDefendant().getAddress().getAddress2(), is(ADDRESS_2));
        assertThat(courtExtractRequested.getDefendant().getAddress().getAddress3(), is(ADDRESS_3));
        assertThat(courtExtractRequested.getDefendant().getAddress().getPostcode(), is(POST_CODE));
        assertThat(courtExtractRequested.getDefendant().getAge(), is(DEFENDANT_AGE));
        assertThat(courtExtractRequested.getDefendant().getDateOfBirth(), is(DOB));

        if (hearingDate != null) {
            //Court
            assertThat(courtExtractRequested.getPublishingCourt().getName(), is(COURT_NAME));

            assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceCounsels().get(0).getName(), is(FULL_NAME));
            assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceCounsels().get(0).getRole(), is(COUNSELS_STATUS));
            assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceOrganisation().getName(), is(ORGANISATION_NAME));
            assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceOrganisation().getAddress().getAddress1(), is(ADDRESS_1));
            assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceOrganisation().getContact().getFax(), is(("fax")));
            //Hearing
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getType(), is(HEARING_TYPE));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getHearingDays().get(0).getDay(), is((ZonedDateTimes.fromString(hearingDate).toLocalDate())));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getHearingDays().get(1).getDay(), is((ZonedDateTimes.fromString(hearingDate2).toLocalDate())));

            //court decision
            assertThat(courtExtractRequested.getCourtDecisions().get(0).getJudicialDisplayName(), is("Chair: Denial Winger1: Denial Winger2: Denial"));
            assertThat(courtExtractRequested.getCourtDecisions().get(0).getRoleDisplayName(), is("District judge"));
            assertThat(courtExtractRequested.getCourtDecisions().get(0).getJudiciary().get(0).getName(), is(FULL_NAME));

            //results
            assertThat(courtExtractRequested.getDefendant().getResults().size(), is(resultsCount));
            assertThat(courtExtractRequested.getDefendant().getResults().get(0).getIsAvailableForCourtExtract(), is(true));
            assertThat(courtExtractRequested.getDefendant().getResults().get(0).getLabel(), is(label));
            assertThat(courtExtractRequested.getDefendant().getResults().get(0).getDelegatedPowers().getFirstName(), is(FIRST_NAME));
            assertThat(courtExtractRequested.getDefendant().getResults().get(0).getDelegatedPowers().getLastName(), is(LAST_NAME));
            assertThat(courtExtractRequested.getDefendant().getResults().get(0).getJudicialResultPrompts().get(0).getLabel(), is(LABEL));
            assertThat(courtExtractRequested.getDefendant().getResults().get(0).getJudicialResultPrompts().get(0).getValue(), is(PROMPT_VALUE));
            assertThat(courtExtractRequested.getDefendant().getResults().get(0).getResultText(), is("resultText"));


            //offences
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getConvictionDate(), is((CONVICTION_DATE)));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getIsAvailableForCourtExtract(), is(true));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getResultWording(), is(resultWording));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getDelegatedPowers().getFirstName(), is(FIRST_NAME));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getDelegatedPowers().getLastName(), is(LAST_NAME));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getJudicialResultPrompts().get(0).getLabel(), is(LABEL));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getJudicialResultPrompts().get(0).getValue(), is(PROMPT_VALUE));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getResults().get(0).getResultText(), is("resultText"));

            //Offence Plea
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getPleas().get(0).getDelegatedPowers().getFirstName(), is(FIRST_NAME));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getPleas().get(0).getDelegatedPowers().getLastName(), is(LAST_NAME));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getPleas().get(0).getPleaDate(), is(PLEA_DATE));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getPleas().get(0).getPleaValue(), is(PLEA_GUILTY));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getIndicatedPlea().getIndicatedPleaValue(), is(IndicatedPleaValue.INDICATED_GUILTY));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getAllocationDecision().getOffenceId(), is(OFFENCE_ID));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getAllocationDecision().getMotReasonCode(), is("4"));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getAllocationDecision().getSequenceNumber(), is(40));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getAllocationDecision().getOriginatingHearingId(), is(HEARING_ID));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getAllocationDecision().getAllocationDecisionDate(), is(CONVICTION_DATE));

            //Offence verdict
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getVerdictType().getCategory(), is((GUILTY)));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getVerdictType().getCategoryType(), is((GUILTY)));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getVerdictType().getDescription(), is((PLEA_GUILTY_DESCRIPTION)));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getJurors().getNumberOfJurors(), is(2));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getJurors().getNumberOfSplitJurors(), is(2));
            assertThat(courtExtractRequested.getDefendant().getOffences().get(0).getVerdicts().get(0).getJurors().getUnanimous(), is(true));
        }
        //parentGuardian
        assertThat(courtExtractRequested.getParentGuardian().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(courtExtractRequested.getParentGuardian().getAddress().getAddress2(), is(ADDRESS_2));
        assertThat(courtExtractRequested.getParentGuardian().getAddress().getPostcode(), is(POST_CODE));
        assertThat(courtExtractRequested.getParentGuardian().getName(), is(FIRST_NAME + " D " + LAST_NAME));

        if (hearingDate != null) {
            //referralReason
            assertThat(courtExtractRequested.getReferralReason(), is(DESCRIPTION));

            //
            assertThat(courtExtractRequested.getCompanyRepresentatives().get(0).getName(), is(FIRST_NAME + " " + LAST_NAME));
            assertThat(courtExtractRequested.getCompanyRepresentatives().get(0).getRole(), is("DIRECTOR"));
            assertThat(courtExtractRequested.getCompanyRepresentatives().get(0).getAttendanceDays().size(), is((1)));

            //courtApplications
            assertThat(courtExtractRequested.getCourtApplications().get(0).getApplicationType(), is(APPLICATION_TYPE));
        }
        // application representation
        assertNotNull(courtExtractRequested.getCourtApplications());
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getName(), is(FIRST_NAME));
        assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getContact().getHome(), is("home"));
        if (hearingDate != null) {
            assertNotNull(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().get(0));
            assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().get(0).getFirstName(), is(FIRST_NAME));
            assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().get(0).getLastName(), is(LAST_NAME));
            assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().get(0).getStatus(), is("Jury"));
            assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().get(0).getRespondentCounsels().get(0).getFirstName(), is(FIRST_NAME));
            assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().get(0).getRespondentCounsels().get(0).getLastName(), is(LAST_NAME));
            assertThat(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().get(0).getRespondentCounsels().get(0).getStatus(), is("Solicitor"));
        }
        assertNotNull(courtExtractRequested.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().get(0));
    }

    private ProsecutionCase getProsecutionCaseForLegalEntityDefendant() {
        final Defendant defendant = Defendant.defendant().withId(DEFENDANT_ID)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation().withAddress(createAddress())
                                .withName("ABC LTD").build()).build())
                .withOffences(Collections.emptyList())
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .build();

        final List<Defendant> defendants = new ArrayList<Defendant>() {{
            add(defendant);
        }};

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(CASE_ID)
                .withDefendants(defendants)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityReference(PAR)
                        .build())
                .build();

        return prosecutionCase;
    }

    private ProsecutionCase createProsecutionCase() {
        final Defendant defendant = Defendant.defendant().withId(DEFENDANT_ID)
                .withPersonDefendant(createPersonDefendant())
                .withAssociatedPersons(asList(AssociatedPerson.associatedPerson()
                        .withPerson(createPerson())
                        .build()))
                .withDefenceOrganisation(Organisation.organisation()
                        .withName(ORGANISATION_NAME)
                        .withAddress(createAddress())
                        .withContact(createContact())
                        .build())
                .withOffences(Collections.emptyList())
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .build();

        final Defendant defendant_2nd = Defendant.defendant().withId(DEFENDANT_ID_2ND)
                .withPersonDefendant(createPersonDefendant())
                .withAssociatedPersons(asList(AssociatedPerson.associatedPerson()
                        .withPerson(createPerson())
                        .build()))
                .withDefenceOrganisation(Organisation.organisation()
                        .withName(ORGANISATION_NAME)
                        .withAddress(createAddress())
                        .withContact(createContact())
                        .build())
                .withOffences(Collections.emptyList())
                .withMasterDefendantId(DEFENDANT_ID_2ND)
                .build();
        final List<Defendant> defendants = new ArrayList<Defendant>() {{
            add(defendant);
            add(defendant_2nd);
        }};

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(CASE_ID)
                .withDefendants(defendants)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityReference(PAR)
                        .build())
                .build();

        return prosecutionCase;
    }

    private GetHearingsAtAGlance createCaseAtAGlance() {
        return createCaseAtAGlance(null);
    }

    private GetHearingsAtAGlance createCaseAtAGlanceWithDefendantHeardInYouthCourt() {
        GetHearingsAtAGlance.Builder builder = GetHearingsAtAGlance.getHearingsAtAGlance().withId(CASE_ID);
        builder.withProsecutionCaseIdentifier(createPCIdentifier());
        builder.withDefendantHearings(createDefendantHearing());
        builder.withHearings(createHearingsWithYouthCourtDetails(asList(DEFENDANT_ID, DEFENDANT_ID_2ND)));
        builder.withCourtApplications(asList(createCourtApplication()));
        return builder.build();
    }

    private GetHearingsAtAGlance createCaseAtAGlance(final List<Hearings> hearingsList) {
        GetHearingsAtAGlance.Builder builder = GetHearingsAtAGlance.getHearingsAtAGlance().withId(CASE_ID);
        builder.withProsecutionCaseIdentifier(createPCIdentifier());
        builder.withDefendantHearings(createDefendantHearing());
        builder.withHearings(isNull(hearingsList) ? createHearingsWithJudicialResults(MASTER_DEFENDANT_ID) : hearingsList);
        builder.withCourtApplications(asList(createCourtApplication()));
        return builder.build();
    }

    private GetHearingsAtAGlance createCaseAtAGlanceWithCourtApplicationParty() {
        GetHearingsAtAGlance.Builder builder = createCaseAtAGlanceBuilder();
        builder.withCourtApplications(asList(createCourtApplicationWithApplicationParty()));
        return builder.build();
    }

    private GetHearingsAtAGlance createCaseAtAGlanceWithoutHearings() {
        GetHearingsAtAGlance.Builder builder = createCaseAtAGlanceBuilder();
        builder.withHearings(Collections.emptyList());
        return builder.build();
    }

    private GetHearingsAtAGlance.Builder createCaseAtAGlanceBuilder() {
        GetHearingsAtAGlance.Builder builder = GetHearingsAtAGlance.getHearingsAtAGlance().withId(CASE_ID);
        builder.withProsecutionCaseIdentifier(createPCIdentifier());
        builder.withDefendantHearings(createDefendantHearing());
        builder.withHearings(createHearings());
        builder.withCourtApplications(asList(createCourtApplication()));
        return builder;
    }

    private CourtApplication createCourtApplicationWithApplicationParty() {
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicant(createApplicationParty())
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(createApplicationParty())
                .withType(createCourtApplicationType())
                .withApplicationReceivedDate(LocalDate.now())
                .withRespondents(createCourtApplicationRespondents())
                .build();
    }


    private List<Hearings> createHearings() {
        return asList(
                Hearings.hearings()
                        .withId(HEARING_ID)
                        .withHearingDays(createHearingDays())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendants(asList(DEFENDANT_ID, DEFENDANT_ID_2ND), HEARING1, HEARING_ID))
                        .withDefendantAttendance(createDefendantAttendance(asList(DEFENDANT_ID, DEFENDANT_ID_2ND)))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING1))
                        .withRespondentCounsels(createRespondentCounsels(HEARING1))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING1))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING1))

                        .build(),
                Hearings.hearings()
                        .withId(HEARING_ID_2)
                        .withHearingDays(createHearingDays2())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendants(asList(DEFENDANT_ID), HEARING2, HEARING_ID_2))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING2))
                        .withRespondentCounsels(createRespondentCounsels(HEARING2))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING2))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING2))
                        .build()
        );
    }

    private List<Hearings> createHearingsWithYouthCourtDetails(final List<UUID> defendandIds) {
        return asList(
                Hearings.hearings()
                        .withId(HEARING_ID)
                        .withHearingDays(createHearingDays())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendants(asList(DEFENDANT_ID, DEFENDANT_ID_2ND), HEARING1, HEARING_ID))
                        .withDefendantAttendance(createDefendantAttendance(asList(DEFENDANT_ID, DEFENDANT_ID_2ND)))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING1))
                        .withRespondentCounsels(createRespondentCounsels(HEARING1))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING1))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING1))
                        .withYouthCourtDefendantIds(defendandIds)
                        .withYouthCourt(YouthCourt.youthCourt()
                                .withName("Youth Court Name")
                                .withWelshName("Welsh Youth Court Name")
                                .withCourtCode(2004)
                                .withYouthCourtId(randomUUID())
                                .build())
                        .build(),
                Hearings.hearings()
                        .withId(HEARING_ID_2)
                        .withHearingDays(createHearingDays2())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendants(defendandIds, HEARING2, HEARING_ID_2))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING2))
                        .withRespondentCounsels(createRespondentCounsels(HEARING2))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING2))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING2))
                        .build());
    }

    private List<Hearings> createHearingsWithJudicialResults(final UUID masterDefendantId) {
        return asList(
                Hearings.hearings()
                        .withId(HEARING_ID)
                        .withHearingDays(createHearingDays())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendants(asList(DEFENDANT_ID, DEFENDANT_ID_2ND), HEARING1, HEARING_ID))
                        .withDefendantAttendance(createDefendantAttendance(asList(DEFENDANT_ID, DEFENDANT_ID_2ND)))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING1))
                        .withRespondentCounsels(createRespondentCounsels(HEARING1))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING1))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING1))
                        .withDefendantJudicialResults(createDefendantJudicialResults(masterDefendantId, HEARING_ID))
                        .build(),
                Hearings.hearings()
                        .withId(HEARING_ID_2)
                        .withHearingDays(createHearingDays2())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendants(asList(DEFENDANT_ID, DEFENDANT_ID_2ND), HEARING2, HEARING_ID_2))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING2))
                        .withRespondentCounsels(createRespondentCounsels(HEARING2))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING2))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING2))
                        .withDefendantJudicialResults(createDefendantJudicialResults(masterDefendantId, HEARING_ID_2))
                        .build()
        );
    }

    private List<DefendantAttendance> createDefendantAttendance(final List<UUID> defendantIds) {
        return defendantIds.stream().map(id -> DefendantAttendance.defendantAttendance()
                .withDefendantId(id)
                .withAttendanceDays(asList(AttendanceDay.attendanceDay()
                                .withDay(LocalDate.now())
                                .withAttendanceType(AttendanceType.IN_PERSON)
                                .build(), AttendanceDay.attendanceDay()
                                .withDay(LocalDate.now())
                                .withAttendanceType(AttendanceType.BY_VIDEO)
                                .build(),
                        AttendanceDay.attendanceDay()
                                .withDay(LocalDate.now())
                                .withAttendanceType(AttendanceType.NOT_PRESENT)
                                .build()))
                .build()).collect(Collectors.toList());
    }

    private List<ApplicantCounsel> createApplicationCounsels(final String hearing) {
        return asList(ApplicantCounsel.applicantCounsel()
                .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                .withStatus("Jury")
                .build());
    }

    private List<RespondentCounsel> createRespondentCounsels(final String hearing) {
        return asList(RespondentCounsel.respondentCounsel()
                .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                .withStatus("Solicitor")
                .build());
    }

    private List<ReferralReason> createDefendantReferralReasons() {
        return asList(ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(DEFENDANT_ID)
                .withDescription(DESCRIPTION)
                .build());
    }


    private List<JudicialRole> createJudiciary() {
        return asList(
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(true)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("District Judge")
                                        .build()
                        )
                        .build(),
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(false)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("Magistrate")
                                        .build()
                        )
                        .build(),
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(false)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("Circuit Judge")
                                        .build()
                        )
                        .build()
        );
    }

    private CourtCentre createCourtCenter() {
        return CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName(COURT_NAME)
                .build();
    }



    private List<Hearings> createHearingsWithJudicialResultsWithCourtExtract(final UUID masterDefendantId) {
        return asList(
                Hearings.hearings()
                        .withId(HEARING_ID)
                        .withHearingDays(createHearingDays())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendantsWithCourtExtract(asList(DEFENDANT_ID, DEFENDANT_ID_2ND), HEARING1))
                        .withDefendantAttendance(createDefendantAttendance(asList(DEFENDANT_ID, DEFENDANT_ID_2ND)))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING1))
                        .withRespondentCounsels(createRespondentCounsels(HEARING1))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING1))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING1))
                        .withDefendantJudicialResults(createDefendantJudicialResultsWithCourtExtract(masterDefendantId))
                        .build(),
                Hearings.hearings()
                        .withId(HEARING_ID_2)
                        .withHearingDays(createHearingDays2())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendantsWithCourtExtract(asList(DEFENDANT_ID, DEFENDANT_ID_2ND), HEARING2))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING2))
                        .withRespondentCounsels(createRespondentCounsels(HEARING2))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING2))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING2))
                        .withDefendantJudicialResults(createDefendantJudicialResultsWithCourtExtract(masterDefendantId))
                        .build()
        );
    }

    private List<Offences> createOffenceCourtExtract(final UUID offenceId) {
        final List<JudicialResultPrompt> prompts = asList(createPrompt(NO_COURT_EXTRACT, PROTECTED_PERSON_S_NAME),
                createPrompt(NO_COURT_EXTRACT, PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1),
                createPrompt(COURT_EXTRACT, THIS_ORDER_IS_MADE_ON));

        final List<JudicialResultPrompt> promptsForMinor = asList(createPrompt(NO_COURT_EXTRACT, MINOR_CREDITOR_FIRST_NAME),
                createPrompt(COURT_EXTRACT, AMOUNT_OF_COMPENSATION));

        final List<JudicialResultPrompt> legacyPrompts = asList(createLegacyPrompt(LEGACY_NAME));


        final List<JudicialResult> results = asList(createJudicialResultWithCourtExtractFlag(true, prompts, RESTRAINING_ORDER),
                createJudicialResultWithCourtExtractFlag(true, promptsForMinor, COMPENSATION),
                createJudicialResultWithCourtExtractFlag(false, promptsForMinor, ORDERING),
                createLegacyJudicialResultWithCourtExtractFlag(legacyPrompts, LEGACY_RESULT)
        );
        return asList(
                Offences.offences()
                        .withId(offenceId)
                        .withConvictionDate(CONVICTION_DATE)
                        .withJudicialResults(results)
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withAllocationDecision(createAllocationDecision())
                        .withVerdicts(createVerdicts())
                        .build()
        );
    }



    private List<DefendantJudicialResult> createDefendantJudicialResultsWithCourtExtract(final UUID masterDefendantId) {
        final List<JudicialResultPrompt> prompts = asList(createPrompt(NO_COURT_EXTRACT, PROTECTED_PERSON_S_NAME),
                createPrompt(NO_COURT_EXTRACT, PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1),
                createPrompt(COURT_EXTRACT, THIS_ORDER_IS_MADE_ON));

        final List<JudicialResultPrompt> promptsForMinor = asList(createPrompt(NO_COURT_EXTRACT, MINOR_CREDITOR_FIRST_NAME),
                createPrompt(COURT_EXTRACT, AMOUNT_OF_COMPENSATION));

        final List<JudicialResultPrompt> legacyPrompts = asList(createLegacyPrompt(LEGACY_NAME));

        return asList(
                DefendantJudicialResult.defendantJudicialResult()
                        .withJudicialResult(createJudicialResultWithCourtExtractFlag(true, prompts, RESTRAINING_ORDER_DEFENDANT_LEVEL))
                        .withMasterDefendantId(masterDefendantId)
                        .build(),
                DefendantJudicialResult.defendantJudicialResult()
                        .withJudicialResult(createJudicialResultWithCourtExtractFlag(true, promptsForMinor, COMPENSATION_DEFENDANT_LEVEL))
                        .withMasterDefendantId(masterDefendantId)
                        .build(),
                DefendantJudicialResult.defendantJudicialResult()
                        .withJudicialResult(createLegacyJudicialResultWithCourtExtractFlag(legacyPrompts, LEGACY_COMPENSATION_DEFENDANT_LEVEL))
                        .withMasterDefendantId(masterDefendantId)
                        .build(),
                DefendantJudicialResult.defendantJudicialResult()
                        .withJudicialResult(                createJudicialResultWithCourtExtractFlag(false, promptsForMinor, ORDERING_DEFENDANT_LEVEL))
                        .withMasterDefendantId(masterDefendantId)
                        .build()
        );
    }


    private List<Defendants> createDefendantsWithCourtExtract(final List<UUID> defendantIdList, final String hearing) {
        return defendantIdList.stream().map(id -> createDefendantWithCourtExtract(id, hearing)).collect(toList());
    }

    private Defendants createDefendantWithCourtExtract(final UUID defendantId, final String hearing) {
        final List<JudicialResultPrompt> prompts = asList(createPrompt(NO_COURT_EXTRACT, PROTECTED_PERSON_S_NAME),
                createPrompt(NO_COURT_EXTRACT, PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1),
                createPrompt(COURT_EXTRACT, THIS_ORDER_IS_MADE_ON));

        final List<JudicialResultPrompt> promptsForMinor = asList(createPrompt(NO_COURT_EXTRACT, MINOR_CREDITOR_FIRST_NAME),
                createPrompt(COURT_EXTRACT, AMOUNT_OF_COMPENSATION));

        final List<JudicialResultPrompt> legacyPrompts = asList(createLegacyPrompt(LEGACY_NAME));


        final List<JudicialResult> results = asList(createJudicialResultWithCourtExtractFlag(true, prompts, RESTRAINING_ORDER),
                createJudicialResultWithCourtExtractFlag(true, promptsForMinor, COMPENSATION),
                createJudicialResultWithCourtExtractFlag(false, promptsForMinor, ORDERING),
                createLegacyJudicialResultWithCourtExtractFlag(legacyPrompts, LEGACY_RESULT)
                );
        return Defendants.defendants()
                .withId(defendantId)
                .withAddress(createAddress())
                .withDateOfBirth(DOB)
                .withAge(DEFENDANT_AGE)
                .withJudicialResults(results)
                .withOffences(createOffenceCourtExtract(OFFENCE_ID))
                .withDefenceOrganisation(createDefenceOrganisation(hearing))
                .withCourtApplications(asList(createCourtApplications()))
                .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                        .withCustody(CUSTODY_TYPE)
                        .withId(CUSTODY_ESTABLISHMENT_UUID)
                        .withName(CUSTODY_ESTABLISHMENT_NAME)
                        .build())
                .build();
    }


    private JudicialResult createJudicialResultWithCourtExtractFlag(final boolean isAvailableForCourtExtract, final List<JudicialResultPrompt> prompts, final String resultLabel) {
        return JudicialResult.judicialResult()
                .withIsAvailableForCourtExtract(isAvailableForCourtExtract)
                .withOrderedHearingId(HEARING_ID)
                .withLabel(resultLabel)
                .withJudicialResultPrompts(prompts)
                .withDelegatedPowers(createDelegatedPower())
                .withResultText("resultText")
                .build();
    }

    private JudicialResultPrompt createPrompt(final String courtExtract, final String label) {
        return
                JudicialResultPrompt.judicialResultPrompt()
                        .withLabel(label)
                        .withCourtExtract(courtExtract)
                        .withValue(PROMPT_VALUE)
                        .build();
    }

    private JudicialResult createLegacyJudicialResultWithCourtExtractFlag(final List<JudicialResultPrompt> prompts, final String resultLabel) {
        return JudicialResult.judicialResult()
                .withLabel(resultLabel)
                .withJudicialResultPrompts(prompts)
                .withDelegatedPowers(createDelegatedPower())
                .withResultText("resultText")
                .build();
    }

    private JudicialResultPrompt createLegacyPrompt(final String label) {
        return
                JudicialResultPrompt.judicialResultPrompt()
                        .withLabel(label)
                        .withValue(PROMPT_VALUE)
                        .build();
    }


    private List<Defendants> createDefendants(final List<UUID> defendantIdList, final String hearing, final UUID hearingId) {
        return defendantIdList.stream().map(id -> createDefendant(id, hearing, hearingId)).collect(toList());
    }

    private Defendants createDefendant(final UUID defendantId, final String hearing, final UUID hearingId) {
        return Defendants.defendants()
                .withId(defendantId)
                .withAddress(createAddress())
                .withDateOfBirth(DOB)
                .withAge(DEFENDANT_AGE)
                .withJudicialResults(createResults(hearingId))
                .withOffences(createOffence(OFFENCE_ID, hearingId))
                .withDefenceOrganisation(createDefenceOrganisation(hearing))
                .withCourtApplications(asList(createCourtApplications()))
                .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                        .withCustody(CUSTODY_TYPE)
                        .withId(CUSTODY_ESTABLISHMENT_UUID)
                        .withName(CUSTODY_ESTABLISHMENT_NAME)
                        .build())
                .build();
    }

    private PersonDefendant createPersonDefendant() {
        return PersonDefendant.personDefendant()
                .withPersonDetails(Person.person()
                        .withAddress(createAddress())
                        .withDateOfBirth(DOB)
                        .withFirstName("Harry")
                        .withMiddleName("JackKane")
                        .withLastName("Junior")
                        .build()).
                        withArrestSummonsNumber("Arrest123").build();

    }

    private List<Offences> createOffence(final UUID offenceId, final UUID hearingId) {
        return asList(
                Offences.offences()
                        .withId(offenceId)
                        .withConvictionDate(CONVICTION_DATE)
                        .withJudicialResults(createResults(hearingId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withAllocationDecision(createAllocationDecision())
                        .withVerdicts(createVerdicts())
                        .build()
        );
    }

    private AllocationDecision createAllocationDecision() {
        return AllocationDecision.allocationDecision()
                .withAllocationDecisionDate(CONVICTION_DATE)
                .withMotReasonCode("4")
                .withSequenceNumber(40)
                .withOffenceId(OFFENCE_ID)
                .withOriginatingHearingId(HEARING_ID)
                .withMotReasonId(randomUUID())
                .withMotReasonDescription("Defendant chooses trial by jury")
                .build();
    }

    private IndicatedPlea createIndicatedPlea() {
        return IndicatedPlea.indicatedPlea()
                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                .build();
    }

    private List<DefendantJudicialResult> createDefendantJudicialResults(final UUID masterDefendantId, final UUID hearingId) {
        return asList(
                DefendantJudicialResult.defendantJudicialResult()
                        .withJudicialResult(createJudicialResult(hearingId))
                        .withMasterDefendantId(masterDefendantId)
                        .build()
        );
    }

    private List<JudicialResult> createResults(final UUID hearingId) {
        return asList(
                JudicialResult.judicialResult()
                        .withIsAvailableForCourtExtract(true)
                        .withLabel(LABEL)
                        .withOrderedHearingId(hearingId)
                        .withJudicialResultPrompts(createPrompts())
                        .withDelegatedPowers(createDelegatedPower())
                        .withResultText("resultText")
                        .withResultWording("resultWording")
                        .build()
        );
    }

    private JudicialResult createJudicialResult(final UUID hearingId) {
        return JudicialResult.judicialResult()
                .withIsAvailableForCourtExtract(true)
                .withLabel(LABEL)
                .withJudicialResultPrompts(createPrompts())
                .withOrderedHearingId(hearingId)
                .withDelegatedPowers(createDelegatedPower())
                .withResultText("resultText")
                .build();
    }

    private DelegatedPowers createDelegatedPower() {
        return DelegatedPowers.delegatedPowers()
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .build();
    }

    private List<JudicialResultPrompt> createPrompts() {
        return asList(
                JudicialResultPrompt.judicialResultPrompt()
                        .withLabel(LABEL)
                        .withCourtExtract(COURT_EXTRACT)
                        .withValue(PROMPT_VALUE)
                        .build()
        );
    }

    private Address createAddress() {
        return Address.address()
                .withAddress1(ADDRESS_1)
                .withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3)
                .withPostcode(POST_CODE)
                .build();
    }

    private List<HearingDay> createHearingDays() {
        return asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_3))
                        .build()
        );
    }

    private List<HearingDay> createHearingDays2() {
        return asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_1))
                        .build()
        );
    }

    private List<DefendantHearings> createDefendantHearing() {
        final List<UUID> hearingIds = new ArrayList<>();
        hearingIds.add(HEARING_ID);
        hearingIds.add(HEARING_ID_2);

        return Stream.of(DEFENDANT_ID, DEFENDANT_ID_2ND).map(id -> DefendantHearings.defendantHearings()
                .withDefendantId(id)
                .withHearingIds(hearingIds)
                .withDefendantName(DEFENDANT_NAME)
                .build()).collect(toList());

    }

    private List<DefendantHearings> createDefendantHearingWithOutHearings() {
        final List<DefendantHearings> defendantHearingsList = new ArrayList<>();
        final List<UUID> hearingIds = new ArrayList<>();
        final DefendantHearings defendantHearings = DefendantHearings.defendantHearings()
                .withDefendantId(DEFENDANT_ID)
                .withHearingIds(hearingIds)
                .withDefendantName(DEFENDANT_NAME)
                .build();
        defendantHearingsList.add(defendantHearings);
        return defendantHearingsList;
    }

    private ProsecutionCaseIdentifier createPCIdentifier() {
        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                .withProsecutionAuthorityId(randomUUID())
                .withProsecutionAuthorityReference(PAR)
                .build();
    }

    private List<Verdict> createVerdicts() {
        return asList(Verdict.verdict()
                .withVerdictType(VerdictType.verdictType()
                        .withCategory(GUILTY)
                        .withCategoryType(GUILTY)
                        .withDescription(PLEA_GUILTY_DESCRIPTION)
                        .withSequence(1)
                        .build())
                .withVerdictDate(LocalDate.of(2018, 02, 02))
                .withJurors(Jurors.jurors()
                        .withNumberOfJurors(2)
                        .withNumberOfSplitJurors(2)
                        .withUnanimous(true)
                        .build())
                .withOffenceId(UUID.randomUUID())
                .build());
    }

    private List<Plea> createPlea() {
        return asList(
                Plea.plea()
                        .withDelegatedPowers(DelegatedPowers.delegatedPowers()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .build())
                        .withPleaDate(PLEA_DATE)
                        .withPleaValue(PLEA_GUILTY)
                        .build());
    }

    private DefenceOrganisation createDefenceOrganisation(final String hearing) {
        return
                DefenceOrganisation.defenceOrganisation().withDefenceOrganisation(
                        Organisation.organisation()
                                .withName(ORGANISATION_NAME)
                                .withAddress(createAddress())
                                .withContact(createContact())
                                .build()
                )
                        .withDefenceCounsels(createDefenceCounsels(hearing))
                        .withDefendantId(DEFENDANT_ID)
                        .build();
    }

    private List<DefenceCounsel> createDefenceCounsels(final String hearing) {
        return asList(
                DefenceCounsel.defenceCounsel()
                        .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                        .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                        .withAttendanceDays(asList(LocalDate.now()))
                        .withStatus(COUNSELS_STATUS)
                        .build()
        );
    }

    private List<ProsecutionCounsel> createProsecutionCounsels(final String hearing) {
        return asList(
                ProsecutionCounsel.prosecutionCounsel()
                        .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                        .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                        .withAttendanceDays(asList(LocalDate.now()))
                        .withStatus(COUNSELS_STATUS)
                        .build()
        );
    }

    private List<CompanyRepresentative> createCompanyRepresentatives(final String hearing) {
        return asList(CompanyRepresentative.companyRepresentative()
                .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                .withPosition("DIRECTOR")
                .withAttendanceDays(asList(LocalDate.now()))
                .build());
    }


    private ContactNumber createContact() {
        return ContactNumber.contactNumber()
                .withFax("fax")
                .withHome("home")
                .withMobile("mobile")
                .build();
    }

    private Person createPerson() {
        return Person.person()
                .withDateOfBirth(DOB)
                .withFirstName(FIRST_NAME)
                .withMiddleName("D")
                .withLastName(LAST_NAME)
                .withAddress(createAddress())
                .withTitle("MR")
                .build();
    }

    private CourtApplication createCourtApplication() {
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(createApplicationParty())
                .withType(createCourtApplicationType())
                .withApplicationReceivedDate(LocalDate.now())
                .withRespondents(createCourtApplicationRespondents())
                .build();
    }

    private CourtApplications createCourtApplications() {
        return CourtApplications.courtApplications()
                .withApplicationId(APPLICATION_ID)
                .withApplicationReceivedDate(LocalDate.now())
                .withOutcome("Granted")
                .withOutcomeDate(OUTCOME_DATE)
                .withRespondents(createRespondents())
                .withJudicialResults(createResults(HEARING_ID))
                .build();
    }

    private List<CourtApplicationParty> createCourtApplicationRespondents() {
        return asList(CourtApplicationParty.courtApplicationParty()
                .withId(UUID.randomUUID())
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(UUID.randomUUID())
                        .build())
                .withRepresentationOrganisation(createOrganisation())
                .build());
    }

    private List<Respondents> createRespondents() {
        return asList(Respondents.respondents()
                .withApplicationResponse("Admitted")
                .withResponseDate(RESPONSE_DATE)
                .build());
    }


    private CourtApplicationParty createApplicationParty() {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withRepresentationOrganisation(createOrganisation())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(DEFENDANT_ID)
                        .build())
                .build();
    }

    private CourtApplicationType createCourtApplicationType() {
        return CourtApplicationType.courtApplicationType()
                .withType(APPLICATION_TYPE)
                .withAppealFlag(true)
                //.withApplicantSynonym(SYNONYM)
                //.withRespondentSynonym(SYNONYM + "R")
                .build();
    }

    private Organisation createOrganisation() {
        return Organisation.organisation()
                .withAddress(createAddress())
                .withName(FIRST_NAME)
                .withContact(createContact())
                .build();
    }

    private JsonObject createJudiciaryJsonObject() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add("requestedNameValue", "requestedNameDesc");
        return judiciaryBuilder.build();
    }
}