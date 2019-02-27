package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class GetCaseAtAGlanceServiceTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @InjectMocks
    private GetCaseAtAGlanceService getCaseAtAGlanceService;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void getCaseAtAGlance() {

        ProsecutionCase prosecutionCase = createProsecutionCase(CASE_ID);
        Hearing hearing = createHearing(prosecutionCase);

        ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase);
        HearingEntity hearingEntity = createHearingEntity(hearing);

        List<CaseDefendantHearingEntity> caseDefendantHearingEntities = new ArrayList<>();

        CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID, HEARING_ID));
        caseDefendantHearingEntity.setHearing(hearingEntity);

        CaseDefendantHearingEntity caseDefendantHearingEntity1 = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity1.setId(new CaseDefendantHearingKey(CASE_ID, UUID.randomUUID(), HEARING_ID));
        caseDefendantHearingEntity1.setHearing(hearingEntity);

        caseDefendantHearingEntities.add(caseDefendantHearingEntity);
        caseDefendantHearingEntities.add(caseDefendantHearingEntity1);

        when(this.prosecutionCaseRepository.findByCaseId(CASE_ID)).thenReturn(prosecutionCaseEntity);
        when(this.caseDefendantHearingRepository.findByCaseId(CASE_ID)).thenReturn(caseDefendantHearingEntities);

        GetCaseAtAGlance response = this.getCaseAtAGlanceService.getCaseAtAGlance(CASE_ID);

        // Prosecution Case Id assertion
        assertThat(response.getId(), is(CASE_ID));

        // Defendant Hearing details
        assertThat(response.getDefendantHearings().size(), is(1));
        assertThat(response.getDefendantHearings().get(0).getDefendantId(), is (DEFENDANT_ID));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().size(), is(1));
        assertThat(response.getDefendantHearings().get(0).getHearingIds().get(0), is(HEARING_ID));
        assertThat(response.getDefendantHearings().get(0).getDefendantName(), is ("John Martin Williams"));

        // Hearing level assertions
        assertThat(response.getHearings().size(), is(1));

        final Hearings hearingResponse = response.getHearings().get(0);
        assertThat(hearingResponse.getId(), is(hearing.getId()));
        assertThat(hearingResponse.getType(), is(hearing.getType().getDescription()));
        assertThat(hearingResponse.getJurisdictionType(), is(uk.gov.justice.progression.courts.JurisdictionType.CROWN));
        assertThat(hearingResponse.getHearingListingStatus(), is(uk.gov.justice.progression.courts.HearingListingStatus.HEARING_INITIALISED));

        // Prosecution Case Identifier assertions
        final ProsecutionCaseIdentifier prosecutionCaseIdentifierResponse = response.getProsecutionCaseIdentifier();
        assertThat(prosecutionCaseIdentifierResponse.getCaseURN(), is(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityCode(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityId(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(prosecutionCaseIdentifierResponse.getProsecutionAuthorityReference(), is(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));

        // Court Centre assertions
        final CourtCentre courtCentreResponse = hearingResponse.getCourtCentre();
        assertThat(courtCentreResponse.getId(), is(hearing.getCourtCentre().getId()));
        assertThat(courtCentreResponse.getRoomId(), is(hearing.getCourtCentre().getRoomId()));
        assertThat(courtCentreResponse.getName(), is(hearing.getCourtCentre().getName()));
        assertThat(courtCentreResponse.getRoomName(), is(hearing.getCourtCentre().getRoomName()));
        assertThat(courtCentreResponse.getWelshName(), is(hearing.getCourtCentre().getWelshName()));
        assertThat(courtCentreResponse.getWelshRoomName(), is(hearing.getCourtCentre().getWelshRoomName()));

        final List<ProsecutionCounsel> prosecutionCounselsResponse = hearingResponse.getProsecutionCounsels();
        assertThat(prosecutionCounselsResponse.get(0).getFirstName(), is(hearing.getProsecutionCounsels().get(0).getFirstName()));
    }

    private Hearing createHearing(ProsecutionCase prosecutionCase) {
        return Hearing.hearing()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Sentencing")
                        .build())
                .withReportingRestrictionReason(RandomGenerator.STRING.next())
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withListedDurationMinutes(20)
                        .withListingSequence(30)
                        .withSittingDay(ZonedDateTime.now())
                        .build()))
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
                        .withDefendantId(DEFENDANT_ID)
                        .build()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();
    }

    private HearingEntity createHearingEntity(Hearing hearing) {

        JsonObject hearingJson = objectToJsonObjectConverter.convert(hearing);

        HearingEntity entity = new HearingEntity();
        entity.setHearingId(HEARING_ID);
        entity.setPayload(hearingJson.toString());
        entity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        return entity;
    }

    private List<DefenceCounsel> createDefenceCounsels() {
        return Arrays.asList(DefenceCounsel.defenceCounsel()
                .withId(randomUUID())
                .withDefendants(Arrays.asList(DEFENDANT_ID))
                .build());
    }

    private ProsecutionCaseEntity createProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {

        JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(CASE_ID);
        prosecutionCaseEntity.setPayload(prosecutionCaseJson.toString());

        return prosecutionCaseEntity;
    }

    private ProsecutionCase createProsecutionCase(UUID caseId) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("TFL")
                        .withCaseURN("xxxxx")
                        .build())
                .withDefendants(createDefendants())
                .build();
    }

    private List<Defendant> createDefendants() {
        List<Defendant> defendants = new ArrayList<>();
        Defendant defendant = Defendant.defendant()
                .withId(DEFENDANT_ID)
                .withPersonDefendant(createPersonDefendant())
                .withOffences(createOffences())
                .build();
        defendants.add(defendant);
        return defendants;
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
                .withStartDate("2018-01-01")
                .withEndDate("2018-01-05")
                .withCount(5)
                .withConvictionDate("2018-02-02")
                .build();
        return Arrays.asList(offence);
    }

    private PersonDefendant createPersonDefendant() {
        return PersonDefendant.personDefendant()
                .withPersonDetails(Person.person()
                        .withFirstName("John")
                        .withMiddleName("Martin")
                        .withLastName("Williams")
                        .withDateOfBirth("2017-02-01")
                        .build())
                .build();
    }
}
