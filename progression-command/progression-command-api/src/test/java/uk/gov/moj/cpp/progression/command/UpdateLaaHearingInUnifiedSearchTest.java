package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateLaaHearingInUnifiedSearchTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private UpdateLaaHearingInUnifiedSearch updateLaaHearingInUnifiedSearch;

    @Test
    public void testHandleUpdateLaaHearingDetailsUnifiedSearch() {
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final JsonObject commandPayload = JsonObjects.createObjectBuilder()
                .add("hearingIds", createArrayBuilder()
                        .add(hearingId1.toString())
                        .add(hearingId2.toString())
                        .build())
                .build();
        final Metadata commandMetadata = metadataBuilder().withName("progression.command.update-hearing-details-in-unified-search")
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID())
                .build();
        final JsonEnvelope commandEnvelope = envelopeFrom(commandMetadata, commandPayload);

        updateLaaHearingInUnifiedSearch.handleUpdateLaaHearingDetailsUnifiedSearch(commandEnvelope);

        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());

        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("progression.command.handler.update-hearing-details-in-unified-search"));
        assertThat(currentEvents.get(1).metadata().name(), Matchers.is("progression.command.handler.update-hearing-details-in-unified-search"));

        assertThat(currentEvents.get(0).payload().toString(), containsString(hearingId1.toString()));
        assertThat(currentEvents.get(1).payload().toString(), containsString(hearingId2.toString()));
    }
}