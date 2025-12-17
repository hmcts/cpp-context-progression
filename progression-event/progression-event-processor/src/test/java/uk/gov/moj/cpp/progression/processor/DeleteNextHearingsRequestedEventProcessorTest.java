package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.progression.courts.DeleteNextHearingsRequested;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)

public class DeleteNextHearingsRequestedEventProcessorTest {

    @InjectMocks
    private DeleteNextHearingsRequestedEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Test
    public void shouldIssueDeleteNextHearingsCommand() {

        final UUID hearingId = randomUUID();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.delete-next-hearings-requested"),
                objectToJsonObjectConverter.convert(DeleteNextHearingsRequested.deleteNextHearingsRequested()
                        .withHearingId(hearingId)
                        .build()));

        this.eventProcessor.processDeleteNextHearingsRequestedEvent(event);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.delete-next-hearings"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), notNullValue());
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", notNullValue()),
                withJsonPath("$.hearingId", is(hearingId.toString())))));

    }

}