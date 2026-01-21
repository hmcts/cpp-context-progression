package uk.gov.moj.cpp.progression.query.view;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.HearingListingStatus.HEARING_RESULTED;
import static uk.gov.justice.progression.courts.GetHearingsAtAGlance.getHearingsAtAGlance;
import static uk.gov.justice.progression.courts.Hearings.hearings;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.query.utils.SearchQueryUtils.prepareSearch;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.progression.courts.CaagDefendants;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.query.CotrDefendant;
import uk.gov.justice.progression.query.CotrDetail;
import uk.gov.justice.progression.query.DefenceAdditionalInfo;
import uk.gov.justice.progression.query.TrialDefendants;
import uk.gov.justice.progression.query.TrialHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.utils.ResultTextFlagBuilder;
import uk.gov.moj.cpp.progression.query.view.service.CotrQueryService;
import uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseCpsProsecutorEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseCpsProsecutorRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseQueryViewTest {

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final String APPLICATION_ARN = new StringGenerator().next();
    private static final String APPLICANT_FIRST_NAME = new StringGenerator().next();
    private static final String APPLICANT_LAST_NAME = new StringGenerator().next();
    private static final String APPLICANT_MIDDLE_NAME = new StringGenerator().next();
    private static final String RESPONDENTS_ORG_NAME = new StringGenerator().next();
    private static final String APPLICATION_PROSECUTOR_NAME = new StringGenerator().next();
    private static final String PROGRESSION_QUERY_PROSECUTIONCASE_CAAG = "progression.query.prosecutioncase.caag";
    private static final String PROGRESSION_QUERY_CASEHEARINGS = "progression.query.casehearings";
    private static final String PROGRESSION_QUERY_CASEHEARINGS_FOR_COURT_EXTRACT = "progression.query.case-hearings-for-court-extract";
    private static final String PROGRESSION_QUERY_CASEDEFENDANTHEARINGS = "progression.query.case-defendant-hearings";
    private static final String PROGRESSION_QUERY_CASE_HEARING_TYPES = "progression.query.case.hearingtypes";

    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final Logger LOGGER = getLogger(ProsecutionCaseQueryViewTest.class);
    private static final String LABEL1 = "label 1";
    private static final String LABEL2 = "label 2";
    private static final String LABEL3 = "label 3";
    private static final String LABEL4 = "label 4";

    private static final UUID CASE_ID1 = UUID.fromString("ad778633-5df9-4f15-881b-9bdde7c71b8b");
    private static final UUID CASE_ID2 = UUID.fromString("b4f8f8b3-6c3d-495c-ad01-c1b8a1349d5a");
    private static final UUID CASE_ID3 = UUID.fromString("b4f8f8b3-6c3d-495c-ad01-c1b8a1349d5b");
    private static final UUID MASTER_DEFENDANT_ID1 = UUID.fromString("2cb9ff20-9197-473f-a6c0-cfbea6a8480a");
    private static final UUID MASTER_DEFENDANT_ID2 = UUID.fromString("2cb9ff20-9197-473f-a6c0-cfbea6a8480b");
    private static final UUID MASTER_DEFENDANT_ID3 = UUID.fromString("2cb9ff20-9197-473f-a6c0-cfbea6a8480c");
    private static final UUID OFFENCE_ID1 = UUID.fromString("31ef1687-98f2-4185-a8a0-2b46b198000b");
    private static final UUID OFFENCE_ID2 = UUID.fromString("8fbfc6bb-55a3-45b6-a5a6-7a66de3e566f");
    private static final UUID OFFENCE_ID3 = UUID.fromString("37adb016-747c-41ab-9217-b286e32bf7ad");
    private static final UUID OFFENCE_ID4 = UUID.fromString("37adb016-747c-41ab-9217-b286e32bf7ae");
    private static final UUID OFFENCE_ID5 = UUID.fromString("37adb016-747c-41ab-9217-b286e32bf7af");
    private static final UUID OFFENCE_ID6 = UUID.fromString("1fee4471-34aa-4594-8759-77624fca1cad");
    public static final String HEARING_TYPES = "hearingTypes";
    public static final String HEARING_ID = "hearingId";
    public static final String TYPE = "type";
    public static final String FIRST_HEARING_TYPE_DESCRIPTION = "First Hearing";
    public static final String SECOND_HEARING_TYPE_DESCRIPTION = "Second hearing";
    public static final String BAIL_HEARING_TYPE_DESCRIPTION = "Bail Variation Application";
    public static final String PLEAS_HEARING_TYPE_DESCRIPTION = "Plea and Trial Preparation";
    private static final String CASE_ALL_HEARINGS_QUERY = "progression.query.case.allhearings";
    private static final String CASE_ALL_HEARINGS_QUERY_VIEW_JSON = "progression.query.case.hearingsatglance.json";

    private static final UUID INACTIVE_APPLICATION_ID = UUID.randomUUID();

    private final Enveloper enveloper = createEnveloper();
    @Mock
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;
    @Mock
    private SearchProsecutionCaseRepository searchCaseRepository;
    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;
    @Mock
    private CivilFeeRepository civilFeeRepository;
    @Mock
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;
    @Mock
    private CourtDocumentRepository courtDocumentRepository;
    @Mock
    private JsonObject jsonObject;
    @Mock
    private JsonObject prosecutionCaseJson1;
    @Mock
    private JsonObject prosecutionCaseJson2;
    @Mock
    private JsonObject prosecutionCaseJson3;
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Spy
    private ListToJsonArrayConverter<CaagDefendants> listToJsonArrayConverter;
    @Spy
    private ListToJsonArrayConverter<Hearings> hearingListToJsonArrayConverter;
    @InjectMocks
    private ProsecutionCaseQuery prosecutionCaseQuery;
    @Mock
    private CourtApplicationCaseRepository courtApplicationCaseRepository;
    @Mock
    private HearingApplicationRepository hearingApplicationRepository;
    @Mock
    private HearingAtAGlanceService hearingAtAGlanceService;
    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private CaseCpsProsecutorRepository caseCpsProsecutorRepository;
    @Spy
    private ListToJsonArrayConverter jsonConverter;
    @Mock
    private CotrQueryService cotrQueryService;
    @Mock
    private GetHearingsAtAGlance hearingsAtAGlance;

    public ProsecutionCaseQueryViewTest() {
    }

    @Spy
    private ResultTextFlagBuilder resultTextFlagBuilder;
    @Mock
    private RelatedReferenceRepository relatedReferenceRepository;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.listToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField(this.hearingListToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.hearingListToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }

    @Test
    public void shouldFindProsecutionCaseById() {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final GetHearingsAtAGlance getCaseAtAGlance = getHearingsAtAGlance()
                .withHearings(asList(hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(new ArrayList<>());
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
    }

    @Test
    public void shouldNotReturnAnyRelatedCaseWhenThereIsNoRelatedCase() {
        final UUID caseId = randomUUID();
        final UUID relatedCaseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                .withId(offenceId)
                .build());
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .withOffences(offences)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .withCaseStatus("ACTIVE")
                .withId(relatedCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("CaseURN").build())
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final GetHearingsAtAGlance getCaseAtAGlance = getHearingsAtAGlance()
                .withHearings(asList(hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList = new ArrayList<>();
        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = new MatchDefendantCaseHearingEntity();
        matchDefendantCaseHearingEntity.setMasterDefendantId(masterDefendantId);
        matchDefendantCaseHearingEntity.setProsecutionCase(prosecutionCaseEntity);
        matchDefendantCaseHearingEntity.setProsecutionCaseId(caseId);
        matchDefendantCaseHearingEntityList.add(matchDefendantCaseHearingEntity);

        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(matchDefendantCaseHearingEntityList);
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
    }

    @Test
    public void shouldAddOldProsecutionAuthorityCode() {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID defendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();


        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .withId(defendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .withProsecutionCaseIdentifier(new ProsecutionCaseIdentifier.Builder().build())
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());


        final GetHearingsAtAGlance getCaseAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance()
                .withHearings(asList(Hearings.hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withId(randomUUID())
                .build();

        final CaseCpsProsecutorEntity caseCpsProsecutorEntity = new CaseCpsProsecutorEntity();
        caseCpsProsecutorEntity.setOldCpsProsecutor("OLDCPSPROSECUTOR");

        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(new ArrayList<>());
        when(caseCpsProsecutorRepository.findBy(caseId)).thenReturn(caseCpsProsecutorEntity);
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        when(hearingAtAGlanceService.getCaseHearings(any(UUID.class))).thenReturn(getHearingsList(masterDefendantId, defendantId));

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
        assertThat(response.payloadAsJsonObject().getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("oldProsecutionAuthorityCode"), is("OLDCPSPROSECUTOR"));
        assertThat(response.payloadAsJsonObject().getJsonObject("hearingsAtAGlance").getJsonObject("prosecutionCaseIdentifier").getString("oldProsecutionAuthorityCode"), is("OLDCPSPROSECUTOR"));
    }

    @Test
    public void shouldFindProsecutionCaseAndRelatedCasesByIdWithMultipleDefendants() {

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", CASE_ID1.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final GetHearingsAtAGlance getCaseAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance()
                .withHearings(asList(Hearings.hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList = buildMatchDefendantCaseHearingEntities();

        when(prosecutionCaseRepository.findByCaseId(CASE_ID1)).thenReturn(matchDefendantCaseHearingEntityList.get(0).getProsecutionCase());
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(matchDefendantCaseHearingEntityList);
        when(hearingAtAGlanceService.getHearingAtAGlance(CASE_ID1)).thenReturn(getCaseAtAGlance);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());

        final JsonArray relatedCases = response.payloadAsJsonObject().getJsonArray("relatedCases");
        assertThat(relatedCases.size(), is(2));

        assertThat(relatedCases.getJsonObject(0).getJsonArray("cases").size(), is(1));
        assertThat(relatedCases.getJsonObject(1).getJsonArray("cases").size(), is(1));

        with(relatedCases.toString())
                .assertThat("$.[*].masterDefendantId", hasItems(MASTER_DEFENDANT_ID1.toString(), MASTER_DEFENDANT_ID2.toString()));
        with(relatedCases.toString())
                .assertThat("$.[*].cases[*].caseId", hasItems(CASE_ID2.toString(), CASE_ID3.toString()));
        with(relatedCases.toString())
                .assertThat("$.[*].cases[*].prosecutionCaseIdentifier.caseURN", hasItems("CaseURN2", "CaseURN3"));
        with(relatedCases.toString())
                .assertThat("$.[*].cases[*].caseStatus", hasItems("ACTIVE2", "ACTIVE3"));
        with(relatedCases.toString())
                .assertThat("$.[*].cases[*].offences[*].id", hasItems(OFFENCE_ID2.toString(), OFFENCE_ID4.toString()));

    }

    @Test
    public void shouldNotFindRelatedCaseIfThereIsNoOffenceForMasterDefendant() {
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", CASE_ID1.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final GetHearingsAtAGlance getCaseAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance()
                .withHearings(asList(Hearings.hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();


        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList = buildMatchDefendantCaseHearingEntitiesWithNoOffence();

        when(prosecutionCaseRepository.findByCaseId(CASE_ID1)).thenReturn(matchDefendantCaseHearingEntityList.get(0).getProsecutionCase());
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(matchDefendantCaseHearingEntityList);
        when(hearingAtAGlanceService.getHearingAtAGlance(CASE_ID1)).thenReturn(getCaseAtAGlance);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());

        final JsonArray relatedCases = response.payloadAsJsonObject().getJsonArray("relatedCases");
        assertThat(relatedCases.size(), is(1));

        assertThat(relatedCases.getJsonObject(0).size(), is(0));

    }

    @Test
    public void shouldFindCaseAtAGlanceGivenProsecutionCaseWithUrn() {
        final UUID masterDefendantId = randomUUID();
        final UUID defendantId = randomUUID();
        final String caseURN = "05PP1000915";
        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseId(PROGRESSION_QUERY_PROSECUTIONCASE_CAAG, randomUUID());
        final String caseId = envelopeWithCaseId.payloadAsJsonObject().getString("caseId");
        final Defendant defendant = defendant().withId(defendantId).withMasterDefendantId(masterDefendantId).build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(fromString(caseId));
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(getProsecutionCase(caseURN, defendant)).toString());

        final CourtApplication courtApplication = getCourtApplicationWithLegalEntityDefendant(defendantId, randomUUID(), masterDefendantId);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplication).toString());

        final CourtApplicationCaseEntity courtApplicationCaseEntity = new CourtApplicationCaseEntity();
        courtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), randomUUID(), UUID.fromString(caseId)));
        courtApplicationCaseEntity.setCourtApplication(courtApplicationEntity);

        when(courtApplicationCaseRepository.findByCaseId(fromString(caseId))).thenReturn(asList(courtApplicationCaseEntity));
        when(prosecutionCaseRepository.findByCaseId(any(UUID.class))).thenReturn(prosecutionCaseEntity);
        when(referenceDataService.getProsecutor(anyString())).thenReturn(Optional.empty());
        when(hearingAtAGlanceService.getCaseHearings(any(UUID.class))).thenReturn(getHearingsList(masterDefendantId, defendantId));
        final RelatedReference relatedReference = new RelatedReference();
        relatedReference.setProsecutionCaseId(UUID.fromString(caseId));
        relatedReference.setReference("testReference");
        relatedReference.setId(UUID.randomUUID());

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(envelopeWithCaseId);
        assertThat(response.payloadAsJsonObject().getString("caseId"), is(caseId));
        assertThat(response.payloadAsJsonObject().getJsonObject("caseDetails").getString("caseURN"), is(caseURN));
        assertThat(response.payloadAsJsonObject().getJsonObject("caseDetails").getJsonArray("caseMarkers"), nullValue());
        assertThat(response.payloadAsJsonObject().getJsonArray("defendants").getJsonObject(0).getString("updatedOn"), is(notNullValue()));

        final JsonArray defendants = response.payloadAsJsonObject().getJsonArray("defendants");
        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(1));
        final JsonArray defendantJudicialResults = defendants.getJsonObject(0).getJsonArray("defendantJudicialResults");
        assertThat(defendantJudicialResults.size(), is(4));
        assertThat(defendantJudicialResults.getJsonObject(0).getJsonString("label").getString(), is(LABEL1));
        assertThat(defendantJudicialResults.getJsonObject(1).getJsonString("label").getString(), is(LABEL2));
        assertThat(defendantJudicialResults.getJsonObject(0).getBoolean("useResultText"), is(true));
        assertThat(defendantJudicialResults.getJsonObject(1).getBoolean("useResultText"), is(false));
        assertThat(defendantJudicialResults.getJsonObject(2).getJsonString("label").getString(), is(LABEL3));
        assertThat(defendantJudicialResults.getJsonObject(3).getJsonString("label").getString(), is(LABEL4));
    }

    @Test
    public void shouldFindApplicationsLinkedToCaseAtAGlanceProsecutionCase() {
        final String caseURN = "05PP1000915";
        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseId(PROGRESSION_QUERY_PROSECUTIONCASE_CAAG, randomUUID());
        final String caseId = envelopeWithCaseId.payloadAsJsonObject().getString("caseId");
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(fromString(caseId));
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(getProsecutionCase(caseURN, defendant().withMasterDefendantId(randomUUID())
                .withId(randomUUID()).build())).toString());
        final CourtApplication courtApplication = getCourtApplicationWithLegalEntityDefendant(randomUUID(), randomUUID(), randomUUID());
        final CourtApplicationEntity parentCourtApplicationEntity = new CourtApplicationEntity();
        parentCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplication).toString());
        final CourtApplicationCaseEntity parentCourtApplicationCaseEntity = new CourtApplicationCaseEntity();
        parentCourtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), randomUUID(), UUID.fromString(caseId)));
        parentCourtApplicationCaseEntity.setCourtApplication(parentCourtApplicationEntity);

        final CourtApplicationEntity childCourtApplicationEntity = new CourtApplicationEntity();
        childCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplication).toString());

        final CourtApplicationCaseEntity childCourtApplicationCaseEntity = new CourtApplicationCaseEntity();
        childCourtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), randomUUID(), UUID.fromString(caseId)));
        childCourtApplicationCaseEntity.setCourtApplication(childCourtApplicationEntity);

        when(prosecutionCaseRepository.findByCaseId(any(UUID.class))).thenReturn(prosecutionCaseEntity);
        when(courtApplicationCaseRepository.findByCaseId(fromString(caseId))).thenReturn(asList(parentCourtApplicationCaseEntity, childCourtApplicationCaseEntity));
        when(referenceDataService.getProsecutor(anyString())).thenReturn(Optional.empty());
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(envelopeWithCaseId);
        assertThat(response.payloadAsJsonObject().get("linkedApplications"), notNullValue());
        assertThat(response.payloadAsJsonObject().getJsonArray("linkedApplications").getJsonObject(0).getString("applicationId"), is(APPLICATION_ID.toString()));
        assertThat(response.payloadAsJsonObject().getJsonArray("defendants").getJsonObject(0).containsKey("updatedOn"),is(false));

    }

    @Test
    public void shouldAddOldProsecutionAuthorityCodeToCaseAtAGlanceProsecutionCase() {
        final UUID applicantId = randomUUID();
        final UUID respondentId = randomUUID();
        final JsonEnvelope jsonEnvelope = buildEnvelope(PROGRESSION_QUERY_PROSECUTIONCASE_CAAG, "progression.query.prosecutioncase.caag.with.urn.json");
        final JsonObject prosecutionCaseEntityJson = jsonEnvelope.payloadAsJsonObject().getJsonObject(PROSECUTION_CASE);

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseId(PROGRESSION_QUERY_PROSECUTIONCASE_CAAG, randomUUID());
        final String caseId = envelopeWithCaseId.payloadAsJsonObject().getString("caseId");

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(fromString(caseId));
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(
                getProsecutionCase(prosecutionCaseEntityJson.getJsonObject("prosecutionCaseIdentifier").getString("caseURN"),
                        defendant().withMasterDefendantId(applicantId).withId(applicantId).build(),
                        defendant().withMasterDefendantId(randomUUID()).withId(respondentId).build())).toString());

        final CourtApplication courtApplication = getCourtApplicationWithLegalEntityDefendant(applicantId, respondentId, applicantId);

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplication).toString());

        final CourtApplicationCaseEntity courtApplicationCaseEntity = new CourtApplicationCaseEntity();
        courtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), randomUUID(), UUID.fromString(caseId)));
        courtApplicationCaseEntity.setCourtApplication(courtApplicationEntity);

        final CaseCpsProsecutorEntity caseCpsProsecutorEntity = new CaseCpsProsecutorEntity();
        caseCpsProsecutorEntity.setOldCpsProsecutor("OLDCPSPROSECUTOR");


        when(prosecutionCaseRepository.findByCaseId(any(UUID.class))).thenReturn(prosecutionCaseEntity);
        when(caseCpsProsecutorRepository.findBy(fromString(caseId))).thenReturn(caseCpsProsecutorEntity);
        when(courtApplicationCaseRepository.findByCaseId(fromString(caseId))).thenReturn(asList(courtApplicationCaseEntity));
        when(referenceDataService.getProsecutor(anyString())).thenReturn(Optional.empty());

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(envelopeWithCaseId);

        assertThat(response.payloadAsJsonObject().getJsonObject("prosecutorDetails").getString("oldProsecutionAuthorityCode"), is("OLDCPSPROSECUTOR"));
        assertThat(response.payloadAsJsonObject().getJsonArray("defendants").getJsonObject(0).getString("updatedOn"), is(notNullValue()));
    }

    @Test
    public void shouldFindCaseById() {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.case").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());


        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        final JsonEnvelope response = prosecutionCaseQuery.getCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
    }

    @Test
    public void shouldReturnCaseHearings() {
        final UUID caseId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID courtCentreId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID courtCentreId2 = randomUUID();
        final ZonedDateTime sittingDay1 = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay2 = ZonedDateTimes.fromString("2020-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay3 = ZonedDateTimes.fromString("2021-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay4 = ZonedDateTimes.fromString("2020-05-30T18:32:04.238Z");


        when(hearingAtAGlanceService.getCaseHearings(caseId)).thenReturn(asList(
                createCaseHearing(hearingId1, courtCentreId1, sittingDay1, sittingDay2),
                createCaseHearing(hearingId2, courtCentreId2, sittingDay3, sittingDay4)));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseId(PROGRESSION_QUERY_CASEHEARINGS, caseId);
        final JsonEnvelope response = prosecutionCaseQuery.getCaseHearings(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();
        final JsonArray hearings = payload.getJsonArray("hearings");
        assertThat(hearings.size(), is(2));

        final Optional<JsonObject> hearing1 = hearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId1.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(hearing1.isPresent(), is(true));
        final String actualCourtCentre1 = hearing1.get().getJsonObject("courtCentre").getString("id");
        assertThat(actualCourtCentre1, is(courtCentreId1.toString()));

        final Optional<JsonObject> hearing2 = hearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId2.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(hearing2.isPresent(), is(true));
        final String actualCourtCentre2 = hearing2.get().getJsonObject("courtCentre").getString("id");
        assertThat(actualCourtCentre2, is(courtCentreId2.toString()));
    }

    @Test
    public void shouldReturnCaseHearingsForCourtExtract() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID courtCentreId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID courtCentreId2 = randomUUID();
        final ZonedDateTime sittingDay1 = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay2 = ZonedDateTimes.fromString("2020-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay3 = ZonedDateTimes.fromString("2021-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay4 = ZonedDateTimes.fromString("2020-05-30T18:32:04.238Z");

        when(hearingAtAGlanceService.getCaseHearingsForCourtExtract(caseId, defendantId)).thenReturn(asList(
                createCaseHearing(hearingId1, courtCentreId1, sittingDay1, sittingDay2),
                createCaseHearing(hearingId2, courtCentreId2, sittingDay3, sittingDay4)));

        when(hearingAtAGlanceService.getApplicationHearingsForCourtExtract(caseId, defendantId)).thenReturn(Map.of(
                        CourtApplication.courtApplication().withId(applicationId)
                                .withType(CourtApplicationType.courtApplicationType().withType("Application title").build())
                                .build(),
                        asList(
                                createCaseHearing(hearingId1, courtCentreId1, sittingDay1, sittingDay2),
                                createCaseHearing(hearingId2, courtCentreId2, sittingDay3, sittingDay4))
                )
        );
        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseIdAndDefendantId(PROGRESSION_QUERY_CASEHEARINGS_FOR_COURT_EXTRACT, caseId, defendantId);

        final JsonEnvelope response = prosecutionCaseQuery.getCaseHearingsForCourtExtract(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();

        final JsonArray hearings = payload.getJsonArray("caseHearings");
        assertThat(hearings.size(), is(2));

        final JsonArray applicationHearings = payload.getJsonArray("linkedApplicationHearings");
        assertThat(applicationHearings.size(), is(2));
        assertThat(applicationHearings.get(0).asJsonObject().getString("id"), is(applicationId.toString()));
        assertThat(applicationHearings.get(0).asJsonObject().getString("title"), is("Application title"));

        final Optional<JsonObject> hearing1 = applicationHearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId1.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(hearing1.isPresent(), is(true));
        final String actualCourtCentre1 = hearing1.get().getJsonObject("courtCentre").getString("id");
        assertThat(actualCourtCentre1, is(courtCentreId1.toString()));

        final Optional<JsonObject> hearing2 = applicationHearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId2.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(hearing2.isPresent(), is(true));
        final String actualCourtCentre2 = hearing2.get().getJsonObject("courtCentre").getString("id");
        assertThat(actualCourtCentre2, is(courtCentreId2.toString()));
    }

    @Test
    public void shouldSortCaseHearingsAndApplicationsHearingsForCourtExtract() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID applicationId2 = randomUUID();
        final UUID applicationId3 = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID hearingId3 = randomUUID();
        final UUID courtCentreId1 = randomUUID();
        final ZonedDateTime sittingDay1 = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay2 = ZonedDateTimes.fromString("2020-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay3 = ZonedDateTimes.fromString("2021-05-30T18:32:04.238Z");

        when(hearingAtAGlanceService.getCaseHearingsForCourtExtract(caseId, defendantId)).thenReturn(asList(
                createCaseHearing(hearingId1, courtCentreId1, sittingDay1, sittingDay2)));

        when(hearingAtAGlanceService.getApplicationHearingsForCourtExtract(caseId, defendantId)).thenReturn(Map.of(
                        CourtApplication.courtApplication().withId(applicationId2)
                                .withType(CourtApplicationType.courtApplicationType().withType("Application title 2").build())
                                .build(),
                        asList(
                                createCaseHearing(hearingId2, courtCentreId1, sittingDay2)),

                        CourtApplication.courtApplication().withId(applicationId)
                                .withType(CourtApplicationType.courtApplicationType().withType("Application title 1").build())
                                .build(),
                        asList(
                                createCaseHearing(hearingId1, courtCentreId1, sittingDay1)),
                    CourtApplication.courtApplication().withId(applicationId3)
                            .withType(CourtApplicationType.courtApplicationType().withType("Application title 3").build())
                            .build(),
                    asList(
                            createCaseHearing(hearingId3, courtCentreId1, sittingDay3))

        ));
        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseIdAndDefendantId(PROGRESSION_QUERY_CASEHEARINGS_FOR_COURT_EXTRACT, caseId, defendantId);

        final JsonEnvelope response = prosecutionCaseQuery.getCaseHearingsForCourtExtract(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();

        final JsonArray hearings = payload.getJsonArray("caseHearings");
        assertThat(hearings.size(), is(1));

        final JsonArray applicationHearings = payload.getJsonArray("linkedApplicationHearings");
        assertThat(applicationHearings.size(), is(3));
        assertThat(applicationHearings.get(0).asJsonObject().getString("id"), is(applicationId.toString()));
        assertThat(applicationHearings.get(0).asJsonObject().getString("title"), is("Application title 1"));
        assertThat(applicationHearings.get(1).asJsonObject().getString("id"), is(applicationId2.toString()));
        assertThat(applicationHearings.get(1).asJsonObject().getString("title"), is("Application title 2"));
        assertThat(applicationHearings.get(2).asJsonObject().getString("id"), is(applicationId3.toString()));
        assertThat(applicationHearings.get(2).asJsonObject().getString("title"), is("Application title 3"));

    }

    @Test
    void shouldNotReturnBoxWorkHearingsForCourtExtract() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID courtCentreId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID courtCentreId2 = randomUUID();
        final ZonedDateTime sittingDay1 = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay2 = ZonedDateTimes.fromString("2020-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay3 = ZonedDateTimes.fromString("2021-05-30T18:32:04.238Z");
        final ZonedDateTime sittingDay4 = ZonedDateTimes.fromString("2020-05-30T18:32:04.238Z");

        when(hearingAtAGlanceService.getCaseHearingsForCourtExtract(caseId, defendantId)).thenReturn(asList(
                createCaseHearing(hearingId1, courtCentreId1, sittingDay1, sittingDay2),
                createBoxWorkHearing(hearingId2, courtCentreId2, sittingDay3, sittingDay4),
                createCaseHearing(hearingId2, courtCentreId2, sittingDay3, sittingDay4)));

        when(hearingAtAGlanceService.getApplicationHearingsForCourtExtract(caseId, defendantId)).thenReturn(Map.of(
                        CourtApplication.courtApplication().withId(applicationId)
                                .withType(CourtApplicationType.courtApplicationType().withType("Application title").build())
                                .build(),
                        asList(
                                createCaseHearing(hearingId1, courtCentreId1, sittingDay1, sittingDay2),
                                createBoxWorkHearing(hearingId2, courtCentreId2, sittingDay3, sittingDay4))
                )
        );
        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseIdAndDefendantId(PROGRESSION_QUERY_CASEHEARINGS_FOR_COURT_EXTRACT, caseId, defendantId);

        final JsonEnvelope response = prosecutionCaseQuery.getCaseHearingsForCourtExtract(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();

        final JsonArray hearings = payload.getJsonArray("caseHearings");
        assertThat(hearings.size(), is(2));

        final JsonArray applicationHearings = payload.getJsonArray("linkedApplicationHearings");
        assertThat(applicationHearings.size(), is(1));
        assertThat(applicationHearings.get(0).asJsonObject().getString("id"), is(applicationId.toString()));
        assertThat(applicationHearings.get(0).asJsonObject().getString("title"), is("Application title"));

        final Optional<JsonObject> hearing1 = applicationHearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId1.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(hearing1.isPresent(), is(true));
        final String actualCourtCentre1 = hearing1.get().getJsonObject("courtCentre").getString("id");
        assertThat(actualCourtCentre1, is(courtCentreId1.toString()));

        final Optional<JsonObject> hearing2 = applicationHearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId2.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(hearing2.isPresent(), is(false));
    }

    @Test
    public void shouldReturnCaseDefendantHearings() {
        final DateTimeFormatter ZONE_DATETIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final UUID hearingId1 = randomUUID();
        final UUID courtCentreId1 = randomUUID();
        final ZonedDateTime sittingDay1 = ZonedDateTime.now();

        final UUID hearingId2 = randomUUID();
        final UUID courtCentreId2 = randomUUID();
        final ZonedDateTime sittingDay2 = ZonedDateTime.now().plusDays(1);

        when(hearingAtAGlanceService.getCaseDefendantHearings(caseId, defendantId)).thenReturn(asList(
                createCaseHearing(hearingId1, courtCentreId1, sittingDay1),
                createCaseHearing(hearingId2, courtCentreId2, sittingDay2)));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseIdAndDefendantId(PROGRESSION_QUERY_CASEDEFENDANTHEARINGS, caseId, defendantId);
        final JsonEnvelope response = prosecutionCaseQuery.getCaseDefendantHearings(envelopeWithCaseId);

        final JsonObject payload = response.payloadAsJsonObject();
        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.getString("defendantId"), is(defendantId.toString()));
        final JsonArray hearings = payload.getJsonArray("hearings");
        assertThat(hearings.size(), is(2));

        final Optional<JsonObject> hearing1 = hearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId1.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(hearing1.isPresent(), is(true));
        assertThat(hearing1.get().getJsonObject("courtCentre").getString("id"), is(courtCentreId1.toString()));
        assertThat(hearing1.get().getJsonArray("hearingDays").getJsonObject(0).getString("sittingDay"), is(sittingDay1.format(ZONE_DATETIME_FORMATTER)));

        final Optional<JsonObject> hearing2 = hearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId2.toString().equals(h.getString("hearingId")))
                .findFirst();
        assertThat(hearing2.isPresent(), is(true));
        assertThat(hearing2.get().getJsonObject("courtCentre").getString("id"), is(courtCentreId2.toString()));
        assertThat(hearing2.get().getJsonArray("hearingDays").getJsonObject(0).getString("sittingDay"), is(sittingDay2.format(ZONE_DATETIME_FORMATTER)));
    }

    @Test
    public void shouldReturnTrialHearingsForAProsecutionCase() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID courtCentreId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID courtCentreId2 = randomUUID();

        when(hearingAtAGlanceService.getTrialHearings(prosecutionCaseId)).thenReturn(asList(
                createTrialHearing(hearingId1, courtCentreId1),
                createTrialHearing(hearingId2, courtCentreId2)));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithProsecutionCaseId("progression.query.cotr-trial-hearings", prosecutionCaseId);
        final JsonEnvelope response = prosecutionCaseQuery.getTrialHearings(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();
        final JsonArray trialHearings = payload.getJsonArray("trialHearings");
        assertThat(trialHearings.size(), is(2));

        final Optional<JsonObject> hearing1 = trialHearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId1.toString().equals(h.getString("id")))
                .findFirst();
        assertThat(hearing1.isPresent(), is(true));
        final String actualCourtCentre1 = hearing1.get().getJsonObject("courtCentre").getString("id");
        assertThat(actualCourtCentre1, is(courtCentreId1.toString()));

        final Optional<JsonObject> hearing2 = trialHearings.stream()
                .map(h -> (JsonObject) h)
                .filter(h -> hearingId2.toString().equals(h.getString("id")))
                .findFirst();
        assertThat(hearing2.isPresent(), is(true));
        final String actualCourtCentre2 = hearing2.get().getJsonObject("courtCentre").getString("id");
        assertThat(actualCourtCentre2, is(courtCentreId2.toString()));
    }

    @Test
    public void shouldReturnCotrDetailsForAProsecutionCase() {
        final UUID cotrId1 = randomUUID();
        final UUID cotrId2 = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final String defendantFistName1 = "John";
        final String defendantLastName1 = "Smith";
        final String defendantFirstName2 = "Mark";
        final String defendantLastName2 = "Parker";
        final LocalDate dateOfBirth1 = LocalDate.of(1987, 10, 05);
        final LocalDate dateOfBirth2 = LocalDate.of(1990, 04, 02);
        final ZonedDateTime hearingDay = ZonedDateTime.now();

        when(cotrQueryService.getCotrDetailsForAProsecutionCase(prosecutionCaseId)).thenReturn(asList(
                createCotrDetail(cotrId1, defendantId1, hearingId1, defendantFistName1, defendantLastName1, dateOfBirth1, hearingDay, true),
                createCotrDetail(cotrId2, defendantId2, hearingId2, defendantFirstName2, defendantLastName2, dateOfBirth2, hearingDay, false)));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithProsecutionCaseId("progression.query.cotr-details", prosecutionCaseId);
        final JsonEnvelope response = prosecutionCaseQuery.getCotrDetails(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();
        final JsonArray cotrDetails = payload.getJsonArray("cotrDetails");
        assertThat(cotrDetails.size(), is(2));

        final JsonObject cotrDetail1 = cotrDetails.getJsonObject(0);
        assertThat(1, is(cotrDetail1.getJsonArray("cotrDefendants").size()));
        assertThat(cotrId1.toString(), is(cotrDetail1.getString("id")));
        assertThat(hearingId1.toString(), is(cotrDetail1.getString("hearingId")));
        assertThat(true, is(cotrDetail1.getBoolean("isArchived")));

        final JsonObject cotrDetail2 = cotrDetails.getJsonObject(1);
        assertThat(1, is(cotrDetail1.getJsonArray("cotrDefendants").size()));
        assertThat(cotrId2.toString(), is(cotrDetail2.getString("id")));
        assertThat(hearingId2.toString(), is(cotrDetail2.getString("hearingId")));
        assertThat(false, is(cotrDetail2.getBoolean("isArchived")));

    }

    @Test
    public void shouldReturnCotrDetailsForAProsecutionCaseLatestHearingDate() {
        final UUID cotrId1 = randomUUID();
        final UUID cotrId2 = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final String defendantFistName1 = "John";
        final String defendantLastName1 = "Smith";
        final String defendantFirstName2 = "Mark";
        final String defendantLastName2 = "Parker";
        final LocalDate dateOfBirth1 = LocalDate.of(1987, 10, 05);
        final LocalDate dateOfBirth2 = LocalDate.of(1990, 04, 02);
        final ZonedDateTime hearingDay = ZonedDateTime.now();

        when(cotrQueryService.getCotrDetailsForAProsecutionCaseByLatestHearingDate(prosecutionCaseId)).thenReturn(asList(
                createCotrDetail(cotrId1, defendantId1, hearingId1, defendantFistName1, defendantLastName1, dateOfBirth1, hearingDay, true),
                createCotrDetail(cotrId2, defendantId2, hearingId2, defendantFirstName2, defendantLastName2, dateOfBirth2, hearingDay, false)));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithProsecutionCaseId("progression.query.cotr.details.prosecutioncase", prosecutionCaseId);
        final JsonEnvelope response = prosecutionCaseQuery.getCotrDetailsByCaseId(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();
        final JsonArray cotrDetails = payload.getJsonArray("cotrDetails");
        assertThat(cotrDetails.size(), is(2));

        final JsonObject cotrDetail1 = cotrDetails.getJsonObject(0);
        assertThat(1, is(cotrDetail1.getJsonArray("cotrDefendants").size()));
        assertThat(cotrId1.toString(), is(cotrDetail1.getString("id")));
        assertThat(hearingId1.toString(), is(cotrDetail1.getString("hearingId")));
        assertThat(true, is(cotrDetail1.getBoolean("isArchived")));

        final JsonObject cotrDetail2 = cotrDetails.getJsonObject(1);
        assertThat(1, is(cotrDetail1.getJsonArray("cotrDefendants").size()));
        assertThat(cotrId2.toString(), is(cotrDetail2.getString("id")));
        assertThat(hearingId2.toString(), is(cotrDetail2.getString("hearingId")));
        assertThat(false, is(cotrDetail2.getBoolean("isArchived")));

    }

    @Test
    public void shouldReturnCotrFormForAProsecutionCase() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final String defendantFirstName1 = "John";
        final String defendantLastName1 = "Smith";
        final LocalDate dateOfBirth1 = LocalDate.of(1987, 10, 05);
        final ZonedDateTime hearingDay = ZonedDateTime.now();

        when(cotrQueryService.getCotrFormForAProsecutionCaseAndCotr(prosecutionCaseId, cotrId)).thenReturn(
                createCotrForm(defendantId1, defendantFirstName1, defendantLastName1, dateOfBirth1, hearingDay, prosecutionCaseId));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithProsecutionCaseIdAndCotrId("progression.query.cotr-form", prosecutionCaseId, cotrId);
        final JsonEnvelope response = prosecutionCaseQuery.getCotrForm(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();
        final JsonObject cotrForm = payload.getJsonObject("cotrForm");

        assertThat(cotrForm.getString("caseId"), is(prosecutionCaseId.toString()));
    }

    @Test
    public void shouldFindApplicationsLinkedToProsecutionCase() {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final List<CourtApplication> courtApplications = new ArrayList<>();
        final CourtApplication courtApplication = getCourtApplication();
        courtApplications.add(courtApplication);
        final GetHearingsAtAGlance getCaseAtAGlance = getHearingsAtAGlance()
                .withHearings(asList(hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .withCourtApplications(courtApplications)
                .build();

        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(new ArrayList<>());
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplication).toString());
        final CourtApplicationCaseEntity courtApplicationCaseEntity = new CourtApplicationCaseEntity();
        courtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), randomUUID(), caseId));
        courtApplicationCaseEntity.setCourtApplication(courtApplicationEntity);

        when(courtApplicationCaseRepository.findByCaseId(caseId)).thenReturn(asList(courtApplicationCaseEntity));
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("linkedApplicationsSummary"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
    }


    @Test
    public void shouldFindApplicationsLinkedToProsecutionCaseWithLegalEntityDefendant() {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);


        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final List<CourtApplication> courtApplications = new ArrayList<>();
        final CourtApplication courtApplication = getCourtApplicationWithLegalEntityDefendant(randomUUID(),randomUUID(), randomUUID());
        courtApplications.add(courtApplication);
        final GetHearingsAtAGlance getCaseAtAGlance = getHearingsAtAGlance()
                .withHearings(asList(hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .withCourtApplications(courtApplications)
                .build();

        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(new ArrayList<>());
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplication).toString());
        final CourtApplicationCaseEntity courtApplicationCaseEntity = new CourtApplicationCaseEntity();
        courtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), randomUUID(), caseId));
        courtApplicationCaseEntity.setCourtApplication(courtApplicationEntity);
        when(courtApplicationCaseRepository.findByCaseId(caseId)).thenReturn(asList(courtApplicationCaseEntity));
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("linkedApplicationsSummary"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
    }

    @Test
    public void shouldFindUserGroupsByMaterialId() {
        final UUID materialId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("q", materialId.toString()).build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.usergroups-by-material-id").build(),
                jsonObject);

        final CourtDocumentMaterialEntity courtDocumentMaterialEntity = new CourtDocumentMaterialEntity();
        courtDocumentMaterialEntity.setUserGroups(asList("Listing Officer"));
        when(courtDocumentMaterialRepository.findBy(materialId)).thenReturn(courtDocumentMaterialEntity);
        final JsonEnvelope response = prosecutionCaseQuery.searchByMaterialId(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("allowedUserGroups").getValuesAs(JsonString.class).stream().map(jsonString -> jsonString.getString()).collect(Collectors.toList()), is(courtDocumentMaterialEntity.getUserGroups()));
    }

    @Test
    public void shouldNotFindUserGroupsByMaterialId() throws Exception {
        final UUID materialId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("q", materialId.toString()).build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.usergroups-by-material-id").build(),
                jsonObject);

        when(courtDocumentMaterialRepository.findBy(materialId)).thenReturn(null);
        final JsonEnvelope response = prosecutionCaseQuery.searchByMaterialId(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("allowedUserGroups").size(), is(0));
    }

    @Test
    public void shouldFindCaseBySearchCriteria() {
        final UUID caseId = randomUUID();
        final String searchCriteria = "John Smith";
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("q", searchCriteria).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.search-cases").build(),
                jsonObject);

        final List<SearchProsecutionCaseEntity> searchProsecutionCaseEntities = new ArrayList<>();
        final SearchProsecutionCaseEntity searchCaseEntity = new SearchProsecutionCaseEntity();
        searchCaseEntity.setCaseId(caseId.toString());
        searchCaseEntity.setReference("PAR-100");
        searchCaseEntity.setDefendantFirstName("John");
        searchCaseEntity.setDefendantMiddleName("S");
        searchCaseEntity.setDefendantLastName("Smith");
        searchCaseEntity.setDefendantDob("1977-01-01");
        searchCaseEntity.setProsecutor("TFL");
        searchCaseEntity.setStatus("SJP Referral");
        searchCaseEntity.setStandaloneApplication(false);
        searchProsecutionCaseEntities.add(searchCaseEntity);
        when(searchCaseRepository.findBySearchCriteria(prepareSearch(searchCriteria.toLowerCase()))).thenReturn(searchProsecutionCaseEntities);
        final JsonEnvelope response = prosecutionCaseQuery.searchCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("searchResults").size(), is(1));
    }

    @Test
    public void shouldNotFindCaseBySearchCriteria() {
        final String searchCriteria = "FirstName LastName";
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("q", searchCriteria.toString()).build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.search-cases").build(),
                jsonObject);

        final JsonEnvelope response = prosecutionCaseQuery.searchCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("searchResults").size(), is(0));
    }

    @Test
    public void shouldReturnCaseStatusAsACTIVEWhenCaseStatusIsNull() {

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList = buildMatchDefendantCaseHearingEntities(null, null);

        final GetHearingsAtAGlance getCaseAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance()
                .withHearings(asList(Hearings.hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                createObjectBuilder().add("caseId", CASE_ID1.toString()).build());

        when(prosecutionCaseRepository.findByCaseId(CASE_ID1)).thenReturn(matchDefendantCaseHearingEntityList.get(0).getProsecutionCase());
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(matchDefendantCaseHearingEntityList);
        when(hearingAtAGlanceService.getHearingAtAGlance(CASE_ID1)).thenReturn(getCaseAtAGlance);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
        assertThat(response.payloadAsJsonObject().getJsonObject("prosecutionCase").getString("caseStatus"), is("ACTIVE"));

        final JsonArray relatedCases = response.payloadAsJsonObject().getJsonArray("relatedCases");
        assertThat(relatedCases.size(), is(1));

        assertThat(relatedCases.getJsonObject(0).getJsonArray("cases").size(), is(1));

        with(relatedCases.toString())
                .assertThat("$.[0].masterDefendantId", is(MASTER_DEFENDANT_ID1.toString()));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].caseId", is(CASE_ID2.toString()));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].prosecutionCaseIdentifier.caseURN", is("CaseURN2"));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].caseStatus", is("ACTIVE"));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].offences[0].id", is(OFFENCE_ID2.toString()));

    }

    @Test
    public void shouldReturnOnlyActiveReleteCaseWhenPrimaryCaseStatusIsActive() {

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList = buildMatchDefendantCaseHearingEntities("ACTIVE", "ACTIVE");

        final GetHearingsAtAGlance getCaseAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance()
                .withHearings(asList(Hearings.hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                createObjectBuilder().add("caseId", CASE_ID1.toString()).build());

        when(prosecutionCaseRepository.findByCaseId(CASE_ID1)).thenReturn(matchDefendantCaseHearingEntityList.get(0).getProsecutionCase());
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(matchDefendantCaseHearingEntityList);
        when(hearingAtAGlanceService.getHearingAtAGlance(CASE_ID1)).thenReturn(getCaseAtAGlance);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
        assertThat(response.payloadAsJsonObject().getJsonObject("prosecutionCase").getString("caseStatus"), is("ACTIVE"));

        final JsonArray relatedCases = response.payloadAsJsonObject().getJsonArray("relatedCases");
        assertThat(relatedCases.size(), is(1));

        assertThat(relatedCases.getJsonObject(0).getJsonArray("cases").size(), is(1));

        with(relatedCases.toString())
                .assertThat("$.[0].masterDefendantId", is(MASTER_DEFENDANT_ID1.toString()));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].caseId", is(CASE_ID2.toString()));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].prosecutionCaseIdentifier.caseURN", is("CaseURN2"));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].caseStatus", is("ACTIVE"));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].offences[0].id", is(OFFENCE_ID2.toString()));

    }

    @Test
    public void shouldNotReturnInActiveReleteCaseWhenPrimaryCaseStatusIsActive() {

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList = buildMatchDefendantCaseHearingEntities("ACTIVE", "INACTIVE");

        final GetHearingsAtAGlance getCaseAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance()
                .withHearings(asList(Hearings.hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                createObjectBuilder().add("caseId", CASE_ID1.toString()).build());

        when(prosecutionCaseRepository.findByCaseId(CASE_ID1)).thenReturn(matchDefendantCaseHearingEntityList.get(0).getProsecutionCase());
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(matchDefendantCaseHearingEntityList);
        when(hearingAtAGlanceService.getHearingAtAGlance(CASE_ID1)).thenReturn(getCaseAtAGlance);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
        assertThat(response.payloadAsJsonObject().getJsonObject("prosecutionCase").getString("caseStatus"), is("ACTIVE"));

        final JsonArray relatedCases = response.payloadAsJsonObject().getJsonArray("relatedCases");
        assertThat(relatedCases.size(), is(1));

        assertThat(relatedCases.getJsonObject(0).size(), is(0));

    }

    @Test
    public void shouldNotReturnActiveReleteCaseWhenPrimaryCaseStatusIsInActive() {

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList = buildMatchDefendantCaseHearingEntities("INACTIVE", "ACTIVE");

        final GetHearingsAtAGlance getCaseAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance()
                .withHearings(asList(Hearings.hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                createObjectBuilder().add("caseId", CASE_ID1.toString()).build());

        when(prosecutionCaseRepository.findByCaseId(CASE_ID1)).thenReturn(matchDefendantCaseHearingEntityList.get(0).getProsecutionCase());
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(matchDefendantCaseHearingEntityList);
        when(hearingAtAGlanceService.getHearingAtAGlance(CASE_ID1)).thenReturn(getCaseAtAGlance);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
        assertThat(response.payloadAsJsonObject().getJsonObject("prosecutionCase").getString("caseStatus"), is("INACTIVE"));

        final JsonArray relatedCases = response.payloadAsJsonObject().getJsonArray("relatedCases");
        assertThat(relatedCases.size(), is(1));

        assertThat(relatedCases.getJsonObject(0).size(), is(0));

    }

    @Test
    public void shouldNotReturnActiveReleteCaseWhenPrimaryCaseStatusIsClosed() {

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList = buildMatchDefendantCaseHearingEntities("CLOSED", "ACTIVE");

        final GetHearingsAtAGlance getCaseAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance()
                .withHearings(asList(Hearings.hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                createObjectBuilder().add("caseId", CASE_ID1.toString()).build());

        when(prosecutionCaseRepository.findByCaseId(CASE_ID1)).thenReturn(matchDefendantCaseHearingEntityList.get(0).getProsecutionCase());
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(matchDefendantCaseHearingEntityList);
        when(hearingAtAGlanceService.getHearingAtAGlance(CASE_ID1)).thenReturn(getCaseAtAGlance);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
        assertThat(response.payloadAsJsonObject().getJsonObject("prosecutionCase").getString("caseStatus"), is("CLOSED"));

        final JsonArray relatedCases = response.payloadAsJsonObject().getJsonArray("relatedCases");
        assertThat(relatedCases.size(), is(1));

        assertThat(relatedCases.getJsonObject(0).size(), is(0));

    }

    @Test
    public void shouldReturnActiveReleteCaseWhenPrimaryCaseStatusIsClosed() {

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList = buildMatchDefendantCaseHearingEntities("CLOSED", "INACTIVE");

        final GetHearingsAtAGlance getCaseAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance()
                .withHearings(asList(Hearings.hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                createObjectBuilder().add("caseId", CASE_ID1.toString()).build());

        when(prosecutionCaseRepository.findByCaseId(CASE_ID1)).thenReturn(matchDefendantCaseHearingEntityList.get(0).getProsecutionCase());
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(matchDefendantCaseHearingEntityList);
        when(hearingAtAGlanceService.getHearingAtAGlance(CASE_ID1)).thenReturn(getCaseAtAGlance);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
        assertThat(response.payloadAsJsonObject().getJsonObject("prosecutionCase").getString("caseStatus"), is("CLOSED"));

        final JsonArray relatedCases = response.payloadAsJsonObject().getJsonArray("relatedCases");
        assertThat(relatedCases.size(), is(1));

        assertThat(relatedCases.getJsonObject(0).getJsonArray("cases").size(), is(1));

        with(relatedCases.toString())
                .assertThat("$.[0].masterDefendantId", is(MASTER_DEFENDANT_ID1.toString()));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].caseId", is(CASE_ID2.toString()));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].prosecutionCaseIdentifier.caseURN", is("CaseURN2"));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].caseStatus", is("INACTIVE"));
        with(relatedCases.toString())
                .assertThat("$.[0].cases[0].offences[0].id", is(OFFENCE_ID2.toString()));

    }

    @Test
    public void shouldReturnCaseHearingTypes() throws IOException {
        final LocalDate today = LocalDate.now();
        final UUID caseId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID hearingId3 = randomUUID();

        when(hearingAtAGlanceService.getCaseHearingEntities(caseId)).thenReturn(asList(
                createHearingEntity(hearingId1, today, PLEAS_HEARING_TYPE_DESCRIPTION),
                createHearingEntity(hearingId2, today.plusDays(1), SECOND_HEARING_TYPE_DESCRIPTION),
                createHearingEntity(hearingId3, today.plusDays(2), BAIL_HEARING_TYPE_DESCRIPTION)));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeForQueryHearingTypes(PROGRESSION_QUERY_CASE_HEARING_TYPES, caseId, today);
        final JsonEnvelope response = prosecutionCaseQuery.getCaseHearingTypes(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();
        final Map<String, String> hearingTypes = payload.getJsonArray(HEARING_TYPES).getValuesAs(JsonObject.class).stream()
                .collect(Collectors.toMap(json -> json.getString(HEARING_ID), json -> json.getString(TYPE)));
        assertThat(hearingTypes.size(), is(1));
        assertThat(hearingTypes.get(hearingId1.toString()), is(PLEAS_HEARING_TYPE_DESCRIPTION));
    }

    @Test
    public void shouldReturnCaseHearingTypesNoMatch() throws IOException {
        final LocalDate today = LocalDate.now();
        final UUID caseId = randomUUID();
        final UUID hearingId1 = randomUUID();

        when(hearingAtAGlanceService.getCaseHearingEntities(caseId)).thenReturn(asList(
                createHearingEntity(hearingId1, today.minusDays(1), PLEAS_HEARING_TYPE_DESCRIPTION)));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeForQueryHearingTypes(PROGRESSION_QUERY_CASE_HEARING_TYPES, caseId, today);
        final JsonEnvelope response = prosecutionCaseQuery.getCaseHearingTypes(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();
        final Map<String, String> hearingTypes = payload.getJsonArray(HEARING_TYPES).getValuesAs(JsonObject.class).stream()
                .collect(Collectors.toMap(json -> json.getString(HEARING_ID), json -> json.getString(TYPE)));
        assertThat(hearingTypes.size(), is(0));
    }

    @Test
    public void shouldReturnCaseHearingTypesHearingWithSameConfirmDate() throws IOException {
        final LocalDate today = LocalDate.now();
        final UUID caseId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID hearingId3 = randomUUID();

        when(hearingAtAGlanceService.getCaseHearingEntities(caseId)).thenReturn(asList(
                createHearingEntity(hearingId1, today, FIRST_HEARING_TYPE_DESCRIPTION),
                createHearingEntity(hearingId2, today.plusDays(1), SECOND_HEARING_TYPE_DESCRIPTION),
                createHearingEntity(hearingId3, today, BAIL_HEARING_TYPE_DESCRIPTION)));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeForQueryHearingTypes(PROGRESSION_QUERY_CASE_HEARING_TYPES, caseId, today);
        final JsonEnvelope response = prosecutionCaseQuery.getCaseHearingTypes(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();
        final Map<String, String> hearingTypes = payload.getJsonArray(HEARING_TYPES).getValuesAs(JsonObject.class).stream()
                .collect(Collectors.toMap(json -> json.getString(HEARING_ID), json -> json.getString(TYPE)));
        assertThat(hearingTypes.size(), is(2));
        assertThat(hearingTypes.get(hearingId1.toString()), is(FIRST_HEARING_TYPE_DESCRIPTION));
    }


    @Test
    public void shouldFindProsecutionAuthorityIdByCaseIds() {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseIds", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutionauthorityid-by-case-id").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(defendants)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final JsonEnvelope response = prosecutionCaseQuery.searchProsecutionAuthorityId(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutors"), notNullValue());
    }

    @Test
    public void shouldFindProsecutorIdOrProsecutionAuthorityIdByCaseIds() {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID prosecutorId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseIds", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutorid-prosecutionauthorityid-by-case-id").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(defendants)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .withProsecutor(Prosecutor.prosecutor().withProsecutorId(prosecutorId).build())
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByProsecutionCaseIds(singletonList(caseId))).thenReturn(singletonList(prosecutionCaseEntity));


        final JsonEnvelope response = prosecutionCaseQuery.searchProsecutorIdProsecutionAuthorityId(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutors"), notNullValue());
        assertThat(response.payloadAsJsonObject().getJsonArray("prosecutors").getJsonObject(0)
                .getJsonObject("prosecutor").get("prosecutorId"), is(notNullValue()));
    }

    @Test
    public void shouldGetAllCaseHearings() throws IOException {
        final UUID caseId = randomUUID();
        final JsonEnvelope envelope = buildEnvelopeWithCaseId(CASE_ALL_HEARINGS_QUERY, caseId);
        final JsonObject jsonObject = getJsonPayload(CASE_ALL_HEARINGS_QUERY_VIEW_JSON);
        final GetHearingsAtAGlance getHearingsAtAGlance = jsonObjectToObjectConverter.convert(jsonObject, GetHearingsAtAGlance.class);
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getHearingsAtAGlance);
        final JsonEnvelope response = prosecutionCaseQuery.getAllCaseHearings(envelope);
        assertThat(response.payloadAsJsonObject().get("allCaseHearings"), is(notNullValue()));
    }

    @Test
    public void shouldReturnEmptyEnvelopeWhenNoLinkedApplicationsOnCaseExists() {
        final UUID caseId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCaseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.active-applications-on-case").build(),
                jsonObject);
        final JsonEnvelope activeApplicationsOnCase = prosecutionCaseQuery.getActiveApplicationsOnCase(jsonEnvelope);
        assertThat(activeApplicationsOnCase.payloadAsJsonObject().getJsonArray("linkedApplications").size(), is(0));
    }

    @Test
    public void shouldReturnOnlyActiveApplicationsOnCaseWhenExists() {
        final UUID caseId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCaseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.active-applications-on-case").build(),
                jsonObject);
        final CourtApplication activeCourtApplication = getCourtApplicationWithLegalEntityDefendant(randomUUID(), randomUUID(), randomUUID());
        final CourtApplication inactiveCourtApplication = getInactiveCourtApplicationWithLegalEntityDefendant();

        final CourtApplicationEntity activeCourtApplicationEntity = new CourtApplicationEntity();
        activeCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(activeCourtApplication).toString());
        activeCourtApplicationEntity.setApplicationId(APPLICATION_ID);

        final CourtApplicationEntity inactiveCourtApplicationEntity = new CourtApplicationEntity();
        inactiveCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(inactiveCourtApplication).toString());
        inactiveCourtApplicationEntity.setApplicationId(INACTIVE_APPLICATION_ID);

        final CourtApplicationCaseEntity activeCourtApplicationCaseEntity = new CourtApplicationCaseEntity();
        activeCourtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), APPLICATION_ID, caseId));
        activeCourtApplicationCaseEntity.setCourtApplication(activeCourtApplicationEntity);

        final CourtApplicationCaseEntity inactiveCourtApplicationCaseEntity = new CourtApplicationCaseEntity();
        inactiveCourtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), INACTIVE_APPLICATION_ID, caseId));
        inactiveCourtApplicationCaseEntity.setCourtApplication(inactiveCourtApplicationEntity);

        when(courtApplicationCaseRepository.findByCaseId(caseId)).thenReturn(Arrays.asList(activeCourtApplicationCaseEntity, inactiveCourtApplicationCaseEntity));
        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(APPLICATION_ID, randomUUID()));
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(randomUUID());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingApplicationEntity.setHearing(hearingEntity);
        when(hearingApplicationRepository.findByApplicationId(APPLICATION_ID)).thenReturn(asList(hearingApplicationEntity));
        final JsonEnvelope activeApplicationsOnCase = prosecutionCaseQuery.getActiveApplicationsOnCase(jsonEnvelope);
        assertThat(activeApplicationsOnCase.payloadAsJsonObject().getJsonArray("linkedApplications").size(), is(1));
        assertThat(activeApplicationsOnCase.payloadAsJsonObject().getJsonArray("linkedApplications")
                .getJsonObject(0).getString("applicationId"), is(APPLICATION_ID.toString()));
    }

    private CourtApplication getInactiveCourtApplicationWithLegalEntityDefendant() {
        final MasterDefendant masterDefendant = MasterDefendant.masterDefendant().
                withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName("ABC LTD").build()).build()).build();
        return CourtApplication.courtApplication()
                .withId(INACTIVE_APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.FINALISED)
                .withType(CourtApplicationType.courtApplicationType().withType("Apil").build())
                .withApplicationReference(APPLICATION_ARN)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(masterDefendant)
                        .build())
                .withRespondents(asList(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName(APPLICATION_PROSECUTOR_NAME)
                                .build())
                        .build(), CourtApplicationParty.courtApplicationParty()
                        .withOrganisation(Organisation.organisation()
                                .withName(RESPONDENTS_ORG_NAME)
                                .build())
                        .build()))
                .build();
    }

    @Test
    public void shouldReturnCaseAllHearingTypes() throws IOException {
        final LocalDate today = LocalDate.now();
        final UUID caseId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID hearingId3 = randomUUID();

        when(hearingAtAGlanceService.getCaseHearingEntities(caseId)).thenReturn(asList(
                createHearingEntity(hearingId1, today, PLEAS_HEARING_TYPE_DESCRIPTION),
                createHearingEntity(hearingId2, today.plusDays(1), SECOND_HEARING_TYPE_DESCRIPTION),
                createHearingEntity(hearingId3, today.plusDays(2), BAIL_HEARING_TYPE_DESCRIPTION)));

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeForQueryHearingTypes(PROGRESSION_QUERY_CASE_HEARING_TYPES, caseId, today);
        final JsonEnvelope response = prosecutionCaseQuery.getCaseAllHearingTypes(envelopeWithCaseId);
        final JsonObject payload = response.payloadAsJsonObject();
        final Map<String, String> hearingTypes = payload.getJsonArray(HEARING_TYPES).getValuesAs(JsonObject.class).stream()
                .collect(Collectors.toMap(json -> json.getString(HEARING_ID), json -> json.getString(TYPE)));
        assertThat(hearingTypes.size(), is(3));
    }

    private CourtApplication getCourtApplication() {
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withType(CourtApplicationType.courtApplicationType().withType("Apil").build())
                .withApplicationReference(APPLICATION_ARN)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName(APPLICANT_FIRST_NAME)
                                .withLastName(APPLICANT_LAST_NAME)
                                .withMiddleName(APPLICANT_MIDDLE_NAME)
                                .build())
                        .build())
                .withRespondents(asList(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName(APPLICATION_PROSECUTOR_NAME)
                                .build())
                        .build(), CourtApplicationParty.courtApplicationParty()
                        .withOrganisation(Organisation.organisation()
                                .withName(RESPONDENTS_ORG_NAME)
                                .build())
                        .build()))
                .build();
    }

    private Hearings createCaseHearing(final UUID hearingId, final UUID courtCentreId, final ZonedDateTime sittingDay1, final ZonedDateTime sittingDay2) {
        return Hearings.hearings()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingListingStatus(HEARING_RESULTED)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(courtCentreId)
                        .withName("name")
                        .withRoomId(randomUUID())
                        .withRoomName("roomName")
                        .build())
                .withHearingDays(
                        asList(createHearingDay(sittingDay1), createHearingDay(sittingDay2))).build();
    }

    private Hearings createBoxWorkHearing(final UUID hearingId, final UUID courtCentreId, final ZonedDateTime sittingDay1, final ZonedDateTime sittingDay2) {
        return Hearings.hearings()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingListingStatus(HEARING_RESULTED)
                .withIsBoxHearing(true)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(courtCentreId)
                        .withName("name")
                        .withRoomId(randomUUID())
                        .withRoomName("roomName")
                        .build())
                .withHearingDays(
                        asList(createHearingDay(sittingDay1), createHearingDay(sittingDay2))).build();
    }

    private HearingDay createHearingDay(final ZonedDateTime sittingDay) {
        HearingDay hearingDay = HearingDay.hearingDay()
                .withSittingDay(sittingDay).build();
        return hearingDay;
    }

    private TrialHearing createTrialHearing(final UUID hearingId, final UUID courtCentreId) {
        return TrialHearing.trialHearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(courtCentreId)
                        .withName("name")
                        .withRoomId(randomUUID())
                        .withRoomName("roomName")
                        .build())
                .withHearingDay(ZonedDateTime.now())
                .withTrialDefendants(asList(TrialDefendants.trialDefendants()
                        .withId(randomUUID())
                        .withFullName("John Smith")
                        .build()))
                .build();
    }

    private JsonObject createCotrForm(final UUID defendantId, final String firstName, final String lastName, final LocalDate dateOfBirth, final ZonedDateTime hearingDay, final UUID prosecutionCaseId) {
        return Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("caseId", prosecutionCaseId.toString())
                .add("hearingId", randomUUID().toString())
                .add("caseUrn", "CASEURN")
                .add("hearingDay", hearingDay.toLocalDate().toString())
                .add("listedDurationMinutes", "20")
                .add("prosecutionFormData", "form data")
                .build();

    }

    private List<DefenceAdditionalInfo> createDefendantAdditionalInfo() {
        final LocalDate addedOn = LocalDate.of(2021, 04, 02);
        DefenceAdditionalInfo defenceAdditionalInfo = DefenceAdditionalInfo.defenceAdditionalInfo()
                .withId(randomUUID())
                .withIsCertificationReady(Boolean.TRUE)
                .withAddedBy(randomUUID())
                .withAddedByName("John Turner")
                .withAddedOn(addedOn)
                .withInformation("")
                .build();
        return asList(defenceAdditionalInfo);
    }

    private CotrDetail createCotrDetail(final UUID cotrId, final UUID defendantId, final UUID hearingId, final String firstName, final String lastName, final LocalDate dateOfBirth, final ZonedDateTime hearingDay, final Boolean isArchived) {
        return CotrDetail.cotrDetail()
                .withId(cotrId)
                .withHearingId(hearingId)
                .withHearingDay(hearingDay)
                .withCotrDefendants(asList(CotrDefendant.cotrDefendant()
                        .withId(defendantId)
                        .withFirstName(firstName)
                        .withLastName(lastName)
                        .withDateOfBirth(dateOfBirth)
                        .build()))
                .withIsArchived(isArchived)
                .build();
    }

    private Hearings createCaseHearing(final UUID hearingId, final UUID courtCentreId, final ZonedDateTime sittingDay) {
        UUID roomId = randomUUID();
        return hearings()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(courtCentreId)
                        .withName("name")
                        .withRoomId(roomId)
                        .withRoomName("roomName")
                        .build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withCourtCentreId(courtCentreId)
                        .withCourtRoomId(roomId).withSittingDay(sittingDay).build()))
                .build();
    }

    private List<Hearings> getHearingsList(final UUID masterDefendantId,final UUID defendantId) {
        return asList(
                hearings()
                        .withHearingListingStatus(HEARING_RESULTED)
                        .withDefendantJudicialResults(asList(
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withJudicialResult(JudicialResult.judicialResult()
                                                .withLabel(LABEL1)
                                                .withResultText("code - " + LABEL1)
                                                .build())
                                        .withMasterDefendantId(masterDefendantId)
                                        .withDefendantId(defendantId)
                                        .build(),
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withJudicialResult(JudicialResult.judicialResult()
                                                .withLabel(LABEL2)
                                                .withResultText(LABEL2)
                                                .build())
                                        .withMasterDefendantId(masterDefendantId)
                                        .withDefendantId(defendantId)
                                        .build()
                        ))
                        .build(),
                hearings()
                        .withHearingListingStatus(HEARING_RESULTED)
                        .withDefendantJudicialResults(asList(
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withJudicialResult(JudicialResult.judicialResult()
                                                .withLabel(LABEL3)
                                                .build())
                                        .withMasterDefendantId(masterDefendantId)
                                        .withDefendantId(defendantId)
                                        .build(),
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withJudicialResult(JudicialResult.judicialResult()
                                                .withLabel(LABEL4)
                                                .build())
                                        .withMasterDefendantId(masterDefendantId)
                                        .withDefendantId(defendantId)
                                        .build()
                        ))
                        .build()
        );
    }

    private CourtApplication getCourtApplicationWithLegalEntityDefendant(final UUID defendantId1, final UUID defendantId2, final UUID masterDefendantId) {
        final MasterDefendant masterDefendant = MasterDefendant.masterDefendant().
                withMasterDefendantId(masterDefendantId).
                withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName("ABC LTD").build()).build()).build();
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withType(CourtApplicationType.courtApplicationType().withType("Apil").build())
                .withApplicationReference(APPLICATION_ARN)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(defendantId1)
                        .withUpdatedOn(LocalDate.now())
                        .withMasterDefendant(masterDefendant)
                        .build())
                .withRespondents(asList(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName(APPLICATION_PROSECUTOR_NAME)
                                .build())
                        .build(), CourtApplicationParty.courtApplicationParty()
                        .withId(defendantId2)
                        .withOrganisation(Organisation.organisation()
                                .withName(RESPONDENTS_ORG_NAME)
                                .build())
                        .withUpdatedOn(LocalDate.now().minusDays(1))
                        .build()))
                .build();
    }

    private JsonEnvelope buildEnvelope(final String eventName, final String payloadFileName) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream(payloadFileName);
             final JsonReader jsonReader = Json.createReader(stream)) {
            final JsonObject payload = jsonReader.readObject();
            final JsonObject jsonObject = Json.createObjectBuilder()
                    .add("caseId", randomUUID().toString()).build();
            return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(eventName), payload);
        } catch (final IOException e) {
            LOGGER.warn("Error in reading payload {}", payloadFileName, e);
        }
        return null;
    }

    private JsonEnvelope buildEnvelopeWithCaseId(final String eventName, final UUID caseId) {
        return envelopeFrom(
                DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(eventName).createdAt(ZonedDateTime.now()),
                createObjectBuilder().add("caseId", caseId.toString()).build());
    }

    private JsonEnvelope buildEnvelopeWithCaseIdAndDefendantId(final String eventName, final UUID caseId, final UUID defendantId) {
        return envelopeFrom(
                DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(eventName).createdAt(ZonedDateTime.now()),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("defendantId", defendantId.toString())
                        .build());
    }

    private JsonEnvelope buildEnvelopeForQueryHearingTypes(final String eventName, final UUID caseId, final LocalDate orderDate) {
        return envelopeFrom(
                DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(eventName).createdAt(ZonedDateTime.now()),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("orderDate", orderDate.toString())
                        .build());
    }

    private JsonEnvelope buildEnvelopeWithProsecutionCaseId(final String eventName, final UUID prosecutionCaseId) {
        return envelopeFrom(
                DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(eventName).createdAt(ZonedDateTime.now()),
                createObjectBuilder().add("prosecutionCaseId", prosecutionCaseId.toString()).build());
    }

    private JsonEnvelope buildEnvelopeWithProsecutionCaseIdAndCotrId(final String eventName, final UUID prosecutionCaseId, final UUID cotrId) {
        return envelopeFrom(
                DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(eventName).createdAt(ZonedDateTime.now()),
                createObjectBuilder()
                        .add("prosecutionCaseId", prosecutionCaseId.toString())
                        .add("cotrId", cotrId.toString())
                        .build());
    }

    private ProsecutionCase getProsecutionCase(final String caseURN, final Defendant... defendants) {
        return ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withCaseURN(caseURN)
                        .build())
                .withDefendants(asList(defendants))
                .build();
    }

    private ProsecutionCase getProsecutionCase(final JsonObject prosecutionCaseEntityJson) {
        return ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(UUID.randomUUID())
                        .withCaseURN(prosecutionCaseEntityJson.getJsonObject("prosecutionCaseIdentifier").getString("caseURN"))
                        .build())
                .withDefendants(emptyList())
                .build();
    }

    private List<Offence> buildOffences(UUID offenceId) {
        List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence().withId(offenceId).build());
        return offences;
    }

    static class ApplicationArbitraryValues {
        final static UUID APPLICATION_ID = UUID.randomUUID();
        final static UUID LINKED_CASE_ID = UUID.randomUUID();
        final static String APPLICATION_ARN = new StringGenerator().next();
        final static String APPLICANT_FIRST_NAME = new StringGenerator().next();
        final static String APPLICANT_LAST_NAME = new StringGenerator().next();
        final static String APPLICANT_MIDDLE_NAME = new StringGenerator().next();
        final static String RESPONDENTS_ORG_NAME = new StringGenerator().next();
        final static String APPLICATION_PROSECUTOR_NAME = new StringGenerator().next();
    }

    private List<MatchDefendantCaseHearingEntity> buildMatchDefendantCaseHearingEntitiesWithNoOffence() {

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", CASE_ID1.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final List<Offence> offences1 = buildOffences(OFFENCE_ID1);
        final List<Offence> offences2 = buildOffences(OFFENCE_ID2);

        final List<Defendant> defendantsForCase1 = new ArrayList<>();
        defendantsForCase1.add(Defendant.defendant()
                .withMasterDefendantId(MASTER_DEFENDANT_ID1)
                .withOffences(offences1)
                .build());

        final List<Defendant> defendantsForCase2 = new ArrayList<>();
        defendantsForCase2.add(Defendant.defendant()
                .withMasterDefendantId(randomUUID())
                .withOffences(offences2)
                .build());

        final ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase()
                .withDefendants(defendantsForCase1)
                .withCaseStatus("ACTIVE1")
                .withId(CASE_ID1)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("Code1")
                        .withCaseURN("CaseURN1")
                        .build())
                .build();

        final ProsecutionCase prosecutionCase2 = ProsecutionCase.prosecutionCase()
                .withDefendants(defendantsForCase2)
                .withCaseStatus("ACTIVE2")
                .withId(CASE_ID2)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("Code2")
                        .withCaseURN("CaseURN2")
                        .build())
                .build();

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = new ArrayList<>();
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID1, CASE_ID1, prosecutionCase1));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID1, CASE_ID2, prosecutionCase2));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID1, CASE_ID2, prosecutionCase2));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID2, CASE_ID1, prosecutionCase1));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID3, CASE_ID2, prosecutionCase2));
        return matchDefendantCaseHearingEntities;

    }

    private List<MatchDefendantCaseHearingEntity> buildMatchDefendantCaseHearingEntities() {
        final List<Offence> offences1 = buildOffences(OFFENCE_ID1);
        final List<Offence> offences2 = buildOffences(OFFENCE_ID2);
        final List<Offence> offences3 = buildOffences(OFFENCE_ID3);
        final List<Offence> offences4 = buildOffences(OFFENCE_ID4);
        final List<Offence> offences5 = buildOffences(OFFENCE_ID5);
        final List<Offence> offences6 = buildOffences(OFFENCE_ID6);

        final List<Defendant> defendantsForCase1 = new ArrayList<>();
        defendantsForCase1.add(Defendant.defendant()
                .withMasterDefendantId(MASTER_DEFENDANT_ID1)
                .withOffences(offences1)
                .build());
        defendantsForCase1.add(Defendant.defendant()
                .withMasterDefendantId(MASTER_DEFENDANT_ID2)
                .withOffences(offences3)
                .build());

        final List<Defendant> defendantsForCase2 = new ArrayList<>();
        defendantsForCase2.add(Defendant.defendant()
                .withMasterDefendantId(MASTER_DEFENDANT_ID1)
                .withOffences(offences2)
                .build());
        defendantsForCase2.add(Defendant.defendant()
                .withMasterDefendantId(MASTER_DEFENDANT_ID3)
                .withOffences(offences6)
                .build());

        final List<Defendant> defendantsForCase3 = new ArrayList<>();
        defendantsForCase3.add(Defendant.defendant()
                .withMasterDefendantId(MASTER_DEFENDANT_ID2)
                .withOffences(offences4)
                .build());
        defendantsForCase3.add(Defendant.defendant()
                .withMasterDefendantId(MASTER_DEFENDANT_ID3)
                .withOffences(offences5)
                .build());

        final ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase()
                .withDefendants(defendantsForCase1)
                .withCaseStatus("ACTIVE1")
                .withId(CASE_ID1)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("Code1")
                        .withCaseURN("CaseURN1")
                        .build())
                .build();

        final ProsecutionCase prosecutionCase2 = ProsecutionCase.prosecutionCase()
                .withDefendants(defendantsForCase2)
                .withCaseStatus("ACTIVE2")
                .withId(CASE_ID2)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("Code2")
                        .withCaseURN("CaseURN2")
                        .build())
                .build();

        final ProsecutionCase prosecutionCase3 = ProsecutionCase.prosecutionCase()
                .withDefendants(defendantsForCase3)
                .withCaseStatus("ACTIVE3")
                .withId(CASE_ID3)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("Code3")
                        .withCaseURN("CaseURN3")
                        .build())
                .build();

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = new ArrayList<>();
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID1, CASE_ID1, prosecutionCase1));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID1, CASE_ID2, prosecutionCase2));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID1, CASE_ID2, prosecutionCase2));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID2, CASE_ID1, prosecutionCase1));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID2, CASE_ID3, prosecutionCase3));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID3, CASE_ID2, prosecutionCase2));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID3, CASE_ID3, prosecutionCase3));
        return matchDefendantCaseHearingEntities;
    }

    private MatchDefendantCaseHearingEntity buildMatchDefendantCaseHearingEntity(UUID masterDefendantId, UUID caseId, ProsecutionCase prosecutionCase) {
        final MatchDefendantCaseHearingEntity entity = new MatchDefendantCaseHearingEntity();
        entity.setId(randomUUID());
        entity.setDefendantId(randomUUID());
        entity.setMasterDefendantId(masterDefendantId);
        entity.setProsecutionCaseId(caseId);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        entity.setProsecutionCase(prosecutionCaseEntity);
        return entity;
    }

    private List<MatchDefendantCaseHearingEntity> buildMatchDefendantCaseHearingEntities(final String status1, final String status2) {


        final List<Offence> offences1 = buildOffences(OFFENCE_ID1);
        final List<Offence> offences2 = buildOffences(OFFENCE_ID2);


        final List<Defendant> defendantsForCase1 = new ArrayList<>();
        defendantsForCase1.add(Defendant.defendant()
                .withMasterDefendantId(MASTER_DEFENDANT_ID1)
                .withOffences(offences1)
                .build());

        final List<Defendant> defendantsForCase2 = new ArrayList<>();
        defendantsForCase2.add(Defendant.defendant()
                .withMasterDefendantId(MASTER_DEFENDANT_ID1)
                .withOffences(offences2)
                .build());

        final ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase()
                .withDefendants(defendantsForCase1)
                .withCaseStatus(status1)
                .withId(CASE_ID1)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("Code1")
                        .withCaseURN("CaseURN1")
                        .build())
                .build();

        final ProsecutionCase prosecutionCase2 = ProsecutionCase.prosecutionCase()
                .withDefendants(defendantsForCase2)
                .withCaseStatus(status2)
                .withId(CASE_ID2)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("Code2")
                        .withCaseURN("CaseURN2")
                        .build())
                .build();

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = new ArrayList<>();
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID1, CASE_ID1, prosecutionCase1));
        matchDefendantCaseHearingEntities.add(buildMatchDefendantCaseHearingEntity(MASTER_DEFENDANT_ID1, CASE_ID2, prosecutionCase2));

        return matchDefendantCaseHearingEntities;
    }

    private String createPayload(final String payloadPath) throws IOException {
        final StringWriter writer = new StringWriter();
        InputStream inputStream = ProsecutionCaseQueryViewTest.class.getResourceAsStream(payloadPath);
        IOUtils.copy(inputStream, writer, UTF_8);
        inputStream.close();
        return writer.toString();
    }

    private HearingEntity createHearingEntity(final UUID hearingId, final LocalDate date, final String description) throws IOException {
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setConfirmedDate(date);
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(createPayload("/hearingDataProsecutionCase.json").replaceAll("DESCRIPTION", description));
        return hearingEntity;
    }

    private JsonObject getJsonPayload(final String fileName) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(fileName), defaultCharset());
        return Json.createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readObject();
    }

    @Test
    public void shouldFindCivilFees() {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final List<CivilFees> feesList = new ArrayList<>();
        feesList.add(CivilFees.civilFees()
                .withPaymentReference("Reference001")
                .withFeeStatus(FeeStatus.OUTSTANDING)
                .withFeeType(FeeType.INITIAL)
                .withFeeId(UUID.randomUUID())
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .withGroupId(UUID.randomUUID())
                .withIsGroupMember(true)
                .withIsGroupMaster(true)
                .withIsCivil(true)
                .withCivilFees(feesList)
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final GetHearingsAtAGlance getCaseAtAGlance = getHearingsAtAGlance()
                .withHearings(asList(hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        CivilFeeEntity civilFeeEntity = new CivilFeeEntity(UUID.randomUUID(),
                uk.gov.moj.cpp.progression.domain.constant.FeeType.INITIAL,
                uk.gov.moj.cpp.progression.domain.constant.FeeStatus.REDUCED,
                "Reference001");

        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(matchDefendantCaseHearingRepository.findByMasterDefendantId(anyList())).thenReturn(new ArrayList<>());
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        when(civilFeeRepository.findBy(any())).thenReturn(civilFeeEntity);
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
    }

    @Test
    public void shouldFindProsecutionCaseDetails() {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase-details").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCaseDetails(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
    }

    @Test
    public void shouldFindGroupMemberCases() {
        final UUID groupId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("groupId", groupId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.group-member-cases").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .withGroupId(groupId)
                .withIsGroupMaster(true)
                .withIsGroupMember(true)
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        when(prosecutionCaseRepository.findByGroupId(groupId)).thenReturn(singletonList(prosecutionCaseEntity));
        final JsonEnvelope response = prosecutionCaseQuery.getGroupMemberCases(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCases"), notNullValue());
    }

    @Test
    public void shouldFindGroupMasterCase() {
        final UUID groupId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("groupId", groupId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.mastercase-details").build(),
                jsonObject);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withMasterDefendantId(masterDefendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(defendants)
                .withGroupId(groupId)
                .withIsGroupMaster(true)
                .withIsGroupMember(true)
                .build();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        when(prosecutionCaseRepository.findByGroupId(groupId)).thenReturn(singletonList(prosecutionCaseEntity));
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionMasterCaseDetails(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("masterCase"), notNullValue());
    }
}
