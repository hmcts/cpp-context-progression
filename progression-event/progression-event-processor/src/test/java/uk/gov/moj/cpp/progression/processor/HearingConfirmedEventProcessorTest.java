package uk.gov.moj.cpp.progression.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.Initiate;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingConfirmedEventProcessorTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
    @InjectMocks
    private HearingConfirmedEventProcessor eventProcessor;
    @Mock
    private Sender sender;
    @Mock
    private HearingConfirmed hearingConfirmed;
    @Mock
    private JsonEnvelope envelope;
    @Mock
    private JsonObject payload;
    @Mock
    private JsonEnvelope finalEnvelope;
    @Mock
    private ProgressionService progressionService;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleHearingConfiremdEventMessage() throws Exception {
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class))
                .thenReturn(hearingConfirmed);

        UUID offenceId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        ConfirmedOffence confirmedOffence = ConfirmedOffence.confirmedOffence()
                .withId(offenceId).build();
        List<ConfirmedOffence> confirmedOffences = new ArrayList<>();
        confirmedOffences.add(confirmedOffence);
        ConfirmedDefendant confirmedDefendant = ConfirmedDefendant.confirmedDefendant().withId(defendantId)
                .withOffences(confirmedOffences).build();

        List<ConfirmedDefendant> confirmedDefendants = new ArrayList<>();
        confirmedDefendants.add(confirmedDefendant);
        ConfirmedProsecutionCase listingPC = ConfirmedProsecutionCase.confirmedProsecutionCase().withDefendants(confirmedDefendants)
                .withId(caseId)
                .build();
        List<ConfirmedProsecutionCase> confirmedProsecutionCases = new ArrayList<>();
        confirmedProsecutionCases.add(listingPC);

        ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing().withProsecutionCases(confirmedProsecutionCases).build();
        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("id", defendantId.toString())
                        .add("offences", Json.createArrayBuilder().add(Json
                                .createObjectBuilder()
                                .add("id", offenceId.toString())
                                .build()).build()).build()).build()).build();

        Offence offenceOne = Offence.offence().withOffenceCode("one")
                .withId(offenceId).build();
        List<Offence> offences = new ArrayList<>();
        offences.add(offenceOne);
        Defendant defendantOne = Defendant.defendant().withId(defendantId)
                .withOffences(offences).build();
        List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendantOne);
        ProsecutionCase pc = ProsecutionCase.prosecutionCase().withId(caseId)
                .withDefendants(defendants)
                .build();
        when(progressionService.getProsecutionCaseDetailById(envelope, caseId.toString())).thenReturn(Optional.of(jsonObject));
        doNothing().when(progressionService).prepareSummonsData(anyObject(), anyObject());

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class))
                .thenReturn(pc);

        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(UUID.randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(UUID.randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());

        when(enveloper.withMetadataFrom(envelope, "hearing.initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.update-defendant-listing-status")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, HearingConfirmedEventProcessor.PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT))
                .thenReturn(enveloperFunction);

        //When
        eventProcessor.processHearingConfirmed(envelope);

        //Then
        verify(sender, times(1)).send(finalEnvelope);

    }

    @Test
    public void shouldCallInitiateHearing() throws Exception {
        final Initiate arbitraryInitiateObj = Initiate.initiate().withHearing(
                Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(UUID.randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(UUID.randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build()
        ).build();
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        //When
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), Initiate.class))
                .thenReturn(arbitraryInitiateObj);


        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(UUID.randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(UUID.randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());

        when(enveloper.withMetadataFrom(envelope, "hearing.initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.update-defendant-listing-status")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, HearingConfirmedEventProcessor.PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT))
                .thenReturn(enveloperFunction);


        eventProcessor.processHearingInitiatedEnrichedEvent(envelope);

        //Then
        verify(sender, times(2)).send(finalEnvelope);
    }
}