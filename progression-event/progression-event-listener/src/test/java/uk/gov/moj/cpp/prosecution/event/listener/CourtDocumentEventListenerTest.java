package uk.gov.moj.cpp.prosecution.event.listener;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.prosecutioncase.event.listener.CourtDocumentEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private CourtDocumentRepository repository;

    @Mock
    private JsonEnvelope envelope;

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
    private ArgumentCaptor<CourtDocumentMaterialEntity> argumentCaptorForCourtDocumentMaterialEntity;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private DefendantDocument defendantDocument;

    @InjectMocks
    private CourtDocumentEventListener eventListener;

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

    private void executeHandleCourtDocumentCreatedWithFinancialMeans(boolean financialMeansFlag,
                                                                     boolean courtDocumentFinancialMeansValueNull) {

        JsonObject courtDocumentPayload = buildDocumentCategoryJsonObject();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);
        Material material = new Material("Generated",
                UUID.fromString("5e1cc18c-76dc-47dd-99c1-d6f87385edf1"),
                "TestPDF",
                null, Arrays.asList("TestUser"));
        List<Material> materialIds = Arrays.asList(material);

        //Setting all the conditions on the Mocks to behave as expected for the code to execute
        when(jsonObjectToObjectConverter.convert(requestMessage.payloadAsJsonObject(), CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(courtDocument.getCourtDocumentId()).thenReturn(UUID.randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getDefendantDocument()).thenReturn(defendantDocument);
        when(defendantDocument.getProsecutionCaseId()).thenReturn(UUID.randomUUID());
        when(defendantDocument.getDefendants()).thenReturn(Collections.singletonList(UUID.randomUUID()));
        when(objectToJsonObjectConverter.convert(courtDocument)).thenReturn(courtDocumentPayload);
        when(courtDocument.getMaterials()).thenReturn(materialIds);
        final UUID courtDocumentId = UUID.fromString("2279b2c3-b0d3-4889-ae8e-1ecc20c39e27");
        when(courtDocument.getCourtDocumentId()).thenReturn(courtDocumentId);
        if (courtDocumentFinancialMeansValueNull) {
            when(courtDocument.getContainsFinancialMeans()).thenReturn(null);
        } else {
            when(courtDocument.getContainsFinancialMeans()).thenReturn(financialMeansFlag);
        }

        eventListener.processCourtDocumentCreated(requestMessage);
        verify(repository, times(1)).save(argumentCaptorForCourtDocumentEntity.capture());
        verify(courtDocumentMaterialRepository, times(1)).save(argumentCaptorForCourtDocumentMaterialEntity.capture());


        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();
        assertEquals(courtDocumentPayload.getJsonObject("courtDocument").getString("courtDocumentId"),
                entity.getCourtDocumentId().toString());
        assertEquals(financialMeansFlag,
                entity.getContainsFinancialMeans());
    }

    private static JsonObject buildDocumentCategoryJsonObject() {

        final JsonObject documentCategory =
                createObjectBuilder().add("defendantDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("defendants", createArrayBuilder().add("e1d32d9d-29ec-4934-a932-22a50f223966"))).build();

        final JsonObject courtDocument =
                createObjectBuilder().add("courtDocument",
                        createObjectBuilder()
                                .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("documentCategory", documentCategory)
                                .add("name", "SJP Notice")
                                .add("documentTypeId", "0bb7b276-9dc0-4af2-83b9-f4acef0c7898")
                                .add("documentTypeDescription", "SJP Notice")
                                .add("mimeType", "pdf")
                                .add("materials", createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1"))
                                .add("containsFinancialMeans", true)
                ).build();

        return courtDocument;
    }


    @Test
    public void shouldHandleApplicationDocumentCreatedEventWithProsecutionIdAndApplicationId() throws Exception {

        final ApplicationDocument applicationDocument = ApplicationDocument.applicationDocument()
                .withProsecutionCaseId(UUID.randomUUID())
                .withApplicationId(UUID.randomUUID())
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtDocument.getCourtDocumentId()).thenReturn(UUID.randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getApplicationDocument()).thenReturn(applicationDocument);
        when(documentCategory.getCaseDocument()).thenReturn(null);
        when(documentCategory.getDefendantDocument()).thenReturn(null);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(courtDocument)).thenReturn(jsonObject);
        eventListener.processCourtDocumentCreated(envelope);
        verify(repository).save(argumentCaptorForCourtDocumentEntity.capture());
        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();
        MatcherAssert.assertThat(entity.getIndices().iterator().next().getProsecutionCaseId(),
                Matchers.is(applicationDocument.getProsecutionCaseId()));
        MatcherAssert.assertThat(entity.getIndices().iterator().next().getApplicationId(),
                Matchers.is(applicationDocument.getApplicationId()));
    }

    @Test
    public void shouldHandleApplicationDocumentCreatedEventWithNoProsecutionId() throws Exception {

        final ApplicationDocument applicationDocument = ApplicationDocument.applicationDocument()
                .withProsecutionCaseId(null)
                .withApplicationId(UUID.randomUUID())
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtDocument.getCourtDocumentId()).thenReturn(UUID.randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getApplicationDocument()).thenReturn(applicationDocument);
        when(documentCategory.getCaseDocument()).thenReturn(null);
        when(documentCategory.getDefendantDocument()).thenReturn(null);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(courtDocument)).thenReturn(jsonObject);
        eventListener.processCourtDocumentCreated(envelope);
        verify(repository).save(argumentCaptorForCourtDocumentEntity.capture());
        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();
        MatcherAssert.assertThat(entity.getIndices().iterator().next().getProsecutionCaseId(),
                Matchers.is(applicationDocument.getProsecutionCaseId()));
        MatcherAssert.assertThat(entity.getIndices().iterator().next().getApplicationId(),
                Matchers.is(applicationDocument.getApplicationId()));
    }

    @Test
    public void shouldHandleNowDocumentCreatedEvent() throws Exception {

        final NowDocument nowDocument = NowDocument.nowDocument()
                .withDefendantId(UUID.randomUUID())
                .withOrderHearingId(UUID.randomUUID())
                .withProsecutionCases(Arrays.asList(UUID.randomUUID()))
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtDocument.getCourtDocumentId()).thenReturn(UUID.randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getApplicationDocument()).thenReturn(null);
        when(documentCategory.getCaseDocument()).thenReturn(null);
        when(documentCategory.getDefendantDocument()).thenReturn(null);
        when(documentCategory.getNowDocument()).thenReturn(nowDocument);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(courtDocument)).thenReturn(jsonObject);
        eventListener.processCourtDocumentCreated(envelope);
        verify(repository).save(argumentCaptorForCourtDocumentEntity.capture());
        final CourtDocumentEntity entity = argumentCaptorForCourtDocumentEntity.getValue();
        MatcherAssert.assertThat(entity.getIndices().iterator().next().getHearingId(),
                Matchers.is(nowDocument.getOrderHearingId()));
        MatcherAssert.assertThat(entity.getIndices().iterator().next().getDefendantId(),
                Matchers.is(nowDocument.getDefendantId()));
    }
}
