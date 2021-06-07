package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;

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
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
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
    public void shouldProcessHearingUpdated() {

        final UUID hearingId = randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("updatedHearing", objectToJsonObjectConverter.convert(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId).build()))
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.listing.hearing-updated"),
                payload);
        this.eventProcessor.processHearingUpdated(event);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.process-hearing-updated"));
        assertThat(captorValue.payload().toString(), isJson(allOf(
                withJsonPath("$.confirmedHearing.id", equalTo(hearingId.toString()))
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