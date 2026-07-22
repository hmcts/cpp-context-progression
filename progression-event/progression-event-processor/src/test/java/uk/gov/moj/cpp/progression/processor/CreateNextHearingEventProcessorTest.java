package uk.gov.moj.cpp.progression.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

@ExtendWith(MockitoExtension.class)
public class CreateNextHearingEventProcessorTest {

    @InjectMocks
    private CreateNextHearingEventProcessor underTest;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;


    @Test
    public void shouldRaisePublishCourtListCommandWhenListingPublicEventIsHandled() {
        final JsonEnvelope publicEvent = prepareEnvelope();

        underTest.processCreateNextHearing(publicEvent);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().payload(), is(publicEvent.payloadAsJsonObject().getJsonObject("createNextHearing")));
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.create-next-hearing"));

    }

    private JsonEnvelope prepareEnvelope() {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("public.listing.court-list-published").build(),
                createObjectBuilder().add("createNextHearing", createObjectBuilder().build()).build());

    }
}
