package uk.gov.justice.api.resource.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.api.resource.service.HearingQueryService.RESULT_LINES;
import static uk.gov.justice.api.resource.service.ReferenceDataService.FIELD_RESULT_DEFINITIONS;
import static uk.gov.justice.api.resource.utils.FileUtil.getPayload;
import static uk.gov.justice.api.resource.utils.FileUtil.jsonFromPath;
import static uk.gov.justice.api.resource.utils.FileUtil.jsonFromString;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.progression.courts.Offences.offences;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.hamcrest.CoreMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import uk.gov.justice.api.resource.dto.DraftResultsWrapper;
import uk.gov.justice.api.resource.dto.ResultDefinition;
import uk.gov.justice.api.resource.dto.ResultLine;
import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.api.resource.service.HearingQueryService;
import uk.gov.justice.api.resource.service.ListingQueryService;
import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.api.resource.utils.payload.PleaValueDescriptionBuilder;
import uk.gov.justice.api.resource.utils.payload.ResultTextFlagBuilder;
import uk.gov.justice.api.resource.utils.payload.PleaValueDescriptionBuilder;
import uk.gov.justice.api.resource.utils.payload.ResultTextFlagBuilder;
import uk.gov.justice.api.resource.service.UsersAndGroupsService;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.ApplicantCounsel;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CompanyRepresentative;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
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
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
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
import uk.gov.justice.progression.courts.exract.ApplicationResults;
import uk.gov.justice.progression.courts.exract.AttendanceDayAndType;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.progression.courts.exract.CrownCourtDecisions;
import uk.gov.justice.progression.courts.exract.DefendantResults;
import uk.gov.justice.progression.courts.exract.JudiciaryNamesByRole;
import uk.gov.justice.progression.courts.exract.Results;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.SeedingHearing;
import uk.gov.moj.cpp.progression.query.api.UserGroupsDetails;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CourtExtractTransformerTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String PROSECUTION_AUTHORITY_CODE = "TFL";
    private static final String GUILTY = "Guilty";
    private static final String PLEA_GUILTY_DESCRIPTION = "plea guilty description";
    private static final String COUNSELS_STATUS = "counsels status";
    private static final String FULL_NAME = "Jack Denial";
    private static final String ORGANISATION_NAME = "Liverpool defence";
    private static final String ASSOCIATED_ORGANISATION_NAME = "Associated Defence Org";
    private static final String DESCRIPTION = "Dodged TFL tickets with passion";
    private static final String APPLICATION_TYPE = "Apil";
    private static final String HEARING1 = "HEARING-1";
    private static final String HEARING2 = "HEARING-2";
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID DEFENDANT_ID_2ND = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID HEARING_ID_2 = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID OFFENCE_ID1 = randomUUID();
    private static final UUID OFFENCE_ID2 = randomUUID();

    private static final LocalDate DOB = LocalDate.of(LocalDate.now().getYear() - 30, 01, 01);

    private static final String HEARING_DATE_1 = "2018-06-01T10:00:00.000Z";
    private static final String HEARING_DATE_2 = "2018-06-04T10:00:00.000Z";
    private static final String HEARING_DATE_3 = "2018-07-01T10:00:00.000Z";

    private static final String CUSTODY_TYPE = "Prison";
    private static final String CUSTODY_ESTABLISHMENT_NAME = "HMP Belmarsh";
    private static final UUID CUSTODY_ESTABLISHMENT_UUID = randomUUID();

    private static final String DEFENDANT_NAME = "Harry JackKane Junior";
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
    private static final UUID USER_ID = randomUUID();
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
    private static final LocalDate APPLICATION_DATE = LocalDate.now();
    private static final String APPLICATION_PARTICULARS = "Application particulars";
    private static final LocalDate ASSOCIATION_START_DATE = LocalDate.now();
    private static final LocalDate ASSOCIATION_END_DATE = ASSOCIATION_START_DATE.plusDays(10);
    private static final UUID SLIP_RULE_AMENDMENT_REASON_ID = UUID.fromString("a02018a1-915c-3343-95ad-abc5f99b339a");
    private static final String REMAND_STATUS = "Custody or remanded into custody";


    private final ProsecutionCase prosecutionCase = createProsecutionCase();
    private CourtExtractTransformer target;

    @InjectMocks
    private TransformationHelper transformationHelper;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private RequestedNameMapper requestedNameMapper;

    @Mock
    private UsersAndGroupsService usersAndGroupsService;

    @Spy
    private CourtExtractHelper courtExtractHelper = new CourtExtractHelper();

    @Mock
    private ListingQueryService listingQueryService;

    @Mock
    private DefenceQueryService defenceQueryService;
    @Mock
    private HearingQueryService hearingQueryService;

    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Mock
    private PleaValueDescriptionBuilder pleaValueDescriptionBuilder;

    @Mock
    private ResultTextFlagBuilder resultTextFlagBuilder;
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());


    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @BeforeEach
    public void init() {
        target = new CourtExtractTransformer();
        setField(this.target, "transformationHelper", transformationHelper);
        setField(this.target, "courtExtractHelper", courtExtractHelper);
        setField(this.target, "listingQueryService", listingQueryService);
        setField(this.target, "referenceDataService", referenceDataService);
        setField(this.target, "defenceQueryService", defenceQueryService);
        setField(this.target, "hearingQueryService", hearingQueryService);
        setField(this.target, "pleaValueDescriptionBuilder", pleaValueDescriptionBuilder);
        setField(this.target, "resultTextFlagBuilder", resultTextFlagBuilder);
        setField(this.target, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
        setField(this.target, "objectToJsonObjectConverter", objectToJsonObjectConverter);
        setField(this.courtExtractHelper, "usersAndGroupsService", usersAndGroupsService);
    }

    @Test
    public void getTransformToRecordSheetPayload_WhenMultipleDefendantsPresent() throws IOException {
        String defendantId = "5c75653a-e264-4b59-a419-66221b725a58";

        final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-multiple-defendants.json"));
        final JsonObject hearingsAtAGlanceJson = prosecutionCasePayload.getJsonObject("hearingsAtAGlance");
        final JsonObject prosecutionCaseJson = prosecutionCasePayload.getJsonObject("prosecutionCase");
        GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
        ProsecutionCase prosecutionCase1 = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance, defendantId, "RecordSheet", emptyList(), randomUUID(), prosecutionCase1);

        final uk.gov.justice.progression.courts.exract.Defendant defendant = courtExtractRequested.getDefendant();

        assertThat(defendant.getHearings().size(), is(1));
        assertThat(defendant.getHearings().get(0).getOffences().size(), is(1));
        assertThat(defendant.getHearings().get(0).getOffences().get(0).getResults().size(), is(1));

    }

    @Test
    public void getTransformToRecordSheetPayload_WhenOneDefendantPresent() throws IOException {
        String defendantId = "e6dc2886-72b6-43c6-a93b-4fae77ee8e41";

        final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-one-defendant.json"));
        final JsonObject hearingsAtAGlanceJson = prosecutionCasePayload.getJsonObject("hearingsAtAGlance");
        final JsonObject prosecutionCaseJson = prosecutionCasePayload.getJsonObject("prosecutionCase");
        GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
        ProsecutionCase prosecutionCase1 = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance, defendantId, "RecordSheet", emptyList(), randomUUID(), prosecutionCase1);

        final uk.gov.justice.progression.courts.exract.Defendant defendant = courtExtractRequested.getDefendant();

        assertThat(defendant.getHearings().size(), is(1));
        assertThat(defendant.getHearings().get(0).getOffences().size(), is(1));
        assertThat(defendant.getHearings().get(0).getOffences().get(0).getResults().size(), is(1));
    }

    @Test
    public void getTransformToRecordSheetPayload_WhenBreachApplicationPresent() throws IOException {
        String defendantId = "d0cea893-2bed-4b80-8e9a-63b5e2c84309";

        final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-with-breach-application.json"));
        final JsonObject hearingsAtAGlanceJson = prosecutionCasePayload.getJsonObject("hearingsAtAGlance");
        final JsonObject prosecutionCaseJson = prosecutionCasePayload.getJsonObject("prosecutionCase");
        GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
        ProsecutionCase prosecutionCase1 = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance, defendantId, "RecordSheet", emptyList(), randomUUID(), prosecutionCase1);

        final uk.gov.justice.progression.courts.exract.Defendant defendant = courtExtractRequested.getDefendant();

        assertThat(defendant.getHearings().size(), is(2));
    }

    @Test
    public void getTransformToRecordSheetPayload_WhenMultipleDefendantsAndHearingsPresent() throws IOException {
        String defendantId = "42df5ca0-0cd6-47a7-b777-ab1a5e822666";

        final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-multiple-defendants-and-hearings.json"));
        final JsonObject hearingsAtAGlanceJson = prosecutionCasePayload.getJsonObject("hearingsAtAGlance");
        final JsonObject prosecutionCaseJson = prosecutionCasePayload.getJsonObject("prosecutionCase");
        GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
        ProsecutionCase prosecutionCase1 = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance, defendantId, "RecordSheet", emptyList(), randomUUID(), prosecutionCase1);

        final uk.gov.justice.progression.courts.exract.Defendant defendant = courtExtractRequested.getDefendant();

        assertThat(defendant.getHearings().size(), is(2));
        assertThat(defendant.getHearings().get(0).getOffences().size(), is(1));
        assertThat(defendant.getHearings().get(0).getOffences().get(0).getResults().size(), is(1));
        assertThat(defendant.getHearings().get(1).getOffences().size(), is(1));
        assertThat(defendant.getHearings().get(1).getOffences().get(0).getResults().size(), is(1));
    }

    @Test
    public void testTransformToCourtExtract_shouldUseDefendantAssociatedDefenceOrganisation_whenPresent() {
        when(requestedNameMapper.getRequestedJudgeName(argThat(Matchers.any(JsonObject.class)))).thenReturn("Denial", "Aladin", "Amit");
        when(referenceDataService.getJudiciary(argThat(Matchers.any(UUID.class)))).thenReturn(Optional.ofNullable(createJudiciaryJsonObject()));
        when(referenceDataService.getProsecutor(argThat(Matchers.any(JsonEnvelope.class)), argThat(Matchers.any(ProsecutionCaseIdentifier.class)))).thenReturn(new uk.gov.justice.progression.courts.exract.ProsecutingAuthority(null, null, null));

        final String extractType = "CrownCourtExtract";
        final GetHearingsAtAGlance hearingsAtAGlance = createHearingsAtGlance();

        final List<String> selectedHearingIds = asList(HEARING_ID.toString(), HEARING_ID_2.toString());
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);
        final uk.gov.justice.progression.courts.exract.Defendant defendant = courtExtractRequested.getDefendant();

        final List<AttendanceDayAndType> attendanceDays = defendant.getAttendanceDays();
        final List<AttendanceDayAndType> hearingLevelAttendanceDays = defendant.getHearings().get(0).getAttendanceDays();
        assertAttendanceDays(attendanceDays);
        assertAttendanceDays(hearingLevelAttendanceDays);

        assertValues(courtExtractRequested, extractType, HEARING_DATE_1, HEARING_DATE_2, 2, "resultWording", "Fine");
        final uk.gov.justice.progression.courts.exract.Hearings hearing1 = defendant.getHearings().get(0);
        assertThat(courtExtractRequested.getCompanyRepresentatives().size(), is(2));
        assertThat(hearing1.getCourtApplications().size(), is((1)));
        assertThat(hearing1.getProsecutionCounsels().size(), is(1));
        assertThat(hearing1.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(2));
        assertThat(hearing1.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(1));
        assertThat(hearing1.getDefenceCounsels().size(), is(1));
        assertThat(defendant.getHearings().get(1).getOffences().size(), is(1));
        assertThat(defendant.getHearings().get(1).getOffences().get(0).getResults().size(), is(1));

        final CourtExtractRequested courtExtract2ndDefendant = target.getCourtExtractRequested(hearingsAtAGlance, DEFENDANT_ID_2ND.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);
        final List<JudicialResult> defendant2ndResults = courtExtract2ndDefendant.getDefendant().getHearings().stream().flatMap(h -> h.getDefendantResults().stream().map(DefendantResults::getResult)).toList();
        assertThat(defendant2ndResults.size(), is(2));
    }

    private static void assertAttendanceDays(final List<AttendanceDayAndType> attendanceDays) {
        assertThat(attendanceDays.size(), is((2)));
        attendanceDays.forEach(ad -> anyOf(is(CourtExtractTransformer.PRESENT_BY_POLICE_VIDEO_LINK),
                is(CourtExtractTransformer.PRESENT_BY_PRISON_VIDEO_LINK),
                is(CourtExtractTransformer.PRESENT_BY_VIDEO_DEFAULT),
                is(CourtExtractTransformer.PRESENT_IN_PERSON)));
    }

    @Test
    public void testTransformToCourtExtract_shouldUseSelectedHearing_whenExtractTypeIsCrownCourtExtractAndOneHearingsSelected() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(createHearingsAtGlance(), DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getId().toString(), is((selectedHearingIds.get(0))));
        assertThat(courtExtractRequested.getCompanyRepresentatives().size(), is(1));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getProsecutionCounsels().size(), is(1));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefenceCounsels().size(), is(1));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(2));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(1));
    }

    @Test
    public void testTransformToCourtExtract_whenExtractTypeIsCrownCourtWithAPendingApplication_expectAppealPendingFlagAsTrue() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(createCaseAtAGlanceWithCourtApplicationParty(), DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getId().toString(), is((selectedHearingIds.get(0))));
        assertThat(courtExtractRequested.getIsAppealPending(), is(true));
    }

    @Test
    public void testTransformToCourtExtract_whenInActiveCaseWithBreachTypeApplication_expectCourtOrderAndClonedOffences() {
        final String extractType = "CrownCourtExtract";
        final UUID breachApplicationHearing = randomUUID();
        final List<String> selectedHearingIds = asList(HEARING_ID.toString(), breachApplicationHearing.toString());
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithBreachTypeApplication(DEFENDANT_ID.toString(), HEARING_ID.toString(), breachApplicationHearing.toString());
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((2)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().isEmpty(), is((true)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getId().toString(), is((selectedHearingIds.get(0))));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(1).getCourtApplications().size(), is((1)));

        final uk.gov.justice.progression.courts.exract.CourtApplications breachApplication = courtExtractRequested.getDefendant().getHearings().get(1).getCourtApplications().get(0);
        assertThat(breachApplication.getCourtOrders().getCanBeSubjectOfBreachProceedings(), is((true)));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().size(), is((1)));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getOffenceCode(), is(("CJ03522")));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getOffenceTitle(), is(("Possess / control TV set with intent another use install without a licence")));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getWording(), is(("Original CaseURN: 28DI8505400, Re-sentenced Original code : CA03012, Original details: Micaela Marks have set up a TV cable without a valid license.")));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getResultTextList().size(), is((2)));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getPlea().getPleaValue(), is(("GUILTY")));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getIndicatedPlea().getIndicatedPleaValue().name(), is(("INDICATED_GUILTY")));
        assertThat(breachApplication.getConvictionDate().toString(), is("2025-05-21"));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(1).getId().toString(), is((selectedHearingIds.get(1))));
        assertThat(courtExtractRequested.getIsAppealPending(), is(false));
    }

    @Test
    public void testTransformToCourtExtract_whenSJPCaseReferredToCC_ensureReferralReasonIsAlwaysPopulatedInTheExtract_whenHearingHasSubsequentHearings() {
        final String extractType = "CrownCourtExtract";
        final String subsequentHearingId = "dc50698d-f7f1-45a8-9ed9-9ea25d355881";
        final List<String> selectedHearingIds = List.of(subsequentHearingId);
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithSJPCaseReferredToCC(DEFENDANT_ID.toString());
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        final String referralReason = courtExtractRequested.getReferralReason();
        assertThat(referralReason, is(("For disqualification (Defendant to attend - special reasons)")));
    }

    @Test
    public void testTransformToCourtExtract_whenLegalAdvisorResultedOffenceWithDelegatedPowers_expectHearingHasAuthorisedLegalAdvisors() {
        final String extractType = "CrownCourtExtract";
        final UUID breachApplicationHearing = randomUUID();
        final List<String> selectedHearingIds = asList(HEARING_ID.toString(), breachApplicationHearing.toString());
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithBreachTypeApplication(DEFENDANT_ID.toString(), HEARING_ID.toString(), breachApplicationHearing.toString());
        when(usersAndGroupsService.getUserGroups(any())).thenReturn(List.of(new UserGroupsDetails(randomUUID(), "Legal Advisers")));

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((2)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getAuthorisedLegalAdvisors().get(0).getFirstName(), is(("Erica")));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getAuthorisedLegalAdvisors().get(0).getLastName(), is(("Wilson")));
    }

    @Test
    public void testTransformToCourtExtract_whenResultAmendedWithSlipRule() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithSlipRuleAmendment(DEFENDANT_ID.toString(), HEARING_ID.toString(), "progression.query.prosecutioncase-result-sliprule-amended.json");
        final UUID applicationId = hearingAtAGlance.getCourtApplications().get(0).getId();
        final List<ResultLine> defendantResultlines = getMockResultlines(DEFENDANT_ID, applicationId, "hearing.query.hearing.get-draft-result-v2.json");
        final LocalDate hearingDay = LocalDate.parse("2025-02-13");
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(HEARING_ID, hearingDay, defendantResultlines, null);


        when(hearingQueryService.getDraftResultsWithAmendments(any(), any(), any())).thenReturn(List.of(draftResultsWrapper));
        when(referenceDataService.getAmendmentReasonId(any(), any())).thenReturn(SLIP_RULE_AMENDMENT_REASON_ID);
        when(referenceDataService.getResultDefinitionsByIds(any(), any())).thenReturn(getResultDefinitions("referencedata.query.referencedata.query-result-definitions-by-ids.json"));

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getResults().size(), is((3)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getResults().get(0).getAmendments().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getResults().get(0).getAmendments().get(0).getDefendantId(), is((DEFENDANT_ID)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getResults().get(0).getAmendments().get(0).getResultText(), is(("Community order England / Wales\nEnd Date 2025-05-15\nResponsible officer a probation officer")));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getResults().get(0).getAmendments().get(0).getAmendmentType(), is(("AMENDED")));
    }

    @Test
    public void testTransformToCourtExtract_whenDefendantOffencesWithCountAndOrderIndex() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithSlipRuleAmendment(DEFENDANT_ID.toString(), HEARING_ID.toString(), "hearing-results/progression.query.prosecutioncase-defendant-offence-count.json");

        when(hearingQueryService.getDraftResultsWithAmendments(any(), any(), any())).thenReturn(emptyList());
        when(referenceDataService.getAmendmentReasonId(any(), any())).thenReturn(SLIP_RULE_AMENDMENT_REASON_ID);

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().size(), is((3)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getCount(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(1).getCount(), is((2)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(2).getCount(), is((0)));
    }

    @Test
    public void testTransformToCourtExtract_whenResultDeletedWithSlipRule() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithSlipRuleAmendment(DEFENDANT_ID.toString(), HEARING_ID.toString(), "progression.query.prosecutioncase-result-sliprule-amended-deleted.json");
        final UUID applicationId = randomUUID();
        final List<ResultLine> defendantResultlines = getMockResultlines(DEFENDANT_ID, applicationId, "hearing.query.hearing.get-draft-result-v2-deleted.json");
        final LocalDate hearingDay = LocalDate.parse("2025-02-28");
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(HEARING_ID, hearingDay, defendantResultlines, null);

        when(hearingQueryService.getDraftResultsWithAmendments(any(), any(), any())).thenReturn(List.of(draftResultsWrapper));
        when(referenceDataService.getAmendmentReasonId(any(), any())).thenReturn(SLIP_RULE_AMENDMENT_REASON_ID);
        when(referenceDataService.getResultDefinitionsByIds(any(), any())).thenReturn(getResultDefinitions("referencedata.query.referencedata.query-result-definitions-by-ids.json"));

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getResults().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getDeletedResults().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getDeletedResults().get(0).getDefendantId(), is((DEFENDANT_ID)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getDeletedResults().get(0).getResultText(), is(("Absolute discharge")));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0).getDeletedResults().get(0).getAmendmentType(), is(("DELETED")));
    }

    @Test
    public void testTransformToCourtExtract_whenResultsAmendedPromptValueWithSlipRule() {

        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithSlipRuleAmendment(DEFENDANT_ID.toString(), HEARING_ID.toString(), "progression.query.prosecutioncase-result-sliprule-prompt-amended.json");
        final UUID applicationId = randomUUID();
        final List<ResultLine> defendantResultlines = getMockResultlines(DEFENDANT_ID, applicationId, "hearing-results/hearing.query.hearing.get-draft-result-v2-amended.json");
        final LocalDate hearingDay = LocalDate.parse("2025-02-28");
        final ZonedDateTime lastSharedTime = ZonedDateTime.parse("2025-03-12T11:02:34.678Z");
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(HEARING_ID, hearingDay, defendantResultlines, lastSharedTime);

        when(hearingQueryService.getDraftResultsWithAmendments(any(), any(), any())).thenReturn(List.of(draftResultsWrapper));
        when(referenceDataService.getAmendmentReasonId(any(), any())).thenReturn(SLIP_RULE_AMENDMENT_REASON_ID);
        when(referenceDataService.getResultDefinitionsByIds(any(), any())).thenReturn(getResultDefinitions("hearing-results/referencedata.query.referencedata.query-result-definitions-by-ids-multiple.json"));

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((1)));
        final uk.gov.justice.progression.courts.exract.Offences offences2 = courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(1);
        assertThat(offences2.getResults().size(), is((1)));
        assertThat(offences2.getResults().get(0).getAmendments().size(), is((1)));
        assertThat(offences2.getResults().get(0).getAmendments().get(0).getAmendmentType(), is(("AMENDED")));
        assertThat(offences2.getResults().get(0).getAmendments().get(0).getResultText(), is(("Conditional discharge\nPeriod of conditional discharge 12 Months")));
    }

    @Test
    public void testTransformToCourtExtract_whenDefendantResultsAmendedWithSlipRule() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithDefendantAndCourtApplicationSlipRuleAmendments(DEFENDANT_ID.toString(), MASTER_DEFENDANT_ID.toString(), HEARING_ID.toString());
        final UUID applicationId = hearingAtAGlance.getCourtApplications().get(0).getId();

        final List<ResultLine> defendantResultlines = getMockResultlines(DEFENDANT_ID, applicationId, "hearing-results/hearing.query.hearing.get-defendant-draft-result-v2.json");
        final LocalDate hearingDay = LocalDate.parse("2025-02-24");
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(HEARING_ID, hearingDay, defendantResultlines, null);

        when(hearingQueryService.getDraftResultsWithAmendments(any(), any(), any())).thenReturn(List.of(draftResultsWrapper));
        when(referenceDataService.getAmendmentReasonId(any(), any())).thenReturn(SLIP_RULE_AMENDMENT_REASON_ID);
        when(referenceDataService.getResultDefinitionsByIds(any(), any())).thenReturn(getResultDefinitions("referencedata.query.referencedata.query-result-definitions-by-ids.json"));

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        final uk.gov.justice.progression.courts.exract.Hearings hearings = courtExtractRequested.getDefendant().getHearings().get(0);
        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((1)));
        assertThat(hearings.getDefendantResults().size(), is((4)));
        assertThat(hearings.getDefendantResults().stream().filter(dr -> nonNull(dr.getAmendments()))
                .allMatch(dr -> dr.getAmendments().get(0).getDefendantId().equals(DEFENDANT_ID)), is(true));

        assertThat(hearings.getDefendantResults().get(0).getAmendments().size(), is((1)));
        assertThat(hearings.getDefendantResults().get(0).getAmendments().get(0).getResultText(), is(("Fined and detained in default of payment until court rises\nType of detention . Detention deemed served by reason of time already spent in custody\nTotal amount enforced 1500.00")));
        assertThat(hearings.getDefendantResults().get(0).getAmendments().get(0).getAmendmentType(), is(("ADDED")));

        assertThat(hearings.getDefendantResults().get(1).getAmendments().size(), is((1)));
        assertThat(hearings.getDefendantResults().get(1).getAmendments().get(0).getResultText(), is(("Collection order\nCollection order type Make payments as ordered")));
        assertThat(hearings.getDefendantResults().get(1).getAmendments().get(0).getAmendmentType(), is(("ADDED")));

        assertThat(hearings.getDefendantResults().get(3).getAmendments().size(), is((1)));
        assertThat(hearings.getDefendantResults().get(3).getAmendments().get(0).getResultText(), is(("Reserve Terms Instalments only\nPayment frequency weekly")));
        assertThat(hearings.getDefendantResults().get(3).getAmendments().get(0).getAmendmentType(), is(("AMENDED")));
    }

    @Test
    public void testTransformToCourtExtract_whenCourtApplicationResultsAmendedWithSlipRule() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithDefendantAndCourtApplicationSlipRuleAmendments(DEFENDANT_ID.toString(), MASTER_DEFENDANT_ID.toString(), HEARING_ID.toString());

        final UUID applicationId = hearingAtAGlance.getCourtApplications().get(0).getId();
        final List<ResultLine> mockResultlines = getMockResultlines(DEFENDANT_ID, applicationId, "hearing-results/hearing.query.hearing.get-defendant-draft-result-v2.json");
        final LocalDate hearingDay = LocalDate.parse("2025-02-24");
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(HEARING_ID, hearingDay, mockResultlines, null);

        when(hearingQueryService.getDraftResultsWithAmendments(any(), any(), any())).thenReturn(List.of(draftResultsWrapper));
        when(referenceDataService.getAmendmentReasonId(any(), any())).thenReturn(SLIP_RULE_AMENDMENT_REASON_ID);
        when(referenceDataService.getResultDefinitionsByIds(any(), any())).thenReturn(getResultDefinitions("referencedata.query.referencedata.query-result-definitions-by-ids.json"));

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        final uk.gov.justice.progression.courts.exract.Hearings hearings = courtExtractRequested.getDefendant().getHearings().get(0);
        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((1)));
        assertThat(hearings.getCourtApplications().size(), is((1)));

        final List<ApplicationResults> applicationResults = hearings.getCourtApplications().get(0).getApplicationResults();
        assertThat(applicationResults.size(), is(2));

        assertThat(applicationResults.get(0).getAmendments().size(), is((1)));
        assertThat(applicationResults.get(0).getAmendments().get(0).getResultText(), is(("Reserve Terms Instalments only\nPayment frequency weekly\nInstalment start date 2025-04-01")));
        assertThat(applicationResults.get(0).getAmendments().get(0).getAmendmentType(), is(("AMENDED")));

        assertThat(applicationResults.get(1).getAmendments().size(), is((1)));
        assertThat(applicationResults.get(1).getAmendments().get(0).getResultText(), is(("Surcharge\nAmount of surcharge 120.00")));
        assertThat(applicationResults.get(1).getAmendments().get(0).getAmendmentType(), is(("ADDED")));
    }

    private List<ResultDefinition> getResultDefinitions(final String pathToJson) {
        final JsonObject resultDefinitions = jsonFromPath(pathToJson);

        return resultDefinitions.getJsonArray(FIELD_RESULT_DEFINITIONS).stream()
                .map(respPayload -> jsonObjectToObjectConverter.convert(respPayload.asJsonObject(), ResultDefinition.class)).collect(toList());
    }

    private List<ResultLine> getMockResultlines(final UUID defendantId, final UUID applicationId, final String pathToPayload) {
        final JsonObject draftResultsPayload = jsonFromString(getPayload(pathToPayload)
                .replaceAll("DEFENDANT_ID", defendantId.toString())
                .replaceAll("APPLICATION_ID", applicationId.toString())
        );

        return draftResultsPayload.getJsonObject(RESULT_LINES).entrySet().stream()
                .map(entry -> jsonObjectToObjectConverter.convert(entry.getValue().asJsonObject(), ResultLine.class))
//                .filter(resultLine -> defendantId.equals(resultLine.getDefendantId()))
                .filter(resultLine -> nonNull(resultLine.getAmendmentsLog()) && isNotEmpty(resultLine.getAmendmentsLog().getAmendmentsRecord()))
                .collect(Collectors.toList());
    }

    @Test
    public void testTransformToCourtExtract_whenInitiallyResultedInMagistrateCourtWithCommittedForSentenceAndLaterResultedInCrownCourt() {
        final String extractType = "CrownCourtExtract";
        final UUID seedingHearingId = randomUUID();
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithMagistrateAndCrownCourtHearings(DEFENDANT_ID.toString(), HEARING_ID.toString(), seedingHearingId.toString());
        final UUID offenceId = hearingAtAGlance.getHearings().get(0).getDefendants().get(0).getOffences().get(0).getId();
        when(listingQueryService.searchHearing(any(), eq(HEARING_ID))).thenReturn(getHearingFromListing(HEARING_ID, seedingHearingId, DEFENDANT_ID, offenceId));
        when(referenceDataService.getResultDefinitionsByIds(any(), any())).thenReturn(singletonList(ResultDefinition.builder()
                .withId(fromString("3132bf21-692c-497a-9de1-56c9e137bab8")).withCategory("I").withResultDefinitionGroup("CommittedToCC").build()));

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getOffences().size(), is((1)));
        final uk.gov.justice.progression.courts.exract.Offences ofcPreviouslyCommittedForSentence = courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0);
        assertThat(ofcPreviouslyCommittedForSentence.getCommittedForSentence().getCourtName(), is(("Lavender Hill Magistrates' Court")));
        assertThat(ofcPreviouslyCommittedForSentence.getCommittedForSentence().getResultTextList().size(), is(1));
        assertThat(ofcPreviouslyCommittedForSentence.getCommittedForSentence().getSittingDay(), notNullValue());
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getId().toString(), is((selectedHearingIds.get(0))));
    }

    @Test
    public void testTransformToCourtExtract_shouldIncludeAdditionalAttributes() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        when(defenceQueryService.getAllAssociatedOrganisations(any(), eq(DEFENDANT_ID_2ND.toString()))).thenReturn(List.of(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withAssociationStartDate(ASSOCIATION_START_DATE)
                .withAssociationEndDate(ASSOCIATION_END_DATE)
                .withDefenceOrganisation(uk.gov.justice.core.courts.DefenceOrganisation.defenceOrganisation()
                        .withOrganisation(Organisation.organisation().withId(randomUUID())
                                .withName(ASSOCIATED_ORGANISATION_NAME)
                                .build()).build())
                .build()));

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(createHearingsAtGlance(), DEFENDANT_ID_2ND.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);

        final List<uk.gov.justice.progression.courts.exract.Offences> offenceList = courtExtractRequested.getDefendant().getHearings().get(0).getOffences();

        assertThat(offenceList.stream().allMatch(o -> nonNull(o.getOrderIndex())), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getApplicationParticulars(), is(APPLICATION_PARTICULARS));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getApplicationDate(), is(APPLICATION_DATE));
        assertThat(courtExtractRequested.getDefendant().getLegalAidStatus(), is("legal aid"));

        final AssociatedDefenceOrganisation associatedDefenceOrganisation = courtExtractRequested.getDefendant().getAssociatedDefenceOrganisations().get(0);
        assertThat(associatedDefenceOrganisation.getDefenceOrganisation().getOrganisation().getName(), is(ASSOCIATED_ORGANISATION_NAME));
        assertThat(associatedDefenceOrganisation.getAssociationStartDate(), is(ASSOCIATION_START_DATE));
        assertThat(associatedDefenceOrganisation.getAssociationEndDate(), is(ASSOCIATION_END_DATE));
    }

    @Test
    public void shouldRetrieveMasterDefendantBasedJudicialResultsWhenGetCourtExtractRequestedInvoked() {
        when(requestedNameMapper.getRequestedJudgeName(argThat(Matchers.any(JsonObject.class)))).thenReturn("Denial");
        when(referenceDataService.getJudiciary(argThat(Matchers.any(UUID.class)))).thenReturn(Optional.ofNullable(createJudiciaryJsonObject()));
        when(referenceDataService.getProsecutor(argThat(Matchers.any(JsonEnvelope.class)), argThat(Matchers.any(ProsecutionCaseIdentifier.class)))).thenReturn(new uk.gov.justice.progression.courts.exract.ProsecutingAuthority(null, null, null));
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
        final GetHearingsAtAGlance hearingsAtAGlance = createHearingsAtGlance();
        when(referenceDataService.getProsecutor(argThat(Matchers.any(JsonEnvelope.class)), argThat(Matchers.any(ProsecutionCaseIdentifier.class)))).thenReturn(new uk.gov.justice.progression.courts.exract.ProsecutingAuthority(null, null, null));

        final CourtExtractRequested courtExtractRequested = target.ejectCase(prosecutionCase, hearingsAtAGlance, DEFENDANT_ID.toString(), randomUUID());

        //then
        assertValues(courtExtractRequested, extractType, null, null, 0, null, null);
        assertThat(courtExtractRequested.getIsAppealPending(), is((true)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(2));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(1));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getApplicationResults().size(), is(1));
    }

    @Test
    public void testEjectCaseWithUnResultedCase_ForLegalEntity() {
        final String extractType = "CrownCourtExtract";
        final GetHearingsAtAGlance hearingsAtAGlance = createHearingsAtGlance();
        when(referenceDataService.getProsecutor(argThat(Matchers.any(JsonEnvelope.class)), argThat(Matchers.any(ProsecutionCaseIdentifier.class)))).thenReturn(new uk.gov.justice.progression.courts.exract.ProsecutingAuthority(null, null, null));
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
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(2));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(1));
    }

    @Test
    public void testEjectCase_shouldExtractAllResultedAndFutureHearings() {
        final String extractType = "CrownCourtExtract";
        final GetHearingsAtAGlance hearingsAtAGlance = createHearingsAtGlance();
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
        assertThat(courtExtractRequested.getPublishingCourt().getName(), is("liver pool"));
        assertThat(courtExtractRequested.getPublishingCourt().getWelshName(), is("Welsh liver pool"));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtCentre().getName(), is("Youth Court Name"));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtCentre().getWelshName(), is("Welsh Youth Court Name"));
    }


    @Test
    public void shouldGenerateACourtExtractAfterFilteringOutResultDefinitionsThatAreNotForTheCourtExtract() {
        final String extractType = "CrownCourtExtract";
        final List<String> selectedHearingIds = singletonList(HEARING_ID.toString());
        final List<Hearings> judicialResultsByMasterDefendantId = createHearingsWithJudicialResultsWithCourtExtract(MASTER_DEFENDANT_ID);

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(createCaseAtAGlance(judicialResultsByMasterDefendantId), DEFENDANT_ID.toString(), extractType, selectedHearingIds, randomUUID(), prosecutionCase);
        verifyDefendantLevelResults(courtExtractRequested);
        verifyDefendantHearingsLevelResults(courtExtractRequested);

        verifyCaseLevelResult(courtExtractRequested);
        verifyCaseHearingsLevelResult(courtExtractRequested);

        verifyOffenceLevelResult(courtExtractRequested);
    }

    @Test
    public void shouldSortOffencesByOffenceOrderIndex_whenJurisdictionMagistrate() {
        final UUID o1 = randomUUID();
        final UUID o2 = randomUUID();
        final List<Offences> offences = List.of(offences().withId(o1).withConvictionDate(CONVICTION_DATE.minusDays(2)).withCount(3).withOrderIndex(5).build(), offences().withId(o2).withConvictionDate(CONVICTION_DATE.plusDays(3)).withOrderIndex(2).build());

        final List<uk.gov.justice.progression.courts.exract.Offences> offencesMags = target.transformOffence(offences, randomUUID(), Map.of(), MAGISTRATES, emptyMap());
        assertThat(offencesMags.get(0).getId(), is(o2));
        assertThat(offencesMags.get(1).getId(), is(o1));
    }

    @Test
    public void shouldSortOffencesByOffenceOrderIndex_whenJurisdictionCrown_andOffenceCountNull() {
        final UUID o1 = randomUUID();
        final UUID o2 = randomUUID();
        final UUID o3 = randomUUID();
        final List<Offences> offences = List.of(offences().withId(o1).withCount(3).withOrderIndex(5).withOffenceTitle("o1").build(),
                offences().withId(o2).withOrderIndex(2).withOffenceTitle("o2").build(),
                offences().withId(o2).withCount(2).withOrderIndex(4).withOffenceTitle("o4").build(),
                offences().withId(o3).withCount(0).withOrderIndex(3).withOffenceTitle("o3").build());

        final List<uk.gov.justice.progression.courts.exract.Offences> offencesMags = target.transformOffence(offences, randomUUID(), Map.of(), CROWN, emptyMap());
        assertThat(offencesMags.get(0).getOffenceTitle(), is("o4"));
        assertThat(offencesMags.get(1).getOffenceTitle(), is("o1"));
        assertThat(offencesMags.get(2).getOffenceTitle(), is("o2"));
        assertThat(offencesMags.get(3).getOffenceTitle(), is("o3"));
    }

    @Test
    public void shouldSortOffencesByOffenceOrderIndex_whenJurisdictionCrown_andOffenceCountNotNull() {
        final UUID o1 = randomUUID();
        final UUID o2 = randomUUID();
        final UUID o3 = randomUUID();
        final UUID o4 = randomUUID();
        final List<Offences> offences = List.of(offences().withId(o1).withCount(3).withOrderIndex(5).build(),
                offences().withId(o2).withCount(1).withOrderIndex(2).build(),
                offences().withId(o3).withCount(5).withOrderIndex(3).build(),
                offences().withId(o4).withCount(5).withOrderIndex(8).build()
        );

        final List<uk.gov.justice.progression.courts.exract.Offences> offencesMags = target.transformOffence(offences, randomUUID(), Map.of(), CROWN, emptyMap());
        assertThat(offencesMags.get(0).getId(), is(o2));
        assertThat(offencesMags.get(1).getId(), is(o1));
        assertThat(offencesMags.get(2).getId(), is(o3));
        assertThat(offencesMags.get(3).getId(), is(o4));
    }

    @Test
    public void shouldSortOffencesByOffenceCountAndThenOrderIndex_whenJurisdictionCrown() {
        final UUID o1 = randomUUID();
        final UUID o2 = randomUUID();
        final UUID o3 = randomUUID();
        final UUID o4 = randomUUID();
        final List<Offences> offences = List.of(offences().withId(o1).withCount(3).withOrderIndex(3).withOffenceTitle("o1").build(),
                offences().withId(o2).withCount(1).withOrderIndex(1).withOffenceTitle("o2").build(),
                offences().withId(o3).withCount(5).withOrderIndex(5).withOffenceTitle("o3").build(),
                offences().withId(o4).withOrderIndex(4).withOffenceTitle("o4").build()
        );

        final List<uk.gov.justice.progression.courts.exract.Offences> offencesMags = target.transformOffence(offences, randomUUID(), Map.of(), CROWN, emptyMap());
        assertThat(offencesMags.get(0).getOffenceTitle(), is("o2"));
        assertThat(offencesMags.get(1).getOffenceTitle(), is("o1"));
        assertThat(offencesMags.get(2).getOffenceTitle(), is("o3"));
        assertThat(offencesMags.get(3).getOffenceTitle(), is("o4"));
    }

    private void verifyOffenceLevelResult(final CourtExtractRequested courtExtractRequested) {
        //Case Level Does Not Exist
        final uk.gov.justice.progression.courts.exract.Offences offenceOne = courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0);
        assertThat(offenceOne.getResults().stream().noneMatch(r -> r.getResult().getLabel().equals(LEGACY_COMPENSATION)), is(true));
        assertThat(offenceOne.getResults().stream().noneMatch(r -> r.getResult().getLabel().equals(ORDERING)), is(true));

        //Defendant Level Exist
        assertThat(offenceOne.getResults().stream().anyMatch(r -> r.getResult().getLabel().equals(COMPENSATION)), is(true));
        assertThat(offenceOne.getResults().stream().anyMatch(r -> r.getResult().getLabel().equals(RESTRAINING_ORDER)), is(true));


        assertThat(offenceOne.getResults().stream()
                .map(Results::getResult)
                .filter(r -> r.getLabel().equals(COMPENSATION))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(AMOUNT_OF_COMPENSATION))), is(true));
        assertThat(offenceOne.getResults().stream()
                .map(Results::getResult)
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(THIS_ORDER_IS_MADE_ON))), is(true));

        // Prompt Should not exist
        assertThat(offenceOne.getResults().stream()
                .map(Results::getResult)
                .filter(r -> r.getLabel().equals(COMPENSATION))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(MINOR_CREDITOR_FIRST_NAME))), is(true));
        assertThat(offenceOne.getResults().stream()
                .map(Results::getResult)
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_NAME))), is(true));
        assertThat(offenceOne.getResults()
                .stream().map(Results::getResult)
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1))), is(true));
    }

    private void verifyCaseLevelResult(final CourtExtractRequested courtExtractRequested) {
        //Case Level Does Not Exist
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream().noneMatch(r -> r.getResult().getLabel().equals(LEGACY_COMPENSATION)), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream().noneMatch(r -> r.getResult().getLabel().equals(ORDERING)), is(true));

        //Defendant Level Exist
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream().anyMatch(r -> r.getResult().getLabel().equals(COMPENSATION)), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream().anyMatch(r -> r.getResult().getLabel().equals(RESTRAINING_ORDER)), is(true));


        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(result -> result.getLabel().equals(COMPENSATION))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(AMOUNT_OF_COMPENSATION))), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(THIS_ORDER_IS_MADE_ON))), is(true));

        // Prompt Should not exist
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(r -> r.getLabel().equals(COMPENSATION))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(MINOR_CREDITOR_FIRST_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1))), is(true));
    }

    private void verifyCaseHearingsLevelResult(final CourtExtractRequested courtExtractRequested) {
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

    private void verifyDefendantHearingsLevelResults(final CourtExtractRequested courtExtractRequested) {
        //Defendant Level Not Exist
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream().noneMatch(r -> r.getResult().getLabel().equals(LEGACY_COMPENSATION_DEFENDANT_LEVEL)), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream().noneMatch(r -> r.getResult().getLabel().equals(ORDERING_DEFENDANT_LEVEL)), is(true));

        //Defendant Level Exist
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream().anyMatch(r -> r.getResult().getLabel().equals(COMPENSATION_DEFENDANT_LEVEL)), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream().anyMatch(r -> r.getResult().getLabel().equals(RESTRAINING_ORDER_DEFENDANT_LEVEL)), is(true));

        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(r -> r.getLabel().equals(COMPENSATION_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(AMOUNT_OF_COMPENSATION))), is(true));

        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().anyMatch(prompt -> prompt.getLabel().equals(THIS_ORDER_IS_MADE_ON))), is(true));

        // Prompt Should not exist
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(r -> r.getLabel().equals(COMPENSATION_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(MINOR_CREDITOR_FIRST_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_NAME))), is(true));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().stream()
                .map(DefendantResults::getResult)
                .filter(r -> r.getLabel().equals(RESTRAINING_ORDER_DEFENDANT_LEVEL))
                .map(JudicialResult::getJudicialResultPrompts).anyMatch(p -> p.stream().noneMatch(prompt -> prompt.getLabel().equals(PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1))), is(true));
    }

    private void assertGetCourtExtractRequested(final CourtExtractRequested courtExtractRequested, String extractType, int resultsCount) {
        assertThat(courtExtractRequested.getIsAppealPending(), is((true)));
        assertValues(courtExtractRequested, extractType, HEARING_DATE_1, HEARING_DATE_2, resultsCount, "resultWording", "Fine");
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().size(), is((1)));
        assertThat(courtExtractRequested.getCompanyRepresentatives().size(), is(1));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getProsecutionCounsels().size(), is(1));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefenceCounsels().size(), is(1));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().size(), is(2));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().size(), is(1));
    }

    private void assertValues(final CourtExtractRequested courtExtractRequested, final String extractType, final String earliestHearingDate, final String hearingDate2, int resultsCount, final String resultWording, final String label) {
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
        assertThat(courtExtractRequested.getDefendant().getRemandStatus(), is(REMAND_STATUS));
        assertThat(courtExtractRequested.getDefendant().getDateOfBirth(), is(DOB));

        if (earliestHearingDate != null) {
            //Court
            assertThat(courtExtractRequested.getPublishingCourt().getName(), is(COURT_NAME));

            assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceOrganisation().getName(), is(ORGANISATION_NAME));
            assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceOrganisation().getAddress().getAddress1(), is(ADDRESS_1));
            assertThat(courtExtractRequested.getDefendant().getDefenceOrganisations().get(0).getDefenceOrganisation().getContact().getFax(), is(("fax")));
            //Hearing
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getType(), is(HEARING_TYPE));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getHearingDays().get(0).getDay(), is((ZonedDateTimes.fromString(earliestHearingDate).toLocalDate())));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getHearingDays().get(1).getDay(), is((ZonedDateTimes.fromString(hearingDate2).toLocalDate())));

            //court decision
            final CrownCourtDecisions crownCourtDecisions = courtExtractRequested.getDefendant().getHearings().get(0).getCrownCourtDecisions();
            final List<JudiciaryNamesByRole> judiciaryNamesByRole = crownCourtDecisions.getJudiciaryNamesByRole();
            assertThat(judiciaryNamesByRole.size(), is(3));
            assertThat(judiciaryNamesByRole.get(0).getRole(), is("District Judge"));
            assertThat(judiciaryNamesByRole.get(0).getNames(), is(List.of("Denial")));

            assertThat(crownCourtDecisions.getDates().size(), is(2));

            //results
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().size(), is(resultsCount));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().get(0).getResult().getIsAvailableForCourtExtract(), is(true));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().get(0).getResult().getLabel(), is(label));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().get(0).getResult().getDelegatedPowers().getFirstName(), is(FIRST_NAME));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().get(0).getResult().getDelegatedPowers().getLastName(), is(LAST_NAME));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().get(0).getResult().getJudicialResultPrompts().get(0).getLabel(), is(LABEL));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().get(0).getResult().getJudicialResultPrompts().get(0).getValue(), is(PROMPT_VALUE));
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getDefendantResults().get(0).getResult().getResultText(), is("resultText"));


            //offences
            final uk.gov.justice.progression.courts.exract.Offences offences = courtExtractRequested.getDefendant().getHearings().get(0).getOffences().get(0);
            assertThat(offences.getConvictionDate(), is((CONVICTION_DATE)));
            assertThat(offences.getResults().get(0).getResult().getIsAvailableForCourtExtract(), is(true));
            assertThat(offences.getResults().get(0).getResult().getResultWording(), is(resultWording));
            assertThat(offences.getResults().get(0).getResult().getDelegatedPowers().getFirstName(), is(FIRST_NAME));
            assertThat(offences.getResults().get(0).getResult().getDelegatedPowers().getLastName(), is(LAST_NAME));
            assertThat(offences.getResults().get(0).getResult().getJudicialResultPrompts().get(0).getLabel(), is(LABEL));
            assertThat(offences.getResults().get(0).getResult().getJudicialResultPrompts().get(0).getValue(), is(PROMPT_VALUE));
            assertThat(offences.getResults().get(0).getResult().getResultText(), is("resultText"));

            //Offence Plea
            assertThat(offences.getPleas().get(0).getDelegatedPowers().getFirstName(), is(FIRST_NAME));
            assertThat(offences.getPleas().get(0).getDelegatedPowers().getLastName(), is(LAST_NAME));
            assertThat(offences.getPleas().get(0).getPleaDate(), is(PLEA_DATE));
            assertThat(offences.getPleas().get(0).getPleaValue(), is(PLEA_GUILTY));
            assertThat(offences.getIndicatedPlea().getIndicatedPleaValue(), is(IndicatedPleaValue.INDICATED_GUILTY));
            assertThat(offences.getAllocationDecision().getOffenceId(), is(OFFENCE_ID));
            assertThat(offences.getAllocationDecision().getMotReasonCode(), is("4"));
            assertThat(offences.getAllocationDecision().getSequenceNumber(), is(40));
            assertThat(offences.getAllocationDecision().getOriginatingHearingId(), is(HEARING_ID));
            assertThat(offences.getAllocationDecision().getAllocationDecisionDate(), is(CONVICTION_DATE));

            //Offence verdict
            assertThat(offences.getVerdicts().get(0).getVerdictType().getCategory(), is((GUILTY)));
            assertThat(offences.getVerdicts().get(0).getVerdictType().getCategoryType(), is((GUILTY)));
            assertThat(offences.getVerdicts().get(0).getVerdictType().getDescription(), is((PLEA_GUILTY_DESCRIPTION)));
            assertThat(offences.getVerdicts().get(0).getJurors().getNumberOfJurors(), is(2));
            assertThat(offences.getVerdicts().get(0).getJurors().getNumberOfSplitJurors(), is(2));
            assertThat(offences.getVerdicts().get(0).getJurors().getUnanimous(), is(true));
        }
        //parentGuardian
        assertThat(courtExtractRequested.getParentGuardian().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(courtExtractRequested.getParentGuardian().getAddress().getAddress2(), is(ADDRESS_2));
        assertThat(courtExtractRequested.getParentGuardian().getAddress().getPostcode(), is(POST_CODE));
        assertThat(courtExtractRequested.getParentGuardian().getName(), is(FIRST_NAME + " D " + LAST_NAME));

        if (earliestHearingDate != null) {
            //referralReason
            assertThat(courtExtractRequested.getReferralReason(), is(DESCRIPTION));

            //
            assertThat(courtExtractRequested.getCompanyRepresentatives().get(0).getName(), is(FIRST_NAME + " " + LAST_NAME));
            assertThat(courtExtractRequested.getCompanyRepresentatives().get(0).getRole(), is("DIRECTOR"));
            assertThat(courtExtractRequested.getCompanyRepresentatives().get(0).getAttendanceDays().size(), is((1)));

            //courtApplications
            assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getApplicationType(), is(APPLICATION_TYPE));
        }
        // application representation
        assertNotNull(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications());
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getAddress().getAddress1(), is(ADDRESS_1));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getName(), is(FIRST_NAME));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getContact().getHome(), is("home"));
        if (earliestHearingDate != null) {
            final uk.gov.justice.progression.courts.exract.Hearings hearingsTwo = courtExtractRequested.getDefendant().getHearings().get(0);
            assertNotNull(hearingsTwo.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().get(0));
            assertThat(hearingsTwo.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().get(0).getFirstName(), is(FIRST_NAME));
            assertThat(hearingsTwo.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().get(0).getLastName(), is(LAST_NAME));
            assertThat(hearingsTwo.getCourtApplications().get(0).getRepresentation().getApplicantRepresentation().getApplicantCounsels().get(0).getStatus(), is("Jury"));
            assertThat(hearingsTwo.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().get(0).getRespondentCounsels().get(0).getFirstName(), is(FIRST_NAME));
            assertThat(hearingsTwo.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().get(0).getRespondentCounsels().get(0).getLastName(), is(LAST_NAME));
            assertThat(hearingsTwo.getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().get(0).getRespondentCounsels().get(0).getStatus(), is("Solicitor"));
        }
        assertNotNull(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().get(0).getRepresentation().getRespondentRepresentation().get(0));
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
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                        .withDefenceOrganisation(uk.gov.justice.core.courts.DefenceOrganisation.defenceOrganisation()
                                .withOrganisation(Organisation.organisation()
                                        .withName(ASSOCIATED_ORGANISATION_NAME)
                                        .withAddress(createAddress())
                                        .withContact(createContact())
                                        .build()).build())
                        .withAssociationStartDate(ASSOCIATION_START_DATE)
                        .withAssociationEndDate(ASSOCIATION_END_DATE)
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

    private GetHearingsAtAGlance createHearingsAtGlance() {
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

    private GetHearingsAtAGlance createHearingAtAGlanceWithBreachTypeApplication(final String defendantId, final String hearingId, final String breachApplicationHearingId) {
        final JsonObject inActiveCaseWithBreachTypeApplication = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-breach-type-application.json")
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("BREACH_H_ID", breachApplicationHearingId));
        final JsonObject hearingsAtAGlanceJson = inActiveCaseWithBreachTypeApplication.getJsonObject("hearingsAtAGlance");
        return jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
    }

    private GetHearingsAtAGlance createHearingAtAGlanceWithSJPCaseReferredToCC(final String defendantId) {
        final JsonObject inActiveCaseWithBreachTypeApplication = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-sjp-referred-to-cc.json")
                .replaceAll("DEFENDANT_ID", defendantId));
        final JsonObject hearingsAtAGlanceJson = inActiveCaseWithBreachTypeApplication.getJsonObject("hearingsAtAGlance");
        return jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
    }

    private GetHearingsAtAGlance createHearingAtAGlanceWithDefendantAndCourtApplicationSlipRuleAmendments(final String defendantId, final String masterDefendantId, final String hearingId) {
        final JsonObject inActiveCaseWithBreachTypeApplication = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-defendant-court-application-results.json")
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("MASTER_D_ID", masterDefendantId)
                .replaceAll("HEARING_ID", hearingId));
        final JsonObject hearingsAtAGlanceJson = inActiveCaseWithBreachTypeApplication.getJsonObject("hearingsAtAGlance");
        return jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
    }

    private GetHearingsAtAGlance createHearingAtAGlanceWithSlipRuleAmendment(final String defendantId, final String hearingId, final String jsonPath) {
        final JsonObject jsonObject = stringToJsonObjectConverter.convert(getPayload(jsonPath)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("HEARING_ID", hearingId));
        final JsonObject hearingsAtAGlanceJson = jsonObject.getJsonObject("hearingsAtAGlance");
        return jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
    }

    private GetHearingsAtAGlance createHearingAtAGlanceWithMagistrateAndCrownCourtHearings(final String defendantId, final String hearingId, final String seedingHearingId) {
        final JsonObject inActiveCaseWithBreachTypeApplication = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-magistrate-and-crown-court.json")
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("SEEDING_HR_ID", seedingHearingId));
        final JsonObject hearingsAtAGlanceJson = inActiveCaseWithBreachTypeApplication.getJsonObject("hearingsAtAGlance");
        return jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
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
                .withApplicationReceivedDate(APPLICATION_DATE)
                .withApplicationParticulars(APPLICATION_PARTICULARS)
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


    private List<Hearings> createHearingsCOC() {
        final LocalDate CONVICTION_DATE1 = LocalDate.of(2020, 04, 04);
        final LocalDate CONVICTION_DATE2 = LocalDate.of(2025, 05, 04);

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
                        .withDefendants(createDefendants(CONVICTION_DATE1, asList(DEFENDANT_ID, DEFENDANT_ID_2ND), asList(randomUUID(), randomUUID(), randomUUID()), HEARING1, HEARING_ID, "CertificateOfConviction"))
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
                        .withDefendants(createDefendants(CONVICTION_DATE2, asList(DEFENDANT_ID), asList(randomUUID(), randomUUID(), randomUUID()), HEARING2, HEARING_ID_2, "CertificateOfConviction"))
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
                .withWelshName("Welsh " + COURT_NAME)
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
                offences()
                        .withId(offenceId)
                        .withOrderIndex(Integer.valueOf(randomNumeric(2)))
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
                        .withJudicialResult(createJudicialResultWithCourtExtractFlag(false, promptsForMinor, ORDERING_DEFENDANT_LEVEL))
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
                .withLegalAidStatus("legal aid")
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

    private List<Defendants> createDefendants(final LocalDate convictionDate, final List<UUID> defendantIdList, final List<UUID> offenceIdList, final String hearing, final UUID hearingId, final String extractType) {
        return defendantIdList.stream().map(id -> createDefendant(convictionDate, id, offenceIdList, hearing, hearingId, extractType)).collect(toList());
    }

    private Defendants createDefendant(final LocalDate convictionDate, final UUID defendantId, final List<UUID> offenceIdList, final String hearing, final UUID hearingId, final String extractType) {
        Defendants.Builder defendantBuilder = Defendants.defendants()
                .withId(defendantId)
                .withAddress(createAddress())
                .withDateOfBirth(DOB)
                .withAge(DEFENDANT_AGE)
                .withLegalAidStatus("legal aid")
                .withJudicialResults(createResults(hearingId))
                .withDefenceOrganisation(createDefenceOrganisation(hearing))
                .withCourtApplications(asList(createCourtApplications()))
                .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                        .withCustody(CUSTODY_TYPE)
                        .withId(CUSTODY_ESTABLISHMENT_UUID)
                        .withName(CUSTODY_ESTABLISHMENT_NAME)
                        .build());

        if("CertificateOfConviction".equals(extractType)) {
            defendantBuilder.withOffences(createOffenceForCOC(convictionDate, offenceIdList, hearingId));
        } else {
            defendantBuilder.withOffences(createOffence(OFFENCE_ID, hearingId));

        }

        return defendantBuilder.build();
    }

    private Defendants createDefendant(final UUID defendantId, final String hearing, final UUID hearingId) {
        return Defendants.defendants()
                .withId(defendantId)
                .withAddress(createAddress())
                .withDateOfBirth(DOB)
                .withAge(DEFENDANT_AGE)
                .withLegalAidStatus("legal aid")
                .withOffences(createOffence(OFFENCE_ID, hearingId))
                .withJudicialResults(createResults(hearingId))
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
                withArrestSummonsNumber("Arrest123")
                .withBailStatus(BailStatus.bailStatus()
                        .withDescription("Custody or remanded into custody")
                        .withCode("C")
                        .withId(randomUUID()).build())
                .build();

    }

    private List<Offences> createOffence(final UUID offenceId, final UUID hearingId) {
        return asList(
                offences()
                        .withId(offenceId)
                        .withOrderIndex(Integer.valueOf(randomNumeric(2)))
                        .withConvictionDate(CONVICTION_DATE)
                        .withJudicialResults(createResults(hearingId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withAllocationDecision(createAllocationDecision())
                        .withVerdicts(createVerdicts()).build()
        );
    }

    private List<Offences> createOffenceForCOC(final LocalDate convictionDate,final List<UUID> offenceIdList, UUID hearingId) {
        return asList(
                offences()
                        .withId(offenceIdList.get(0))
                        .withOrderIndex(Integer.valueOf(randomNumeric(2)))
                        .withCount(3)
                        .withConvictionDate(convictionDate.plusWeeks(1))
                        .withJudicialResults(createResults(hearingId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withAllocationDecision(createAllocationDecision())
                        .withIndictmentParticular("Offence 1 Indictment Particular")
                        .withVerdicts(createVerdicts()).build(),
                offences()
                        .withId(offenceIdList.get(1))
                        .withCount(1)
                        .withOrderIndex(Integer.valueOf(randomNumeric(1)))
                        .withConvictionDate(convictionDate.plusDays(2))
                        .withJudicialResults(createResults(hearingId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withIndictmentParticular("Offence 2 Indictment Particular")
                        .withAllocationDecision(createAllocationDecision())
                        .withVerdicts(createVerdicts()).build(),
                offences()
                        .withId(offenceIdList.get(2))
                        .withCount(5)
                        .withOrderIndex(Integer.valueOf(randomNumeric(2)))
                        .withConvictionDate(convictionDate.plusWeeks(1))
                        .withJudicialResults(createResults(hearingId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withAllocationDecision(createAllocationDecision())
                        .withVerdicts(createVerdicts())
                        .withProceedingsConcluded(true)
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
                .withUserId(USER_ID)
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
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_1))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build()
        );
    }

    private List<HearingDay> createHearingDays2() {
        return asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_3))
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
                                .withUserId(USER_ID)
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

    private CourtApplication createBreachCourtOrder() {
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(createApplicationParty())
                .withType(createCourtApplicationType())
                .withApplicationReceivedDate(APPLICATION_DATE)
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withRespondents(createCourtApplicationRespondents())
                .withCourtOrder(createCourtOrder())
                .build();

    }

    private CourtApplication createCourtApplication() {
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(createApplicationParty())
                .withType(createCourtApplicationType())
                .withApplicationReceivedDate(APPLICATION_DATE)
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withRespondents(createCourtApplicationRespondents())
                .build();
    }

    private CourtApplications createCourtApplications() {
        return CourtApplications.courtApplications()
                .withApplicationId(APPLICATION_ID)
                .withApplicationReceivedDate(APPLICATION_DATE)
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

    private CourtOrder createCourtOrder() {
        return CourtOrder.courtOrder().withCourtOrderOffences(
                asList(CourtOrderOffence.courtOrderOffence()
                        .withOffence(Offence.offence()
                                .withId(randomUUID())
                                .withOffenceTitle("Title")
                                .withOrderIndex(5)
                                .withCount(3)
                                .withConvictionDate(CONVICTION_DATE.plusWeeks(3)).build())
                        .build(),
                        CourtOrderOffence.courtOrderOffence()
                                .withOffence(Offence.offence()
                                        .withId(randomUUID())
                                        .withOffenceTitle("Title")
                                        .withOrderIndex(5)
                                        .withCount(3)
                                        .withConvictionDate(CONVICTION_DATE.plusDays(3)).build())
                                .build())).build();
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

    private static Hearing getHearingFromListing(final UUID hearingId, final UUID seedingHearingId, final UUID defendantId, final UUID offenceId) {
        return Hearing.hearing().withId(hearingId)
                .withListedCases(singletonList(ListedCase.listedCase()
                        .withDefendants(singletonList(uk.gov.moj.cpp.listing.domain.Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(singletonList(uk.gov.moj.cpp.listing.domain.Offence.offence()
                                        .withId(offenceId)
                                        .withSeedingHearing(Optional.of(SeedingHearing.seedingHearing()
                                                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                                                .withSittingDay(LocalDate.now().toString())
                                                .withSeedingHearingId(seedingHearingId).build()))
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }

}