package uk.gov.moj.cpp.progression.query.view;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.query.utils.SearchQueryUtils.prepareSearch;
import static uk.gov.moj.cpp.progression.query.view.ProsecutionCaseQueryViewTest.ApplicationArbitraryValues.APPLICANT_FIRST_NAME;
import static uk.gov.moj.cpp.progression.query.view.ProsecutionCaseQueryViewTest.ApplicationArbitraryValues.APPLICANT_LAST_NAME;
import static uk.gov.moj.cpp.progression.query.view.ProsecutionCaseQueryViewTest.ApplicationArbitraryValues.APPLICANT_MIDDLE_NAME;
import static uk.gov.moj.cpp.progression.query.view.ProsecutionCaseQueryViewTest.ApplicationArbitraryValues.APPLICATION_ARN;
import static uk.gov.moj.cpp.progression.query.view.ProsecutionCaseQueryViewTest.ApplicationArbitraryValues.APPLICATION_ID;
import static uk.gov.moj.cpp.progression.query.view.ProsecutionCaseQueryViewTest.ApplicationArbitraryValues.APPLICATION_PROSECUTOR_NAME;
import static uk.gov.moj.cpp.progression.query.view.ProsecutionCaseQueryViewTest.ApplicationArbitraryValues.LINKED_CASE_ID;
import static uk.gov.moj.cpp.progression.query.view.ProsecutionCaseQueryViewTest.ApplicationArbitraryValues.RESPONDENTS_ORG_NAME;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.view.service.GetCaseAtAGlanceService;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseQueryViewTest {

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
    private StringToJsonObjectConverter stringToJsonObjectConverter ;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ProsecutionCaseQuery prosecutionCaseQuery;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Mock
    private GetCaseAtAGlanceService getCaseAtAGlanceService;


    @Test
    public void shouldFindProsecutionCaseById() throws Exception {
        final UUID caseId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final ProsecutionCaseEntity prosecutionCaseEntity=new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload("{}");

        final GetCaseAtAGlance getCaseAtAGlance = GetCaseAtAGlance.getCaseAtAGlance()
                .withHearings(Arrays.asList(Hearings.hearings().build()))
                .withDefendantHearings(Arrays.asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .build();

        final CourtDocumentEntity courtDocumentEntity=new CourtDocumentEntity();
        final Material material = Material.material().withId(randomUUID()).build();
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(this.jsonObject);
        when(courtDocumentRepository.findByProsecutionCaseId(caseId)).thenReturn(Arrays.asList(courtDocumentEntity));
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtDocument.class)).thenReturn(CourtDocument.courtDocument().withIsRemoved(false).withMaterials(Collections.singletonList(material)).build());
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(this.jsonObject);
        when(getCaseAtAGlanceService.getCaseAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("courtDocuments").size(), is(1));
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("caseAtAGlance"), notNullValue());
    }


    @Test
    public void shouldFindApplicationsLinkedToProsecutionCase() throws Exception {
        final UUID caseId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final ProsecutionCaseEntity prosecutionCaseEntity=new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload("{}");

        List <CourtApplication> courtApplications = new ArrayList<>();
        CourtApplication courtApplication = getCourtApplication();
        courtApplications.add(courtApplication);
        final GetCaseAtAGlance getCaseAtAGlance = GetCaseAtAGlance.getCaseAtAGlance()
                .withHearings(Arrays.asList(Hearings.hearings().build()))
                .withDefendantHearings(Arrays.asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .withCourtApplications(courtApplications)
                .build();

        final CourtDocumentEntity courtDocumentEntity=new CourtDocumentEntity();
        final Material material = Material.material().withId(randomUUID()).build();
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(this.jsonObject);
        when(courtDocumentRepository.findByProsecutionCaseId(caseId)).thenReturn(Arrays.asList(courtDocumentEntity));
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtDocument.class)).thenReturn(CourtDocument.courtDocument().withIsRemoved(false).withMaterials(Collections.singletonList(material)).build());
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(this.jsonObject);
        when(getCaseAtAGlanceService.getCaseAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setLinkedCaseId(LINKED_CASE_ID);
        courtApplicationEntity.setPayload("{}");
        when(courtApplicationRepository.findByLinkedCaseId(caseId)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtApplication.class)).thenReturn(courtApplication);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("linkedApplicationsSummary"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("caseAtAGlance"), notNullValue());
    }

    @Test
    public void shouldFindApplicationsLinkedToProsecutionCaseWithLegalEntityDefendant() {
        final UUID caseId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.prosecutioncase").build(),
                jsonObject);

        final ProsecutionCaseEntity prosecutionCaseEntity=new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload("{}");

        List <CourtApplication> courtApplications = new ArrayList<>();
        CourtApplication courtApplication = getCourtApplicationWithLegalEntityDefendant();
        courtApplications.add(courtApplication);
        final GetCaseAtAGlance getCaseAtAGlance = GetCaseAtAGlance.getCaseAtAGlance()
                .withHearings(Arrays.asList(Hearings.hearings().build()))
                .withDefendantHearings(Arrays.asList(DefendantHearings.defendantHearings().build()))
                .withId(randomUUID())
                .withCourtApplications(courtApplications)
                .build();

        final CourtDocumentEntity courtDocumentEntity=new CourtDocumentEntity();
        final Material material = Material.material().withId(randomUUID()).build();
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(this.jsonObject);
        when(courtDocumentRepository.findByProsecutionCaseId(caseId)).thenReturn(Arrays.asList(courtDocumentEntity));
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtDocument.class)).thenReturn(CourtDocument.courtDocument().withIsRemoved(false).withMaterials(Collections.singletonList(material)).build());
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(this.jsonObject);
        when(getCaseAtAGlanceService.getCaseAtAGlance(caseId)).thenReturn(getCaseAtAGlance);
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setLinkedCaseId(LINKED_CASE_ID);
        courtApplicationEntity.setPayload("{}");
        when(courtApplicationRepository.findByLinkedCaseId(caseId)).thenReturn(Arrays.asList(courtApplicationEntity));
        when(jsonObjectToObjectConverter.convert(this.jsonObject, CourtApplication.class)).thenReturn(courtApplication);

        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().get("linkedApplicationsSummary"), notNullValue());
        assertThat(response.payloadAsJsonObject().get("caseAtAGlance"), notNullValue());
    }

    @Test
    public void shouldFindUserGroupsByMaterialId() throws Exception {
        final UUID materialId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("q", materialId.toString()).build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.usergroups-by-material-id").build(),
                jsonObject);

        final CourtDocumentMaterialEntity courtDocumentMaterialEntity = new CourtDocumentMaterialEntity();
        courtDocumentMaterialEntity.setUserGroups(Arrays.asList("Listing Officer"));
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
                .add("q", searchCriteria.toString()).build();

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
                .withRespondents(Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
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

    private CourtApplication getCourtApplicationWithLegalEntityDefendant() {
        Defendant defendant = Defendant.defendant().
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
                .withRespondents(Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
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
}