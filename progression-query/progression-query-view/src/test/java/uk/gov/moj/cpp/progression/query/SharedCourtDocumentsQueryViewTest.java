package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.string;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.justice.progression.courts.SharedCourtDocumentsLinksForApplication;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.progression.query.view.service.SharedAllCourtDocumentsService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CpsSendNotificationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CpsSendNotificationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedCourtDocumentRepository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @Spy
    private ListToJsonArrayConverter<SharedCourtDocumentsLinksForApplication> sharedCourtDocumentsLinksForApplicationListToJsonArrayConverter;

    @InjectMocks
    private SharedCourtDocumentsQueryView sharedCourtDocumentsQueryView;

    @Mock
    private CpsSendNotificationRepository cpsSendNotificationRepository;

    @Mock
    private HearingApplicationRepository hearingApplicationRepository;

    @Mock
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Mock
    private SharedAllCourtDocumentsService sharedAllCourtDocumentsService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.sharedCourtDocumentsLinksForApplicationListToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.sharedCourtDocumentsLinksForApplicationListToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
    }

    @Test
    public void shouldThrowExceptionWhenCaseIDNotSentInTheRequest() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().add("hearingId",
                hearingId.toString()).add("userGroupId", userGroupId.toString()).add("defendantId", defendantId.toString()));
        assertThrows(BadRequestException.class, () -> sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope));
    }

    @Test
    public void shouldThrowExceptionWhenDefendantAndCaseIDNotSentInTheRequest() {
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().add("hearingId",
                hearingId.toString()).add("userGroupId", userGroupId.toString()));
        assertThrows(BadRequestException.class, () -> sharedCourtDocumentsQueryView.getSharedCourtDocuments(jsonEnvelope));
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

        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);

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

        when(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId)).thenReturn(mockSharedCourtDocuments);

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

    @Test
    void shouldGetApplicationSharedCourtDocumentsLinksWhenCaseInActiveApplicationHearingIsTrial() {
        final UUID applicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final JsonObject body = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .add("hearingId", hearingId.toString())
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);

        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final String caseUrn1 = string(8).next();
        final String caseUrn2 = string(8).next();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final ProsecutionCaseEntity prosecutionCaseEntity1 = new ProsecutionCaseEntity();
        prosecutionCaseEntity1.setCaseId(caseId1);
        prosecutionCaseEntity1.setPayload("{\"defendants\":[{\"id\":\""+defendantId1+"\"}],\"caseStatus\":\"INACTIVE\"}");
        final ProsecutionCaseEntity prosecutionCaseEntity2 = new ProsecutionCaseEntity();
        prosecutionCaseEntity2.setCaseId(caseId2);
        prosecutionCaseEntity2.setPayload("{\"defendants\":[{\"id\":\""+defendantId2+"\"}],\"caseStatus\":\"INACTIVE\"}");

        final CourtApplicationCaseEntity courtApplicationCase1 = new CourtApplicationCaseEntity();
        courtApplicationCase1.setId(new CourtApplicationCaseKey(randomUUID(), applicationId, caseId1));
        courtApplicationCase1.setCaseReference(caseUrn1);

        final CourtApplicationCaseEntity courtApplicationCase2 = new CourtApplicationCaseEntity();
        courtApplicationCase2.setId(new CourtApplicationCaseKey(randomUUID(), applicationId, caseId2));
        courtApplicationCase2.setCaseReference(caseUrn2);

        when(courtApplicationCaseRepository.findByApplicationId(applicationId)).thenReturn(asList(courtApplicationCase1, courtApplicationCase2));
        when(prosecutionCaseRepository.findByCaseId(caseId1)).thenReturn(prosecutionCaseEntity1);
        when(prosecutionCaseRepository.findByCaseId(caseId2)).thenReturn(prosecutionCaseEntity2);
        final SharedCourtDocumentsLinksForApplication sharedCourtDocumentsLinksForApplication1 = SharedCourtDocumentsLinksForApplication.sharedCourtDocumentsLinksForApplication()
                .withDefendantId(randomUUID())
                .withDefendantName(string(12).next())
                .build();
        final SharedCourtDocumentsLinksForApplication sharedCourtDocumentsLinksForApplication2 = SharedCourtDocumentsLinksForApplication.sharedCourtDocumentsLinksForApplication()
                .withDefendantId(randomUUID())
                .withDefendantName(string(12).next())
                .build();
        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        HearingEntity hearingEntity = new HearingEntity();
        final String trialTypeId = randomUUID().toString();
        hearingEntity.setPayload(("{\"type\":{\"description\":\"Application\",\"id\":\"" + trialTypeId + "\"}}").replaceAll("%NOW%", ZonedDateTime.now().toString()));
        hearingApplicationEntity.setHearing(hearingEntity);
        final JsonArray jsonHearingTypesArray = JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder().add("id", trialTypeId).add("trialTypeFlag", true).build())
                .add(JsonObjects.createObjectBuilder().add("id", randomUUID().toString()).add("trialTypeFlag", false).build())
                .build();

        when(hearingApplicationRepository.findBy(new HearingApplicationKey(applicationId, hearingId))).thenReturn(hearingApplicationEntity);
        when(referenceDataService.getHearingTypes(jsonEnvelope)).thenReturn(jsonHearingTypesArray);
        when(sharedAllCourtDocumentsService.getSharedAllCourtDocumentsForTrialHearing(jsonEnvelope, caseId1, caseUrn1, singletonList(Defendant.defendant().withId(defendantId1).build()), hearingId))
                .thenReturn(singletonList(sharedCourtDocumentsLinksForApplication1));
        when(sharedAllCourtDocumentsService.getSharedAllCourtDocumentsForTrialHearing(jsonEnvelope, caseId2, caseUrn2, singletonList(Defendant.defendant().withId(defendantId2).build()), hearingId))
                .thenReturn(singletonList(sharedCourtDocumentsLinksForApplication2));

        final JsonEnvelope result = sharedCourtDocumentsQueryView.getApplicationSharedCourtDocumentsLinks(jsonEnvelope);

        final JsonArray sharedCourtDocumentsSummary = result.asJsonObject().getJsonArray("sharedCourtDocumentsLinksForApplication");
        assertThat(sharedCourtDocumentsSummary.size(), is(2));
        assertThat(jsonObjectToObjectConverter.convert((JsonObject) sharedCourtDocumentsSummary.get(0), SharedCourtDocumentsLinksForApplication.class), is(sharedCourtDocumentsLinksForApplication1));
        assertThat(jsonObjectToObjectConverter.convert((JsonObject) sharedCourtDocumentsSummary.get(1), SharedCourtDocumentsLinksForApplication.class), is(sharedCourtDocumentsLinksForApplication2));

    }

    @Test
    void shouldGetApplicationSharedCourtDocumentsLinksWhenCaseInactiveAndApplicationHearingIsNotTrial() {
        final UUID applicationId = randomUUID();
        final UUID applicationHearingId = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final String caseUrn1 = string(8).next();
        final String caseUrn2 = string(8).next();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final JsonObject body = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .add("hearingId", applicationHearingId.toString())
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);

        final ProsecutionCaseEntity prosecutionCaseEntity1 = new ProsecutionCaseEntity();
        prosecutionCaseEntity1.setCaseId(caseId1);
        prosecutionCaseEntity1.setPayload("{\"defendants\":[{\"id\":\""+defendantId1+"\"}],\"caseStatus\":\"INACTIVE\"}");
        final ProsecutionCaseEntity prosecutionCaseEntity2 = new ProsecutionCaseEntity();
        prosecutionCaseEntity2.setCaseId(caseId2);
        prosecutionCaseEntity2.setPayload("{\"defendants\":[{\"id\":\""+defendantId2+"\"}],\"caseStatus\":\"INACTIVE\"}");

        final CourtApplicationCaseEntity courtApplicationCase1 = new CourtApplicationCaseEntity();
        courtApplicationCase1.setId(new CourtApplicationCaseKey(randomUUID(), applicationId, caseId1));
        courtApplicationCase1.setCaseReference(caseUrn1);

        final CourtApplicationCaseEntity courtApplicationCase2 = new CourtApplicationCaseEntity();
        courtApplicationCase2.setId(new CourtApplicationCaseKey(randomUUID(), applicationId, caseId2));
        courtApplicationCase2.setCaseReference(caseUrn2);

        when(courtApplicationCaseRepository.findByApplicationId(applicationId)).thenReturn(asList(courtApplicationCase1, courtApplicationCase2));
        when(prosecutionCaseRepository.findByCaseId(caseId1)).thenReturn(prosecutionCaseEntity1);
        when(prosecutionCaseRepository.findByCaseId(caseId2)).thenReturn(prosecutionCaseEntity2);

        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        HearingEntity hearingEntity = new HearingEntity();
        final String nonTrialTypeId = randomUUID().toString();
        hearingEntity.setPayload(("{\"type\":{\"description\":\"Application\",\"id\":\"" + nonTrialTypeId + "\"}}"));
        hearingApplicationEntity.setHearing(hearingEntity);
        final JsonArray jsonHearingTypesArray = JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder().add("id", randomUUID().toString()).add("trialTypeFlag", true).build())
                .add(JsonObjects.createObjectBuilder().add("id", nonTrialTypeId).add("trialTypeFlag", false).build())
                .build();

        when(hearingApplicationRepository.findBy(new HearingApplicationKey(applicationId, applicationHearingId))).thenReturn(hearingApplicationEntity);
        when(referenceDataService.getHearingTypes(jsonEnvelope)).thenReturn(jsonHearingTypesArray);
        final SharedCourtDocumentsLinksForApplication sharedCourtDocumentsLinksForApplication1 = SharedCourtDocumentsLinksForApplication.sharedCourtDocumentsLinksForApplication()
                .withDefendantId(randomUUID())
                .withDefendantName(string(12).next())
                .build();
        final SharedCourtDocumentsLinksForApplication sharedCourtDocumentsLinksForApplication2 = SharedCourtDocumentsLinksForApplication.sharedCourtDocumentsLinksForApplication()
                .withDefendantId(randomUUID())
                .withDefendantName(string(12).next())
                .build();
        when(sharedAllCourtDocumentsService.getSharedAllCourtDocuments(caseId1, caseUrn1, singletonList(Defendant.defendant().withId(defendantId1).build())))
                .thenReturn(singletonList(sharedCourtDocumentsLinksForApplication1));
        when(sharedAllCourtDocumentsService.getSharedAllCourtDocuments(caseId2, caseUrn2, singletonList(Defendant.defendant().withId(defendantId2).build())))
                .thenReturn(singletonList(sharedCourtDocumentsLinksForApplication2));

        final JsonEnvelope result = sharedCourtDocumentsQueryView.getApplicationSharedCourtDocumentsLinks(jsonEnvelope);

        final JsonArray sharedCourtDocumentsSummary = result.asJsonObject().getJsonArray("sharedCourtDocumentsLinksForApplication");
        assertThat(jsonObjectToObjectConverter.convert((JsonObject) sharedCourtDocumentsSummary.get(0), SharedCourtDocumentsLinksForApplication.class), is(sharedCourtDocumentsLinksForApplication1));
        assertThat(jsonObjectToObjectConverter.convert((JsonObject) sharedCourtDocumentsSummary.get(1), SharedCourtDocumentsLinksForApplication.class), is(sharedCourtDocumentsLinksForApplication2));
    }

    @Test
    void shouldGetApplicationSharedCourtDocumentsLinksWhenCaseNotInactive() {
        final UUID applicationId = randomUUID();
        final UUID applicationHearingId = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final String caseUrn1 = string(8).next();
        final String caseUrn2 = string(8).next();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final JsonObject body = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .add("hearingId", applicationHearingId.toString())
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), body);

        final ProsecutionCaseEntity prosecutionCaseEntity1 = new ProsecutionCaseEntity();
        prosecutionCaseEntity1.setCaseId(caseId1);
        prosecutionCaseEntity1.setPayload("{\"defendants\":[{\"id\":\""+defendantId1+"\"}],\"caseStatus\":\"ACTIVE\"}");
        final ProsecutionCaseEntity prosecutionCaseEntity2 = new ProsecutionCaseEntity();
        prosecutionCaseEntity2.setCaseId(caseId2);
        prosecutionCaseEntity2.setPayload("{\"defendants\":[{\"id\":\""+defendantId2+"\"}],\"caseStatus\":\"ACTIVE\"}");

        final CourtApplicationCaseEntity courtApplicationCase1 = new CourtApplicationCaseEntity();
        courtApplicationCase1.setId(new CourtApplicationCaseKey(randomUUID(), applicationId, caseId1));
        courtApplicationCase1.setCaseReference(caseUrn1);

        final CourtApplicationCaseEntity courtApplicationCase2 = new CourtApplicationCaseEntity();
        courtApplicationCase2.setId(new CourtApplicationCaseKey(randomUUID(), applicationId, caseId2));
        courtApplicationCase2.setCaseReference(caseUrn2);

        when(courtApplicationCaseRepository.findByApplicationId(applicationId)).thenReturn(asList(courtApplicationCase1, courtApplicationCase2));
        when(prosecutionCaseRepository.findByCaseId(caseId1)).thenReturn(prosecutionCaseEntity1);
        when(prosecutionCaseRepository.findByCaseId(caseId2)).thenReturn(prosecutionCaseEntity2);

        final JsonEnvelope result = sharedCourtDocumentsQueryView.getApplicationSharedCourtDocumentsLinks(jsonEnvelope);

        final JsonArray sharedCourtDocumentsSummary = result.asJsonObject().getJsonArray("sharedCourtDocumentsLinksForApplication");
        assertThat(sharedCourtDocumentsSummary.isEmpty(), is(true));

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

    private List<SharedCourtDocumentEntity> getMockSharedCourtDocumentEntities(final UUID courtDocumentId, final UUID hearingId, final UUID groupId, final UUID caseId) {
        return singletonList(new SharedCourtDocumentEntity(randomUUID(), courtDocumentId, hearingId, groupId, null, caseId, null, null, null));
    }


    private CourtDocumentEntity getMockCourtDocumentEntity(final UUID courtDocumentId) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setPayload(getCourtDocumentPayload(courtDocumentId).toString());
        courtDocumentEntity.setIsRemoved(false);
        return courtDocumentEntity;
    }

    private CourtDocumentEntity getMockApplicationCourtDocumentEntity(final UUID courtDocumentId) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(generateApplicationDocument(courtDocumentId)).toString());
        courtDocumentEntity.setIsRemoved(false);
        return courtDocumentEntity;
    }



    private CourtDocumentEntity getMockDefendantCourtDocumentEntity(final UUID courtDocumentId, final UUID defendantId) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(generateDefendantLevelDocument(courtDocumentId, defendantId)).toString());
        courtDocumentEntity.setIsRemoved(false);
        return courtDocumentEntity;
    }

    private CourtDocumentEntity getMockCaseLevelCourtDocumentEntity(final UUID courtDocumentId) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(generateCaseLevelDocument(courtDocumentId)).toString());
        courtDocumentEntity.setIsRemoved(false);
        return courtDocumentEntity;
    }

    private CourtDocumentEntity getMockNowCourtDocumentEntity(final UUID courtDocumentId, final UUID defendantId) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(generateNowDocument(courtDocumentId, defendantId)).toString());
        courtDocumentEntity.setIsRemoved(false);
        return courtDocumentEntity;
    }

    private JsonObject getCourtDocumentPayload(final UUID courtDocumentId) {
        return createObjectBuilder()
                .add("PayloadThings", "PayloadThings")
                .add("courtDocumentId", courtDocumentId.toString())
                .build();
    }

    private HearingEntity getHearingEntity(final UUID hearingId, final ZonedDateTime sittingDay){
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(getHearingPayload(hearingId, sittingDay)).toString());
        return hearingEntity;
    }

    private Hearing getHearingPayload(final UUID hearingId, final ZonedDateTime sittingDay) {
        return Hearing.hearing()
                .withId(hearingId)
                .withHearingDays(asList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .build();
    }

    private CpsSendNotificationEntity createCpsSendNotificationEntity(final UUID courtDocumentId){
        final CpsSendNotificationEntity cpsSendNotificationEntity = new CpsSendNotificationEntity();
        cpsSendNotificationEntity.setSendToCps(true);
        cpsSendNotificationEntity.setCourtDocumentId(courtDocumentId);

        return cpsSendNotificationEntity;
    }
}