package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingMovedToUnallocated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HmiEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private HmiEventProcessor hmiEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;


    @Test
    public void shouldCallCommandWhenPublicEventReceived() {
        final UUID hearingId = UUID.randomUUID();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.staginghmi.hearing-updated-from-hmi"),
                createObjectBuilder().add("hearingId", hearingId.toString()).build());

        hmiEventProcessor.handleHearingUpdatedFromHmi(event);

        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.update-hearing-from-hmi"));
        assertThat(envelopeArgumentCaptor.getValue().payload().getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldRaiseListingNumberEventWhenHaringMovedToUnAllocated() {
        final UUID hearingId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();

        final HearingMovedToUnallocated hearingMovedToUnallocated = HearingMovedToUnallocated.hearingMovedToUnallocated()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(prosecutionCaseId)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(offenceId)
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-moved-to-unallocated"),
                objectToJsonObjectConverter.convert(hearingMovedToUnallocated));

        hmiEventProcessor.handleHearingMovedToUnallocated(event);

        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.decrease-listing-number-for-prosecution-case"));
        assertThat(envelopeArgumentCaptor.getValue().payload().getString("prosecutionCaseId"), is(prosecutionCaseId.toString()));
        assertThat(envelopeArgumentCaptor.getValue().payload().getJsonArray("offenceIds").getString(0), is(offenceId.toString()));
    }
}
