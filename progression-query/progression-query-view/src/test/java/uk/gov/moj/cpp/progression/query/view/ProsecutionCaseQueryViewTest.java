package uk.gov.moj.cpp.progression.query.view;

import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.progression.courts.GetHearingsAtAGlance.getHearingsAtAGlance;
import static uk.gov.justice.progression.courts.Hearings.hearings;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.progression.query.utils.SearchQueryUtils.prepareSearch;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.CaagDefendants;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseQueryViewTest {

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID LINKED_CASE_ID = UUID.randomUUID();
    private static final String APPLICATION_ARN = new StringGenerator().next();
    private static final String APPLICANT_FIRST_NAME = new StringGenerator().next();
    private static final String APPLICANT_LAST_NAME = new StringGenerator().next();
    private static final String APPLICANT_MIDDLE_NAME = new StringGenerator().next();
    private static final String RESPONDENTS_ORG_NAME = new StringGenerator().next();
    private static final String APPLICATION_PROSECUTOR_NAME = new StringGenerator().next();
    private static final String PROGRESSION_QUERY_PROSECUTIONCASE_CAAG = "progression.query.prosecutioncase.caag";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final Logger LOGGER = getLogger(ProsecutionCaseQueryViewTest.class);
    private static final String LABEL1 = "label 1";
    private static final String LABEL2 = "label 2";
    private static final String LABEL3 = "label 3";
    private static final String LABEL4 = "label 4";
    private final Enveloper enveloper = createEnveloper();
    @Mock
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;
    @Mock
    private SearchProsecutionCaseRepository searchCaseRepository;
    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;
    @Mock
    private CourtDocumentRepository courtDocumentRepository;
    @Mock
    private JsonObject jsonObject;
    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Mock
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
    private CourtApplicationRepository courtApplicationRepository;
    @Mock
    private HearingAtAGlanceService hearingAtAGlanceService;
    @Mock
    private ReferenceDataService referenceDataService;


    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

        setField(this.listToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField(this.hearingListToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.hearingListToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
    }

    @Test
    public void shouldFindProsecutionCaseById() throws Exception {
        final UUID caseId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload("{}");

        final GetHearingsAtAGlance getCaseAtAGlance = getHearingsAtAGlance()
                .withHearings(asList(hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(this.jsonObject);
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtApplication.class)).thenReturn(CourtApplication.courtApplication().build());
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
    }

    @Test
    public void shouldFindCaseAtAGlanceGivenProsecutionCaseWithUrn() {
        final UUID masterDefendantId = randomUUID();
        final String caseURN = "05PP1000915";
        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseId(PROGRESSION_QUERY_PROSECUTIONCASE_CAAG);
        final String caseId = envelopeWithCaseId.payloadAsJsonObject().getString("caseId");
        final Defendant defendant = defendant().withMasterDefendantId(masterDefendantId).build();

        when(prosecutionCaseRepository.findByCaseId(any(UUID.class))).thenReturn(new ProsecutionCaseEntity());
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any(Class.class))).thenReturn(getProsecutionCase(caseURN, defendant));
        when(referenceDataService.getProsecutor(anyString())).thenReturn(Optional.empty());
        when(hearingAtAGlanceService.getCaseHearings(any(UUID.class))).thenReturn(getHearingsList(masterDefendantId));

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(envelopeWithCaseId);
        assertThat(response.payloadAsJsonObject().getString("caseId"), is(caseId));
        assertThat(response.payloadAsJsonObject().getJsonObject("caseDetails").getString("caseURN"), is(caseURN));
        assertThat(response.payloadAsJsonObject().getJsonObject("caseDetails").getJsonArray("caseMarkers"), nullValue());

        final JsonArray defendants = response.payloadAsJsonObject().getJsonArray("defendants");
        assertThat(defendants, notNullValue());
        assertThat(defendants.size(), is(1));
        final JsonArray defendantJudicialResults = defendants.getJsonObject(0).getJsonArray("defendantJudicialResults");
        assertThat(defendantJudicialResults.size(), is(4));
        assertThat(defendantJudicialResults.getJsonObject(0).getJsonString("label").getString(), is(LABEL1));
        assertThat(defendantJudicialResults.getJsonObject(1).getJsonString("label").getString(), is(LABEL2));
        assertThat(defendantJudicialResults.getJsonObject(2).getJsonString("label").getString(), is(LABEL3));
        assertThat(defendantJudicialResults.getJsonObject(3).getJsonString("label").getString(), is(LABEL4));
    }


    @Test
    public void shouldFindApplicationsLinkedToCaseAtAGlanceProsecutionCase() {
        final String caseURN = "05PP1000915";
        final JsonEnvelope jsonEnvelope = buildEnvelope(PROGRESSION_QUERY_PROSECUTIONCASE_CAAG, "progression.query.prosecutioncase.caag.with.urn.json");
        final JsonObject prosecutionCaseEntityJson = jsonEnvelope.payloadAsJsonObject().getJsonObject(PROSECUTION_CASE);

        final JsonEnvelope envelopeWithCaseId = buildEnvelopeWithCaseId(PROGRESSION_QUERY_PROSECUTIONCASE_CAAG);
        final String caseId = envelopeWithCaseId.payloadAsJsonObject().getString("caseId");

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(fromString(caseId));
        prosecutionCaseEntity.setPayload(prosecutionCaseEntityJson.toString());

        final CourtApplication courtApplication = getCourtApplicationWithLegalEntityDefendant();

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setLinkedCaseId(LINKED_CASE_ID);
        courtApplicationEntity.setPayload("{}");

        final GetHearingsAtAGlance getCaseAtAGlance = getHearingsAtAGlance()
                .withHearings(asList(
                        hearings()
                                .build()
                ))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        when(prosecutionCaseRepository.findByCaseId(any(UUID.class))).thenReturn(prosecutionCaseEntity);
        when(courtApplicationRepository.findByLinkedCaseId(fromString(caseId))).thenReturn(asList(courtApplicationEntity));
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(prosecutionCaseEntityJson);
        when(jsonObjectToObjectConverter.convert(prosecutionCaseEntityJson, ProsecutionCase.class))
                .thenReturn(getProsecutionCase(caseURN));
        when(referenceDataService.getProsecutor(anyString())).thenReturn(Optional.empty());

        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert("{}"), CourtApplication.class))
                .thenReturn(courtApplication);
        when(hearingAtAGlanceService.getHearingAtAGlance(fromString(caseId))).thenReturn(getCaseAtAGlance);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(envelopeWithCaseId);

        assertThat(response.payloadAsJsonObject().get("linkedApplications"), notNullValue());
    }


    @Test
    public void shouldFindApplicationsLinkedToProsecutionCase() {
        final UUID caseId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload("{}");

        final List<CourtApplication> courtApplications = new ArrayList<>();
        final CourtApplication courtApplication = getCourtApplication();
        courtApplications.add(courtApplication);
        final GetHearingsAtAGlance getCaseAtAGlance = getHearingsAtAGlance()
                .withHearings(asList(hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .withCourtApplications(courtApplications)
                .build();

        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        final Material material = Material.material().withId(randomUUID()).build();
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(this.jsonObject);
        when(courtDocumentRepository.findByProsecutionCaseId(caseId)).thenReturn(asList(courtDocumentEntity));
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtDocument.class)).thenReturn(CourtDocument.courtDocument().withIsRemoved(false).withMaterials(Collections.singletonList(material)).build());
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtDocument.class)).thenReturn(CourtDocument.courtDocument().withMaterials(Collections.singletonList(material)).build());
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setLinkedCaseId(LINKED_CASE_ID);
        courtApplicationEntity.setPayload("{}");
        when(courtApplicationRepository.findByLinkedCaseId(caseId)).thenReturn(asList(courtApplicationEntity));
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtApplication.class)).thenReturn(courtApplication);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("linkedApplicationsSummary"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("hearingsAtAGlance"), notNullValue());
    }


    @Test
    public void shouldFindApplicationsLinkedToProsecutionCaseWithLegalEntityDefendant() {
        final UUID caseId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload("{}");

        final List<CourtApplication> courtApplications = new ArrayList<>();
        final CourtApplication courtApplication = getCourtApplicationWithLegalEntityDefendant();
        courtApplications.add(courtApplication);
        final GetHearingsAtAGlance getCaseAtAGlance = getHearingsAtAGlance()
                .withHearings(asList(hearings().build()))
                .withDefendantHearings(asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .withCourtApplications(courtApplications)
                .build();

        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        final Material material = Material.material().withId(randomUUID()).build();
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(this.jsonObject);
        when(courtDocumentRepository.findByProsecutionCaseId(caseId)).thenReturn(asList(courtDocumentEntity));
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtDocument.class)).thenReturn(CourtDocument.courtDocument().withIsRemoved(false).withMaterials(Collections.singletonList(material)).build());
        when(hearingAtAGlanceService.getHearingAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtDocument.class)).thenReturn(CourtDocument.courtDocument().withMaterials(Collections.singletonList(material)).build());
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setLinkedCaseId(LINKED_CASE_ID);
        courtApplicationEntity.setPayload("{}");
        when(courtApplicationRepository.findByLinkedCaseId(caseId)).thenReturn(asList(courtApplicationEntity));
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtApplication.class)).thenReturn(courtApplication);

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
        when(searchCaseRepository.findBySearchCriteria(prepareSearch(searchCriteria))).thenReturn(searchProsecutionCaseEntities);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(jsonObject);
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

        when(searchCaseRepository.findBySearchCriteria(searchCriteria)).thenReturn(new ArrayList<>());
        final JsonEnvelope response = prosecutionCaseQuery.searchCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("searchResults").size(), is(0));
    }

    private CourtApplication getCourtApplication() {
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withType(CourtApplicationType.courtApplicationType().withApplicationType("Apil").build())
                .withApplicationReference(APPLICATION_ARN)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName(APPLICANT_FIRST_NAME)
                                .withLastName(APPLICANT_LAST_NAME)
                                .withMiddleName(APPLICANT_MIDDLE_NAME)
                                .build())
                        .build())
                .withRespondents(asList(CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                        .withName(APPLICATION_PROSECUTOR_NAME)
                                        .build())
                                .build())
                        .build(), CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                .withOrganisation(Organisation.organisation()
                                        .withName(RESPONDENTS_ORG_NAME)
                                        .build())
                                .build())
                        .build()))
                .build();
    }

    private List<Hearings> getHearingsList(final UUID masterDefendantId) {
        return asList(
                hearings()
                        .withDefendantJudicialResults(asList(
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withJudicialResult(JudicialResult.judicialResult()
                                                .withLabel(LABEL1)
                                                .build())
                                        .withMasterDefendantId(masterDefendantId)
                                        .build(),
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withJudicialResult(JudicialResult.judicialResult()
                                                .withLabel(LABEL2)
                                                .build())
                                        .withMasterDefendantId(masterDefendantId)
                                        .build()
                        ))
                        .build(),
                hearings()
                        .withDefendantJudicialResults(asList(
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withJudicialResult(JudicialResult.judicialResult()
                                                .withLabel(LABEL3)
                                                .build())
                                        .withMasterDefendantId(masterDefendantId)
                                        .build(),
                                DefendantJudicialResult.defendantJudicialResult()
                                        .withJudicialResult(JudicialResult.judicialResult()
                                                .withLabel(LABEL4)
                                                .build())
                                        .withMasterDefendantId(masterDefendantId)
                                        .build()
                        ))
                        .build()
        );
    }

    private CourtApplication getCourtApplicationWithLegalEntityDefendant() {
        final Defendant defendant = defendant().
                withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName("ABC LTD").build()).build()).build();
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withType(CourtApplicationType.courtApplicationType().withApplicationType("Apil").build())
                .withApplicationReference(APPLICATION_ARN)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withDefendant(defendant)
                        .build())
                .withRespondents(asList(CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                        .withName(APPLICATION_PROSECUTOR_NAME)
                                        .build())
                                .build())
                        .build(), CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty().withDefendant(defendant)
                                .withOrganisation(Organisation.organisation()
                                        .withName(RESPONDENTS_ORG_NAME)
                                        .build())
                                .build())
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

    private JsonEnvelope buildEnvelopeWithCaseId(final String eventName) {
        return envelopeFrom(
                DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(eventName).createdAt(ZonedDateTime.now()),
                createObjectBuilder().add("caseId", randomUUID().toString()).build());
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
}
