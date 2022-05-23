package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.NotificationService;

import java.util.UUID;

import javax.json.Json;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NowsMaterialStatusEventProcessorTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private MaterialService materialService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private NowsMaterialStatusEventProcessor eventProcessor;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessStatusUpdatedForEmail() {
        final UUID caseId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final String status = "generated";
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.nows-material-status-updated"),
                createObjectBuilder()
                        .add("details", createObjectBuilder()
                                .add("applicationId", applicationId.toString())
                                .add("caseId", caseId.toString())
                                .add("materialId", materialId.toString())
                                .add("fileId", materialId.toString())
                                .add("hearingId", materialId.toString())
                                .add("userId", materialId.toString())
                                .add("firstClassLetter", false)
                                .add("secondClassLetter", false)
                                .add("emailNotifications", Json.createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("sendToAddress", "sendToAddress")
                                                .build())
                                        .build())
                                .build())
                        .add("status", status)
                        .add("welshTranslationRequired", false)
                        .build());

        eventProcessor.processStatusUpdated(event);

        verify(notificationService).sendEmail(Mockito.eq(event), Mockito.eq(caseId), Mockito.eq(applicationId), Mockito.eq(materialId), Mockito.any());
    }

    @Test
    public void shouldProcessStatusUpdatedForFirstClassLetter() {
        final UUID caseId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final String status = "generated";
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.nows-material-status-updated"),
                createObjectBuilder()
                        .add("details", createObjectBuilder()
                                .add("applicationId", applicationId.toString())
                                .add("caseId", caseId.toString())
                                .add("materialId", materialId.toString())
                                .add("fileId", materialId.toString())
                                .add("hearingId", materialId.toString())
                                .add("userId", materialId.toString())
                                .add("firstClassLetter", true)
                                .add("secondClassLetter", false)
                                .build())
                        .add("status", status)
                        .add("welshTranslationRequired", false)
                        .build());

        eventProcessor.processStatusUpdated(event);

        verify(notificationService).sendLetter(Mockito.eq(event), Mockito.any(UUID.class), Mockito.eq(caseId), Mockito.eq(applicationId), Mockito.eq(materialId), Mockito.eq(true));
        verify(notificationService, never()).sendEmail(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldProcessStatusUpdatedForSecondClassLetter() {
        final UUID caseId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final String status = "generated";
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.nows-material-status-updated"),
                createObjectBuilder()
                        .add("details", createObjectBuilder()
                                .add("applicationId", applicationId.toString())
                                .add("caseId", caseId.toString())
                                .add("materialId", materialId.toString())
                                .add("fileId", materialId.toString())
                                .add("hearingId", materialId.toString())
                                .add("userId", materialId.toString())
                                .add("firstClassLetter", false)
                                .add("secondClassLetter", true)
                                .build())
                            .add("status", status)
                            .add("welshTranslationRequired", false)
                        .build());

        eventProcessor.processStatusUpdated(event);

        verify(notificationService).sendLetter(Mockito.eq(event), Mockito.any(UUID.class), Mockito.eq(caseId), Mockito.eq(applicationId), Mockito.eq(materialId), Mockito.eq(false));
        verify(notificationService, never()).sendEmail(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldProcessRequestRecorded() {
        final UUID fileId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.nows-material-request-recorded"),
                createObjectBuilder()
                        .add("context", createObjectBuilder()
                                .add("fileId", fileId.toString())
                                .add("materialId", materialId.toString())
                                .build())
                        .build());

        eventProcessor.processRequestRecorded(event);

        verify(materialService).uploadMaterial(fileId, materialId, event);
    }

}