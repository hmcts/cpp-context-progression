package uk.gov.moj.cpp.progression.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingUnallocatedCourtroomRemoved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class HearingUnallocatedCourtroomRemovedEventProcessorTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID PROSECUTION_CASE_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID OFFENCE_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Logger LOGGER;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonEnvelope finalEnvelope;

    @InjectMocks
    private HearingUnallocatedCourtroomRemovedEventProcessor processor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @BeforeEach
    public void setUp() {
        // Setup default behavior for logger
    }

    @Test
    public void shouldProcessPublicHearingUnallocatedCourtroomRemovedEvent() {
        // Given
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.listing.hearing-unallocated-courtroom-removed"),
                createJsonObjectWithHearingId());

        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), any(String.class)))
                .thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        // When
        processor.processHearingUnallocatedCourtRoomRemoved(jsonEnvelope);

        // Then
        verify(enveloper).withMetadataFrom(jsonEnvelope, "progression.command.unallocate-hearing-remove-courtroom");
        verify(enveloperFunction).apply(any(JsonObject.class));
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void shouldHandleHearingUnallocatedCourtroomRemovedEventWithOffences() {
        // Given
        final Hearing hearing = createHearingWithOffences();
        final HearingEntity hearingEntity = createHearingEntity(hearing);
        final HearingUnallocatedCourtroomRemoved event = HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                .withHearingId(HEARING_ID)
                .withEstimatedMinutes(30)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-unallocated-courtroom-removed"),
                objectToJsonObjectConverter.convert(event));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        // When
        processor.handleHearingUnallocatedCourtroomRemoved(jsonEnvelope);

        // Then
        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope sentEnvelope = envelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata().name(), is("progression.command.decrease-listing-number-for-prosecution-case"));
        assertThat(sentEnvelope.payloadAsJsonObject().getString("prosecutionCaseId"), is(PROSECUTION_CASE_ID.toString()));
        assertThat(sentEnvelope.payloadAsJsonObject().getJsonArray("offenceIds").size(), is(2));
        assertThat(sentEnvelope.payloadAsJsonObject().getJsonArray("offenceIds").getString(0), is(OFFENCE_ID.toString()));
        assertThat(sentEnvelope.payloadAsJsonObject().getJsonArray("offenceIds").getString(1), is(OFFENCE_ID_2.toString()));
    }

    @Test
    public void shouldNotProcessWhenHearingIdIsNull() {
        // Given
        final HearingUnallocatedCourtroomRemoved event = HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                .withHearingId(null)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-unallocated-courtroom-removed"),
                objectToJsonObjectConverter.convert(event));

        // When
        processor.handleHearingUnallocatedCourtroomRemoved(jsonEnvelope);

        // Then
        verify(hearingRepository, never()).findBy(any());
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldNotProcessWhenHearingEntityIsNull() {
        // Given
        final HearingUnallocatedCourtroomRemoved event = HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                .withHearingId(HEARING_ID)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-unallocated-courtroom-removed"),
                objectToJsonObjectConverter.convert(event));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(null);

        // When
        processor.handleHearingUnallocatedCourtroomRemoved(jsonEnvelope);

        // Then
        verify(hearingRepository).findBy(HEARING_ID);
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldNotProcessWhenHearingEntityPayloadIsNull() {
        // Given
        final HearingUnallocatedCourtroomRemoved event = HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                .withHearingId(HEARING_ID)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-unallocated-courtroom-removed"),
                objectToJsonObjectConverter.convert(event));

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(null);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        // When
        processor.handleHearingUnallocatedCourtroomRemoved(jsonEnvelope);

        // Then
        verify(hearingRepository).findBy(HEARING_ID);
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldNotProcessWhenOriginalHearingIsNull() {
        // Given
        final HearingUnallocatedCourtroomRemoved event = HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                .withHearingId(HEARING_ID)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-unallocated-courtroom-removed"),
                objectToJsonObjectConverter.convert(event));

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload("{}");

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        // When
        processor.handleHearingUnallocatedCourtroomRemoved(jsonEnvelope);

        // Then
        verify(hearingRepository).findBy(HEARING_ID);
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldNotSendCommandWhenNoOffences() {
        // Given
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(PROSECUTION_CASE_ID)
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(DEFENDANT_ID)
                                .withOffences(singletonList(Offence.offence()
                                        .withId(null)
                                        .build()))
                                .build()))
                        .build()))
                .build();

        final HearingEntity hearingEntity = createHearingEntity(hearing);
        final HearingUnallocatedCourtroomRemoved event = HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                .withHearingId(HEARING_ID)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-unallocated-courtroom-removed"),
                objectToJsonObjectConverter.convert(event));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        // When
        processor.handleHearingUnallocatedCourtroomRemoved(jsonEnvelope);

        // Then
        verify(hearingRepository).findBy(HEARING_ID);
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldHandleMultipleProsecutionCasesWithOffences() {
        // Given
        final UUID prosecutionCaseId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(asList(
                        ProsecutionCase.prosecutionCase()
                                .withId(PROSECUTION_CASE_ID)
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(DEFENDANT_ID)
                                        .withOffences(asList(
                                                Offence.offence().withId(OFFENCE_ID).build(),
                                                Offence.offence().withId(OFFENCE_ID_2).build()))
                                        .build()))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(prosecutionCaseId2)
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(asList(
                                                Offence.offence().withId(offenceId3).build(),
                                                Offence.offence().withId(offenceId4).build()))
                                        .build()))
                                .build()))
                .build();

        final HearingEntity hearingEntity = createHearingEntity(hearing);
        final HearingUnallocatedCourtroomRemoved event = HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                .withHearingId(HEARING_ID)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-unallocated-courtroom-removed"),
                objectToJsonObjectConverter.convert(event));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        // When
        processor.handleHearingUnallocatedCourtroomRemoved(jsonEnvelope);

        // Then
        verify(sender, times(2)).send(envelopeCaptor.capture());
        final java.util.List<JsonEnvelope> sentEnvelopes = envelopeCaptor.getAllValues();
        
        assertThat(sentEnvelopes.size(), is(2));
        assertThat(sentEnvelopes.get(0).metadata().name(), is("progression.command.decrease-listing-number-for-prosecution-case"));
        assertThat(sentEnvelopes.get(1).metadata().name(), is("progression.command.decrease-listing-number-for-prosecution-case"));
    }

    @Test
    public void shouldHandleHearingWithNoProsecutionCases() {
        // Given
        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .build();

        final HearingEntity hearingEntity = createHearingEntity(hearing);
        final HearingUnallocatedCourtroomRemoved event = HearingUnallocatedCourtroomRemoved.hearingUnallocatedCourtroomRemoved()
                .withHearingId(HEARING_ID)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-unallocated-courtroom-removed"),
                objectToJsonObjectConverter.convert(event));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        // When
        processor.handleHearingUnallocatedCourtroomRemoved(jsonEnvelope);

        // Then
        verify(hearingRepository).findBy(HEARING_ID);
        verify(sender, never()).send(any());
    }

    private Hearing createHearingWithOffences() {
        return Hearing.hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(PROSECUTION_CASE_ID)
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(DEFENDANT_ID)
                                .withOffences(asList(
                                        Offence.offence().withId(OFFENCE_ID).build(),
                                        Offence.offence().withId(OFFENCE_ID_2).build()))
                                .build()))
                        .build()))
                .build();
    }

    private HearingEntity createHearingEntity(final Hearing hearing) {
        try {
            final String payload = objectMapper.writeValueAsString(hearing);
            final HearingEntity hearingEntity = new HearingEntity();
            hearingEntity.setHearingId(HEARING_ID);
            hearingEntity.setPayload(payload);
            return hearingEntity;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject createJsonObjectWithHearingId() {
        return javax.json.Json.createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .build();
    }
}

