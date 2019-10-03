package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseApplicationEjectedEventProcessorTest {

    private static final String REMOVAL_REASON = "REASON";
    @InjectMocks
    private CaseApplicationEjectedEventProcessor caseApplicationEjectedEventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonObject prosecutionCaseJsonObject;

    @Mock
    private JsonObject applicationAtAGlance;


    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private GetCaseAtAGlance caseAtAGlance;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleCaseEjectedEventEventMessage() {
        final String prosecutionCaseId = randomUUID().toString();
        final UUID hearingId = randomUUID();
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getString("prosecutionCaseId")).thenReturn(prosecutionCaseId);
        when(payload.getString("removalReason")).thenReturn(REMOVAL_REASON);
        when(enveloper.withMetadataFrom(envelope, "public.progression.events.case-or-application-ejected")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.getProsecutionCaseDetailById(envelope,
                prosecutionCaseId)).thenReturn(of(prosecutionCaseJsonObject));
        when(prosecutionCaseJsonObject.getJsonObject("caseAtAGlance")).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, GetCaseAtAGlance.class)).thenReturn(caseAtAGlance);
        when(caseAtAGlance.getHearings()).thenReturn(singletonList(Hearings.hearings().withId(hearingId).build()));
        //When
        caseApplicationEjectedEventProcessor.processCaseEjected(envelope);

        //Then
        verify(sender).send(finalEnvelope);
        verify(progressionService).getProsecutionCaseDetailById(envelope, prosecutionCaseId);
    }

    @Test
    public void shouldHandleApplicationEjectedEventEventMessage() {
        final String applicationId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(envelope, "public.progression.events.case-or-application-ejected")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(payload.getString("applicationId")).thenReturn(applicationId);
        when(payload.getString("removalReason")).thenReturn(REMOVAL_REASON);
        when(progressionService.getCourtApplicationById(envelope,
                applicationId)).thenReturn(of(applicationAtAGlance));
        when(applicationAtAGlance.getJsonArray("hearings")).thenReturn(Json.createArrayBuilder().add(payload).build());
        when(payload.getString("id")).thenReturn(hearingId);

        //When
        caseApplicationEjectedEventProcessor.processApplicationEjected(envelope);

        //Then
        verify(sender).send(finalEnvelope);
        verify(progressionService).getCourtApplicationById(envelope, applicationId);
    }
}
