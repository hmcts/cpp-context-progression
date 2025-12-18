package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.core.courts.CourtDocument.courtDocument;
import static uk.gov.justice.core.courts.CourtDocumentPrintTimeUpdated.courtDocumentPrintTimeUpdated;
import static uk.gov.justice.core.courts.Material.material;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentPrintTimeUpdated;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.events.CourtApplicationDocumentUpdated;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentTypeRBAC;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.util.FileUtil;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentEventListenerTest {

    public static final String CASE_DOCUMENT_ID = "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27";

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();;

    @Mock
    private CourtDocumentRepository repository;

    @Mock
    private CourtDocumentIndexRepository courtDocumentIndexRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private Envelope<CourtApplicationDocumentUpdated> courtApplicationDocumentUpdatedEnvelope;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @Mock
    private CourtsDocumentCreated courtsDocumentCreated;

    @Mock
    private CourtDocument courtDocument;

    @Mock
    private DocumentCategory documentCategory;

    @Captor
    private ArgumentCaptor<CourtDocumentEntity> argumentCaptorForCourtDocumentEntity;

    @Captor
    private ArgumentCaptor<CourtDocumentTypeRBAC> argumentCaptorForCourtDocumentRBAC;

    @Captor
    private ArgumentCaptor<CourtDocumentMaterialEntity> argumentCaptorForCourtDocumentMaterialEntity;

    @Captor
    private ArgumentCaptor<Object> objectArgumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private DefendantDocument defendantDocument;
    @Mock
    private NowDocument nowDocument;
    @Mock
    private CourtApplicationRepository applicationRepository;

    @InjectMocks
    private CourtDocumentEventListener eventListener;

    private static JsonObject buildDocumentCategoryJsonObject() {

        final JsonObject documentCategory =
                createObjectBuilder().add("defendantDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", CASE_DOCUMENT_ID)
                                .add("defendants", createArrayBuilder().add("e1d32d9d-29ec-4934-a932-22a50f223966"))).build();

        final JsonObject courtDocument =
                createObjectBuilder().add("courtDocument",
                        createObjectBuilder()
                                .add("courtDocumentId", CASE_DOCUMENT_ID)
                                .add("documentCategory", documentCategory)
                                .add("name", "SJP Notice")
                                .add("documentTypeId", "0bb7b276-9dc0-4af2-83b9-f4acef0c7898")
                                .add("documentTypeDescription", "SJP Notice")
                                .add("mimeType", "pdf")
                                .add("materials", createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1"))
                                .add("containsFinancialMeans", true)
                                .add("courtDocumentTypeRBAC",
                                        JsonObjects.createObjectBuilder()
                                                .add("uploadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer").build()).build())
                                                .add("readUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build())
                                                .add("downloadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build()).build())
                                .add("seqNum",10)

                ).build();
        return courtDocument;
    }

    private static DocumentTypeRBAC createDocumentTypeRBACObject() {
        return DocumentTypeRBAC.documentTypeRBAC()
                .withUploadUserGroups(Arrays.asList("Listing Officer"))
                .withReadUserGroups(Arrays.asList("Listing Officer", "Magistrates"))
                .withDownloadUserGroups(Arrays.asList("Listing Officer", "Magistrates"))
                .build();

    }

    @Test
    public void shouldHandleCourtDocumentCreatedEventWithContainsFinancialsMeansTrue() throws Exception {

        executeHandleCourtDocumentCreatedWithFinancialMeans(true, false);
    }

    @Test
    public void shouldHandleCourtDocumentCreatedEventWithContainsFinancialsMeansFalse() throws Exception {

        executeHandleCourtDocumentCreatedWithFinancialMeans(false, false);

    }

    @Test
    public void shouldHandleCourtDocumentCreatedEventWithContainsFinancialsMeansNull() throws Exception {

        executeHandleCourtDocumentCreatedWithFinancialMeans(false, true);

    }

    @Test
    public void shouldHandleCourtDocumentCreatedEventWithDocumentTypeRBACExists() throws Exception {

        executeHandleCourtDocumentCreatedWithDocumentRBACType(true);

    }

    @Test
    public void shouldHandleCourtDocumentCreatedEventWithDocumentTypeRBACIsNullExists() throws Exception {

        executeHandleCourtDocumentCreatedWithDocumentRBACType(false);

    }

    @Test
    public void shouldProcessCourApplicationDocumentUpdated() {
        final UUID oldApplicationId = randomUUID();
        final UUID applicationId = randomUUID();
        final CourtApplicationDocumentUpdated payload = CourtApplicationDocumentUpdated.courtApplicationDocumentUpdated()
                .withOldApplicationId(oldApplicationId)
                .withApplicationId(applicationId)
                .build();

        when(courtApplicationDocumentUpdatedEnvelope.payload()).thenReturn(payload);

        eventListener.processCourApplicationDocumentUpdated(courtApplicationDocumentUpdatedEnvelope);

        verify(courtDocumentIndexRepository).updateApplicationIdByApplicationId(applicationId, oldApplicationId);
    }

    private void executeHandleCourtDocumentCreatedWithFinancialMeans(final boolean financialMeansFlag,
                                                                     final boolean courtDocumentFinancialMeansValueNull) {

        final JsonObject courtDocumentPayload = buildDocumentCategoryJsonObject();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.court-application-document-updated"),
                courtDocumentPayload);
        setUpMockData(courtDocumentPayload, requestMessage);
        if (courtDocumentFinancialMeansValueNull) {
            when(courtDocument.getContainsFinancialMeans()).thenReturn(null);
        } else {
            when(courtDocument.getContainsFinancialMeans()).thenReturn(financialMeansFlag);
        }

        processCourtDocumentCreatedAndVerify(requestMessage);


        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();
        assertEquals(courtDocumentPayload.getJsonObject("courtDocument").getString("courtDocumentId"),
                entity.getCourtDocumentId().toString());
        assertEquals(financialMeansFlag,
                entity.getContainsFinancialMeans());
    }

    @Test
    public void shouldHandleCourtDocumentCreatedEventForStandaloneApplication() {

        final JsonObject courtDocumentPayload = stringToJsonObjectConverter.convert(FileUtil.getPayload("json/progression.event.court-document-created_for-standalone-application.json")
                .replaceAll("%COURT_DOCUMENT_ID%", CASE_DOCUMENT_ID)
                .replaceAll("%CASE_ID%", CASE_DOCUMENT_ID)
        );

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.court-document-created"),
                courtDocumentPayload);

        final Material material = new Material("Generated",
                fromString("5e1cc18c-76dc-47dd-99c1-d6f87385edf1"),
                "TestPDF",
                null,
                null,
                null, Arrays.asList("TestUser"));
        final List<Material> materialIds = Arrays.asList(material);

        //Setting all the conditions on the Mocks to behave as expected for the code to execute
        when(jsonObjectToObjectConverter.convert(requestMessage.payloadAsJsonObject(), CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(courtDocument.getCourtDocumentId()).thenReturn(fromString(CASE_DOCUMENT_ID));

        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getNowDocument()).thenReturn(nowDocument);
        when(nowDocument.getProsecutionCases()).thenReturn(singletonList(fromString(CASE_DOCUMENT_ID)));
        when(objectToJsonObjectConverter.convert(any())).thenReturn(courtDocumentPayload);
        when(courtDocument.getMaterials()).thenReturn(materialIds);
        when(courtDocument.getContainsFinancialMeans()).thenReturn(null);
        when(applicationRepository.findByApplicationId(fromString(CASE_DOCUMENT_ID))).thenReturn(new CourtApplicationEntity());

        eventListener.processCourtDocumentCreated(requestMessage);

        verify(repository, times(1)).save(argumentCaptorForCourtDocumentEntity.capture());
        verify(courtDocumentMaterialRepository, times(1)).save(argumentCaptorForCourtDocumentMaterialEntity.capture());

        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();
        final CourtDocumentIndexEntity courtDocumentIndexEntity = entity.getIndices().iterator().next();
        assertThat(courtDocumentIndexEntity.getApplicationId(), is(fromString(CASE_DOCUMENT_ID)));
        assertThat(courtDocumentIndexEntity.getProsecutionCaseId(), nullValue());

    }

    @Test
    public void shouldHandleCourtDocumentCreatedEventWhenNowsDocumentGeneratedForCase() {

        final JsonObject courtDocumentPayload = stringToJsonObjectConverter.convert(FileUtil.getPayload("json/progression.event.court-document-created_for-standalone-application.json")
                .replaceAll("%COURT_DOCUMENT_ID%", CASE_DOCUMENT_ID)
                .replaceAll("%CASE_ID%", CASE_DOCUMENT_ID)
        );

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.court-document-created"),
                courtDocumentPayload);

        final Material material = new Material("Generated",
                fromString("5e1cc18c-76dc-47dd-99c1-d6f87385edf1"),
                "TestPDF",
                null,
                null,
                null, Arrays.asList("TestUser"));
        final List<Material> materialIds = Arrays.asList(material);

        //Setting all the conditions on the Mocks to behave as expected for the code to execute
        when(jsonObjectToObjectConverter.convert(requestMessage.payloadAsJsonObject(), CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(courtDocument.getCourtDocumentId()).thenReturn(fromString(CASE_DOCUMENT_ID));

        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getNowDocument()).thenReturn(nowDocument);
        when(nowDocument.getProsecutionCases()).thenReturn(singletonList(fromString(CASE_DOCUMENT_ID)));
        when(objectToJsonObjectConverter.convert(any())).thenReturn(courtDocumentPayload);
        when(courtDocument.getMaterials()).thenReturn(materialIds);
        when(courtDocument.getContainsFinancialMeans()).thenReturn(null);

        eventListener.processCourtDocumentCreated(requestMessage);

        verify(repository, times(1)).save(argumentCaptorForCourtDocumentEntity.capture());
        verify(courtDocumentMaterialRepository, times(1)).save(argumentCaptorForCourtDocumentMaterialEntity.capture());

        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();
        final CourtDocumentIndexEntity courtDocumentIndexEntity = entity.getIndices().iterator().next();
        assertThat(courtDocumentIndexEntity.getProsecutionCaseId(), is(fromString(CASE_DOCUMENT_ID)));
        assertThat(courtDocumentIndexEntity.getApplicationId(), nullValue());

    }

    private void executeHandleCourtDocumentCreatedWithDocumentRBACType(final boolean rbacTypeexists) {

        final JsonObject courtDocumentPayload = buildDocumentCategoryJsonObject();
        final JsonEnvelope requestMessage = getCreateDocumentEnvelope(courtDocumentPayload);
        setUpMockData(courtDocumentPayload, requestMessage);
        if (!rbacTypeexists) {
            when(courtDocument.getDocumentTypeRBAC()).thenReturn(null);
        } else {
            when(courtDocument.getDocumentTypeRBAC()).thenReturn(createDocumentTypeRBACObject());
        }
        when(courtDocument.getSeqNum()).thenReturn(10);

        processCourtDocumentCreatedAndVerify(requestMessage);


        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();

        assertThat(courtDocumentPayload.getJsonObject("courtDocument").getString("courtDocumentId"), is(entity.getCourtDocumentId().toString()));
        assertThat(entity.getSeqNum().intValue(), is(10) );

        if (rbacTypeexists) {
            assertNotNull(entity.getCourtDocumentTypeRBAC().getCreateUserGroups());
            assertNotNull(entity.getCourtDocumentTypeRBAC().getDownloadUserGroups());
            assertNotNull(entity.getCourtDocumentTypeRBAC().getReadUserGroups());
        }

    }

    private JsonEnvelope getCreateDocumentEnvelope(final JsonObject courtDocumentPayload) {
        return JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);
    }

    private void processCourtDocumentCreatedAndVerify(final JsonEnvelope requestMessage) {
        eventListener.processCourtDocumentCreated(requestMessage);
        verify(repository, times(1)).save(argumentCaptorForCourtDocumentEntity.capture());
        verify(courtDocumentMaterialRepository, times(1)).save(argumentCaptorForCourtDocumentMaterialEntity.capture());
    }

    private void setUpMockData(final JsonObject courtDocumentPayload, final JsonEnvelope requestMessage) {
        final Material material = new Material("Generated",
                fromString("5e1cc18c-76dc-47dd-99c1-d6f87385edf1"),
                "TestPDF",
                null,
                null,
                null, Arrays.asList("TestUser"));
        final List<Material> materialIds = Arrays.asList(material);

        //Setting all the conditions on the Mocks to behave as expected for the code to execute
        when(jsonObjectToObjectConverter.convert(requestMessage.payloadAsJsonObject(), CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(courtDocument.getCourtDocumentId()).thenReturn(fromString(CASE_DOCUMENT_ID));

        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getDefendantDocument()).thenReturn(defendantDocument);
        when(defendantDocument.getProsecutionCaseId()).thenReturn(randomUUID());
        when(defendantDocument.getDefendants()).thenReturn(singletonList(randomUUID()));
        when(objectToJsonObjectConverter.convert(any())).thenReturn(courtDocumentPayload);
        when(courtDocument.getMaterials()).thenReturn(materialIds);
    }

    @Test
    public void shouldHandleApplicationDocumentCreatedEventWithProsecutionIdAndApplicationId() throws Exception {

        final ApplicationDocument applicationDocument = ApplicationDocument.applicationDocument()
                .withProsecutionCaseId(randomUUID())
                .withApplicationId(randomUUID())
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(courtDocument.getCourtDocumentId()).thenReturn(randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getApplicationDocument()).thenReturn(applicationDocument);
        when(documentCategory.getCaseDocument()).thenReturn(null);
        when(documentCategory.getDefendantDocument()).thenReturn(null);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        eventListener.processCourtDocumentCreated(envelope);
        verify(repository).save(argumentCaptorForCourtDocumentEntity.capture());
        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();
        assertThat(entity.getIndices().iterator().next().getProsecutionCaseId(),
                Matchers.is(applicationDocument.getProsecutionCaseId()));
        assertThat(entity.getIndices().iterator().next().getApplicationId(),
                Matchers.is(applicationDocument.getApplicationId()));
    }

    @Test
    public void shouldHandleApplicationDocumentCreatedEventWithNoProsecutionId() throws Exception {

        final ApplicationDocument applicationDocument = ApplicationDocument.applicationDocument()
                .withProsecutionCaseId(null)
                .withApplicationId(randomUUID())
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(courtDocument.getCourtDocumentId()).thenReturn(randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getApplicationDocument()).thenReturn(applicationDocument);
        when(documentCategory.getCaseDocument()).thenReturn(null);
        when(documentCategory.getDefendantDocument()).thenReturn(null);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        eventListener.processCourtDocumentCreated(envelope);
        verify(repository).save(argumentCaptorForCourtDocumentEntity.capture());
        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();
        assertThat(entity.getIndices().iterator().next().getProsecutionCaseId(),
                Matchers.is(applicationDocument.getProsecutionCaseId()));
        assertThat(entity.getIndices().iterator().next().getApplicationId(),
                Matchers.is(applicationDocument.getApplicationId()));
    }

    @Test
    public void shouldHandleNowDocumentCreatedEvent() throws Exception {

        final NowDocument nowDocument = NowDocument.nowDocument()
                .withDefendantId(randomUUID())
                .withOrderHearingId(randomUUID())
                .withProsecutionCases(Arrays.asList(randomUUID(), randomUUID()))
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(courtDocument.getCourtDocumentId()).thenReturn(randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getApplicationDocument()).thenReturn(null);
        when(documentCategory.getDefendantDocument()).thenReturn(null);
        when(documentCategory.getNowDocument()).thenReturn(nowDocument);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        eventListener.processCourtDocumentCreated(envelope);
        verify(repository).save(argumentCaptorForCourtDocumentEntity.capture());

        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();

        assertThat(entity.getIndices().size(), Matchers.is(2));
        assertThat(entity.getIndices().iterator().next().getHearingId(), Matchers.is(nowDocument.getOrderHearingId()));
        assertThat(entity.getIndices().iterator().next().getDefendantId(), Matchers.is(nowDocument.getDefendantId()));

    }

    @Test
    public void shouldProcessCourtDocumentPrinted() {
        final ObjectToJsonObjectConverter jsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
        final UUID courtDocumentId = randomUUID();
        final UUID materialId = randomUUID();
        final ZonedDateTime printedAt = now();
        final CourtDocumentPrintTimeUpdated documentPrintTimeUpdated = courtDocumentPrintTimeUpdated()
                .withCourtDocumentId(courtDocumentId)
                .withMaterialId(materialId)
                .withPrintedAt(printedAt)
                .build();
        final Envelope<CourtDocumentPrintTimeUpdated> envelope =
                envelopeFrom(metadataWithRandomUUID("progression.event.court-document-print-time-updated"),
                        documentPrintTimeUpdated);

        final CourtDocument courtDocument = courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeId(UUID.randomUUID())
                .withMaterials(singletonList(material()
                        .withId(materialId)
                        .build()))
                .build();

        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setPayload(jsonObjectConverter.convert(courtDocument).toString());

        when(repository.findBy(courtDocumentId)).thenReturn(courtDocumentEntity);
        when(jsonObjectToObjectConverter.convert(any(), eq(CourtDocument.class))).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        eventListener.processCourtDocumentPrinted(envelope);
        verify(repository).save(courtDocumentEntity);
        verify(objectToJsonObjectConverter).convert(objectArgumentCaptor.capture());
        final CourtDocument savedCourtDocument = (CourtDocument)objectArgumentCaptor.getValue();
        assertThat(savedCourtDocument.getCourtDocumentId(), is(documentPrintTimeUpdated.getCourtDocumentId()));
        assertThat(savedCourtDocument.getMaterials().get(0).getPrintedDateTime(), is(printedAt));
    }

    private static JsonObjectBuilder buildUserGroup(final String userGroupName) {
        return JsonObjects.createObjectBuilder().add("cppGroup", JsonObjects.createObjectBuilder().add("id", randomUUID().toString()).add("groupName", userGroupName));
    }
}
