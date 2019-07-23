package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationRejected;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

@RunWith(MockitoJUnitRunner.class)
public class CourtApplicationProcessorTest {

    @InjectMocks
    private CourtApplicationProcessor courtApplicationProcessor;

    @Mock
    private Sender sender;

    @Mock
    private CourtApplicationCreated courtApplicationCreated;

    @Mock
    private CourtApplicationRejected courtApplicationRejected;

    @Mock
    private CourtApplication courtApplication;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void processCourtApplicationCreated() {
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), CourtApplicationCreated.class))
                .thenReturn(courtApplicationCreated);
        when(courtApplicationCreated.getCourtApplication()).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(courtApplication)).thenReturn(payload);
        when(courtApplicationCreated.getArn()).thenReturn("arn");
        when(courtApplication.getLinkedCaseId()).thenReturn(UUID.randomUUID());
        when(courtApplication.getApplicationReference()).thenReturn(STRING.next());
        when(courtApplication.getId()).thenReturn(UUID.randomUUID());
        when(enveloper.withMetadataFrom(envelope, "public.progression.court-application-created")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.add-court-application-to-case")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        //When
        courtApplicationProcessor.processCourtApplicationCreated(envelope);

        //Then
        verify(sender,times(2)).send(finalEnvelope);
    }

    @Test
    public void processCourtApplicationRejected() {
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), CourtApplicationRejected.class))
                .thenReturn(courtApplicationRejected);
        when(courtApplicationRejected.getApplicationId()).thenReturn("id");
        when(enveloper.withMetadataFrom(envelope, "public.progression.court-application-rejected")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        //When
        courtApplicationProcessor.processCourtApplicationRejected(envelope);

        //Then
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void shouldHandleProcessCourtApplicationChangedEventMessage() {
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(envelope, "public.progression.court-application-changed")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        //When
        courtApplicationProcessor.processCourtApplicationChanged(envelope);

        //Then
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void shouldHandleProcessCourtApplicationUpdatedEventMessage() {
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(envelope, "public.progression.court-application-updated")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        //When
        courtApplicationProcessor.processCourtApplicationUpdated(envelope);

        //Then
        verify(sender).send(finalEnvelope);
    }
}