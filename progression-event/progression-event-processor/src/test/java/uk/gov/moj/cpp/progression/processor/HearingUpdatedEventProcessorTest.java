package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;


import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingOffencesUpdated;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.cpp.progression.events.NewDefendantAddedToHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

@RunWith(MockitoJUnitRunner.class)
public class HearingUpdatedEventProcessorTest {

    @Captor
    private ArgumentCaptor<ConfirmedHearing> confirmedHearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> applicationIdsArgumentCaptor;

    @Captor
    private ArgumentCaptor<HearingListingStatus> hearingListingStatusArgumentCaptor;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private HearingUpdatedEventProcessor eventProcessor;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private Sender sender;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void initMocks() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleHearingConfirmedEventMessage() {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .build();
        final HearingUpdatedProcessed hearingUpdatedProcessed = HearingUpdatedProcessed.hearingUpdatedProcessed()
                .withConfirmedHearing(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withId(randomUUID())
                                .build()))
                        .build())
                .withHearing(hearing)
                .build();


        JsonEnvelope jsonEnvelope = getJsonEnvelopeForHearingUpdatedProcessed(hearingUpdatedProcessed);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        eventProcessor.publishHearingDetailChangedPublicEvent(jsonEnvelope);

        verify(progressionService, times(1)).updateHearingListingStatusToHearingUpdate(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture());
        assertEquals(hearing, hearingArgumentCaptor.getValue());

        verify(progressionService, times(1)).publishHearingDetailChangedPublicEvent(envelopeArgumentCaptor.capture(), confirmedHearingArgumentCaptor.capture());
        assertEquals(hearingUpdatedProcessed.getConfirmedHearing().getId(), confirmedHearingArgumentCaptor.getValue().getId());
    }

    @Test
    public void shouldHandleHearingOffenceUpdated() {
        final UUID hearingId = randomUUID();
        final HearingOffencesUpdated hearingOffencesUpdated = HearingOffencesUpdated.hearingOffencesUpdated().withHearingId(hearingId).build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.hearing-offences-updated"),
                objectToJsonObjectConverter.convert(hearingOffencesUpdated));
        eventProcessor.handleHearingOffenceUpdated(jsonEnvelope);
        ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(progressionService, times(1)).populateHearingToProbationCaseworker(envelopeArgumentCaptor.capture(), uuidArgumentCaptor.capture());
        assertThat(uuidArgumentCaptor.getValue(), is(hearingId));
    }

    @Test
    public void shouldHandleAddedNewDefendantToHearing() {
        final UUID hearingId = randomUUID();
        final NewDefendantAddedToHearing newDefendantAddedToHearing = NewDefendantAddedToHearing.newDefendantAddedToHearing().withHearingId(hearingId).build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.new-defendant-added-to-hearing"),
                objectToJsonObjectConverter.convert(newDefendantAddedToHearing));
        eventProcessor.addedNewDefendantToHearing(jsonEnvelope);
        ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(progressionService, times(1)).populateHearingToProbationCaseworker(envelopeArgumentCaptor.capture(), uuidArgumentCaptor.capture());
        assertThat(uuidArgumentCaptor.getValue(), is(hearingId));
    }


    @Test
    public void shouldProcessHearingUpdatedWhenProsecutionCaseListed() {

        final UUID hearingId = randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("updatedHearing", objectToJsonObjectConverter.convert(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId).build()))
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.listing.hearing-updated"),
                payload);
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(Collections.singletonList(ProsecutionCase.prosecutionCase()
                        .withDefendants(Collections.singletonList(Defendant.defendant()
                                .withOffences(Collections.singletonList(Offence.offence()
                                        .withJudicialResults(Collections.singletonList(JudicialResult.judicialResult().build()))
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus","SENT_FOR_LISTING")
                .build());
        when(progressionService.getHearing(event, hearingId.toString())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        this.eventProcessor.processHearingUpdated(event);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.process-hearing-updated"));
        assertThat(captorValue.payload().toString(), isJson(allOf(
                withJsonPath("$.confirmedHearing.id", equalTo(hearingId.toString())),
                withJsonPath("$.updatedHearing.id", equalTo(hearingId.toString())),
                withoutJsonPath("$.updatedHearing.prosecutionCases[0].defendants[0].offences[0].judicialResults")
        )));
    }

    @Test
    public void shouldProcessHearingUpdatedWhenProsecutionCaseInitialised() {

        final UUID hearingId = randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("updatedHearing", objectToJsonObjectConverter.convert(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId).build()))
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.listing.hearing-updated"),
                payload);
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(Collections.singletonList(ProsecutionCase.prosecutionCase()
                        .withDefendants(Collections.singletonList(Defendant.defendant()
                                .withOffences(Collections.singletonList(Offence.offence()
                                        .withJudicialResults(Collections.singletonList(JudicialResult.judicialResult().build()))
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus","HEARING_INITIALISED")
                .build());
        when(progressionService.getHearing(event, hearingId.toString())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        this.eventProcessor.processHearingUpdated(event);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.process-hearing-updated"));
        assertThat(captorValue.payload().toString(), isJson(allOf(
                withJsonPath("$.confirmedHearing.id", equalTo(hearingId.toString())),
                withJsonPath("$.updatedHearing.id", equalTo(hearingId.toString()))
        )));
    }

    @Test
    public void shouldHandleHearingConfirmedEventMessageWithCourtApplication() {
        final UUID hearingId = UUID.randomUUID();
        final HearingUpdated hearingUpdated = HearingUpdated.hearingUpdated()
                .withUpdatedHearing(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withId(UUID.randomUUID())
                                .build()))
                        .withCourtApplicationIds(Arrays.asList(UUID.randomUUID()))
                        .build())
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .build();

        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus", "SENT_FOR_LISTING")
                .build());

        JsonEnvelope jsonEnvelope = getJsonEnvelope(hearingUpdated);
        when(progressionService.getHearing(jsonEnvelope, hearingId.toString())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        eventProcessor.processHearingUpdated(jsonEnvelope);

        verify(progressionService, times(1)).updateHearingListingStatusToHearingUpdate(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture());
        assertEquals(hearing, hearingArgumentCaptor.getValue());

        verify(progressionService, times(1)).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());

        verify(progressionService, times(1)).publishHearingDetailChangedPublicEvent(envelopeArgumentCaptor.capture(), confirmedHearingArgumentCaptor.capture());
        assertEquals(hearingUpdated.getUpdatedHearing().getId(), confirmedHearingArgumentCaptor.getValue().getId());

        verify(progressionService, never()).populateHearingToProbationCaseworker(eq(jsonEnvelope), eq(hearingId));
    }

    @Test
    public void shouldHearingOffenceVerdictUpdated(){
        final JsonObject payload = createObjectBuilder().build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("public.hearing.hearing-offence-verdict-updated"),
                payload);
        eventProcessor.hearingOffenceVerdictUpdated(jsonEnvelope);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.update-hearing-offence-verdict"));

    }

    private JsonEnvelope getJsonEnvelopeForHearingUpdatedProcessed(final HearingUpdatedProcessed hearingUpdatedProcessed) {
        return JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-updated-processed"),
                objectToJsonObjectConverter.convert(hearingUpdatedProcessed));
    }

    private JsonEnvelope getJsonEnvelope(final HearingUpdated hearingUpdated) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.listing.hearing-updated"),
                objectToJsonObjectConverter.convert(hearingUpdated));
    }

}
