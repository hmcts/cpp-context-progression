package uk.gov.moj.cpp.progression.command;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RemoveDeletedHearingChildEntriesByBdfCommandApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private RemoveDeletedHearingChildEntriesByBdfCommandApi commandApi;

    @Test
    public void shouldRaiseCommandToRemoveDeletedHearingChildEntriesBdf() throws Exception {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("progression.command.remove-deleted-hearing-child-entries-bdf"),
                Json.createObjectBuilder()
                        .build()
        );

        commandApi.handle(jsonEnvelope);

        verify(sender).send(isA(Envelope.class));
    }
}
