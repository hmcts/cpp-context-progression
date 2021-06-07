package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingExtendedProcessed;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1607"})
public class ExtendedHearingProcessorTest {

    @InjectMocks
    private ExtendedHearingProcessor eventProcessor;

    @Mock
    private HearingExtendedProcessed hearingExtendedProcessed;

    @Mock
    private HearingExtended hearingExtended;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Sender sender;
    @Mock
    private ProgressionService progressionService;
    @Mock
    private CourtApplication courtApplication;
    @Mock
    private ProsecutionCase prosecutionCase;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Captor
    private ArgumentCaptor<DefaultEnvelope> senderJsonEnvelopeCaptor;

    @Test
    public void shouldHandleHearingExtendedEventMessageForExistingHearingForApplication() {
        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withCourtApplications(Arrays.asList(courtApplication)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingExtendedProcessed.class)).thenReturn(hearingExtendedProcessed);
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtApplication.class))).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(Hearing.class))).thenReturn(Hearing.hearing().build());
        when(hearingExtendedProcessed.getHearingRequest()).thenReturn(hearingListingNeeds);
        when(hearingExtendedProcessed.getHearing()).thenReturn(Hearing.hearing().build());

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.hearing-extended-processed"),
                jsonEnvelope.payloadAsJsonObject());
        this.eventProcessor.processed(event);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        verify(progressionService).updateDefendantYouthForProsecutionCase(any(), anyList());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("public.progression.events.hearing-extended"));
    }

    @Test
    public void shouldHandleHearingExtendedEventMessageForExistingHearingForCase() {


        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withProsecutionCases(Arrays.asList(prosecutionCase)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingExtendedProcessed.class)).thenReturn(hearingExtendedProcessed);
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtApplication.class))).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(Hearing.class))).thenReturn(Hearing.hearing().build());
        when(hearingExtendedProcessed.getHearingRequest()).thenReturn(hearingListingNeeds);
        when(hearingExtendedProcessed.getHearing()).thenReturn(Hearing.hearing().build());
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.hearing-extended-processed"),
                jsonEnvelope.payloadAsJsonObject());
        this.eventProcessor.processed(event);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        verify(progressionService, times(1)).linkProsecutionCasesToHearing(any(JsonEnvelope.class),any(UUID.class),any(List.class));
        verify(progressionService).updateDefendantYouthForProsecutionCase(any(), anyList());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("public.progression.events.hearing-extended"));
    }

    @Test
    public void shouldProcessHearingExtended() {
        final UUID hearingId = UUID.randomUUID();
        final UUID shadowOffence = UUID.randomUUID();
        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(Arrays.asList(prosecutionCase)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingExtended.class)).thenReturn(hearingExtended);
        when(hearingExtended.getHearingRequest()).thenReturn(hearingListingNeeds);

        when(hearingExtended.getShadowListedOffences()).thenReturn(Arrays.asList(shadowOffence));
        when(objectToJsonObjectConverter.convert(Mockito.any(HearingListingNeeds.class))).thenReturn(payload);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.hearing-extended"),
                jsonEnvelope.payloadAsJsonObject());
        this.eventProcessor.process(event);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("progression.command.process-hearing-extended"));
        final JsonObject commandPayload = (JsonObject) senderJsonEnvelopeCaptor.getValue().payload();
        assertThat(commandPayload.getJsonArray("shadowListedOffences").getString(0), is(shadowOffence.toString()));

    }

}

