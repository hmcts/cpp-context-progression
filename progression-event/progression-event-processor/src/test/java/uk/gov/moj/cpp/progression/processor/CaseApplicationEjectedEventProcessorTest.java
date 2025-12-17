package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.AzureFunctionService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseApplicationEjectedEventProcessorTest {

    private static final String REMOVAL_REASON = "REASON";
    private static final String SURREY_POLICE_ORIG_ORGANISATION = "045AA00";
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
    private GetHearingsAtAGlance hearingsAtAGlance;

    @Mock
    private ProsecutionCase prosecutionCase;

    @Mock
    private ProsecutionCaseIdentifier prosecutionCaseIdentifier;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private AzureFunctionService azureFunctionService;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleCaseEjectedEventEventMessage() throws IOException {
        final String prosecutionCaseId = randomUUID().toString();
        final UUID hearingId = randomUUID();
        final String prosecutionCaseURN = randomUUID().toString();
        final String prosecutionCaseAuthorityCode = randomUUID().toString();
        final InitiationCode initiationCode = InitiationCode.Q;

        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getString("prosecutionCaseId")).thenReturn(prosecutionCaseId);
        when(payload.getString("removalReason")).thenReturn(REMOVAL_REASON);
        when(enveloper.withMetadataFrom(envelope, "public.progression.events.case-or-application-ejected")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.getProsecutionCaseDetailById(envelope,
                prosecutionCaseId)).thenReturn(of(prosecutionCaseJsonObject));
        when(prosecutionCaseJsonObject.getJsonObject("hearingsAtAGlance")).thenReturn(payload);
        when(prosecutionCaseJsonObject.getJsonObject("prosecutionCase")).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, GetHearingsAtAGlance.class)).thenReturn(hearingsAtAGlance);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCase.class)).thenReturn(prosecutionCase);
        when(hearingsAtAGlance.getHearings()).thenReturn(singletonList(Hearings.hearings().withId(hearingId).build()));
        when(prosecutionCase.getProsecutionCaseIdentifier()).thenReturn(prosecutionCaseIdentifier);
        when(prosecutionCase.getOriginatingOrganisation()).thenReturn(SURREY_POLICE_ORIG_ORGANISATION);
        when(prosecutionCaseIdentifier.getProsecutionAuthorityReference()).thenReturn(prosecutionCaseURN);
        when(prosecutionCase.getInitiationCode()).thenReturn(initiationCode);

        //When
        caseApplicationEjectedEventProcessor.processCaseEjected(envelope);


        //Then
        verify(sender).send(finalEnvelope);
        verify(progressionService, times(2)).getProsecutionCaseDetailById(envelope, prosecutionCaseId);
        verify(azureFunctionService).makeFunctionCall(anyString());

    }

    @Test
    public void shouldHandleCaseEjectedViaBdfEventEventMessage() throws IOException {
        final String prosecutionCaseId = randomUUID().toString();
        final UUID hearingId = randomUUID();
        final String prosecutionCaseURN = randomUUID().toString();
        final String prosecutionCaseAuthorityCode = randomUUID().toString();
        final InitiationCode initiationCode = InitiationCode.Q;

        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getString("prosecutionCaseId")).thenReturn(prosecutionCaseId);
        when(payload.getString("removalReason")).thenReturn(REMOVAL_REASON);
        when(enveloper.withMetadataFrom(envelope, "public.progression.events.case-or-application-ejected")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.getProsecutionCaseDetailById(envelope,
                prosecutionCaseId)).thenReturn(of(prosecutionCaseJsonObject));
        when(prosecutionCaseJsonObject.getJsonObject("hearingsAtAGlance")).thenReturn(payload);
        when(prosecutionCaseJsonObject.getJsonObject("prosecutionCase")).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, GetHearingsAtAGlance.class)).thenReturn(hearingsAtAGlance);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCase.class)).thenReturn(prosecutionCase);
        when(hearingsAtAGlance.getHearings()).thenReturn(singletonList(Hearings.hearings().withId(hearingId).build()));
        when(prosecutionCase.getProsecutionCaseIdentifier()).thenReturn(prosecutionCaseIdentifier);
        when(prosecutionCase.getOriginatingOrganisation()).thenReturn(SURREY_POLICE_ORIG_ORGANISATION);
        when(prosecutionCaseIdentifier.getProsecutionAuthorityReference()).thenReturn(prosecutionCaseURN);
        when(prosecutionCase.getInitiationCode()).thenReturn(initiationCode);

        //When
        caseApplicationEjectedEventProcessor.processCaseEjectedViaBdf(envelope);


        //Then
        verify(sender).send(finalEnvelope);
        verify(progressionService, times(2)).getProsecutionCaseDetailById(envelope, prosecutionCaseId);
        verify(azureFunctionService).makeFunctionCall(anyString());

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
