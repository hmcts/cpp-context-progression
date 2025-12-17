package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.progression.domain.constant.FeatureGuardNames.FEATURE_HEARINGNOWS;

import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowDocumentRequestApiTest {
    private static final String ADD_NOW_DOCUMENT_REQUEST_NAME = "progression.add-now-document-request";
    private static final String ADD_NOW_DOCUMENT_REQUEST_COMMAND_NAME = "progression.command.add-now-document-request";

    @Mock
    private Sender sender;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private NowDocumentRequestApi nowDocumentRequestApi;

    @Test
    public void shouldHandleNowDocumentRequestCommand() {
        assertThat(NowDocumentRequestApi.class, isHandlerClass(COMMAND_API)
                .with(method("addNowDocumentRequest").thatHandles(ADD_NOW_DOCUMENT_REQUEST_NAME)));
    }

    @Test
    public void shouldRecordNowDocumentRequestWhenHearingNowsFeatureIsNotEnabled() {

        final JsonEnvelope commandEnvelope = buildEnvelope();
        when(featureControlGuard.isFeatureEnabled(FEATURE_HEARINGNOWS)).thenReturn(false);

        nowDocumentRequestApi.addNowDocumentRequest(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(ADD_NOW_DOCUMENT_REQUEST_COMMAND_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldNotProcessNowDocumentRequestWhenHearingNowsFeatureIsEnabled() {

        final JsonEnvelope commandEnvelope = buildEnvelope();
        when(featureControlGuard.isFeatureEnabled(FEATURE_HEARINGNOWS)).thenReturn(true);

        nowDocumentRequestApi.addNowDocumentRequest(commandEnvelope);
        verifyNoInteractions(sender);
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("nowDocumentRequest", Json.createObjectBuilder().add("materialId", randomUUID().toString()).build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ADD_NOW_DOCUMENT_REQUEST_NAME)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}
