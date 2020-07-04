package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.core.courts.HearingListingStatus.HEARING_INITIALISED;
import static uk.gov.justice.core.courts.HearingListingStatus.SENT_FOR_LISTING;
import static uk.gov.justice.core.courts.HearingListingStatus.HEARING_RESULTED;

import uk.gov.justice.core.courts.ApplicantCounsel;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationOutcome;
import uk.gov.justice.core.courts.CourtApplicationOutcomeType;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationResponse;
import uk.gov.justice.core.courts.CourtApplicationResponseType;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.RespondentCounsel;
import uk.gov.justice.progression.courts.CourtApplications;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.LatestHearingJurisdictionType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GetHearingAtAGlanceServiceTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String HEARING_PAYLOAD = "{\"courtCentre\":{\"address\":{\"address1\":\"176A Lavender Hill\",\"address2\":\"London\",\"address3\":\"\",\"address4\":\"\",\"address5\":\"\",\"postcode\":\"SW11 1JU\"},\"id\":\"f8254db1-1683-483e-afb3-b87fde5a0a26\",\"name\":\"Lavender Hill Magistrates' Court\",\"roomId\":\"9e4932f7-97b2-3010-b942-ddd2624e4dd8\",\"roomName\":\"Courtroom 01\"},\"hasSharedResults\":false,\"hearingDays\":[{\"listedDurationMinutes\":1," +
            "\"listingSequence\":0,\"sittingDay\":\"2020-02-28T09:00:00.000Z\"},{\"listedDurationMinutes\":20,\"listingSequence\":0,\"sittingDay\":\"2020-02-29T10:00:00.000Z\"},{\"listedDurationMinutes\":20,\"listingSequence\":0,\"sittingDay\":\"2020-03-01T10:00:00.000Z\"},{\"listedDurationMinutes\":20,\"listingSequence\":0,\"sittingDay\":\"2020-03-03T10:00:00.000Z\"},{\"listedDurationMinutes\":2,\"listingSequence\":0,\"sittingDay\":\"2020-03-04T15:00:00.000Z\"}],\"hearingLanguage\":\"ENGLISH\",\"id\":\"133fc23e-746e-49f0-80bc-64add08d62ec\",\"judiciary\":[{\"firstName\":\"Shamim (Sham)\",\"isBenchChairman\":true,\"isDeputy\":true,\"judicialId\":\"0a87257e-5308-397d-b432-30edf4ad1dae\"," +
            "\"judicialRoleType\":{\"judicialRoleTypeId\":\"0a87257e-5308-397d-b432-30edf4ad1dae\",\"judiciaryType\":\"DJ\"},\"lastName\":\"Qureshi\",\"title\":\"Mr\"}],\"jurisdictionType\":\"CROWN\",\"prosecutionCases\":[{\"defendants\":[{\"id\":\"2a2eeb66-d01b-49c2-8dfc-324d91cac2f7\",\"offences\":[{\"arrestDate\":\"2006-05-04\",\"chargeDate\":\"2004-12-09\",\"count\":0,\"id\":\"09c380f9-e127-4638-bd1e-55079dff953a\",\"modeOfTrial\":\"Summary\",\"offenceCode\":\"CA03014\",\"offenceDefinitionId\":\"d6bd72ad-37bf-330d-bcc6-215728949d3e\",\"offenceTitle\":\"Fail / refuse give assistance to person executing Communications Act search warrant\",\"orderIndex\":500,\"startDate\":\"2004-12-09\"," +
            "\"wording\":\"Has a violent past and fear that he will commit further offences and\\n                interfere with witnesse\"}],\"personDefendant\":{\"arrestSummonsNumber\":\"TFL\",\"bailStatus\":{\"code\":\"C\",\"description\":\"Remanded into Custody\",\"id\":\"12e69486-4d01-3403-a50a-7419ca040635\"},\"personDetails\":{\"address\":{\"address1\":\"56Police House\",\"address2\":\"StreetDescription\",\"address3\":\"Locality2O\"},\"documentationLanguageNeeds\":\"ENGLISH\",\"gender\":\"MALE\",\"lastName\":\"Ormsby\"}},\"prosecutionAuthorityReference\":\"TFL\",\"prosecutionCaseId\":\"" + CASE_ID + "\"}],\"id\":\"" + CASE_ID + "\",\"initiationCode\":\"C\",\"originatingOrganisation\":" +
            "\"B01BH00\",\"prosecutionCaseIdentifier\":{\"prosecutionAuthorityCode\":\"DVL2\",\"prosecutionAuthorityId\":\"bcdca7df-ab21-45f6-bc19-f883cf3d407e\",\"caseURN\":\"72GD8580920\"}}],\"type\":{\"description\":\"First Hearing\",\"id\":\"4a0e892d-c0c5-3c51-95b8-704d8c781776\"}}";

    private static final String HEARING_PAYLOAD_WITH_NO_HEARING_DAYS = "{\"courtCentre\":{\"address\":{\"address1\":\"176A Lavender Hill\",\"address2\":\"London\",\"address3\":\"\",\"address4\":\"\",\"address5\":\"\",\"postcode\":\"SW11 1JU\"},\"id\":\"f8254db1-1683-483e-afb3-b87fde5a0a26\",\"name\":\"Lavender Hill Magistrates' Court\",\"roomId\":\"9e4932f7-97b2-3010-b942-ddd2624e4dd8\",\"roomName\":\"Courtroom 01\"},\"hasSharedResults\":false,\"hearingDays\":[],\"hearingLanguage\":\"ENGLISH\",\"id\":\"133fc23e-746e-49f0-80bc-64add08d62ec\",\"judiciary\":[{\"firstName\":\"Shamim (Sham)\",\"isBenchChairman\":true,\"isDeputy\":true,\"judicialId\":\"0a87257e-5308-397d-b432-30edf4ad1dae\"," +
            "\"judicialRoleType\":{\"judicialRoleTypeId\":\"0a87257e-5308-397d-b432-30edf4ad1dae\",\"judiciaryType\":\"DJ\"},\"lastName\":\"Qureshi\",\"title\":\"Mr\"}],\"jurisdictionType\":\"CROWN\",\"prosecutionCases\":[{\"defendants\":[{\"id\":\"2a2eeb66-d01b-49c2-8dfc-324d91cac2f7\",\"offences\":[{\"arrestDate\":\"2006-05-04\",\"chargeDate\":\"2004-12-09\",\"count\":0,\"id\":\"09c380f9-e127-4638-bd1e-55079dff953a\",\"modeOfTrial\":\"Summary\",\"offenceCode\":\"CA03014\",\"offenceDefinitionId\":\"d6bd72ad-37bf-330d-bcc6-215728949d3e\",\"offenceTitle\":\"Fail / refuse give assistance to person executing Communications Act search warrant\",\"orderIndex\":500,\"startDate\":\"2004-12-09\"," +
            "\"wording\":\"Has a violent past and fear that he will commit further offences and\\n                interfere with witnesse\"}],\"personDefendant\":{\"arrestSummonsNumber\":\"TFL\",\"bailStatus\":{\"code\":\"C\",\"description\":\"Remanded into Custody\",\"id\":\"12e69486-4d01-3403-a50a-7419ca040635\"},\"personDetails\":{\"address\":{\"address1\":\"56Police House\",\"address2\":\"StreetDescription\",\"address3\":\"Locality2O\"},\"documentationLanguageNeeds\":\"ENGLISH\",\"gender\":\"MALE\",\"lastName\":\"Ormsby\"}},\"prosecutionAuthorityReference\":\"TFL\",\"prosecutionCaseId\":\"" + CASE_ID + "\"}],\"id\":\"" + CASE_ID + "\",\"initiationCode\":\"C\",\"originatingOrganisation\":" +
            "\"B01BH00\",\"prosecutionCaseIdentifier\":{\"prosecutionAuthorityCode\":\"DVL2\",\"prosecutionAuthorityId\":\"bcdca7df-ab21-45f6-bc19-f883cf3d407e\",\"caseURN\":\"72GD8580920\"}}],\"type\":{\"description\":\"First Hearing\",\"id\":\"4a0e892d-c0c5-3c51-95b8-704d8c781776\"}}";
    private static final UUID DEFENDANT_ID_1 = randomUUID();
    private static final UUID DEFENDANT_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID_3 = randomUUID();
    private static final UUID CASE_HEARING_ID_1 = randomUUID();
    private static final UUID CASE_HEARING_ID_2 = randomUUID();
    private static final UUID CASE_HEARING_ID_3 = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID APPLICATION_HEARING_ID = randomUUID();
    private static final LocalDate LOCALDATE_NOW = LocalDate.now();
    private static final UUID GENERIC_UUID = randomUUID();
    private static final String EMPTY_STRING = "";
    private final UUID LAA_STATUS_ID = randomUUID();

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Mock
    private HearingApplicationRepository hearingApplicationRepository;

    @InjectMocks
    private GetHearingAtAGlanceService getHearingAtAGlanceService;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private CaseDefendantHearingEntity caseDefendantHearingEntity;
    @Mock
    private HearingEntity hearingEntity;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void hearingAtAGlanceHearingWithCaseWithOneHearingWithTwoDefendantsNoApplications() {

        ProsecutionCase prosecutionCase = createProsecutionCase(CASE_ID, Arrays.asList(DEFENDANT_ID_1, DEFENDANT_ID_2));
        Hearing caseHearing = createCaseHearing(prosecutionCase, null, CASE_HEARING_ID_1);

        ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase);
        HearingEntity caseHearingEntity = createHearingEntity(caseHearing, CASE_HEARING_ID_1, HEARING_INITIALISED);

        List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();

        CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_1, CASE_HEARING_ID_1));
        caseDefendantHearingEntity1.setHearing(caseHearingEntity);

        CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_2, CASE_HEARING_ID_1));
        caseDefendantHearingEntity2.setHearing(caseHearingEntity);

        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity2);

        when(this.prosecutionCaseRepository.findByCaseId(CASE_ID)).thenReturn(prosecutionCaseEntity);
        when(this.caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(caseDefendantHearingEntities);

        GetHearingsAtAGlance response = this.getHearingAtAGlanceService.getHearingAtAGlance(CASE_ID);

        // Prosecution Case Id assertion
        assertThat(response.getId(), is(CASE_ID));

        // Defendant Hearing details
        assertThat(response.getDefendantHearings().size(), is(2));
        // Defendant 1
        assertThat(response.getDefendantHearings().get(0).getDefendantId(), is(DEFENDANT_ID_1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(0).getDefendantName(), is("John Williams"));

        // Defendant 2
        assertThat(response.getDefendantHearings().get(1).getDefendantId(), is(DEFENDANT_ID_2));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(1).getDefendantName(), is("John Williams"));

        // Hearing level assertions
        assertThat(response.getHearings().size(), is(1));

        final Hearings hearingResponse = response.getHearings().get(0);
        assertThat(hearingResponse.getId(), is(caseHearing.getId()));
        assertThat(hearingResponse.getType().getDescription(), is(caseHearing.getType().getDescription()));
        assertThat(hearingResponse.getJurisdictionType(), is(uk.gov.justice.progression.courts.JurisdictionType.CROWN));
        assertThat(hearingResponse.getHearingListingStatus(), is(uk.gov.justice.progression.courts.HearingListingStatus.HEARING_INITIALISED));

        // Hearing level defendant details
        assertThat(hearingResponse.getDefendants().size(), is(2));
        assertEquals("15", hearingResponse.getDefendants().get(0).getAge());
        assertEquals("15", hearingResponse.getDefendants().get(1).getAge());

        // Prosecution Case Identifier assertions
        final ProsecutionCaseIdentifier prosecutionCaseIdentifierResponse = response.getProsecutionCaseIdentifier();
        assertThat(prosecutionCaseIdentifierResponse.getCaseURN(), is(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityCode(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityId(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityReference(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));

        // Court Centre assertions
        final CourtCentre courtCentreResponse = hearingResponse.getCourtCentre();
        assertThat(courtCentreResponse.getId(), is(caseHearing.getCourtCentre().getId()));
        assertThat(courtCentreResponse.getRoomId(), is(caseHearing.getCourtCentre().getRoomId()));
        assertThat(courtCentreResponse.getName(), is(caseHearing.getCourtCentre().getName()));
        assertThat(courtCentreResponse.getRoomName(), is(caseHearing.getCourtCentre().getRoomName()));
        assertThat(courtCentreResponse.getWelshName(), is(caseHearing.getCourtCentre().getWelshName()));
        assertThat(courtCentreResponse.getWelshRoomName(), is(caseHearing.getCourtCentre().getWelshRoomName()));

        final List<ProsecutionCounsel> prosecutionCounselsResponse = hearingResponse.getProsecutionCounsels();
        assertThat(prosecutionCounselsResponse.get(0).getFirstName(), is(caseHearing.getProsecutionCounsels().get(0).getFirstName()));
        assertThat(hearingResponse.getHasResultAmended(), is(true));
    }

    @Test
    public void hearingAtAGlanceHearingWithCaseWithTwoHearingsWithTwoDefendantsNoApplications() {

        ProsecutionCase prosecutionCase = createProsecutionCase(CASE_ID, Arrays.asList(DEFENDANT_ID_1, DEFENDANT_ID_2));
        Hearing caseHearing1 = createCaseHearing(prosecutionCase, null, CASE_HEARING_ID_1);

        ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase);
        HearingEntity caseHearingEntity1 = createHearingEntity(caseHearing1, CASE_HEARING_ID_1, HEARING_INITIALISED);
        HearingEntity caseHearingEntity2 = createHearingEntity(caseHearing1, CASE_HEARING_ID_2, HEARING_INITIALISED);

        List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();

        CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_1, CASE_HEARING_ID_1));
        caseDefendantHearingEntity1.setHearing(caseHearingEntity1);

        CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_2, CASE_HEARING_ID_1));
        caseDefendantHearingEntity2.setHearing(caseHearingEntity2);

        CaseDefendantHearingEntity caseDefendantHearingEntity3 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity3.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_1, CASE_HEARING_ID_2));
        caseDefendantHearingEntity3.setHearing(caseHearingEntity1);

        CaseDefendantHearingEntity caseDefendantHearingEntity4 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity4.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_2, CASE_HEARING_ID_2));
        caseDefendantHearingEntity4.setHearing(caseHearingEntity2);

        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity2);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity3);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity4);

        when(this.prosecutionCaseRepository.findByCaseId(CASE_ID)).thenReturn(prosecutionCaseEntity);
        when(this.caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(caseDefendantHearingEntities);

        GetHearingsAtAGlance response = this.getHearingAtAGlanceService.getHearingAtAGlance(CASE_ID);

        // Prosecution Case Id assertion
        assertThat(response.getId(), is(CASE_ID));

        // Defendant Hearing details
        assertThat(response.getDefendantHearings().size(), is(2));
        // Defendant 1
        assertThat(response.getDefendantHearings().get(0).getDefendantId(), is(DEFENDANT_ID_1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().size(), is(2));
        assertThat(response.getDefendantHearings().get(0).getDefendantName(), is("John Williams"));
        assertThat(response.getHearings().get(0).getDefendants().get(0).getOffences().get(0).getLaaApplnReference().getStatusId(), is(LAA_STATUS_ID));
        assertThat(response.getHearings().get(0).getDefendants().get(0).getLegalAidStatus(), is("Granted"));

        // Defendant 2
        assertThat(response.getDefendantHearings().get(1).getDefendantId(), is(DEFENDANT_ID_2));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().size(), is(2));
        assertThat(response.getDefendantHearings().get(1).getDefendantName(), is("John Williams"));
        assertThat(response.getHearings().get(0).getDefendants().get(0).getOffences().get(0).getLaaApplnReference().getStatusId(), is(LAA_STATUS_ID));

        // Hearing level assertions
        assertThat(response.getHearings().size(), is(2));

        final Hearings hearingResponse = response.getHearings().get(0);
        assertThat(hearingResponse.getId(), is(caseHearing1.getId()));
        assertThat(hearingResponse.getType().getDescription(), is(caseHearing1.getType().getDescription()));
        assertThat(hearingResponse.getJurisdictionType(), is(uk.gov.justice.progression.courts.JurisdictionType.CROWN));
        assertThat(hearingResponse.getHearingListingStatus(), is(uk.gov.justice.progression.courts.HearingListingStatus.HEARING_INITIALISED));

        // Hearing level defendant details
        assertThat(hearingResponse.getDefendants().size(), is(2));

        // Prosecution Case Identifier assertions
        final ProsecutionCaseIdentifier prosecutionCaseIdentifierResponse = response.getProsecutionCaseIdentifier();
        assertThat(prosecutionCaseIdentifierResponse.getCaseURN(), is(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityCode(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityId(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityReference(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));

        // Court Centre assertions
        final CourtCentre courtCentreResponse = hearingResponse.getCourtCentre();
        assertThat(courtCentreResponse.getId(), is(caseHearing1.getCourtCentre().getId()));
        assertThat(courtCentreResponse.getRoomId(), is(caseHearing1.getCourtCentre().getRoomId()));
        assertThat(courtCentreResponse.getName(), is(caseHearing1.getCourtCentre().getName()));
        assertThat(courtCentreResponse.getRoomName(), is(caseHearing1.getCourtCentre().getRoomName()));
        assertThat(courtCentreResponse.getWelshName(), is(caseHearing1.getCourtCentre().getWelshName()));
        assertThat(courtCentreResponse.getWelshRoomName(), is(caseHearing1.getCourtCentre().getWelshRoomName()));

        final List<ProsecutionCounsel> prosecutionCounselsResponse = hearingResponse.getProsecutionCounsels();
        assertThat(prosecutionCounselsResponse.get(0).getFirstName(), is(caseHearing1.getProsecutionCounsels().get(0).getFirstName()));
        assertThat(hearingResponse.getHasResultAmended(), is(true));
    }

    @Test
    public void hearingAtAGlanceHearingWithCaseWithTwoDefendantsAndOneApplicationInSameHearing() {

        ProsecutionCase prosecutionCase = createProsecutionCase(CASE_ID, Arrays.asList(DEFENDANT_ID_1, DEFENDANT_ID_2));
        CourtApplication courtApplication = createCourtApplicationWithDefendants(APPLICATION_ID, DEFENDANT_ID_1);
        Hearing caseHearing = createCaseHearing(prosecutionCase, courtApplication, CASE_HEARING_ID_1);

        ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase);
        HearingEntity caseHearingEntity = createHearingEntity(caseHearing, CASE_HEARING_ID_1, HEARING_INITIALISED);

        List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();

        CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_1, CASE_HEARING_ID_1));
        caseDefendantHearingEntity1.setHearing(caseHearingEntity);

        CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_2, CASE_HEARING_ID_1));
        caseDefendantHearingEntity2.setHearing(caseHearingEntity);

        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity2);


        //Hearing applicationHearing = createApplicationHearing(courtApplication, CASE_HEARING_ID_1);

        CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        courtApplicationEntity.setPayload(courtApplication.toString());

        HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(APPLICATION_ID, CASE_HEARING_ID_1));
        hearingApplicationEntity.setHearing(caseHearingEntity);

        when(this.prosecutionCaseRepository.findByCaseId(CASE_ID)).thenReturn(prosecutionCaseEntity);
        when(this.caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(caseDefendantHearingEntities);
        when(this.courtApplicationRepository.findByLinkedCaseId(CASE_ID)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(this.hearingApplicationRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Arrays.asList(hearingApplicationEntity));

        GetHearingsAtAGlance response = this.getHearingAtAGlanceService.getHearingAtAGlance(CASE_ID);

        // Prosecution Case Id assertion
        assertThat(response.getId(), is(CASE_ID));

        // Defendant Hearing details
        assertThat(response.getDefendantHearings().size(), is(2));
        // Defendant 1
        assertThat(response.getDefendantHearings().get(0).getDefendantId(), is(DEFENDANT_ID_1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(0).getDefendantName(), is("John Williams"));

        // Defendant 2
        assertThat(response.getDefendantHearings().get(1).getDefendantId(), is(DEFENDANT_ID_2));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(1).getDefendantName(), is("John Williams"));

        // Hearing level assertions
        assertThat(response.getHearings().size(), is(1));

        final Hearings hearingResponse = response.getHearings().get(0);
        assertThat(hearingResponse.getId(), is(caseHearing.getId()));
        assertThat(hearingResponse.getType().getDescription(), is(caseHearing.getType().getDescription()));
        assertThat(hearingResponse.getJurisdictionType(), is(uk.gov.justice.progression.courts.JurisdictionType.CROWN));
        assertThat(hearingResponse.getHearingListingStatus(), is(uk.gov.justice.progression.courts.HearingListingStatus.HEARING_INITIALISED));

        // Hearing level defendant details
        assertThat(hearingResponse.getDefendants().size(), is(2));
        assertThat(hearingResponse.getDefendants().get(0).getCourtApplications().size(), is(1));

        CourtApplications courtApplications = hearingResponse.getDefendants().get(0).getCourtApplications().get(0);
        assertThat(courtApplications.getRespondents().get(0).getApplicationResponse(), is("Sentencing"));
        assertThat(courtApplications.getRespondents().get(0).getResponseDate(), is(LocalDate.of(2018, 10, 11)));

        // Prosecution Case Identifier assertions
        final ProsecutionCaseIdentifier prosecutionCaseIdentifierResponse = response.getProsecutionCaseIdentifier();
        assertThat(prosecutionCaseIdentifierResponse.getCaseURN(), is(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityCode(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityId(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityReference(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));

        // Court Centre assertions
        final CourtCentre courtCentreResponse = hearingResponse.getCourtCentre();
        assertThat(courtCentreResponse.getId(), is(caseHearing.getCourtCentre().getId()));
        assertThat(courtCentreResponse.getRoomId(), is(caseHearing.getCourtCentre().getRoomId()));
        assertThat(courtCentreResponse.getName(), is(caseHearing.getCourtCentre().getName()));
        assertThat(courtCentreResponse.getRoomName(), is(caseHearing.getCourtCentre().getRoomName()));
        assertThat(courtCentreResponse.getWelshName(), is(caseHearing.getCourtCentre().getWelshName()));
        assertThat(courtCentreResponse.getWelshRoomName(), is(caseHearing.getCourtCentre().getWelshRoomName()));

        final List<ProsecutionCounsel> prosecutionCounselsResponse = hearingResponse.getProsecutionCounsels();
        assertThat(prosecutionCounselsResponse.get(0).getFirstName(), is(caseHearing.getProsecutionCounsels().get(0).getFirstName()));
        assertThat(hearingResponse.getHasResultAmended(), is(true));
    }

    @Test
    public void hearingAtAGlanceHearingWithCaseWithTwoDefendantsAndOneApplicationInDifferentHearings() {

        ProsecutionCase prosecutionCase = createProsecutionCase(CASE_ID, Arrays.asList(DEFENDANT_ID_1, DEFENDANT_ID_2));
        Hearing caseHearing = createCaseHearing(prosecutionCase, null, CASE_HEARING_ID_1);

        ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase);
        HearingEntity caseHearingEntity = createHearingEntity(caseHearing, CASE_HEARING_ID_1, HEARING_INITIALISED);

        List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();

        CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_1, CASE_HEARING_ID_1));
        caseDefendantHearingEntity1.setHearing(caseHearingEntity);

        CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_2, CASE_HEARING_ID_1));
        caseDefendantHearingEntity2.setHearing(caseHearingEntity);

        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity2);

        CourtApplication courtApplication = createCourtApplicationWithDefendants(APPLICATION_ID, DEFENDANT_ID_1);
        Hearing applicationHearing = createApplicationHearing(courtApplication, APPLICATION_HEARING_ID);
        HearingEntity applicationHearingEntity = createHearingEntity(applicationHearing, APPLICATION_HEARING_ID, HEARING_INITIALISED);

        CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        courtApplicationEntity.setPayload(courtApplication.toString());

        HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(APPLICATION_ID, APPLICATION_HEARING_ID));
        hearingApplicationEntity.setHearing(applicationHearingEntity);

        when(this.prosecutionCaseRepository.findByCaseId(CASE_ID)).thenReturn(prosecutionCaseEntity);
        when(this.caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(caseDefendantHearingEntities);
        when(this.courtApplicationRepository.findByLinkedCaseId(CASE_ID)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(this.hearingApplicationRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Arrays.asList(hearingApplicationEntity));

        GetHearingsAtAGlance response = this.getHearingAtAGlanceService.getHearingAtAGlance(CASE_ID);

        // Prosecution Case Id assertion
        assertThat(response.getId(), is(CASE_ID));

        // Defendant Hearing details
        assertThat(response.getDefendantHearings().size(), is(2));
        // Defendant 1
        assertThat(response.getDefendantHearings().get(0).getDefendantId(), is(DEFENDANT_ID_1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(0).getDefendantName(), is("John Williams"));

        // Defendant 2
        assertThat(response.getDefendantHearings().get(1).getDefendantId(), is(DEFENDANT_ID_2));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(1).getDefendantName(), is("John Williams"));

        // Hearing level assertions
        assertThat(response.getHearings().size(), is(2));

        // Hearing 1
        final Hearings hearingResponse1 = response.getHearings().get(0);
        assertThat(hearingResponse1.getId(), is(caseHearing.getId()));
        assertThat(hearingResponse1.getType().getDescription(), is(caseHearing.getType().getDescription()));
        assertThat(hearingResponse1.getJurisdictionType(), is(uk.gov.justice.progression.courts.JurisdictionType.CROWN));
        assertThat(hearingResponse1.getHearingListingStatus(), is(uk.gov.justice.progression.courts.HearingListingStatus.HEARING_INITIALISED));

        assertThat(hearingResponse1.getDefendants().size(), is(2));

        assertThat(applicationHearing.getIsBoxHearing(), is(true));

        // Prosecution Case Identifier assertions
        final ProsecutionCaseIdentifier prosecutionCaseIdentifierResponse = response.getProsecutionCaseIdentifier();
        assertThat(prosecutionCaseIdentifierResponse.getCaseURN(), is(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityCode(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityId(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityReference(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));

        // Court Centre assertions
        final CourtCentre courtCentreResponse = hearingResponse1.getCourtCentre();
        assertThat(courtCentreResponse.getId(), is(caseHearing.getCourtCentre().getId()));
        assertThat(courtCentreResponse.getRoomId(), is(caseHearing.getCourtCentre().getRoomId()));
        assertThat(courtCentreResponse.getName(), is(caseHearing.getCourtCentre().getName()));
        assertThat(courtCentreResponse.getRoomName(), is(caseHearing.getCourtCentre().getRoomName()));
        assertThat(courtCentreResponse.getWelshName(), is(caseHearing.getCourtCentre().getWelshName()));
        assertThat(courtCentreResponse.getWelshRoomName(), is(caseHearing.getCourtCentre().getWelshRoomName()));

        final List<ProsecutionCounsel> prosecutionCounselsResponse = hearingResponse1.getProsecutionCounsels();
        assertThat(prosecutionCounselsResponse.get(0).getFirstName(), is(caseHearing.getProsecutionCounsels().get(0).getFirstName()));

        assertThat(hearingResponse1.getHasResultAmended(), is(true));

        // Hearing 2
        final Hearings hearingResponse2 = response.getHearings().get(1);
        assertThat(hearingResponse2.getId(), is(applicationHearing.getId()));
        assertThat(hearingResponse2.getType().getDescription(), is(applicationHearing.getType().getDescription()));
        assertThat(hearingResponse2.getJurisdictionType(), is(uk.gov.justice.progression.courts.JurisdictionType.CROWN));
        assertThat(hearingResponse2.getHearingListingStatus(), is(uk.gov.justice.progression.courts.HearingListingStatus.HEARING_INITIALISED));

        // Hearing level defendant details
        assertThat(hearingResponse2.getDefendants().size(), is(1));
        assertEquals("16", hearingResponse2.getDefendants().get(0).getAge());

        assertThat(hearingResponse2.getDefendants().get(0).getCourtApplications().size(), is(1));

        // Court Application Counsels assertions
        ApplicantCounsel applicantCounsel = hearingResponse2.getApplicantCounsels().get(0);
        assertEquals(GENERIC_UUID, applicantCounsel.getId());
        assertEquals("Applicant Counsel Title", applicantCounsel.getTitle());
        assertEquals("Applicant Counsel First Name", applicantCounsel.getFirstName());
        assertEquals("Applicant Counsel Middle Name", applicantCounsel.getMiddleName());
        assertEquals("Applicant Counsel Last Name", applicantCounsel.getLastName());
        assertEquals("Applicant Counsel Status", applicantCounsel.getStatus());
        assertEquals(DEFENDANT_ID_1, applicantCounsel.getApplicants().get(0));
        assertEquals(LOCALDATE_NOW, applicantCounsel.getAttendanceDays().get(0));

        RespondentCounsel respondentCounsel = hearingResponse2.getRespondentCounsels().get(0);
        assertEquals(GENERIC_UUID, respondentCounsel.getId());
        assertEquals("Respondent Counsel Title", respondentCounsel.getTitle());
        assertEquals("Respondent Counsel First Name", respondentCounsel.getFirstName());
        assertEquals("Respondent Counsel Middle Name", respondentCounsel.getMiddleName());
        assertEquals("Respondent Counsel Last Name", respondentCounsel.getLastName());
        assertEquals("Respondent Counsel Status", respondentCounsel.getStatus());
        assertEquals(DEFENDANT_ID_1, respondentCounsel.getRespondents().get(0));
        assertEquals(LOCALDATE_NOW, respondentCounsel.getAttendanceDays().get(0));
    }

    @Test
    public void hearingAtAGlanceHearingWithCaseWithTwoDefendantsAndOneApplicationWithIndividualInSameHearing() {

        ProsecutionCase prosecutionCase = createProsecutionCase(CASE_ID, Arrays.asList(DEFENDANT_ID_1, DEFENDANT_ID_2));
        CourtApplication courtApplication = createCourtApplicationWithIndividuals(APPLICATION_ID, randomUUID());
        Hearing caseHearing = createCaseHearing(prosecutionCase, courtApplication, CASE_HEARING_ID_1);

        ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase);
        HearingEntity caseHearingEntity = createHearingEntity(caseHearing, CASE_HEARING_ID_1, HEARING_INITIALISED);

        List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();

        CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_1, CASE_HEARING_ID_1));
        caseDefendantHearingEntity1.setHearing(caseHearingEntity);

        CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_2, CASE_HEARING_ID_1));
        caseDefendantHearingEntity2.setHearing(caseHearingEntity);

        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity2);

        CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        courtApplicationEntity.setPayload(courtApplication.toString());

        HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(APPLICATION_ID, CASE_HEARING_ID_1));
        hearingApplicationEntity.setHearing(caseHearingEntity);

        when(this.prosecutionCaseRepository.findByCaseId(CASE_ID)).thenReturn(prosecutionCaseEntity);
        when(this.caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(caseDefendantHearingEntities);
        when(this.courtApplicationRepository.findByLinkedCaseId(CASE_ID)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(this.hearingApplicationRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Arrays.asList(hearingApplicationEntity));

        GetHearingsAtAGlance response = this.getHearingAtAGlanceService.getHearingAtAGlance(CASE_ID);

        // Prosecution Case Id assertion
        assertThat(response.getId(), is(CASE_ID));

        // Defendant Hearing detailss
        assertThat(response.getDefendantHearings().size(), is(2));
        // Defendant 1
        assertThat(response.getDefendantHearings().get(0).getDefendantId(), is(DEFENDANT_ID_1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(0).getDefendantName(), is("John Williams"));

        // Defendant 2
        assertThat(response.getDefendantHearings().get(1).getDefendantId(), is(DEFENDANT_ID_2));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(1).getDefendantName(), is("John Williams"));

        // Hearing level assertions
        assertThat(response.getHearings().size(), is(1));

        final Hearings hearingResponse = response.getHearings().get(0);
        assertThat(hearingResponse.getId(), is(caseHearing.getId()));
        assertThat(hearingResponse.getType().getDescription(), is(caseHearing.getType().getDescription()));
        assertThat(hearingResponse.getJurisdictionType(), is(uk.gov.justice.progression.courts.JurisdictionType.CROWN));
        assertThat(hearingResponse.getHearingListingStatus(), is(uk.gov.justice.progression.courts.HearingListingStatus.HEARING_INITIALISED));

        // Hearing level defendant details
        assertThat(hearingResponse.getDefendants().size(), is(3));
        assertThat(hearingResponse.getDefendants().get(2).getCourtApplications().size(), is(1));
        assertThat(hearingResponse.getDefendants().get(2).getName(), is("FIRST MIDDLE LAST"));

        // Prosecution Case Identifier assertions
        final ProsecutionCaseIdentifier prosecutionCaseIdentifierResponse = response.getProsecutionCaseIdentifier();
        assertThat(prosecutionCaseIdentifierResponse.getCaseURN(), is(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityCode(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityId(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityReference(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));

        // Court Centre assertions
        final CourtCentre courtCentreResponse = hearingResponse.getCourtCentre();
        assertThat(courtCentreResponse.getId(), is(caseHearing.getCourtCentre().getId()));
        assertThat(courtCentreResponse.getRoomId(), is(caseHearing.getCourtCentre().getRoomId()));
        assertThat(courtCentreResponse.getName(), is(caseHearing.getCourtCentre().getName()));
        assertThat(courtCentreResponse.getRoomName(), is(caseHearing.getCourtCentre().getRoomName()));
        assertThat(courtCentreResponse.getWelshName(), is(caseHearing.getCourtCentre().getWelshName()));
        assertThat(courtCentreResponse.getWelshRoomName(), is(caseHearing.getCourtCentre().getWelshRoomName()));

        final List<ProsecutionCounsel> prosecutionCounselsResponse = hearingResponse.getProsecutionCounsels();
        assertThat(prosecutionCounselsResponse.get(0).getFirstName(), is(caseHearing.getProsecutionCounsels().get(0).getFirstName()));

        assertThat(hearingResponse.getHasResultAmended(), is(true));
    }

    @Test
    public void hearingAtAGlanceHearingWithCaseWithTwoDefendantsAndOneApplicationWithOrganisationInSameHearing() {

        ProsecutionCase prosecutionCase = createProsecutionCase(CASE_ID, Arrays.asList(DEFENDANT_ID_1, DEFENDANT_ID_2));
        CourtApplication courtApplication = createCourtApplicationWithOrganisation(APPLICATION_ID, randomUUID());
        Hearing caseHearing = createCaseHearing(prosecutionCase, courtApplication, CASE_HEARING_ID_1);

        ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase);
        HearingEntity caseHearingEntity = createHearingEntity(caseHearing, CASE_HEARING_ID_1, HEARING_INITIALISED);

        List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();

        CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_1, CASE_HEARING_ID_1));
        caseDefendantHearingEntity1.setHearing(caseHearingEntity);

        CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_2, CASE_HEARING_ID_1));
        caseDefendantHearingEntity2.setHearing(caseHearingEntity);

        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity2);

        CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        courtApplicationEntity.setPayload(courtApplication.toString());

        HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(APPLICATION_ID, CASE_HEARING_ID_1));
        hearingApplicationEntity.setHearing(caseHearingEntity);

        when(this.prosecutionCaseRepository.findByCaseId(CASE_ID)).thenReturn(prosecutionCaseEntity);
        when(this.caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(caseDefendantHearingEntities);
        when(this.courtApplicationRepository.findByLinkedCaseId(CASE_ID)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(this.hearingApplicationRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Arrays.asList(hearingApplicationEntity));

        GetHearingsAtAGlance response = this.getHearingAtAGlanceService.getHearingAtAGlance(CASE_ID);

        // Prosecution Case Id assertion
        assertThat(response.getId(), is(CASE_ID));

        // Defendant Hearing details
        assertThat(response.getDefendantHearings().size(), is(2));
        // Defendant 1
        assertThat(response.getDefendantHearings().get(0).getDefendantId(), is(DEFENDANT_ID_1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(0).getDefendantName(), is("John Williams"));

        // Defendant 2
        assertThat(response.getDefendantHearings().get(1).getDefendantId(), is(DEFENDANT_ID_2));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(1).getDefendantName(), is("John Williams"));

        // Hearing level assertions
        assertThat(response.getHearings().size(), is(1));

        final Hearings hearingResponse = response.getHearings().get(0);
        assertThat(hearingResponse.getId(), is(caseHearing.getId()));
        assertThat(hearingResponse.getType().getDescription(), is(caseHearing.getType().getDescription()));
        assertThat(hearingResponse.getJurisdictionType(), is(uk.gov.justice.progression.courts.JurisdictionType.CROWN));
        assertThat(hearingResponse.getHearingListingStatus(), is(uk.gov.justice.progression.courts.HearingListingStatus.HEARING_INITIALISED));

        // Hearing level defendant details
        assertThat(hearingResponse.getDefendants().size(), is(3));
        assertThat(hearingResponse.getDefendants().get(2).getCourtApplications().size(), is(1));
        assertThat(hearingResponse.getDefendants().get(2).getName(), is("Lava Timber Limited"));

        // Prosecution Case Identifier assertions
        final ProsecutionCaseIdentifier prosecutionCaseIdentifierResponse = response.getProsecutionCaseIdentifier();
        assertThat(prosecutionCaseIdentifierResponse.getCaseURN(), is(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityCode(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityId(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityReference(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));

        // Court Centre assertions
        final CourtCentre courtCentreResponse = hearingResponse.getCourtCentre();
        assertThat(courtCentreResponse.getId(), is(caseHearing.getCourtCentre().getId()));
        assertThat(courtCentreResponse.getRoomId(), is(caseHearing.getCourtCentre().getRoomId()));
        assertThat(courtCentreResponse.getName(), is(caseHearing.getCourtCentre().getName()));
        assertThat(courtCentreResponse.getRoomName(), is(caseHearing.getCourtCentre().getRoomName()));
        assertThat(courtCentreResponse.getWelshName(), is(caseHearing.getCourtCentre().getWelshName()));
        assertThat(courtCentreResponse.getWelshRoomName(), is(caseHearing.getCourtCentre().getWelshRoomName()));

        final List<ProsecutionCounsel> prosecutionCounselsResponse = hearingResponse.getProsecutionCounsels();
        assertThat(prosecutionCounselsResponse.get(0).getFirstName(), is(caseHearing.getProsecutionCounsels().get(0).getFirstName()));

        assertThat(hearingResponse.getHasResultAmended(), is(true));
    }

    @Test
    public void hearingAtAGlanceHearingWithCaseWithTwoDefendantsAndOneApplicationWithProsecutingAuthorityInSameHearing() {

        ProsecutionCase prosecutionCase = createProsecutionCase(CASE_ID, Arrays.asList(DEFENDANT_ID_1, DEFENDANT_ID_2));
        CourtApplication courtApplication = createCourtApplicationWithProsecutingAuthority(APPLICATION_ID, randomUUID());
        Hearing caseHearing = createCaseHearing(prosecutionCase, courtApplication, CASE_HEARING_ID_1);

        ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase);
        HearingEntity caseHearingEntity = createHearingEntity(caseHearing, CASE_HEARING_ID_1, HEARING_INITIALISED);

        List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();

        CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_1, CASE_HEARING_ID_1));
        caseDefendantHearingEntity1.setHearing(caseHearingEntity);

        CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_2, CASE_HEARING_ID_1));
        caseDefendantHearingEntity2.setHearing(caseHearingEntity);

        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity2);

        CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        courtApplicationEntity.setPayload(courtApplication.toString());

        HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(APPLICATION_ID, CASE_HEARING_ID_1));
        hearingApplicationEntity.setHearing(caseHearingEntity);

        when(this.prosecutionCaseRepository.findByCaseId(CASE_ID)).thenReturn(prosecutionCaseEntity);
        when(this.caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(caseDefendantHearingEntities);
        when(this.courtApplicationRepository.findByLinkedCaseId(CASE_ID)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(this.hearingApplicationRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Arrays.asList(hearingApplicationEntity));

        GetHearingsAtAGlance response = this.getHearingAtAGlanceService.getHearingAtAGlance(CASE_ID);

        // Prosecution Case Id assertion
        assertThat(response.getId(), is(CASE_ID));

        // Defendant Hearing details
        assertThat(response.getDefendantHearings().size(), is(2));
        // Defendant 1
        assertThat(response.getDefendantHearings().get(0).getDefendantId(), is(DEFENDANT_ID_1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(0).getDefendantName(), is("John Williams"));

        // Defendant 2
        assertThat(response.getDefendantHearings().get(1).getDefendantId(), is(DEFENDANT_ID_2));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(1).getHearingIds().get(0), is(CASE_HEARING_ID_1));
        assertThat(response.getDefendantHearings().get(1).getDefendantName(), is("John Williams"));

        // Hearing level assertions
        assertThat(response.getHearings().size(), is(1));

        final Hearings hearingResponse = response.getHearings().get(0);
        assertThat(hearingResponse.getId(), is(caseHearing.getId()));
        assertThat(hearingResponse.getType().getDescription(), is(caseHearing.getType().getDescription()));
        assertThat(hearingResponse.getJurisdictionType(), is(uk.gov.justice.progression.courts.JurisdictionType.CROWN));
        assertThat(hearingResponse.getHearingListingStatus(), is(uk.gov.justice.progression.courts.HearingListingStatus.HEARING_INITIALISED));

        // Hearing level defendant details
        assertThat(hearingResponse.getDefendants().size(), is(3));
        assertThat(hearingResponse.getDefendants().get(2).getCourtApplications().size(), is(1));
        assertThat(hearingResponse.getDefendants().get(2).getName(), is("TFL"));

        // Prosecution Case Identifier assertions
        final ProsecutionCaseIdentifier prosecutionCaseIdentifierResponse = response.getProsecutionCaseIdentifier();
        assertThat(prosecutionCaseIdentifierResponse.getCaseURN(), is(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityCode(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityId(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityReference(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));

        // Court Centre assertions
        final CourtCentre courtCentreResponse = hearingResponse.getCourtCentre();
        assertThat(courtCentreResponse.getId(), is(caseHearing.getCourtCentre().getId()));
        assertThat(courtCentreResponse.getRoomId(), is(caseHearing.getCourtCentre().getRoomId()));
        assertThat(courtCentreResponse.getName(), is(caseHearing.getCourtCentre().getName()));
        assertThat(courtCentreResponse.getRoomName(), is(caseHearing.getCourtCentre().getRoomName()));
        assertThat(courtCentreResponse.getWelshName(), is(caseHearing.getCourtCentre().getWelshName()));
        assertThat(courtCentreResponse.getWelshRoomName(), is(caseHearing.getCourtCentre().getWelshRoomName()));

        final List<ProsecutionCounsel> prosecutionCounselsResponse = hearingResponse.getProsecutionCounsels();
        assertThat(prosecutionCounselsResponse.get(0).getFirstName(), is(caseHearing.getProsecutionCounsels().get(0).getFirstName()));

        assertThat(hearingResponse.getHasResultAmended(), is(true));
    }

    @Test
    public void shouldGetAllHearingsAssociatedToACase() {

        when(caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(singletonList(caseDefendantHearingEntity));
        when(caseDefendantHearingEntity.getHearing()).thenReturn(hearingEntity);
        when(hearingEntity.getListingStatus()).thenReturn(HearingListingStatus.HEARING_INITIALISED);
        when(hearingEntity.getPayload()).thenReturn(HEARING_PAYLOAD);
        when(caseDefendantHearingEntity.getId()).thenReturn(new CaseDefendantHearingKey(CASE_ID, randomUUID(), randomUUID()));

        List<Hearings> caseHearings = this.getHearingAtAGlanceService.getCaseHearings(CASE_ID);

        assertThat(caseHearings.isEmpty(), is(false));
    }

    @Test
    public void shouldGetAllHearingsAssociatedToACaseWithNoDefendantAge() {

        when(caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(singletonList(caseDefendantHearingEntity));
        when(caseDefendantHearingEntity.getHearing()).thenReturn(hearingEntity);
        when(hearingEntity.getListingStatus()).thenReturn(HearingListingStatus.HEARING_INITIALISED);
        when(hearingEntity.getPayload()).thenReturn(HEARING_PAYLOAD_WITH_NO_HEARING_DAYS);
        when(caseDefendantHearingEntity.getId()).thenReturn(new CaseDefendantHearingKey(CASE_ID, randomUUID(), randomUUID()));

        List<Hearings> caseHearings = this.getHearingAtAGlanceService.getCaseHearings(CASE_ID);
        assertThat(caseHearings.isEmpty(), is(false));
        assertThat(caseHearings.get(0).getDefendants().get(0).getAge(), is (EMPTY_STRING));
    }

    @Test
    public void shouldNotReturnHearingsSentForListing() {

        ProsecutionCase prosecutionCase = createProsecutionCase(CASE_ID, Arrays.asList(DEFENDANT_ID_1, DEFENDANT_ID_2, DEFENDANT_ID_3));
        Hearing caseHearing = createCaseHearing(prosecutionCase, null, CASE_HEARING_ID_1);

        ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase);
        HearingEntity hearingEntity1 = createHearingEntity(caseHearing, CASE_HEARING_ID_1, HEARING_INITIALISED);
        HearingEntity hearingEntity2 = createHearingEntity(caseHearing, CASE_HEARING_ID_2, SENT_FOR_LISTING);
        HearingEntity hearingEntity3 = createHearingEntity(caseHearing, CASE_HEARING_ID_3, HEARING_RESULTED);

        List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();

        CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_1, CASE_HEARING_ID_1));
        caseDefendantHearingEntity1.setHearing(hearingEntity1);

        CaseDefendantHearingEntity caseDefendantHearingEntity2 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity2.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_2, CASE_HEARING_ID_2));
        caseDefendantHearingEntity2.setHearing(hearingEntity2);

        CaseDefendantHearingEntity caseDefendantHearingEntity3 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity3.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID_3, CASE_HEARING_ID_3));
        caseDefendantHearingEntity3.setHearing(hearingEntity3);

        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity2);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity3);

        when(this.prosecutionCaseRepository.findByCaseId(CASE_ID)).thenReturn(prosecutionCaseEntity);
        when(this.caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(caseDefendantHearingEntities);

        GetHearingsAtAGlance response = this.getHearingAtAGlanceService.getHearingAtAGlance(CASE_ID);

        // Prosecution Case Id assertion
        assertThat(response.getId(), is(CASE_ID));

        // Defendant Hearing details
        assertThat(response.getHearings().size(), is(2));
        assertThat(response.getLatestHearingJurisdictionType(), is(LatestHearingJurisdictionType.CROWN));
    }

    private CourtApplication createCourtApplicationWithDefendants(UUID courtApplicationId, UUID defendantId) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withApplicationType("BREACH")
                        .build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName("TFL")
                                .build())
                        .build())
                .withRespondents(createDefendantRespondents(defendantId))
                .withApplicationOutcome(CourtApplicationOutcome.courtApplicationOutcome()
                        .withApplicationOutcomeDate(LocalDate.now())
                        .withApplicationOutcomeType(CourtApplicationOutcomeType.courtApplicationOutcomeType().withDescription("OUT TYPE").build())
                        .build())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .build();
    }

    private CourtApplication createCourtApplicationWithIndividuals(UUID courtApplicationId, UUID personId) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withApplicationType("BREACH")
                        .withIsAppealApplication(true)
                        .build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName("TFL")
                                .build())
                        .build())
                .withRespondents(createIndividualRespondents(personId))
                .withApplicationOutcome(CourtApplicationOutcome.courtApplicationOutcome()
                        .withApplicationOutcomeType(CourtApplicationOutcomeType.courtApplicationOutcomeType().withDescription("OUT TYPE").build())
                        .withApplicationOutcomeDate(LocalDate.now())
                        .build())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .build();
    }

    private CourtApplication createCourtApplicationWithOrganisation(UUID courtApplicationId, UUID organisationId) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withApplicationType("BREACH")
                        .withIsAppealApplication(true)
                        .build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName("TFL")
                                .build())
                        .build())
                .withRespondents(createOrganisationRespondents(organisationId))
                .withApplicationOutcome(CourtApplicationOutcome.courtApplicationOutcome()
                        .withApplicationOutcomeType(CourtApplicationOutcomeType.courtApplicationOutcomeType().withDescription("OUT TYPE").build())
                        .withApplicationOutcomeDate(LocalDate.now())
                        .build())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .build();
    }

    private CourtApplication createCourtApplicationWithProsecutingAuthority(UUID courtApplicationId, UUID organisationId) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType()
                        .withApplicationType("BREACH")
                        .withIsAppealApplication(true)
                        .build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName("TFL")
                                .build())
                        .build())
                .withRespondents(createProsecutingAuthorityRespondents(organisationId))
                .withApplicationOutcome(CourtApplicationOutcome.courtApplicationOutcome()
                        .withApplicationOutcomeType(CourtApplicationOutcomeType.courtApplicationOutcomeType().withDescription("OUT TYPE").build())
                        .withApplicationOutcomeDate(LocalDate.now())
                        .build())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .build();
    }

    private List<CourtApplicationRespondent> createDefendantRespondents(UUID defendantId) {
        return Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withDefendant(createDefendant(DEFENDANT_ID_1))
                        .build())
                .withApplicationResponse(CourtApplicationResponse.courtApplicationResponse()
                        .withApplicationResponseType(CourtApplicationResponseType.courtApplicationResponseType()
                                .withDescription("Sentencing")
                                .build())
                        .withApplicationResponseDate(LocalDate.of(2018, 10, 11))
                        .build())
                .build());
    }

    private List<CourtApplicationRespondent> createIndividualRespondents(UUID personId) {
        return Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                        .withId(personId)
                        .withPersonDetails(Person.person()
                                .withFirstName("FIRST")
                                .withMiddleName("MIDDLE")
                                .withLastName("LAST")
                                .build())
                        .build())
                .build());
    }

    private List<CourtApplicationRespondent> createOrganisationRespondents(UUID organisationId) {
        return Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                        .withId(organisationId)
                        .withOrganisation(Organisation.organisation()
                                .withName("Lava Timber Limited")
                                .build())
                        .build())
                .build());
    }

    private List<CourtApplicationRespondent> createProsecutingAuthorityRespondents(UUID organisationId) {
        return Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                        .withId(organisationId)
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityCode("TFL")
                                .build())
                        .build())
                .build());
    }

    private Hearing createCaseHearing(ProsecutionCase prosecutionCase, CourtApplication courtApplication, UUID hearingId) {
        return Hearing.hearing()
                .withId(hearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Sentencing")
                        .build())
                .withReportingRestrictionReason(RandomGenerator.STRING.next())
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                                .withListedDurationMinutes(20)
                                .withListingSequence(30)
                                .withSittingDay(ZonedDateTimes.fromString("2019-07-20T00:00:00.000Z"))
                                .build(),
                        HearingDay.hearingDay()
                                .withListedDurationMinutes(20)
                                .withListingSequence(30)
                                .withSittingDay(ZonedDateTimes.fromString("2019-07-16T00:00:00.000Z"))
                                .build()
                ))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .withName("name")
                        .withRoomName("room name")
                        .withWelshName("welsh name")
                        .withWelshRoomName("welsh room name")
                        .build())
                .withProsecutionCounsels(Arrays.asList(ProsecutionCounsel.prosecutionCounsel()
                        .withFirstName("first name")
                        .build()))
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .withDefenceCounsels(createDefenceCounsels())
                .withDefendantAttendance(Arrays.asList(DefendantAttendance.defendantAttendance()
                        .withDefendantId(DEFENDANT_ID_1)
                        .build()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(courtApplication != null ? Arrays.asList(courtApplication) : null)
                .build();
    }

    private Hearing createApplicationHearing(CourtApplication courtApplication, UUID applicationHearingId) {
        return Hearing.hearing()
                .withId(applicationHearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Application")
                        .build())
                .withReportingRestrictionReason(RandomGenerator.STRING.next())
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                                .withListedDurationMinutes(20)
                                .withListingSequence(30)
                                .withSittingDay(ZonedDateTimes.fromString("2019-07-22T00:00:00.000Z"))
                                .build(),
                        HearingDay.hearingDay()
                                .withListedDurationMinutes(20)
                                .withListingSequence(30)
                                .withSittingDay(ZonedDateTimes.fromString("2019-07-20T00:00:00.000Z"))
                                .build()
                ))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .withName("name")
                        .withRoomName("room name")
                        .withWelshName("welsh name")
                        .withWelshRoomName("welsh room name")
                        .build())
                .withProsecutionCounsels(Arrays.asList(ProsecutionCounsel.prosecutionCounsel()
                        .withFirstName("first name")
                        .build()))
                .withCourtApplications(Arrays.asList(courtApplication))
                .withDefenceCounsels(createDefenceCounsels())
                .withApplicantCounsels(createApplicantCounsel())
                .withRespondentCounsels(createRespondentCounsel())
                .withDefendantAttendance(Arrays.asList(DefendantAttendance.defendantAttendance()
                        .withDefendantId(DEFENDANT_ID_1)
                        .build()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withIsBoxHearing(true)
                .withCourtApplications(courtApplication != null ? Arrays.asList(courtApplication) : null)
                .build();
    }

    private HearingEntity createHearingEntity(Hearing hearing, UUID hearingId, HearingListingStatus hearingListingStatus) {

        JsonObject hearingJson = objectToJsonObjectConverter.convert(hearing);

        HearingEntity entity = new HearingEntity();
        entity.setHearingId(hearingId);
        entity.setPayload(hearingJson.toString());
        entity.setListingStatus(hearingListingStatus);

        return entity;
    }

    private List<DefenceCounsel> createDefenceCounsels() {
        return Arrays.asList(DefenceCounsel.defenceCounsel()
                .withId(randomUUID())
                .withDefendants(Arrays.asList(DEFENDANT_ID_1))
                .build());
    }

    private ProsecutionCaseEntity createProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {

        JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(CASE_ID);
        prosecutionCaseEntity.setPayload(prosecutionCaseJson.toString());

        return prosecutionCaseEntity;
    }

    private ProsecutionCase createProsecutionCase(UUID caseId, List<UUID> defendantIds) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("TFL")
                        .withCaseURN("xxxxx")
                        .build())
                .withDefendants(createDefendants(defendantIds))
                .build();
    }

    private List<Defendant> createDefendants(List<UUID> defendantIds) {
        return defendantIds.stream().map(this::createDefendant).collect(Collectors.toList());
    }

    private Defendant createDefendant(UUID defendantId) {
        return Defendant.defendant()
                .withId(defendantId)
                .withPersonDefendant(createPersonDefendant())
                .withOffences(createOffences())
                .withLegalAidStatus("Granted")
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withAmendmentDate(LocalDate.now())
                        .build()))
                .build();
    }

    private List<Offence> createOffences() {
        Offence offence = Offence.offence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode(randomUUID().toString())
                .withOffenceTitle("Offence Title")
                .withOffenceTitleWelsh("Offence Title Welsh")
                .withOffenceLegislation("Offence Legislation")
                .withOffenceLegislationWelsh("Offence Legislation Welsh")
                .withWording("Wording")
                .withWordingWelsh("Wording Welsh")
                .withStartDate(LocalDate.of(2018, 01, 01))
                .withEndDate(LocalDate.of(2018, 01, 05))
                .withCount(5)
                .withConvictionDate(LocalDate.of(2018, 02, 02))
                .withLaaApplnReference(LaaReference.laaReference()
                        .withStatusCode(RandomGenerator.STRING.next())
                        .withStatusId(LAA_STATUS_ID)
                        .withStatusDescription(RandomGenerator.STRING.next())
                        .withStatusDate(LocalDate.now())
                        .withApplicationReference(RandomGenerator.STRING.next())
                        .build())
                .build();
        return Arrays.asList(offence);
    }

    private PersonDefendant createPersonDefendant() {
        return PersonDefendant.personDefendant()
                .withPersonDetails(Person.person()
                        .withFirstName("John")
                        .withLastName("Williams")
                        .withDateOfBirth(LocalDate.of(2003, 07, 20))
                        .build())
                .build();
    }

    private List<ApplicantCounsel> createApplicantCounsel() {
        return Arrays.asList(ApplicantCounsel.applicantCounsel()
                .withId(GENERIC_UUID)
                .withTitle("Applicant Counsel Title")
                .withFirstName("Applicant Counsel First Name")
                .withMiddleName("Applicant Counsel Middle Name")
                .withLastName("Applicant Counsel Last Name")
                .withStatus("Applicant Counsel Status")
                .withApplicants(Arrays.asList(DEFENDANT_ID_1))
                .withAttendanceDays(Arrays.asList(LOCALDATE_NOW)
                )
                .build());
    }

    private List<RespondentCounsel> createRespondentCounsel() {
        return Arrays.asList(RespondentCounsel.respondentCounsel()
                .withId(GENERIC_UUID)
                .withTitle("Respondent Counsel Title")
                .withFirstName("Respondent Counsel First Name")
                .withMiddleName("Respondent Counsel Middle Name")
                .withLastName("Respondent Counsel Last Name")
                .withStatus("Respondent Counsel Status")
                .withRespondents(Arrays.asList(DEFENDANT_ID_1))
                .withAttendanceDays(Arrays.asList(LOCALDATE_NOW)
                )
                .build());
    }

}
