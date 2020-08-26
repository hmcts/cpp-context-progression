package uk.gov.moj.cpp.progression.query;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.refdata.providers.RbacProvider;
import uk.gov.moj.cpp.progression.domain.constant.NotificationStatus;
import uk.gov.moj.cpp.progression.domain.constant.NotificationType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentTypeRBAC;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;


@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentQueryViewTest {

    @InjectMocks
    private CourtDocumentQuery target;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CourtDocumentTransform courtDocumentTransform;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Mock
    private CourtDocumentRepository courtDocumentRepository;

    @Captor
    private ArgumentCaptor<CourtDocument> courtDocumentArgumentCaptor;

    @Mock
    private NotificationStatusRepository notificationStatusRepository;

    @Mock
    private RbacProvider rbacProvider;

    @Mock
    private Requester requester;

    @Mock
    uk.gov.justice.services.messaging.Envelope<JsonObject> response;

    @Before
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
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
                .withSeqNum(10)
                .withContainsFinancialMeans(false)
                .build();
    }

    private CourtDocument removedCourtDocument(final UUID id) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentTypeId(UUID.randomUUID())
                .withMaterials(asList(
                        Material.material()
                                .withId(UUID.randomUUID())
                                .withUserGroups(asList("Court Clerks"))
                                .build()
                ))
                .withSeqNum(10)
                .withContainsFinancialMeans(false)
                .build();
    }

    private CourtDocument courtDocumentNowTypeWithRbacDetails(final UUID id) {
        final List<String> userGroups = singletonList("group1");
        final DocumentTypeRBAC documentTypeRBAC = new DocumentTypeRBAC(userGroups, userGroups, now(), now().plusMonths(5), userGroups, userGroups);
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentCategory(documentCategoryNowTemplate())
                .withDocumentTypeId(randomUUID())
                .withDocumentTypeRBAC(documentTypeRBAC)
                .withMaterials(singletonList(Material.material().withId(id).withUserGroups(userGroups).build()))
                .build();
    }

    private CourtDocument courtDocumentNowType(final UUID id) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentCategory(documentCategoryNowTemplate())
                .withDocumentTypeId(UUID.randomUUID())
                .build();
    }

    private CourtDocument courtDocumentApplicationType(final UUID id, Integer seqNum, DocumentTypeRBAC documentTypeRBAC) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentCategory(documentCategoryApplicationTemplate())
                .withDocumentTypeId(UUID.randomUUID())
                .withMaterials(asList(
                        Material.material()
                                .withId(UUID.randomUUID())
                                .withUserGroups(asList("Court Clerks"))
                                .build()
                ))
                .withSeqNum(seqNum)
                .withDocumentTypeRBAC(documentTypeRBAC)
                .build();
    }

    private NowDocument nowDocumentTemplate() {
        return NowDocument.nowDocument()
                .withOrderHearingId(UUID.randomUUID())
                .withDefendantId(UUID.randomUUID())
                .withProsecutionCases(Arrays.asList(UUID.randomUUID()))
                .build();
    }

    private ApplicationDocument applicationDocumentTemplate() {
        return ApplicationDocument.applicationDocument()
                .withApplicationId(UUID.randomUUID())
                .withProsecutionCaseId(UUID.randomUUID())
                .build();
    }

    private DocumentCategory documentCategoryNowTemplate() {
        return DocumentCategory.documentCategory()
                .withNowDocument(nowDocumentTemplate())
                .build();
    }

    private DocumentCategory documentCategoryApplicationTemplate() {
        return DocumentCategory.documentCategory()
                .withApplicationDocument(applicationDocumentTemplate())
                .build();
    }

    private CourtDocumentEntity courtDocumentEntity(final UUID id, CourtDocument courtDocument) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(id);
        courtDocumentEntity.setIsRemoved(false);
        courtDocumentEntity.setPayload(objectToString(courtDocument));
        courtDocumentEntity.setSeqNum(courtDocument.getSeqNum());
        return courtDocumentEntity;
    }

    private CourtDocumentEntity courtDocumentEntityWithRbac(final UUID id, CourtDocument courtDocument) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(id);
        courtDocumentEntity.setIsRemoved(false);
        courtDocumentEntity.setPayload(objectToString(courtDocument));
        courtDocumentEntity.setSeqNum(courtDocument.getSeqNum());
        CourtDocumentTypeRBAC courtDocumentTypeRBAC = new CourtDocumentTypeRBAC();
        courtDocumentTypeRBAC.setReadUserGroups(courtDocument.getDocumentTypeRBAC().getReadUserGroups());
        courtDocumentTypeRBAC.setDownloadUserGroups(courtDocument.getDocumentTypeRBAC().getDownloadUserGroups());
        courtDocumentTypeRBAC.setCreateUserGroups(courtDocument.getDocumentTypeRBAC().getUploadUserGroups());
        courtDocumentEntity.setCourtDocumentTypeRBAC(courtDocumentTypeRBAC);
        courtDocumentEntity.setDocumentCategory("");
        return courtDocumentEntity;
    }

    private CourtDocumentEntity removedCourtDocumentEntity(final UUID id, CourtDocument courtDocument) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(id);
        courtDocumentEntity.setIsRemoved(true);
        courtDocumentEntity.setPayload(objectToString(courtDocument));
        courtDocumentEntity.setSeqNum(courtDocument.getSeqNum());
        return courtDocumentEntity;
    }

    private NotificationStatusEntity notificationStatusEntity(final UUID prosecutionCaseId) {
        final NotificationStatusEntity entity = new NotificationStatusEntity();
        entity.setId(randomUUID());
        entity.setNotificationId(randomUUID());
        entity.setCaseId(prosecutionCaseId);
        entity.setMaterialId(UUID.randomUUID());
        entity.setNotificationStatus(NotificationStatus.NOTIFICATION_REQUEST);
        entity.setNotificationType(NotificationType.PRINT);
        entity.setStatusCode(1);
        entity.setErrorMessage("Test Error Message");
        entity.setUpdated(ZonedDateTime.now());
        return entity;
    }

    private ProsecutionCaseEntity createProsecutionCaseEntity(final ProsecutionCase prosecutionCase,
                                                              final UUID caseId) {

        JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);
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
                .withId(randomUUID())
                .withPersonDefendant(createPersonDefendant())
                .withOffences(createOffences())
                .build();
        defendants.add(defendant);
        return defendants;
    }

    private PersonDefendant createPersonDefendant() {
        return PersonDefendant.personDefendant()
                .withPersonDetails(Person.person()
                        .withFirstName("John")
                        .withMiddleName("Martin")
                        .withLastName("Williams")
                        .withDateOfBirth(LocalDate.of(2017, 2, 1))
                        .build())
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
                .withStartDate(LocalDate.of(2018, 1, 1))
                .withEndDate(LocalDate.of(2018, 1, 5))
                .withCount(5)
                .withConvictionDate(LocalDate.of(2018, 2, 2))
                .build();
        return Arrays.asList(offence);
    }


    @Test
    public void shouldFindDocumentById() throws Exception {
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.ID_PARAMETER, courtDocumentId.toString()).build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENT_SEARCH_NAME).build(),
                jsonObject);
        final CourtDocument courtDocument = courtDocument(courtDocumentId);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);

        when(courtDocumentRepository.findBy(courtDocumentId)).thenReturn(courtDocumentEntity);

        final JsonEnvelope jsonEnvelopeOut = target.getCourtDocument(jsonEnvelopeIn);
        final CourtDocument courtDocumentOut = jsonObjectToObjectConverter
                .convert(jsonEnvelopeOut.payloadAsJsonObject()
                        .getJsonObject(CourtDocumentQuery.COURT_DOCUMENT_RESULT_FIELD), CourtDocument.class);
        assertThat(courtDocumentOut.getCourtDocumentId(), is(courtDocument.getCourtDocumentId()));
        assertThat(courtDocumentOut.getDocumentTypeId(), is(courtDocument.getDocumentTypeId()));
        assertThat(courtDocumentOut.getContainsFinancialMeans(), is(courtDocument.getContainsFinancialMeans()));
    }

    @Test
    public void shouldNotFindDocumentWhenIsRemoveIsTrue() throws Exception {
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.ID_PARAMETER, courtDocumentId.toString()).build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENT_SEARCH_NAME).build(),
                jsonObject);
        final CourtDocument remCourtDocument = removedCourtDocument(courtDocumentId);

        final CourtDocumentEntity courtDocumentEntity = removedCourtDocumentEntity(courtDocumentId, remCourtDocument);

        when(courtDocumentRepository.findBy(courtDocumentId)).thenReturn(courtDocumentEntity);

        final JsonEnvelope jsonEnvelopeOut = target.getCourtDocument(jsonEnvelopeIn);

        assertThat(jsonEnvelopeOut.payload(), is(JsonValue.NULL));

    }

    @Test
    public void shouldFindDocumentsByCaseIdPermitted() {
        shouldFindDocuments(true, true, singletonList(randomUUID()), null, null, "Court Clerks");
    }

    @Test
    public void shouldFindDocumentsByCaseIdsAndApplicationIdPermitted() {
        shouldFindDocuments(true, true, asList(UUID.randomUUID(), UUID.randomUUID()), null, asList(UUID.randomUUID(), UUID.randomUUID()), "Court Clerks");
    }


    @Test
    public void shouldFindDocumentsByDefendantIdPermitted() {
        shouldFindDocuments(true, true, null, UUID.randomUUID(), null, "Court Clerks");
    }

    @Test
    public void shouldNotFindDocumentsByDefendantIdIfDocumentBelongsToOtherDefendant() {

        final UUID defendantId = randomUUID();

        final JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        final Map<UUID, CourtDocumentIndex.Builder> id2ExpectedCourtDocumentIndex = new HashMap<>();
        final Map<UUID, UUID> courtDocumentId2Id = new HashMap<>();

        addId(null, randomUUID(), null, id2ExpectedCourtDocumentIndex, courtDocumentId2Id);
        jsonBuilder.add(CourtDocumentQuery.DEFENDANT_ID_SEARCH_PARAM, defendantId.toString());

        JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENTS_SEARCH_NAME).build(),
                jsonBuilder.build());

        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups((Action) any(), (List<String>) any())).thenReturn(true);
        when(rbacProvider.isLoggedInUserAllowedToReadDocument((Action) any())).thenReturn(true);

        Answer<?> transformResult = new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                CourtDocument courtDocument = (CourtDocument) invocationOnMock.getArguments()[0];
                UUID id = courtDocumentId2Id.get(courtDocument.getCourtDocumentId());
                return id2ExpectedCourtDocumentIndex.get(id);
            }
        };

        when(courtDocumentTransform.transform(any())).thenAnswer(transformResult);

        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocuments(jsonEnvelopeIn);
        CourtDocumentsSearchResult result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtDocumentsSearchResult.class);

        assertThat(result.getDocumentIndices().size(), is(0));


    }

    @Test
    public void shouldFindDocumentsByCaseIdNotPermitted() {
        shouldFindDocuments(true, false, asList(UUID.randomUUID()), null, null, "group1");
    }

    @Test
    public void shouldNotFindDocumentsByDefendantIdPermitted() {
        shouldFindDocuments(false, true, null, UUID.randomUUID(), null, "group1");
    }

    private void addId(List<UUID> caseId, UUID defendantId, List<UUID> applicationId, Map<UUID, CourtDocumentIndex.Builder> id2ExpectedCourtDocumentIndex,
                       Map<UUID, UUID> courtDocumentId2Id) {
        final UUID courtDocumentId = UUID.randomUUID();
        final CourtDocument courtDocument = courtDocument(courtDocumentId);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);

        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withDocument(courtDocument)
                .withHearingIds(asList(UUID.randomUUID()))
                .withType("Defendant profile notes");

        UUID id = null;
        if (isNotEmpty(caseId)) {
            id = caseId.get(0);
            courtDocumentIndexBuilder.withCaseIds(caseId)
                    .withCategory("Defendant level");
            when(courtDocumentRepository.findByProsecutionCaseIds(caseId)).thenReturn(asList(courtDocumentEntity));
        } else if (isNotEmpty(applicationId)) {
            id = applicationId.get(0);
            courtDocumentIndexBuilder.withDocument(CourtDocument.courtDocument()
                    .withMaterials(asList(Material.material().build()))
                    .withDocumentCategory(DocumentCategory.documentCategory().withApplicationDocument(ApplicationDocument.
                            applicationDocument().withApplicationId(applicationId.get(0)).build()).build())
                    .build())
                    .withCategory("Applicatiom level");
            ;
            when(courtDocumentRepository.findByApplicationIds(applicationId)).thenReturn(asList(courtDocumentEntity));
        } else if (defendantId != null) {
            id = defendantId;
            courtDocumentIndexBuilder.withDefendantIds(asList(defendantId))
                    .withCategory("Defendant level");
            ;
            when(courtDocumentRepository.findByDefendantId(defendantId)).thenReturn(asList(courtDocumentEntity));
        }

        id2ExpectedCourtDocumentIndex.put(id, courtDocumentIndexBuilder);
        courtDocumentId2Id.put(courtDocument.getCourtDocumentId(), id);

    }

    private void shouldFindDocuments(final boolean rbackReadPermitted, final boolean permitted, final List<UUID> caseIds, final UUID defendantId, final List<UUID> applicationIds, final String userGroup) {
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", userGroup).build())
                .build();
        final JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        final Map<UUID, CourtDocumentIndex.Builder> id2ExpectedCourtDocumentIndex = new HashMap<>();
        final Map<UUID, UUID> courtDocumentId2Id = new HashMap<>();
        if (caseIds != null) {
            addId(caseIds, null, null, id2ExpectedCourtDocumentIndex, courtDocumentId2Id);
            jsonBuilder.add(CourtDocumentQuery.CASE_ID_SEARCH_PARAM, caseIds.stream().map(UUID::toString).collect(Collectors.joining(",")));
        }
        if (applicationIds != null) {
            addId(null, null, applicationIds, id2ExpectedCourtDocumentIndex, courtDocumentId2Id);
            jsonBuilder.add(CourtDocumentQuery.APPLICATION_ID_SEARCH_PARAM, applicationIds.stream().map(UUID::toString).collect(Collectors.joining(",")));

        }
        if (defendantId != null) {
            addId(null, defendantId, null, id2ExpectedCourtDocumentIndex, courtDocumentId2Id);
            jsonBuilder.add(CourtDocumentQuery.DEFENDANT_ID_SEARCH_PARAM, defendantId.toString());
        }

        final JsonObject jsonObject = jsonBuilder.build();

        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENTS_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonObject);

        when(rbacProvider.isLoggedInUserAllowedToReadDocument((Action) any())).thenReturn(rbackReadPermitted);
        mockUserGroups(userGroupArray);

        Answer<?> transformResult = new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                CourtDocument courtDocument = (CourtDocument) invocationOnMock.getArguments()[0];
                UUID id = courtDocumentId2Id.get(courtDocument.getCourtDocumentId());
                return id2ExpectedCourtDocumentIndex.get(id);
            }
        };

        when(courtDocumentTransform.transform(any())).thenAnswer(transformResult);

        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocuments(jsonEnvelopeIn);
        final CourtDocumentsSearchResult result = jsonObjectToObjectConverter
                .convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtDocumentsSearchResult.class);

        if (!permitted || !rbackReadPermitted) {
            assertThat(result.getDocumentIndices().size(), is(0));
        } else {
            assertThat(result.getDocumentIndices().size(), is(id2ExpectedCourtDocumentIndex.size()));

            id2ExpectedCourtDocumentIndex.forEach(
                    (id, expectedIndexBuilder) -> {
                        CourtDocumentIndex actualIndex = result.getDocumentIndices().stream().filter(
                                cdi -> cdi.getCaseIds() != null && cdi.getCaseIds().stream().anyMatch(idin -> idin.equals(id))
                                        || cdi.getDefendantIds() != null && cdi.getDefendantIds().stream().anyMatch(idIn -> idIn.equals(id))
                                        //this mess in necessary because output document index does not have applicatiinId field
                                        || (cdi.getDocument() != null && cdi.getDocument().getDocumentCategory() != null
                                        && cdi.getDocument().getDocumentCategory().getApplicationDocument() != null
                                        && id.equals(cdi.getDocument().getDocumentCategory().getApplicationDocument().getApplicationId()))

                        ).findAny().orElse(null);
                        assertThat(actualIndex, org.hamcrest.Matchers.notNullValue());
                        CourtDocumentIndex expectedIndex = expectedIndexBuilder.build();
                        assertThat(actualIndex.getType(), is(expectedIndex.getType()));
                        assertThat(actualIndex.getCategory(), is(expectedIndex.getCategory()));
                        assertThat(actualIndex.getHearingIds().get(0), is(expectedIndex.getHearingIds().get(0)));
                        final int materialCount = actualIndex.getDocument().getMaterials().size();
                        assertThat(materialCount, is(1));
                    }
            );

        }
    }

    @Test
    public void shouldFindDocumentsByApplicationId() {
        boolean permitted = true;
        final UUID applicationId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.APPLICATION_ID, applicationId.toString())
                .add(CourtDocumentQuery.APPLICATION_ID, applicationId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENTS_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonObject);
        final List<String> userGroups = singletonList("group1");
        final DocumentTypeRBAC documentTypeRBAC = new DocumentTypeRBAC(userGroups, userGroups, now(), now().plusMonths(5), null, null);
        final CourtDocument courtDocument = courtDocumentApplicationType(courtDocumentId, 10, documentTypeRBAC);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);
        when(courtDocumentRepository.findByApplicationIds(Collections.singletonList(applicationId))).thenReturn(asList(courtDocumentEntity));
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();
        mockUserGroups(userGroupArray);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(applicationId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(UUID.randomUUID()))
                .withHearingIds(asList(UUID.randomUUID()))
                .withType("Defendant profile notes");

        when(courtDocumentTransform.transform(any())).thenReturn(courtDocumentIndexBuilder);

        when(rbacProvider.isLoggedInUserAllowedToReadDocument((Action) any())).thenReturn(true);

        final CourtDocumentIndex expectedIndex = courtDocumentIndexBuilder.build();
        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocuments(jsonEnvelopeIn);
        final CourtDocumentsSearchResult result = jsonObjectToObjectConverter
                .convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtDocumentsSearchResult.class);
        if (permitted) {
            assertThat(result.getDocumentIndices().size(), is(1));
            final CourtDocumentIndex courtDocumentIndexOut = result.getDocumentIndices().get(0);
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


    @Test
    public void shouldNotFindDocumentsWhenIsRemovedisTrue() {
        boolean permitted = true;
        final UUID applicationId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.APPLICATION_ID, applicationId.toString())
                .add(CourtDocumentQuery.APPLICATION_ID, applicationId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENTS_SEARCH_NAME).build(),
                jsonObject);
        final CourtDocument courtDocument = removedCourtDocument(courtDocumentId);
        final CourtDocumentEntity courtDocumentEntity = removedCourtDocumentEntity(courtDocumentId, courtDocument);
        when(courtDocumentRepository.findByApplicationIds(Collections.singletonList(applicationId))).thenReturn(asList(courtDocumentEntity));
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups((Action) any(), (List<String>) any())).thenReturn(permitted);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(applicationId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(UUID.randomUUID()))
                .withHearingIds(asList(UUID.randomUUID()))
                .withType("Defendant profile notes");

        when(courtDocumentTransform.transform(any())).thenReturn(courtDocumentIndexBuilder);

        when(rbacProvider.isLoggedInUserAllowedToReadDocument((Action) any())).thenReturn(true);

        final CourtDocumentIndex expectedIndex = courtDocumentIndexBuilder.build();
        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocuments(jsonEnvelopeIn);
        assertTrue(jsonEnvelopeOut.payloadAsJsonObject().getJsonArray("documentIndices").isEmpty());
    }

    @Test
    public void shouldNotListDocumentsWithNoReadOnlyAccessAndListAllOther() {
        boolean permitted = true;
        final UUID applicationId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.APPLICATION_ID, applicationId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENTS_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonObject);
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final UUID nowCourtDocumentId = randomUUID();
        final List userGroups = singletonList("Court Clerks");
        final DocumentTypeRBAC documentTypeRBAC1 = new DocumentTypeRBAC(userGroups, userGroups, now(), now().plusMonths(5), userGroups, userGroups);
        final CourtDocument courtDocument1 = courtDocumentApplicationType(nowCourtDocumentId, 20, documentTypeRBAC1);
        final CourtDocumentEntity courtDocumentEntity1 = courtDocumentEntityWithRbac(nowCourtDocumentId, courtDocument1);

        final UUID secondNowCourtDocumentId = randomUUID();
        final CourtDocument courtDocument2 = courtDocumentApplicationType(secondNowCourtDocumentId, 10, documentTypeRBAC1);
        final CourtDocumentEntity courtDocumentEntity2 = courtDocumentEntityWithRbac(secondNowCourtDocumentId, courtDocument2);


        final UUID courtDocumentId = UUID.randomUUID();
        final CourtDocument courtDocument = courtDocumentApplicationType(courtDocumentId, 30, documentTypeRBAC1);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);

        when(courtDocumentRepository.findByApplicationIds(Collections.singletonList(applicationId))).thenReturn(asList(courtDocumentEntity1, courtDocumentEntity2, courtDocumentEntity));
        mockUserGroups(userGroupArray);

        final CourtDocumentIndex.Builder courtDocumentIndexBuilder1 = createCourtDocumentIndex(courtDocument1, applicationId);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder2 = createCourtDocumentIndex(courtDocument2, applicationId);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = createCourtDocumentIndex(courtDocument1, applicationId);

        when(courtDocumentTransform.transform(any())).thenReturn(courtDocumentIndexBuilder1)
                .thenReturn(courtDocumentIndexBuilder2)
                .thenReturn(courtDocumentIndexBuilder);

        when(rbacProvider.isLoggedInUserAllowedToReadDocument((Action) any()))
                .thenReturn(true)
                .thenReturn(false)
                .thenReturn(true);

        final CourtDocumentIndex expectedIndex1 = courtDocumentIndexBuilder1.build();
        final CourtDocumentIndex expectedIndex2 = courtDocumentIndexBuilder2.build();
        final CourtDocumentIndex expectedIndex = courtDocumentIndexBuilder.build();
        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocuments(jsonEnvelopeIn);
        final CourtDocumentsSearchResult result = jsonObjectToObjectConverter
                .convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtDocumentsSearchResult.class);

        assertThat(result.getDocumentIndices().size(), is(2));

        final CourtDocumentIndex courtDocumentIndexOut = result.getDocumentIndices().get(0);
        assertThat(courtDocumentIndexOut.getType(), is(expectedIndex1.getType()));
        assertThat(courtDocumentIndexOut.getCategory(), is(expectedIndex1.getCategory()));
        assertThat(courtDocumentIndexOut.getDefendantIds().get(0), is(expectedIndex1.getDefendantIds().get(0)));
        assertThat(courtDocumentIndexOut.getCaseIds().get(0), is(expectedIndex1.getCaseIds().get(0)));
        assertThat(courtDocumentIndexOut.getHearingIds().get(0), is(expectedIndex1.getHearingIds().get(0)));

        final CourtDocumentIndex courtDocumentIndexOut1 = result.getDocumentIndices().get(1);
        assertThat(courtDocumentIndexOut1.getType(), is(expectedIndex2.getType()));
        assertThat(courtDocumentIndexOut1.getCategory(), is(expectedIndex2.getCategory()));
        assertThat(courtDocumentIndexOut1.getDefendantIds().get(0), is(expectedIndex2.getDefendantIds().get(0)));
        assertThat(courtDocumentIndexOut1.getCaseIds().get(0), is(expectedIndex2.getCaseIds().get(0)));
        assertThat(courtDocumentIndexOut1.getHearingIds().get(0), is(expectedIndex2.getHearingIds().get(0)));

        verify(courtDocumentTransform, times(2)).transform(courtDocumentArgumentCaptor.capture());

        final CourtDocument courtDocumentTransformed = courtDocumentArgumentCaptor.getValue();
        final int materialCount = courtDocumentTransformed.getMaterials().size();
        assertThat(materialCount, is(1));
    }

    private CourtDocumentIndex.Builder createCourtDocumentIndex(CourtDocument courtDocument, UUID applicationId) {
        return CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(applicationId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(UUID.randomUUID()))
                .withHearingIds(asList(UUID.randomUUID()))
                .withDocument(courtDocument)
                .withType("Defendant profile notes");
    }

    @Test
    public void shouldFindDocumentsByHearingId() {
        boolean permitted = false;
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.DEFENDANT_ID_PARAMETER, defendantId.toString())
                .add(CourtDocumentQuery.HEARING_ID_PARAMETER, hearingId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENTS_NOW_SEARCH_NAME).build(),
                jsonObject);
        final CourtDocument courtDocument = courtDocumentNowType(courtDocumentId);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);
        when(courtDocumentRepository.findCourtDocumentForNow(hearingId, "NOW_DOCUMENT", defendantId)).thenReturn(asList(courtDocumentEntity));
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCategory("NOW_DOCUMENT")
                .withDefendantIds(asList(defendantId))
                .withHearingIds(asList(hearingId))
                .withType("Defendant profile notes");

        when(courtDocumentTransform.transform(any())).thenReturn(courtDocumentIndexBuilder);

        final CourtDocumentIndex expectedIndex = courtDocumentIndexBuilder.build();
        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsByHearingId(jsonEnvelopeIn);
        final CourtDocumentsSearchResult result = jsonObjectToObjectConverter
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

    @Test
    public void shouldFindDocumentsByHearingIdWithRbac() {
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "group1").build())
                .build();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.DEFENDANT_ID_PARAMETER, defendantId.toString())
                .add(CourtDocumentQuery.HEARING_ID_PARAMETER, hearingId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.COURT_DOCUMENTS_NOW_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonObject);
        final CourtDocument courtDocument = courtDocumentNowTypeWithRbacDetails(courtDocumentId);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntityWithRbac(courtDocumentId, courtDocument);
        when(courtDocumentRepository.findCourtDocumentForNow(hearingId, "", defendantId)).thenReturn(singletonList(courtDocumentEntity));
        mockUserGroups(userGroupArray);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCategory("NOW_DOCUMENT")
                .withDefendantIds(singletonList(defendantId))
                .withHearingIds(singletonList(hearingId))
                .withType("Defendant profile notes")
                .withCaseIds(singletonList(hearingId));

        when(courtDocumentTransform.transform(any())).thenReturn(courtDocumentIndexBuilder);

        final CourtDocumentIndex expectedIndex = courtDocumentIndexBuilder.build();
        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsByHearingId(jsonEnvelopeIn);
        final CourtDocumentsSearchResult result = jsonObjectToObjectConverter
                .convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtDocumentsSearchResult.class);

        assertThat(result.getDocumentIndices().size(), is(1));
        final CourtDocumentIndex courtDocumentIndexOut = result.getDocumentIndices().get(0);
        assertThat(courtDocumentIndexOut.getType(), is(expectedIndex.getType()));
        assertThat(courtDocumentIndexOut.getCategory(), is(expectedIndex.getCategory()));
        assertThat(courtDocumentIndexOut.getDefendantIds().get(0), is(expectedIndex.getDefendantIds().get(0)));
        assertThat(courtDocumentIndexOut.getCaseIds().get(0), is(expectedIndex.getCaseIds().get(0)));
        assertThat(courtDocumentIndexOut.getHearingIds().get(0), is(expectedIndex.getHearingIds().get(0)));
        verify(courtDocumentTransform, times(1)).transform(courtDocumentArgumentCaptor.capture());

        final CourtDocument courtDocumentTransformed = courtDocumentArgumentCaptor.getValue();
        final int materialCount = courtDocumentTransformed.getMaterials().size();
        assertThat(materialCount, is(1));
    }

    private void mockUserGroups(final JsonArray userGroupArray) {
        final JsonObject groups = Json.createObjectBuilder().add("groups", userGroupArray).build();
        when(response.payload()).thenReturn(groups);
        when(requester.requestAsAdmin(any(JsonEnvelope.class), any(Class.class))).thenReturn(response);
    }


    @Test
    public void shouldGetCourtDocumentNotificationStatus() {
        final UUID caseId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CourtDocumentQuery.CASE_ID_SEARCH_PARAM, caseId.toString())
                .build();

        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(CourtDocumentQuery.PROSECUTION_NOTIFICATION_STATUS).build(),
                jsonObject);

        final ProsecutionCase prosecutionCase = createProsecutionCase(randomUUID());
        final ProsecutionCaseEntity prosecutionCaseEntity = createProsecutionCaseEntity(prosecutionCase, caseId);

        final CourtDocument courtDocument = courtDocument(courtDocumentId);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);
        final NotificationStatusEntity notificationStatusEntity = notificationStatusEntity(caseId);

        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        when(courtDocumentRepository.findByProsecutionCaseId(caseId)).thenReturn(asList(courtDocumentEntity));
        when(notificationStatusRepository.save(notificationStatusEntity)).thenReturn(notificationStatusEntity);
        when(notificationStatusRepository.findByCaseId(caseId)).thenReturn(asList(notificationStatusEntity));

        final JsonEnvelope jsonEnvelopeOut = target.getCaseNotifications(jsonEnvelopeIn);
        assertThat(jsonEnvelopeOut.payloadAsJsonObject().getJsonArray("notificationStatus").size(), is(1));
    }
}


