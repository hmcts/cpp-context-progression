package uk.gov.moj.cpp.progression.query;

import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.APPLICATION_ID;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.APPLICATION_ID_SEARCH_PARAM;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.CASE_ID_SEARCH_PARAM;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.COURT_DOCUMENTS_NOW_SEARCH_NAME;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.COURT_DOCUMENTS_SEARCH_NAME;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.COURT_DOCUMENTS_SEARCH_WITH_PAGINATION_NAME;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.COURT_DOCUMENT_SEARCH_NAME;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.DEFENDANT_ID_PARAMETER;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.DEFENDANT_ID_SEARCH_PARAM;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.HEARING_ID_PARAMETER;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.ID_PARAMETER;
import static uk.gov.moj.cpp.progression.query.CourtDocumentQueryView.PROSECUTION_NOTIFICATION_STATUS;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.CourtdocumentsWithPagination;
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
import uk.gov.moj.cpp.accesscontrol.refdata.providers.RbacProvider;
import uk.gov.moj.cpp.progression.domain.constant.NotificationStatus;
import uk.gov.moj.cpp.progression.domain.constant.NotificationType;
import uk.gov.moj.cpp.progression.domain.pojo.SearchCriteria;
import uk.gov.moj.cpp.progression.query.view.service.CourtDocumentIndexService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentTypeRBAC;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NotificationStatusEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CpsSendNotificationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NotificationStatusRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentQueryViewTest {

    private static final String SECTION = "section";
    private static final String SORT_FIELD = "sortField";
    private static final String SORT_ORDER = "sortOrder";
    private static final String ASC = "asc";
    private static final String PAGE = "page";
    private static final String PAGE_SIZE = "pageSize";
    private static final String CASE_ID = "caseId";
    public static final String DESC = "desc";
    public static final String DATE = "date";
    public static final String DOCUMENT_NAME = "documentName";

    @InjectMocks
    private CourtDocumentQueryView target;

    @Mock
    private CourtDocumentIndexService courtDocumentIndexService;

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

    @Mock
    uk.gov.justice.services.messaging.Envelope<JsonObject> responseFromRefData;

    @Mock
    private CpsSendNotificationRepository cpsSendNotificationRepository;

    private static final UUID DOCUMENT_TYPE_ID_1 = UUID.fromString("460f7ec0-c002-11e8-a355-529269fb1459");
    private static final UUID DOCUMENT_TYPE_ID_2 = UUID.fromString("460f8154-c002-11e8-a355-529269fb1459");
    private static final UUID DOCUMENT_TYPE_ID_3 = UUID.fromString("460f851e-c002-11e8-a355-529269fb1459");
    private static final UUID DOCUMENT_TYPE_ID_4 = UUID.fromString("460f8974-c002-11e8-a355-529269fb1459");

    @BeforeEach
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

    private CourtDocument courtDocument(final UUID id, final UUID documentTypeId, String userGroup) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentTypeId(documentTypeId)
                .withMaterials(asList(
                        Material.material()
                                .withId(UUID.randomUUID())
                                .withUserGroups(asList(userGroup == null ? "Court Clerks" : "Legal Advisers"))
                                .build()
                ))
                .withSeqNum(10)
                .withContainsFinancialMeans(false)
                .build();
    }

    private CourtDocument removedCourtDocument(final UUID id, final UUID documentTypeId) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentTypeId(documentTypeId)
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
                .withDocumentTypeId(DOCUMENT_TYPE_ID_1)
                .withDocumentTypeRBAC(documentTypeRBAC)
                .withMaterials(singletonList(Material.material().withId(id).withUserGroups(userGroups).build()))
                .build();
    }

    private CourtDocument courtDocumentNowType(final UUID id) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentCategory(documentCategoryNowTemplate())
                .withDocumentTypeId(DOCUMENT_TYPE_ID_1)
                .build();
    }

    private CourtDocument courtDocumentApplicationType(final UUID id, Integer seqNum, DocumentTypeRBAC documentTypeRBAC, final UUID documentTypeId) {
        return CourtDocument.courtDocument()
                .withCourtDocumentId(id)
                .withDocumentCategory(documentCategoryApplicationTemplate())
                .withDocumentTypeId(documentTypeId)
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
                .add(ID_PARAMETER, courtDocumentId.toString()).build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENT_SEARCH_NAME).build(),
                jsonObject);
        final CourtDocument courtDocument = courtDocument(courtDocumentId, DOCUMENT_TYPE_ID_1, "Court Clerks");
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);

        when(courtDocumentRepository.findBy(courtDocumentId)).thenReturn(courtDocumentEntity);

        final JsonEnvelope jsonEnvelopeOut = target.getCourtDocument(jsonEnvelopeIn);
        final CourtDocument courtDocumentOut = jsonObjectToObjectConverter
                .convert(jsonEnvelopeOut.payloadAsJsonObject()
                        .getJsonObject(CourtDocumentQueryView.COURT_DOCUMENT_RESULT_FIELD), CourtDocument.class);
        assertThat(courtDocumentOut.getCourtDocumentId(), is(courtDocument.getCourtDocumentId()));
        assertThat(courtDocumentOut.getDocumentTypeId(), is(courtDocument.getDocumentTypeId()));
        assertThat(courtDocumentOut.getContainsFinancialMeans(), is(courtDocument.getContainsFinancialMeans()));
    }

    @Test
    public void shouldNotFindDocumentWhenIsRemoveIsTrue() throws Exception {
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(ID_PARAMETER, courtDocumentId.toString()).build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENT_SEARCH_NAME).build(),
                jsonObject);
        final CourtDocument remCourtDocument = removedCourtDocument(courtDocumentId, DOCUMENT_TYPE_ID_1);

        final CourtDocumentEntity courtDocumentEntity = removedCourtDocumentEntity(courtDocumentId, remCourtDocument);

        when(courtDocumentRepository.findBy(courtDocumentId)).thenReturn(courtDocumentEntity);

        final JsonEnvelope jsonEnvelopeOut = target.getCourtDocument(jsonEnvelopeIn);

        assertThat(jsonEnvelopeOut.payload(), is(JsonValue.NULL));

    }

    @Test
    public void shouldFindDocumentsByCaseIdPermitted() throws IOException {
        shouldFindDocuments(true, true, singletonList(randomUUID()), null, null, "Court Clerks", false, true);
    }

    @Test
    public void shouldFindDocumentsByCaseIdPermittedWhenProsecutingTrue() throws IOException {
        shouldFindDocuments(true, true, singletonList(randomUUID()), null, null, "Court Clerks", true, true);
    }


    @Test
    public void shouldFindDocumentsByCaseIdsAndApplicationIdPermitted() throws IOException {
        shouldFindDocuments(true, true, asList(UUID.randomUUID(), UUID.randomUUID()), null, asList(UUID.randomUUID(), UUID.randomUUID()), "Court Clerks", false, true);
    }


    @Test
    public void shouldFindDocumentsByDefendantIdPermitted() throws IOException {
        shouldFindDocuments(true, true, null, UUID.randomUUID(), null, "Court Clerks", false, true);
    }

    @Test
    public void shouldNotFindDocumentsByDefendantIdIfDocumentBelongsToOtherDefendant() throws IOException {

        final UUID defendantId = randomUUID();
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();
        final JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        final Map<UUID, CourtDocumentIndex.Builder> id2ExpectedCourtDocumentIndex = new HashMap<>();
        final Map<UUID, UUID> courtDocumentId2Id = new HashMap<>();
        final UUID documentTypeId = DOCUMENT_TYPE_ID_1;
        addId(null, randomUUID(), null, id2ExpectedCourtDocumentIndex, courtDocumentId2Id, documentTypeId, null);
        jsonBuilder.add(DEFENDANT_ID_SEARCH_PARAM, defendantId.toString());

        JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENTS_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonBuilder.build());
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        Answer<?> transformResult = new Answer<>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                CourtDocument courtDocument = (CourtDocument) invocationOnMock.getArguments()[0];
                UUID id = courtDocumentId2Id.get(courtDocument.getCourtDocumentId());
                return id2ExpectedCourtDocumentIndex.get(id);
            }
        };
        when(courtDocumentTransform.transform(any(), any())).thenAnswer(transformResult);

        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocuments(jsonEnvelopeIn);
        CourtDocumentsSearchResult result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtDocumentsSearchResult.class);

        assertThat(result.getDocumentIndices().size(), is(0));
    }

    @Test
    public void shouldFindDocumentsByPaginationOrderBySectionAsc() throws IOException {

        final UUID caseId = randomUUID();
        final int pageSize = 2;

        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .build();

        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final JsonEnvelope jsonEnvelopeIn = getJsonEnvelopeForQueryRequest(caseId, pageSize, 1, SECTION, ASC, null);

        when(courtDocumentIndexService.getCourtDocumentIndexByCriteria(any())).thenReturn(asList(
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders0", "2020-09-18T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders3", "2020-09-20T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders1", "2020-09-17T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders2", "2020-09-21T08:56:14.195Z"))
        ));
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        //query first page
        JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(jsonEnvelopeIn);
        CourtdocumentsWithPagination result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(4));
        assertThat(result.getCourtDocuments().size(), is(pageSize));
        assertThat(result.getPaginationData().getPage(), is(1));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(ASC));
        assertThat(result.getPaginationData().getSortField().toString(), is(SECTION));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders0"));
        assertThat(result.getCourtDocuments().get(1).getDocumentTypeDescription(), is("Court Final orders1"));

        //query second page
        jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(getJsonEnvelopeForQueryRequest(caseId, pageSize, 2, SECTION, ASC, null));
        result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(4));
        assertThat(result.getCourtDocuments().size(), is(pageSize));
        assertThat(result.getPaginationData().getPage(), is(2));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(ASC));
        assertThat(result.getPaginationData().getSortField().toString(), is(SECTION));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders2"));
        assertThat(result.getCourtDocuments().get(1).getDocumentTypeDescription(), is("Court Final orders3"));

    }

    @Test
    public void shouldFindDocumentsByPaginationOrderBySectionDesc() throws IOException {

        final UUID caseId = randomUUID();
        final int pageSize = 2;

        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .build();

        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final JsonEnvelope jsonEnvelopeIn = getJsonEnvelopeForQueryRequest(caseId, pageSize, 1, SECTION, DESC, null);

        when(courtDocumentIndexService.getCourtDocumentIndexByCriteria(any())).thenReturn(asList(
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders0", "2020-09-18T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders3", "2020-09-20T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders1", "2020-09-17T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders2", "2020-09-21T08:56:14.195Z"))
        ));
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        //query first page
        JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(jsonEnvelopeIn);
        CourtdocumentsWithPagination result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(4));
        assertThat(result.getCourtDocuments().size(), is(pageSize));
        assertThat(result.getPaginationData().getPage(), is(1));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(DESC));
        assertThat(result.getPaginationData().getSortField().toString(), is(SECTION));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders3"));
        assertThat(result.getCourtDocuments().get(1).getDocumentTypeDescription(), is("Court Final orders2"));

        //query second page
        jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(getJsonEnvelopeForQueryRequest(caseId, pageSize, 2, SECTION, DESC, null));
        result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(4));
        assertThat(result.getCourtDocuments().size(), is(pageSize));
        assertThat(result.getPaginationData().getPage(), is(2));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(DESC));
        assertThat(result.getPaginationData().getSortField().toString(), is(SECTION));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders1"));
        assertThat(result.getCourtDocuments().get(1).getDocumentTypeDescription(), is("Court Final orders0"));

    }

    @Test
    public void shouldFindDocumentsByPaginationOrderByDateAsc() throws IOException {

        final UUID caseId = randomUUID();
        final int pageSize = 2;

        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .build();

        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final JsonEnvelope jsonEnvelopeIn = getJsonEnvelopeForQueryRequest(caseId, pageSize, 1, DATE, ASC, null);

        when(courtDocumentIndexService.getCourtDocumentIndexByCriteria(any())).thenReturn(asList(
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders0", "2020-09-18T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders3", "2020-09-20T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders1", "2020-09-17T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders2", "2020-09-21T08:56:14.195Z"))
        ));
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        //query first page
        JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(jsonEnvelopeIn);
        CourtdocumentsWithPagination result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(4));
        assertThat(result.getCourtDocuments().size(), is(pageSize));
        assertThat(result.getPaginationData().getPage(), is(1));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(ASC));
        assertThat(result.getPaginationData().getSortField().toString(), is(DATE));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders1"));
        assertThat(result.getCourtDocuments().get(0).getMaterial().getUploadDateTime().toString(), is("2020-09-17T08:56:14.195Z[UTC]"));
        assertThat(result.getCourtDocuments().get(1).getDocumentTypeDescription(), is("Court Final orders0"));
        assertThat(result.getCourtDocuments().get(1).getMaterial().getUploadDateTime().toString(), is("2020-09-18T08:56:14.195Z[UTC]"));

        //query second page
        jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(getJsonEnvelopeForQueryRequest(caseId, pageSize, 2, DATE, ASC, null));
        result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(4));
        assertThat(result.getCourtDocuments().size(), is(pageSize));
        assertThat(result.getPaginationData().getPage(), is(2));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(ASC));
        assertThat(result.getPaginationData().getSortField().toString(), is(DATE));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders3"));
        assertThat(result.getCourtDocuments().get(0).getMaterial().getUploadDateTime().toString(), is("2020-09-20T08:56:14.195Z[UTC]"));
        assertThat(result.getCourtDocuments().get(1).getDocumentTypeDescription(), is("Court Final orders2"));
        assertThat(result.getCourtDocuments().get(1).getMaterial().getUploadDateTime().toString(), is("2020-09-21T08:56:14.195Z[UTC]"));

    }


    @Test
    public void shouldFindDocumentsByPaginationOrderByDateDesc() throws IOException {

        final UUID caseId = randomUUID();
        final int pageSize = 2;

        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .build();

        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final JsonEnvelope jsonEnvelopeIn = getJsonEnvelopeForQueryRequest(caseId, pageSize, 1, DATE, DESC, null);

        when(courtDocumentIndexService.getCourtDocumentIndexByCriteria(any())).thenReturn(asList(
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders0", "2020-09-18T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders3", "2020-09-20T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders1", "2020-09-17T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders2", "2020-09-21T08:56:14.195Z"))
        ));
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        //query first page
        JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(jsonEnvelopeIn);
        CourtdocumentsWithPagination result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(4));
        assertThat(result.getCourtDocuments().size(), is(pageSize));
        assertThat(result.getPaginationData().getPage(), is(1));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(DESC));
        assertThat(result.getPaginationData().getSortField().toString(), is(DATE));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders2"));
        assertThat(result.getCourtDocuments().get(0).getMaterial().getUploadDateTime().toString(), is("2020-09-21T08:56:14.195Z[UTC]"));
        assertThat(result.getCourtDocuments().get(1).getDocumentTypeDescription(), is("Court Final orders3"));
        assertThat(result.getCourtDocuments().get(1).getMaterial().getUploadDateTime().toString(), is("2020-09-20T08:56:14.195Z[UTC]"));

        //query second page
        jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(getJsonEnvelopeForQueryRequest(caseId, pageSize, 2, DATE, DESC, null));
        result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(4));
        assertThat(result.getCourtDocuments().size(), is(pageSize));
        assertThat(result.getPaginationData().getPage(), is(2));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(DESC));
        assertThat(result.getPaginationData().getSortField().toString(), is(DATE));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders0"));
        assertThat(result.getCourtDocuments().get(0).getMaterial().getUploadDateTime().toString(), is("2020-09-18T08:56:14.195Z[UTC]"));
        assertThat(result.getCourtDocuments().get(1).getDocumentTypeDescription(), is("Court Final orders1"));
        assertThat(result.getCourtDocuments().get(1).getMaterial().getUploadDateTime().toString(), is("2020-09-17T08:56:14.195Z[UTC]"));

    }

    @Test
    public void shouldFindDocumentsByPaginationOrderByDateDescWithDocumentNameFilteringFullMatch() throws IOException {

        final UUID caseId = randomUUID();
        final int pageSize = 2;

        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .build();

        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final JsonEnvelope jsonEnvelopeIn = getJsonEnvelopeForQueryRequest(caseId, pageSize, 1, DATE, DESC, "Court Final orders1");

        when(courtDocumentIndexService.getCourtDocumentIndexByCriteria(any())).thenReturn(asList(
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders0", "2020-09-18T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders3", "2020-09-20T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders1", "2020-09-17T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders2", "2020-09-21T08:56:14.195Z"))
        ));
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        //query first page
        JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(jsonEnvelopeIn);
        CourtdocumentsWithPagination result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(1));
        assertThat(result.getCourtDocuments().size(), is(1));
        assertThat(result.getPaginationData().getPage(), is(1));
        assertThat(result.getPaginationData().getPageSize(), is(pageSize));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(DESC));
        assertThat(result.getPaginationData().getSortField().toString(), is(DATE));
        assertThat(result.getCourtDocuments().get(0).getName(), is("Court Final orders1"));

    }

    @Test
    public void shouldFindDocumentsByPaginationOrderByDateDescWithDocumentNameFilteringPartialMatch() throws IOException {

        final UUID caseId = randomUUID();
        final int pageSize = 2;

        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .build();

        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final JsonEnvelope jsonEnvelopeIn = getJsonEnvelopeForQueryRequest(caseId, pageSize, 1, DATE, DESC, "cd");

        when(courtDocumentIndexService.getCourtDocumentIndexByCriteria(any())).thenReturn(asList(
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "AbcCourt Final orders0", "2020-09-18T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "BcdCourt Final orders3", "2020-09-20T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "CdeCourt Final orders1", "2020-09-17T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "DefCourt Final orders2", "2020-09-21T08:56:14.195Z"))
        ));
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        //query first page
        JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(jsonEnvelopeIn);
        CourtdocumentsWithPagination result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(1));
        assertThat(result.getCourtDocuments().size(), is(1));
        assertThat(result.getPaginationData().getPage(), is(1));
        assertThat(result.getPaginationData().getPageSize(), is(pageSize));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(DESC));
        assertThat(result.getPaginationData().getSortField().toString(), is(DATE));
        assertThat(result.getCourtDocuments().get(0).getName(), is("CdeCourt Final orders1"));

    }

    @Test
    public void shouldFindDocumentsAndFilterRestrictedDocumentTypesByPaginationOrderBySectionAsc() throws IOException {

        final UUID caseId = randomUUID();
        final int pageSize = 2;

        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .build();

        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final JsonEnvelope jsonEnvelopeIn = getJsonEnvelopeForQueryRequest(caseId, pageSize, 1, SECTION, ASC, null);

        when(courtDocumentIndexService.getCourtDocumentIndexByCriteria(any())).thenReturn(asList(
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload-restricted-doc-type.json", "Court Final orders0", "2020-09-18T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders3", "2020-09-20T08:56:14.195Z"))
        ));
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        //query first page
        JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(jsonEnvelopeIn);
        CourtdocumentsWithPagination result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(1));
        assertThat(result.getCourtDocuments().size(), is(1));
        assertThat(result.getPaginationData().getPage(), is(1));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(ASC));
        assertThat(result.getPaginationData().getSortField().toString(), is(SECTION));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders3"));

    }

    @Test
    public void shouldFindNoDocumentWithPaginationDueToNotAuthorisedDocumentType() throws IOException {

        final UUID caseId = randomUUID();
        final int pageSize = 2;

        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .build();

        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final JsonEnvelope jsonEnvelopeIn = getJsonEnvelopeForQueryRequest(caseId, pageSize, 1, SECTION, ASC, null);

        when(courtDocumentIndexService.getCourtDocumentIndexByCriteria(any())).thenReturn(asList(
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload-restricted-doc-type.json", "Court Final orders0", "2020-09-18T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload-restricted-doc-type.json", "Court Final orders3", "2020-09-20T08:56:14.195Z"))
        ));
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        //query first page
        JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(jsonEnvelopeIn);
        CourtdocumentsWithPagination result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(0));
        assertThat(result.getCourtDocuments().size(), is(0));
        assertThat(result.getPaginationData().getPage(), is(1));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(ASC));
        assertThat(result.getPaginationData().getSortField().toString(), is(SECTION));

    }

    @Test
    public void shouldFindDocumentsAndFilterRestrictedDocumentByPaginationOrderBySectionAsc() throws IOException {

        final UUID caseId = randomUUID();
        final int pageSize = 2;

        final SearchCriteria searchCriteria = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .build();

        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final JsonEnvelope jsonEnvelopeIn = getJsonEnvelopeForQueryRequest(caseId, pageSize, 1, SECTION, ASC, null);

        when(courtDocumentIndexService.getCourtDocumentIndexByCriteria(any())).thenReturn(asList(
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload-restricted-document.json", "Court Final orders0", "2020-09-18T08:56:14.195Z")),
                getCourtDocumentIndexEntity(getCourtDocumentPayload("court-document-payload.json", "Court Final orders3", "2020-09-20T08:56:14.195Z"))
        ));
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);

        //query first page
        JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsWithPagination(jsonEnvelopeIn);
        CourtdocumentsWithPagination result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtdocumentsWithPagination.class);
        assertThat(result.getPaginationData().getTotalRecordCount(), is(1));
        assertThat(result.getCourtDocuments().size(), is(1));
        assertThat(result.getPaginationData().getPage(), is(1));
        assertThat(result.getPaginationData().getPageSize(), is(2));
        assertThat(result.getPaginationData().getSortOrder().toString(), is(ASC));
        assertThat(result.getPaginationData().getSortField().toString(), is(SECTION));
        assertThat(result.getCourtDocuments().get(0).getDocumentTypeDescription(), is("Court Final orders3"));

    }

    private JsonEnvelope getJsonEnvelopeForQueryRequest(final UUID caseId, final int pageSize, final int page, final String sortField, final String sortOrder, final String documentName) {
        final JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add(SORT_FIELD, sortField);
        jsonBuilder.add(SORT_ORDER, sortOrder);
        jsonBuilder.add(CASE_ID, caseId.toString());
        jsonBuilder.add(PAGE, page);
        jsonBuilder.add(PAGE_SIZE, pageSize);
        if (nonNull(documentName)) {
            jsonBuilder.add(DOCUMENT_NAME, documentName);
        }

        JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENTS_SEARCH_WITH_PAGINATION_NAME)
                        .withUserId(randomUUID().toString())
                        .build(),
                jsonBuilder.build());
        return jsonEnvelopeIn;
    }

    private String getCourtDocumentPayload(final String resourceName, final String section, final String uploadDate) throws IOException {
        return getJsonPayloadAsString(resourceName)
                .replace("TYPE_DESCRIPTION", section)
                .replace("DOCUMENT_NAME", section)
                .replace("UPLOAD_DATE", uploadDate);
    }

    private CourtDocumentIndexEntity getCourtDocumentIndexEntity(final String payload) {
        final CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setCourtDocument(getCourtDocumentEntity(payload));
        courtDocumentIndexEntity.setId(randomUUID());
        return courtDocumentIndexEntity;
    }

    private CourtDocumentEntity getCourtDocumentEntity(final String payload) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(randomUUID());
        courtDocumentEntity.setName("doc name");
        courtDocumentEntity.setPayload(payload);
        return courtDocumentEntity;
    }


    @Test
    public void shouldFindDocumentsByCaseIdNotPermitted() throws IOException {
        shouldFindDocuments(true, false, asList(UUID.randomUUID()), null, null, "group1", false, false);
    }

    @Test
    public void shouldNotFindDocumentsByDefendantIdPermitted() throws IOException {
        shouldFindDocuments(false, true, null, UUID.randomUUID(), null, "group1", false, false);
    }

    private void addId(List<UUID> caseId, UUID defendantId, List<UUID> applicationId, Map<UUID, CourtDocumentIndex.Builder> id2ExpectedCourtDocumentIndex,
                       Map<UUID, UUID> courtDocumentId2Id, final UUID documentTypeId, final String userGroup) {
        final UUID courtDocumentId = UUID.randomUUID();
        final CourtDocument courtDocument = courtDocument(courtDocumentId, documentTypeId, userGroup);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);

        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withDocument(courtDocument)
                .withHearingIds(asList(UUID.randomUUID()))
                .withType("Defendant profile notes");

        UUID id = null;
        if (isNotEmpty(caseId) || defendantId != null || isNotEmpty(applicationId)) {
            if (isNotEmpty(caseId) && defendantId != null) {
                id = defendantId;
                courtDocumentIndexBuilder.withCaseIds(caseId).withDefendantIds(asList(defendantId))
                        .withCategory("Defendant level");
                when(courtDocumentRepository.
                        findByProsecutionCaseIdAndDefendantId(caseId, asList(defendantId))).thenReturn(asList(courtDocumentEntity));
            } else if (isNotEmpty(caseId)) {
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
                when(courtDocumentRepository.findByDefendantId(any())).thenReturn(asList(courtDocumentEntity));
            }
        }

        id2ExpectedCourtDocumentIndex.put(id, courtDocumentIndexBuilder);
        courtDocumentId2Id.put(courtDocument.getCourtDocumentId(), id);

    }

    private void shouldFindDocuments(final boolean rbackReadPermitted, final boolean permitted, final List<UUID> caseIds, final UUID defendantId, final List<UUID> applicationIds, final String userGroup, final boolean isProsecuting, final boolean shouldStub) throws IOException {
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", userGroup).build())
                .build();

        final JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("prosecutingCase", isProsecuting);
        final Map<UUID, CourtDocumentIndex.Builder> id2ExpectedCourtDocumentIndex = new HashMap<>();
        final Map<UUID, UUID> courtDocumentId2Id = new HashMap<>();
        if (isNotEmpty(caseIds) || defendantId != null || isNotEmpty(applicationIds)) {
            if (isNotEmpty(caseIds) && defendantId != null) {
                addId(caseIds, defendantId, null, id2ExpectedCourtDocumentIndex, courtDocumentId2Id, DOCUMENT_TYPE_ID_1, userGroup);
                jsonBuilder.add(CASE_ID_SEARCH_PARAM, caseIds.stream().map(UUID::toString).collect(Collectors.joining(",")));
                jsonBuilder.add(DEFENDANT_ID_SEARCH_PARAM, defendantId.toString());
            } else if (caseIds != null) {
                addId(caseIds, null, null, id2ExpectedCourtDocumentIndex, courtDocumentId2Id, DOCUMENT_TYPE_ID_1, null);
                jsonBuilder.add(CASE_ID_SEARCH_PARAM, caseIds.stream().map(UUID::toString).collect(Collectors.joining(",")));
            } else if (applicationIds != null) {
                addId(null, null, applicationIds, id2ExpectedCourtDocumentIndex, courtDocumentId2Id, DOCUMENT_TYPE_ID_2, null);
                jsonBuilder.add(APPLICATION_ID_SEARCH_PARAM, applicationIds.stream().map(UUID::toString).collect(Collectors.joining(",")));

            } else if (defendantId != null) {
                addId(null, defendantId, null, id2ExpectedCourtDocumentIndex, courtDocumentId2Id, DOCUMENT_TYPE_ID_4, null);
                jsonBuilder.add(DEFENDANT_ID_SEARCH_PARAM, defendantId.toString());
            }
        }

        final JsonObject jsonObject = jsonBuilder.build();

        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENTS_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonObject);

        mockReferenceData(jsonEnvelopeIn, userGroupArray);


        Answer<?> transformResult = new Answer<>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                CourtDocument courtDocument = (CourtDocument) invocationOnMock.getArguments()[0];
                UUID id = courtDocumentId2Id.get(courtDocument.getCourtDocumentId());
                return id2ExpectedCourtDocumentIndex.get(id);
            }
        };
        if(shouldStub) {
            when(courtDocumentTransform.transform(any(), any())).thenAnswer(transformResult);
        }
        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocuments(jsonEnvelopeIn);
        final CourtDocumentsSearchResult result = jsonObjectToObjectConverter
                .convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtDocumentsSearchResult.class);

        if (!permitted || !rbackReadPermitted) {
            assertThat(result.getDocumentIndices().size(), is(0));
        } else if (!isProsecuting) {
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

        }else{
            assertThat(result.getDocumentIndices().size(), is(0));
        }
    }

    @Test
    public void shouldFindAllDocuments() {

        final UUID caseId = randomUUID();
        final JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        final Map<UUID, CourtDocumentIndex.Builder> id2ExpectedCourtDocumentIndex = new HashMap<>();
        final Map<UUID, UUID> courtDocumentId2Id = new HashMap<>();
        addId(singletonList(caseId), null, null, id2ExpectedCourtDocumentIndex, courtDocumentId2Id, DOCUMENT_TYPE_ID_1, null);
        jsonBuilder.add(CASE_ID_SEARCH_PARAM, caseId.toString());

        final JsonObject jsonObject = jsonBuilder.build();

        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENTS_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonObject);


        Answer<?> transformResult = new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                CourtDocument courtDocument = (CourtDocument) invocationOnMock.getArguments()[0];
                UUID id = courtDocumentId2Id.get(courtDocument.getCourtDocumentId());
                return id2ExpectedCourtDocumentIndex.get(id);
            }
        };

        when(courtDocumentTransform.transform(any(), any())).thenAnswer(transformResult);

        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocumentsAll(jsonEnvelopeIn);
        final CourtDocumentsSearchResult result = jsonObjectToObjectConverter.convert(jsonEnvelopeOut.payloadAsJsonObject(), CourtDocumentsSearchResult.class);

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

    private JsonObject getJsonPayload(final String fileName) throws IOException {
        return Json.createReader(
                        new ByteArrayInputStream(getJsonPayloadAsString(fileName).getBytes()))
                .readObject();
    }

    private String getJsonPayloadAsString(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), defaultCharset());
    }

    @Test
    public void shouldFindDocumentsByApplicationId() throws IOException {
        boolean permitted = true;
        final UUID applicationId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(APPLICATION_ID, applicationId.toString())
                .add(APPLICATION_ID, applicationId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENTS_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonObject);
        final List<String> userGroups = singletonList("group1");
        final DocumentTypeRBAC documentTypeRBAC = new DocumentTypeRBAC(userGroups, userGroups, now(), now().plusMonths(5), null, null);
        final CourtDocument courtDocument = courtDocumentApplicationType(courtDocumentId, 10, documentTypeRBAC, DOCUMENT_TYPE_ID_1);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);
        when(courtDocumentRepository.findByApplicationIds(Collections.singletonList(applicationId))).thenReturn(asList(courtDocumentEntity));
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();
        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(applicationId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(UUID.randomUUID()))
                .withHearingIds(asList(UUID.randomUUID()))
                .withType("Defendant profile notes");

        when(courtDocumentTransform.transform(any(), any())).thenReturn(courtDocumentIndexBuilder);

        mockReferenceData(jsonEnvelopeIn, userGroupArray);
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
            verify(courtDocumentTransform, times(1)).transform(courtDocumentArgumentCaptor.capture(), any());

            final CourtDocument courtDocumentTransformed = courtDocumentArgumentCaptor.getValue();
            final int materialCount = courtDocumentTransformed.getMaterials().size();
            final int expectedMaterialCount = permitted ? 1 : 0;
            assertThat(materialCount, is(expectedMaterialCount));
        } else {
            assertThat(result.getDocumentIndices().size(), is(0));
        }
    }


    @Test
    public void shouldNotFindDocumentsWhenIsRemovedisTrue() throws IOException {
        boolean permitted = true;
        final UUID applicationId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(APPLICATION_ID, applicationId.toString())
                .add(APPLICATION_ID, applicationId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENTS_SEARCH_NAME).withUserId(randomUUID().toString()).build(),
                jsonObject);
        final CourtDocument courtDocument = removedCourtDocument(courtDocumentId, DOCUMENT_TYPE_ID_1);
        final CourtDocumentEntity courtDocumentEntity = removedCourtDocumentEntity(courtDocumentId, courtDocument);
        when(courtDocumentRepository.findByApplicationIds(Collections.singletonList(applicationId))).thenReturn(asList(courtDocumentEntity));
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCaseIds(asList(applicationId))
                .withCategory("Defendant level")
                .withDefendantIds(asList(UUID.randomUUID()))
                .withHearingIds(asList(UUID.randomUUID()))
                .withType("Defendant profile notes");

        mockUserGroups(userGroupArray, jsonEnvelopeIn);
        mockReferenceData(jsonEnvelopeIn, userGroupArray);
        final CourtDocumentIndex expectedIndex = courtDocumentIndexBuilder.build();
        final JsonEnvelope jsonEnvelopeOut = target.searchCourtDocuments(jsonEnvelopeIn);
        assertTrue(jsonEnvelopeOut.payloadAsJsonObject().getJsonArray("documentIndices").isEmpty());
    }

    @Test
    public void shouldNotListDocumentsWithNoReadOnlyAccessAndListAllOther() throws IOException {
        final UUID applicationId = UUID.randomUUID();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(APPLICATION_ID, applicationId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENTS_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonObject);
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "Court Clerks").build())
                .build();

        final UUID nowCourtDocumentId = randomUUID();
        final List userGroups = singletonList("Court Clerks");
        final DocumentTypeRBAC documentTypeRBAC1 = new DocumentTypeRBAC(userGroups, userGroups, now(), now().plusMonths(5), userGroups, userGroups);
        final DocumentTypeRBAC documentTypeRBAC2 = new DocumentTypeRBAC(userGroups, userGroups, now(), now().plusMonths(5), userGroups, userGroups);
        final CourtDocument courtDocument1 = courtDocumentApplicationType(nowCourtDocumentId, 20, documentTypeRBAC1, DOCUMENT_TYPE_ID_1);
        final CourtDocumentEntity courtDocumentEntity1 = courtDocumentEntityWithRbac(nowCourtDocumentId, courtDocument1);

        final UUID secondNowCourtDocumentId = randomUUID();
        final CourtDocument courtDocument2 = courtDocumentApplicationType(secondNowCourtDocumentId, 10, documentTypeRBAC2, DOCUMENT_TYPE_ID_2);
        final CourtDocumentEntity courtDocumentEntity2 = courtDocumentEntityWithRbac(secondNowCourtDocumentId, courtDocument2);


        final UUID courtDocumentId = UUID.randomUUID();
        final CourtDocument courtDocument = courtDocumentApplicationType(courtDocumentId, 30, documentTypeRBAC1, DOCUMENT_TYPE_ID_3);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntity(courtDocumentId, courtDocument);

        when(courtDocumentRepository.findByApplicationIds(Collections.singletonList(applicationId))).thenReturn(asList(courtDocumentEntity1, courtDocumentEntity2, courtDocumentEntity));
        //mockUserGroups(userGroupArray, jsonEnvelopeIn);

        final CourtDocumentIndex.Builder courtDocumentIndexBuilder1 = createCourtDocumentIndex(courtDocument1, applicationId);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder2 = createCourtDocumentIndex(courtDocument2, applicationId);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = createCourtDocumentIndex(courtDocument1, applicationId);

        when(courtDocumentTransform.transform(any(), any())).thenReturn(courtDocumentIndexBuilder1)
                .thenReturn(courtDocumentIndexBuilder2)
                .thenReturn(courtDocumentIndexBuilder);

        mockReferenceData(jsonEnvelopeIn, userGroupArray);

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

        verify(courtDocumentTransform, times(2)).transform(courtDocumentArgumentCaptor.capture(), any());

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
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(DEFENDANT_ID_PARAMETER, defendantId.toString())
                .add(HEARING_ID_PARAMETER, hearingId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENTS_NOW_SEARCH_NAME).build(),
                jsonObject);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCategory("NOW_DOCUMENT")
                .withDefendantIds(asList(defendantId))
                .withHearingIds(asList(hearingId))
                .withType("Defendant profile notes");

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
            verify(courtDocumentTransform, times(1)).transform(courtDocumentArgumentCaptor.capture(), cpsSendNotificationRepository);

            final CourtDocument courtDocumentTransformed = courtDocumentArgumentCaptor.getValue();
            final int materialCount = courtDocumentTransformed.getMaterials().size();
            final int expectedMaterialCount = permitted ? 1 : 0;
            assertThat(materialCount, is(expectedMaterialCount));
        } else {
            assertThat(result.getDocumentIndices().size(), is(0));
        }
    }

    @Test
    public void shouldFindDocumentsByHearingIdWithRbac() throws IOException {
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final JsonArray userGroupArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("groupName", "group1").build())
                .build();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(DEFENDANT_ID_PARAMETER, defendantId.toString())
                .add(HEARING_ID_PARAMETER, hearingId.toString())
                .build();
        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(COURT_DOCUMENTS_NOW_SEARCH_NAME)
                        .withUserId(randomUUID().toString()).build(),
                jsonObject);
        final CourtDocument courtDocument = courtDocumentNowTypeWithRbacDetails(courtDocumentId);
        final CourtDocumentEntity courtDocumentEntity = courtDocumentEntityWithRbac(courtDocumentId, courtDocument);
        when(courtDocumentRepository.findCourtDocumentForNow(hearingId, "", defendantId)).thenReturn(singletonList(courtDocumentEntity));
        mockReferenceData(jsonEnvelopeIn, userGroupArray);
        final CourtDocumentIndex.Builder courtDocumentIndexBuilder = CourtDocumentIndex.courtDocumentIndex()
                .withCategory("NOW_DOCUMENT")
                .withDefendantIds(singletonList(defendantId))
                .withHearingIds(singletonList(hearingId))
                .withType("Defendant profile notes")
                .withCaseIds(singletonList(hearingId));

        when(courtDocumentTransform.transform(any(), any())).thenReturn(courtDocumentIndexBuilder);

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
        verify(courtDocumentTransform, times(1)).transform(courtDocumentArgumentCaptor.capture(), any());

        final CourtDocument courtDocumentTransformed = courtDocumentArgumentCaptor.getValue();
        final int materialCount = courtDocumentTransformed.getMaterials().size();
        assertThat(materialCount, is(1));
    }

    private void mockUserGroups(final JsonArray userGroupArray, final JsonEnvelope jsonEnvelopeIn) {

    }


    private void mockReferenceData(final JsonEnvelope jsonEnvelopeIn, final JsonArray userGroupArray) throws IOException {

        final JsonObject documentsAccess = getJsonPayload("get-all-document-type-access.json");
        final JsonObject groups = Json.createObjectBuilder().add("groups", userGroupArray).build();

        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            JsonObject request = envelope.payloadAsJsonObject();
            JsonObject responsePayload = null;
            if (envelope.metadata().name().equals("referencedata.get-all-document-type-access")) {
                responsePayload = documentsAccess;
            } else if (envelope.metadata().name().equals("usersgroups.get-groups-by-user")) {
                responsePayload = groups;
            }
            return envelopeFrom(envelope.metadata(), responsePayload);
        });


    }

    @Test
    public void shouldGetCourtDocumentNotificationStatus() {
        final UUID caseId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();
        final UUID documentTypeId = DOCUMENT_TYPE_ID_1;

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add(CASE_ID_SEARCH_PARAM, caseId.toString())
                .build();

        final JsonEnvelope jsonEnvelopeIn = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(PROSECUTION_NOTIFICATION_STATUS).build(),
                jsonObject);

        final ProsecutionCase prosecutionCase = createProsecutionCase(randomUUID());

        final CourtDocument courtDocument = courtDocument(courtDocumentId, documentTypeId,"Court Clerks");
        final NotificationStatusEntity notificationStatusEntity = notificationStatusEntity(caseId);

        when(notificationStatusRepository.findByCaseId(caseId)).thenReturn(asList(notificationStatusEntity));

        final JsonEnvelope jsonEnvelopeOut = target.getCaseNotifications(jsonEnvelopeIn);
        assertThat(jsonEnvelopeOut.payloadAsJsonObject().getJsonArray("notificationStatus").size(), is(1));
    }

    @Test
    public void shouldGetCourtDocumentByCaseIdAndDefedantId() throws IOException {
        shouldFindDocuments(true, true, asList(UUID.randomUUID(), UUID.randomUUID()), UUID.randomUUID(), null, "Legal Advisers", false, true);
    }
}


