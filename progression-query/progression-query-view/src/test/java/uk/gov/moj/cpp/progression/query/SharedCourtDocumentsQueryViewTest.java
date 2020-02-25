package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedCourtDocumentRepository;

import java.io.StringReader;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SharedCourtDocumentsQueryViewTest {

    @Mock
    private SharedCourtDocumentRepository sharedCourtDocumentRepository;

    @Mock
    private CourtDocumentRepository courtDocumentRepository;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private CourtDocumentTransform courtDocumentTransform;

    @InjectMocks
    private SharedCourtDocumentsQueryView sharedCourtDocumentsQueryView;

    @Before
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowExceptionWhenCaseIDNotSentInTheRequest() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().add("hearingId",
                hearingId.toString()).add("userGroupId", userGroupId.toString()).add("defendantId", defendantId.toString()));
        sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowExceptionWhenDefendantIDNotSentInTheRequest() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID caseId = randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().add("hearingId",
                hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()));
        sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowExceptionWhenDefendantAndCaseIDNotSentInTheRequest() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().add("hearingId",
                hearingId.toString()).add("userGroupId", userGroupId.toString()));
        sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);
    }

    @Test
    public void shouldReturnSharedCourtDocumentPayloads() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<SharedCourtDocumentEntity> mockSharedCourtDocuments = getMockSharedCourtDocumentEntities(courtDocumentId, hearingId, userGroupId);
        final CourtDocumentEntity mockCourtDocumentEntity = getMockCourtDocumentEntity(courtDocumentId);

        when(courtDocumentRepository.findBy(courtDocumentId)).thenReturn(mockCourtDocumentEntity);
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);
        when(jsonObjectToObjectConverter.convert(jsonFromString(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(courtDocument(courtDocumentId));

        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(caseId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(UUID.randomUUID()))
                .withHearingIds(asList(UUID.randomUUID()))
                .withDocument(courtDocument(courtDocumentId))
                .withType("Defendant profile notes");

        when(courtDocumentTransform.transform(Mockito.any())).thenReturn(courtDocumentIndexBuilder);

        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);
        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);

        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(1));
        assertThat(((JsonObject) sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").get(0)).getJsonObject("document").getString("courtDocumentId"), is(courtDocumentId.toString()));
    }

    @Test
    public void shouldReturnEmptyListWhenNoRecordsInSharedCourtDocumentsDatabase() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(emptyList());
        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());


        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);
        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);

        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(0));
    }



    private static JsonObject jsonFromString(final String jsonObjectStr) {
        try (final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            return jsonReader.readObject();
        }
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
                .withContainsFinancialMeans(false)
                .build();
    }



    private List<SharedCourtDocumentEntity> getMockSharedCourtDocumentEntities(final UUID courtDocumentId, final UUID hearingId, final UUID groupId) {
        return singletonList(new SharedCourtDocumentEntity(randomUUID(), courtDocumentId, hearingId, groupId, null, null, null,null));
    }


    private CourtDocumentEntity getMockCourtDocumentEntity(final UUID courtDocumentId) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setPayload(getCourtDocumentPayload(courtDocumentId).toString());
        courtDocumentEntity.setIsRemoved(false);
        return courtDocumentEntity;
    }

    private JsonObject getCourtDocumentPayload(final UUID courtDocumentId) {
        return createObjectBuilder()
                .add("PayloadThings", "PayloadThings")
                .add("courtDocumentId", courtDocumentId.toString())
                .build();
    }
}