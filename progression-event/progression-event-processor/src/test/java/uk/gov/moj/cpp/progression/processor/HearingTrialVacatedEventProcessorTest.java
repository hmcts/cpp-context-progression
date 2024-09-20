package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.progression.courts.HearingTrialVacated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingTrialVacatedEventProcessorTest {


    private static final String PROGRESSION_COMMAND_FOR_TRIAL_VACATED = "progression.command.hearing-trial-vacated";
    private static final String PROGRESSION_EVENT_FOR_TRIAL_VACATED = "progression.event.hearing-trial-vacated";
    private static final String PUBLIC_PROGRESSION_FOR_TRIAL_VACATED = "public.hearing.trial-vacated";


    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private HearingTrialVacatedEventProcessor hearingTrialVacatedEventProcessor;
    @Mock
    private ProgressionService progressionService;

    @Test
    public void shouldHandleHearingTrialVacatedCommand() {
        final UUID hearingId = randomUUID();
        final UUID vacatedTrialReasonId = randomUUID();
        final JsonObject hearingTrialVacated = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("vacatedTrialReasonId", vacatedTrialReasonId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(PUBLIC_PROGRESSION_FOR_TRIAL_VACATED),
                hearingTrialVacated);

        hearingTrialVacatedEventProcessor.handleHearingTrialVacatedEvent(envelope);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is(PROGRESSION_COMMAND_FOR_TRIAL_VACATED));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.vacatedTrialReasonId", equalTo(vacatedTrialReasonId.toString())))));

    }


    @Test
    public void shouldHandleListingTrialVacatedEvent() {
        final UUID hearingId = randomUUID();
        final UUID vacatedTrialReasonId = randomUUID();
        final JsonObject hearingTrialVacated = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("vacatedTrialReasonId", vacatedTrialReasonId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.listing.vacated-trial-updated"),
                hearingTrialVacated);

        hearingTrialVacatedEventProcessor.handleListingTrialVacatedEvent(envelope);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is(PROGRESSION_COMMAND_FOR_TRIAL_VACATED));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withJsonPath("$.vacatedTrialReasonId", equalTo(vacatedTrialReasonId.toString())))));

    }

    @Test
    public void shouldHandleListingTrialVacatedEventWithOutId() {
        final UUID hearingId = randomUUID();
        final UUID vacatedTrialReasonId = randomUUID();
        final JsonObject hearingTrialVacated = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.listing.vacated-trial-updated"),
                hearingTrialVacated);

        hearingTrialVacatedEventProcessor.handleListingTrialVacatedEvent(envelope);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is(PROGRESSION_COMMAND_FOR_TRIAL_VACATED));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId.toString())),
                withoutJsonPath("$.vacatedTrialReasonId"))));

    }

    @Test
    public void shouldHandleHearingTrialVacatedEvent() {
        final UUID hearingId = randomUUID();
        final UUID vacatedTrialReasonId = randomUUID();
        final JsonObject hearingTrialVacated = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("vacatedTrialReasonId", vacatedTrialReasonId.toString())
                .build();

        HearingTrialVacated trialVacated = HearingTrialVacated.hearingTrialVacated().withVacatedTrialReasonId(vacatedTrialReasonId).withHearingId(hearingId).build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(PROGRESSION_EVENT_FOR_TRIAL_VACATED),
                hearingTrialVacated);
        when(jsonObjectConverter.convert(hearingTrialVacated, HearingTrialVacated.class)).thenReturn(trialVacated);

        hearingTrialVacatedEventProcessor.hearingTrialVacatedEvent(envelope);

        verify(progressionService).populateHearingToProbationCaseworker(eq(envelope), eq(hearingId));

    }

}
