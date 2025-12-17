package uk.gov.moj.cpp.progression.processor;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.CreateNextHearing;
import uk.gov.justice.core.courts.DeletePreviousHearings;
import uk.gov.justice.core.courts.DeletePreviousHearingsAndCreateNextHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

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

@ExtendWith(MockitoExtension.class)
public class DeletePreviousHearingsAndCreateNextHearingProcessorTest {

    @InjectMocks
    private DeletePreviousHearingsAndCreateNextHearingProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Test
    public void shouldIssueDeletePreviousHearingsAndCreateNextHearingCommand() {

        final UUID hearingId = randomUUID();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.delete-previous-hearings-and-create-next-hearing"),
                objectToJsonObjectConverter.convert(DeletePreviousHearingsAndCreateNextHearing.deletePreviousHearingsAndCreateNextHearing()
                        .withCreateNextHearing(CreateNextHearing.createNextHearing()
                                .withHearing(Hearing.hearing()
                                        .withId(hearingId)
                                        .build())
                                .build())
                        .withDeletePreviousHearings(DeletePreviousHearings.deletePreviousHearings().withHearingId(hearingId).build())
                        .build()));

        this.eventProcessor.processDeletePreviousHearingsAndCreateNextHearingRequestedEvent(event);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.delete-previous-hearings-and-create-next-hearing"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), notNullValue());
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(allOf(
                withJsonPath("$.createNextHearing.hearing.id", notNullValue()),
                withJsonPath("$.createNextHearing.hearing.id", is(hearingId.toString())))));

    }
}
