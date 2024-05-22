package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtDocument.courtDocument;
import static uk.gov.justice.core.courts.DocumentCategory.documentCategory;
import static uk.gov.justice.core.courts.Material.material;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.utils.PayloadUtil.getPayloadAsJsonObject;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.CpsEmailNotificationService;
import uk.gov.moj.cpp.progression.service.CpsRestNotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.CourtDocumentTransformer;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentNotifiedProcessorTest {

    private final String prosecutionCaseSampleWithCourtDocument = "progression.event.court-document-send-to-cps.json";

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @InjectMocks
    private CourtDocumentNotifiedProcessor courtDocumentNotifiedProcessor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private CpsEmailNotificationService cpsEmailNotificationService;

    @Mock
    private CpsRestNotificationService cpsRestNotificationService;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Mock
    private CourtDocumentTransformer courtDocumentTransformer;

    private String prosecutionCaseId;
    private Optional<JsonObject> prosecutionCaseJsonOptional;
    private CourtDocument courtDocument;

    @Before
    public void initMocks() throws IOException {
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        final UUID materialId = randomUUID();
        prosecutionCaseJsonOptional = of(getPayloadAsJsonObject(prosecutionCaseSampleWithCourtDocument));
        prosecutionCaseId = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase").getString("id");
        courtDocument = courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument()
                                .withProsecutionCaseId(fromString(prosecutionCaseId))
                                .build())
                        .build())
                .withMaterials(singletonList(material().withId(materialId).build()))
                .build();
        final JsonObject courtDocumentJsonObject = createObjectBuilder().build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(Optional.of(randomUUID().toString()));


        when(payload.getJsonObject("courtDocument")).thenReturn(courtDocumentJsonObject);
        when(jsonObjectConverter.convert(courtDocumentJsonObject, CourtDocument.class)).thenReturn(courtDocument);
        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope, prosecutionCaseId)).thenReturn(prosecutionCaseJsonOptional);
    }

    @Test
    public void shouldProcessCourtDocumentSendToCPS_WhenFeatureToggleIsOffForDefenceDisclosure() {
        when(featureControlGuard.isFeatureEnabled("defenceDisclosure")).thenReturn(false);

        courtDocumentNotifiedProcessor.processCourtDocumentSendToCPS(jsonEnvelope);
        verify(cpsEmailNotificationService).sendEmailToCps(jsonEnvelope, courtDocument, fromString(prosecutionCaseId), prosecutionCaseJsonOptional.get());
    }

    @Test
    public void shouldProcessCourtDocumentSendToCPS_WhenFeatureToggleIsOnForDefenceDisclosure() {
        final String transformedPayload = Json.createObjectBuilder().add("a", "b").build().toString();
        when(courtDocumentTransformer.transform(any(CourtDocument.class), any(Optional.class), any(JsonEnvelope.class), any(String.class))).thenReturn(of(transformedPayload));
        when(featureControlGuard.isFeatureEnabled("defenceDisclosure")).thenReturn(true);

        courtDocumentNotifiedProcessor.processCourtDocumentSendToCPS(jsonEnvelope);
        verify(courtDocumentTransformer).transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);
        verify(cpsEmailNotificationService, never()).sendEmailToCps(jsonEnvelope, courtDocument, fromString(prosecutionCaseId), prosecutionCaseJsonOptional.get());
        verify(cpsRestNotificationService).sendMaterial(transformedPayload, courtDocument.getCourtDocumentId(), jsonEnvelope);
    }

    @Test
    public void shouldProcessCourtDocumentSendToCPS_WhenSendToCpsTrue() {
        final String transformedPayload = Json.createObjectBuilder().add("a", "b").build().toString();
        when(courtDocumentTransformer.transform(any(CourtDocument.class), any(Optional.class), any(JsonEnvelope.class), any(String.class))).thenReturn(of(transformedPayload));

        courtDocument = courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument()
                                .withProsecutionCaseId(fromString(prosecutionCaseId))
                                .build())
                        .build())
                .withMaterials(singletonList(material().withId(randomUUID()).build()))
                .withSendToCps(true)
                .withNotificationType("pet-form-finalised")
                .build();
        final JsonObject courtDocumentJsonObject = createObjectBuilder().build();
        when(jsonObjectConverter.convert(courtDocumentJsonObject, CourtDocument.class)).thenReturn(courtDocument);

        courtDocumentNotifiedProcessor.processCourtDocumentSendToCPS(jsonEnvelope);
        verify(courtDocumentTransformer).transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);
        verify(cpsEmailNotificationService, never()).sendEmailToCps(jsonEnvelope, courtDocument, fromString(prosecutionCaseId), prosecutionCaseJsonOptional.get());
        verify(cpsRestNotificationService).sendMaterial(transformedPayload, courtDocument.getCourtDocumentId(), jsonEnvelope);
    }

    @Test
    public void shouldProcessOPACourtDocumentSendToCPS_WhenSendToCpsTrue() {
        final String transformedPayload = Json.createObjectBuilder().add("a", "b").build().toString();
        when(courtDocumentTransformer.transform(any(CourtDocument.class), any(Optional.class), any(JsonEnvelope.class), any(String.class))).thenReturn(of(transformedPayload));

        courtDocument = courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument()
                                .withProsecutionCaseId(fromString(prosecutionCaseId))
                                .build())
                        .build())
                .withMaterials(singletonList(material().withId(randomUUID()).build()))
                .withSendToCps(true)
                .withNotificationType("opa-form-submitted")
                .build();
        final JsonObject courtDocumentJsonObject = createObjectBuilder().build();
        when(jsonObjectConverter.convert(courtDocumentJsonObject, CourtDocument.class)).thenReturn(courtDocument);

        courtDocumentNotifiedProcessor.processCourtDocumentSendToCPS(jsonEnvelope);
        verify(courtDocumentTransformer).transform(courtDocument, prosecutionCaseJsonOptional, jsonEnvelope, null);
        verify(cpsEmailNotificationService, never()).sendEmailToCps(jsonEnvelope, courtDocument, fromString(prosecutionCaseId), prosecutionCaseJsonOptional.get());
        verify(cpsRestNotificationService).sendMaterial(transformedPayload, courtDocument.getCourtDocumentId(), jsonEnvelope);
    }
}
