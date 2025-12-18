package uk.gov.moj.cpp.progression.command;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;

import uk.gov.justice.services.messaging.JsonObjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RemoveDeletedHearingChildEntriesByBdfCommandApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private RemoveDeletedHearingChildEntriesByBdfCommandApi commandApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldRaiseCommandToRemoveDeletedHearingChildEntriesBdf() throws Exception {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("progression.command.remove-deleted-hearing-child-entries-bdf"),
                JsonObjects.createObjectBuilder()
                        .build()
        );

        commandApi.handle(jsonEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
    }
}
