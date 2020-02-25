package uk.gov.moj.cpp.progression.command;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import javax.json.Json;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;


@RunWith(MockitoJUnitRunner.class)
public class ShareCourtDocumentCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @InjectMocks
    private ShareCourtDocumentCommandApi shareCourtDocumentCommandApi;

    @Test
    public void shouldShareCourtDocument() {
        shareCourtDocumentCommandApi.handle(getMockEnvelope());

        verify(sender, times(1)).send(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is("progression.command.share-court-document"));
    }

    private JsonEnvelope getMockEnvelope() {
        return envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUIDAndName(), createObjectBuilder());
    }

}
