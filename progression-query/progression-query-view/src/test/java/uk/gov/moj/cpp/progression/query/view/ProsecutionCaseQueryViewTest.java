package uk.gov.moj.cpp.progression.query.view;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.query.utils.SearchQueryUtils.prepareSearch;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    private JsonObject courtDocumentJson;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter ;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ProsecutionCaseQuery prosecutionCaseQuery;


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

        final CourtDocumentEntity courtDocumentEntity=new CourtDocumentEntity();
        final Material material = Material.material().withId(randomUUID()).build();
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(courtDocumentJson);
        when(courtDocumentRepository.findByProsecutionCaseId(caseId)).thenReturn(Arrays.asList(courtDocumentEntity));
        when(jsonObjectToObjectConverter.convert(courtDocumentJson, CourtDocument.class)).thenReturn(CourtDocument.courtDocument().withIsRemoved(false).withMaterials(Collections.singletonList(material)).build());
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(courtDocumentJson);
        final JsonEnvelope response = prosecutionCaseQuery.getProsecutionCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("courtDocuments").size(), is(1));
        assertThat(response.payloadAsJsonObject().get("prosecutionCase"), notNullValue());
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
}


