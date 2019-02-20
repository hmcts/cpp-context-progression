package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentQueryViewTest {

    @InjectMocks
    private CourtDocumentQuery target;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CourtDocumentTransform courtDocumentTransform;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Before
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    private String objectToString(final Object o) {
        final StringWriter sr = new StringWriter();
        final JsonWriter jsonWriter = Json.createWriter(sr);
        jsonWriter.writeObject(objectToJsonObjectConverter.convert(o));
        return sr.toString();
    }

    private CourtDocument courtDocument(final UUID id) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentTypeId(UUID.randomUUID())
                .withMaterials(asList(
                        Material.material()
                                .withId(UUID.randomUUID())
                                .withUserGroups(asList("Court Clerks"))
                                .build()
                ))
                .build();
    }

    @Mock
    private CourtDocumentRepository courtDocumentRepository;

    private CourtDocumentEntity courtDocumentEntity(final UUID id, CourtDocument courtDocument) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(id);
        courtDocumentEntity.setPayload(objectToString(courtDocument));
        return courtDocumentEntity;
    }

    @Test
    public void shouldFindDocumentById() throws Exception {
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.ID_PARAMETER, courtDocumentId.toString()).build();
        JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENT_SEARCH_NAME).build(),
                jsonObject);
        final CourtDocument courtDocument = courtDocument(courtDocumentId);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);

        when(courtDocumentRepository.findBy(courtDocumentId)).thenReturn(courtDocumentEntity);

        final JsonEnvelope jsonEnvelopeOut = target.getCourtDocument(jsonEnvelopeIn);
        CourtDocument courtDocumentOut = jsonObjectToObjectConverter
                .convert(jsonEnvelopeOut.payloadAsJsonObject()
                        .getJsonObject(CourtDocumentQuery.COURT_DOCUMENT_RESULT_FIELD), CourtDocument.class);
        assertThat(courtDocumentOut.getCourtDocumentId(), is(courtDocument.getCourtDocumentId()));
        assertThat(courtDocumentOut.getDocumentTypeId(), is(courtDocument.getDocumentTypeId()));
    }

    @Test
    public void shouldFindDocumentsByCaseIdPermitted() {
        shouldFindDocumentsByCaseId(true);
    }

    @Test
    public void shouldFindDocumentsByCaseIdNotPermitted() {
        shouldFindDocumentsByCaseId(false);
    }

    @Captor
    private ArgumentCaptor<CourtDocument> courtDocumentArgumentCaptor;

    private void shouldFindDocumentsByCaseId(final boolean permitted) {
        final UUID caseId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.CASE_IDS_SEARCH_PARAM, caseId.toString())
                .add(CourtDocumentQuery.CASE_IDS_SEARCH_PARAM, caseId.toString())
                .build();
        JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENTS_SEARCH_NAME).build(),
                jsonObject);
        final CourtDocument courtDocument = courtDocument(courtDocumentId);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);
        when(courtDocumentRepository.findByProsecutionCaseId(caseId)).thenReturn(asList(courtDocumentEntity));
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups((Action) Mockito.any(), (List<String>) Mockito.any())).thenReturn(permitted);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(caseId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(UUID.randomUUID()))
                .withHearingIds(asList(UUID.randomUUID()))
                .withType("Defendant profile notes");

        when(courtDocumentTransform.transform(Mockito.any())).thenReturn(courtDocumentIndexBuilder);

        CourtDocumentIndex expectedIndex = courtDocumentIndexBuilder.build();
        final JsonEnvelope jsonEnvelopeOut = target.searchcourtdocumentsByCaseId(jsonEnvelopeIn);
        CourtDocumentsSearchResult result = jsonObjectToObjectConverter
                .convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtDocumentsSearchResult.class);
        if (permitted) {
            assertThat(result.getDocumentIndices().size(), is(1));
            CourtDocumentIndex courtDocumentIndexOut = result.getDocumentIndices().get(0);
            assertThat(courtDocumentIndexOut.getType(), is(expectedIndex.getType()));
            assertThat(courtDocumentIndexOut.getCategory(), is(expectedIndex.getCategory()));
            assertThat(courtDocumentIndexOut.getDefendantIds().get(0), is(expectedIndex.getDefendantIds().get(0)));
            assertThat(courtDocumentIndexOut.getCaseIds().get(0), is(expectedIndex.getCaseIds().get(0)));
            assertThat(courtDocumentIndexOut.getHearingIds().get(0), is(expectedIndex.getHearingIds().get(0)));
            verify(courtDocumentTransform, times(1)).transform(courtDocumentArgumentCaptor.capture());

            final CourtDocument courtDocumentTransformed = courtDocumentArgumentCaptor.getValue();
            final int materialCount = courtDocumentTransformed.getMaterials().size();
            final int expectedMaterialCount = permitted ? 1 : 0;
            assertThat(materialCount, is(expectedMaterialCount));
        } else {
            assertThat(result.getDocumentIndices().size(), is(0));
        }
    }


/*    @Test
    public void shouldFindUserGroupsByMaterialId() throws Exception {
        final UUID materialId = randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("q", materialId.toString()).build();
        JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.usergroups-by-material-id").build(),
                jsonObject);

        CourtDocumentMaterialEntity courtDocumentMaterialEntity = new CourtDocumentMaterialEntity();
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
        JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.usergroups-by-material-id").build(),
                jsonObject);

        when(courtDocumentMaterialRepository.findBy(materialId)).thenReturn(null);
        final JsonEnvelope response = prosecutionCaseQuery.searchByMaterialId(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("allowedUserGroups").size(), is(0));
    }

    @Test
    public void shouldFindCaseBySearchCriteria() throws Exception {
        final UUID caseId = randomUUID();
        final String searchCriteria = "FirstName LastName";
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("q", searchCriteria.toString()).build();

        JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.search-cases").build(),
                jsonObject);

        List<SearchProsecutionCaseEntity> searchProsecutionCaseEntities = new ArrayList<>();
        SearchProsecutionCaseEntity searchProsecutionCaseEntity = new SearchProsecutionCaseEntity();
        searchProsecutionCaseEntity.setCaseId(caseId.toString());
        searchProsecutionCaseEntity.setResultPayload(Json.createObjectBuilder().build().toString());
        searchProsecutionCaseEntities.add(searchProsecutionCaseEntity);
        when(searchCaseRepository.findBySearchCriteria(prepareSearch(searchCriteria))).thenReturn(searchProsecutionCaseEntities);
        final JsonEnvelope response = prosecutionCaseQuery.searchCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("searchResults").size(), is(1));
    }

    @Test
    public void shouldNotFindCaseBySearchCriteria() throws Exception {
        final String searchCriteria = "FirstName LastName";
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("q", searchCriteria.toString()).build();
        JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.search-cases").build(),
                jsonObject);

        when(searchCaseRepository.findBySearchCriteria(searchCriteria)).thenReturn(new ArrayList<>());
        final JsonEnvelope response = prosecutionCaseQuery.searchCase(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("searchResults").size(), is(0));
    }
    */
}


