package uk.gov.moj.cpp.progression.command;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class AddCourtDocumentApiTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private AddCourtDocumentApi addCourtDocumentApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    private static final String ADD_COURT_DOCUMENT_NAME = "progression.add-court-document";
    private static final String ADD_COURT_DOCUMENT_COMMAND_NAME = "progression.command.add-court-document";

    @Test
    public void shouldHandleAddDocumentCommand() {
        assertThat(AddCourtDocumentApi.class, isHandlerClass(COMMAND_API)
                .with(method("handle").thatHandles(ADD_COURT_DOCUMENT_NAME)));
    }

    @Test
    public void shouldAddDocument() {

        final JsonEnvelope commandEnvelope = envelope().with(metadataWithRandomUUID(ADD_COURT_DOCUMENT_NAME)).build();

        addCourtDocumentApi.handle(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(commandEnvelope).withName(ADD_COURT_DOCUMENT_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

}
