package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CpsSendNotificationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CpsSendNotificationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedCourtDocumentRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @InjectMocks
    private SharedCourtDocumentsQueryView sharedCourtDocumentsQueryView;

    @Mock
    private CpsSendNotificationRepository cpsSendNotificationRepository;

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

        when(courtDocumentRepository.findByCourtDocumentIdsAndAreNotRemoved(singletonList(mockCourtDocumentEntity.getCourtDocumentId()))).thenReturn(Arrays.asList(mockCourtDocumentEntity));
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);
        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(courtDocument(courtDocumentId));

        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(caseId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(defendantId))
                .withHearingIds(asList(randomUUID()))
                .withDocument(courtDocument(courtDocumentId))
                .withType("Defendant profile notes");

        when(courtDocumentTransform.transform(Mockito.any(), Mockito.any())).thenReturn(courtDocumentIndexBuilder);

        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);
        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);

        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(1));
        assertThat(((JsonObject) sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").get(0)).getJsonObject("document").getString("courtDocumentId"), is(courtDocumentId.toString()));
    }

    @Test
    public void shouldReturnCourtDocumentPayloadsForCaseLevel() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<SharedCourtDocumentEntity> mockSharedCourtDocuments = getMockSharedCourtDocumentEntities(courtDocumentId, hearingId, userGroupId, caseId, defendantId);
        final CourtDocumentEntity mockCourtDocumentEntity = getMockCourtDocumentEntity(courtDocumentId);

        when(courtDocumentRepository.findByCourtDocumentIdsAndAreNotRemoved(singletonList(mockCourtDocumentEntity.getCourtDocumentId()))).thenReturn(Arrays.asList(mockCourtDocumentEntity));
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);
        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(generateCaseLevelDocument(courtDocumentId));

        when(courtDocumentTransform.transform(Mockito.any(), Mockito.any())).thenCallRealMethod();

        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);
        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);

        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(1));
        assertThat(((JsonObject) sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").get(0)).getJsonObject("document").getString("courtDocumentId"), is(courtDocumentId.toString()));
    }

    @Test
    public void shouldReturnCourtDocumentPayloadsForDefendantLevelDocument() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<SharedCourtDocumentEntity> mockSharedCourtDocuments = getMockSharedCourtDocumentEntities(courtDocumentId, hearingId, userGroupId, caseId, defendantId);
        final CourtDocumentEntity mockCourtDocumentEntity = getMockCourtDocumentEntity(courtDocumentId);

        when(courtDocumentRepository.findByCourtDocumentIdsAndAreNotRemoved(singletonList(mockCourtDocumentEntity.getCourtDocumentId()))).thenReturn(Arrays.asList(mockCourtDocumentEntity));
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);
        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(generateDefendantLevelDocument(courtDocumentId, defendantId));

        when(courtDocumentTransform.transform(Mockito.any(), Mockito.any())).thenCallRealMethod();

        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);
        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);

        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(1));
        assertThat(((JsonObject) sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").get(0)).getJsonObject("document").getString("courtDocumentId"), is(courtDocumentId.toString()));
    }

    @Test
    public void shouldNotReturnCourtDocumentPayloadsForDefendantLevelDocumentWhichIsBelongsToOtherDefendant() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<SharedCourtDocumentEntity> mockSharedCourtDocuments = getMockSharedCourtDocumentEntities(courtDocumentId, hearingId, userGroupId, caseId, defendantId);
        final CourtDocumentEntity mockCourtDocumentEntity = getMockCourtDocumentEntity(courtDocumentId);

        when(courtDocumentRepository.findBy(courtDocumentId)).thenReturn(mockCourtDocumentEntity);
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);
        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(generateDefendantLevelDocument(courtDocumentId, randomUUID()));

        when(courtDocumentTransform.transform(Mockito.any(), Mockito.any())).thenCallRealMethod();

        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);
        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);

        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(0));
    }

    @Test
    public void shouldReturnCourtDocumentPayloadsForNowDocument() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<SharedCourtDocumentEntity> mockSharedCourtDocuments = getMockSharedCourtDocumentEntities(courtDocumentId, hearingId, userGroupId, caseId, defendantId);
        final CourtDocumentEntity mockCourtDocumentEntity = getMockCourtDocumentEntity(courtDocumentId);

        when(courtDocumentRepository.findByCourtDocumentIdsAndAreNotRemoved(singletonList(mockCourtDocumentEntity.getCourtDocumentId()))).thenReturn(Arrays.asList(mockCourtDocumentEntity));
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);
        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(generateNowDocument(courtDocumentId, defendantId));

        when(courtDocumentTransform.transform(Mockito.any(), Mockito.any())).thenCallRealMethod();

        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);
        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);

        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(1));
        assertThat(((JsonObject) sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").get(0)).getJsonObject("document").getString("courtDocumentId"), is(courtDocumentId.toString()));
    }

    @Test
    public void shouldNotReturnCourtDocumentPayloadsForNowDocumentWhichIsBelongsToOtherDefendant() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<SharedCourtDocumentEntity> mockSharedCourtDocuments = getMockSharedCourtDocumentEntities(courtDocumentId, hearingId, userGroupId, caseId, defendantId);
        final CourtDocumentEntity mockCourtDocumentEntity = getMockCourtDocumentEntity(courtDocumentId);

        when(courtDocumentRepository.findBy(courtDocumentId)).thenReturn(mockCourtDocumentEntity);
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);
        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(generateNowDocument(courtDocumentId, randomUUID()));

        when(courtDocumentTransform.transform(Mockito.any(), Mockito.any())).thenCallRealMethod();

        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);
        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);

        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(0));
    }

    @Test
    public void shouldReturnCourtDocumentPayloadsForApplicationDocument() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<SharedCourtDocumentEntity> mockSharedCourtDocuments = getMockSharedCourtDocumentEntities(courtDocumentId, hearingId, userGroupId, caseId, defendantId);
        final CourtDocumentEntity mockCourtDocumentEntity = getMockCourtDocumentEntity(courtDocumentId);

        when(courtDocumentRepository.findByCourtDocumentIdsAndAreNotRemoved(singletonList(mockCourtDocumentEntity.getCourtDocumentId()))).thenReturn(Arrays.asList(mockCourtDocumentEntity));
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);
        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(generateApplicationDocument(courtDocumentId));

        when(courtDocumentTransform.transform(Mockito.any(),Mockito.any())).thenCallRealMethod();

        when(cpsSendNotificationRepository.findBy(mockCourtDocumentEntity.getCourtDocumentId())).thenReturn(createCpsSendNotificationEntity(mockCourtDocumentEntity.getCourtDocumentId()));

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

    @Test
    public void shouldReturnEmptySharedCourtDocumentsOnBatchWhenCourtDocumentsReturnsEmpty() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<SharedCourtDocumentEntity> mockSharedCourtDocuments = getMockSharedCourtDocumentEntities(courtDocumentId, hearingId, userGroupId);
        final CourtDocumentEntity mockCourtDocumentEntity = getMockCourtDocumentEntity(courtDocumentId);

        when(courtDocumentRepository.findByCourtDocumentIdsAndAreNotRemoved(singletonList(mockCourtDocumentEntity.getCourtDocumentId()))).thenReturn(Arrays.asList());
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);
        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(courtDocument(courtDocumentId));

        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(caseId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(defendantId))
                .withHearingIds(asList(randomUUID()))
                .withDocument(courtDocument(courtDocumentId))
                .withType("Defendant profile notes");

        when(courtDocumentTransform.transform(Mockito.any(),Mockito.any())).thenReturn(courtDocumentIndexBuilder);

        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);
        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);

        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(0));

    }

    @Test
    public void shouldReturnFourSharedCourtDocumentsOnBatchWiseWhenCourtDocumentsRepositoryMockReturnsTwoRecordsForEachBatch() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final CourtDocumentEntity mockCourtDocumentEntity = getMockCourtDocumentEntity(courtDocumentId);

        final List<SharedCourtDocumentEntity> sharedCourtDocumentEntities = new ArrayList<>();
        IntStream.range(0, 51).forEach(operand -> sharedCourtDocumentEntities.add(new SharedCourtDocumentEntity(randomUUID(), courtDocumentId, hearingId, randomUUID(), null, null, null, null, null)));
        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(sharedCourtDocumentEntities);

        final List<CourtDocumentEntity> courtDocumentEntities = new ArrayList<>();
        IntStream.range(0, 2).forEach(operand -> courtDocumentEntities.add(mockCourtDocumentEntity));
        when(courtDocumentRepository.findByCourtDocumentIdsAndAreNotRemoved(any())).thenReturn(courtDocumentEntities);

        when(jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(mockCourtDocumentEntity.getPayload()), CourtDocument.class)).thenReturn(courtDocument(courtDocumentId));

        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(caseId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(defendantId))
                .withHearingIds(asList(randomUUID()))
                .withDocument(courtDocument(courtDocumentId))
                .withType("Defendant profile notes");

        when(courtDocumentTransform.transform(Mockito.any(), Mockito.any())).thenReturn(courtDocumentIndexBuilder);

        final JsonObjectBuilder body = createObjectBuilder().add("hearingId", hearingId.toString()).add("userGroupId", userGroupId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString());
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);

        final JsonEnvelope sharedCourtDocumentsEnvelope = sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope);
        assertThat(sharedCourtDocumentsEnvelope.payloadAsJsonObject().getJsonArray("documentIndices").size(), is(4));

    }

    private CourtDocument courtDocument(final UUID id) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentTypeId(randomUUID())
                .withMaterials(asList(
                        Material.material()
                                .withId(randomUUID())
                                .withUserGroups(asList("Court Clerks"))
                                .build()
                ))
                .withContainsFinancialMeans(false)
                .build();
    }

    private CourtDocument generateCaseLevelDocument(final UUID courtDocumentId) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(randomUUID()).build()).build())
                .withMaterials(asList(
                        Material.material()
                                .withId(randomUUID())
                                .withUserGroups(asList("Court Clerks"))
                                .build()
                ))
                .withContainsFinancialMeans(false)
                .build();
    }

    private CourtDocument generateDefendantLevelDocument(final UUID courtDocumentId, final UUID defendantId) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withDefendantDocument(DefendantDocument.defendantDocument().withProsecutionCaseId(randomUUID()).withDefendants(singletonList(defendantId)).build()).build())
                .withMaterials(asList(
                        Material.material()
                                .withId(randomUUID())
                                .withUserGroups(asList("Court Clerks"))
                                .build()
                ))
                .withContainsFinancialMeans(false)
                .build();
    }

    private CourtDocument generateApplicationDocument(final UUID courtDocumentId) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withApplicationDocument(ApplicationDocument.applicationDocument().withApplicationId(randomUUID()).withProsecutionCaseId(randomUUID()).build()).build())
                .withMaterials(asList(
                        Material.material()
                                .withId(randomUUID())
                                .withUserGroups(asList("Court Clerks"))
                                .build()
                ))
                .withContainsFinancialMeans(false)
                .build();
    }

    private CourtDocument generateNowDocument(final UUID courtDocumentId, final UUID defendantId) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withNowDocument(NowDocument.nowDocument().withProsecutionCases(singletonList(randomUUID())).withDefendantId(defendantId).build()).build())
                .withMaterials(asList(
                        Material.material()
                                .withId(randomUUID())
                                .withUserGroups(asList("Court Clerks"))
                                .build()
                ))
                .withContainsFinancialMeans(false)
                .build();
    }


    private List<SharedCourtDocumentEntity> getMockSharedCourtDocumentEntities(final UUID courtDocumentId, final UUID hearingId, final UUID groupId) {
        return singletonList(new SharedCourtDocumentEntity(randomUUID(), courtDocumentId, hearingId, groupId, null, null, null, null, null));
    }

    private List<SharedCourtDocumentEntity> getMockSharedCourtDocumentEntities(final UUID courtDocumentId, final UUID hearingId, final UUID groupId, final UUID caseId, final UUID defendantId) {
        return singletonList(new SharedCourtDocumentEntity(randomUUID(), courtDocumentId, hearingId, groupId, null, caseId, defendantId, null, null));
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

    private CpsSendNotificationEntity createCpsSendNotificationEntity(final UUID courtDocumentId){
        final CpsSendNotificationEntity cpsSendNotificationEntity = new CpsSendNotificationEntity();
        cpsSendNotificationEntity.setSendToCps(true);
        cpsSendNotificationEntity.setCourtDocumentId(courtDocumentId);

        return cpsSendNotificationEntity;
    }
}