package uk.gov.moj.cpp.progression.listener.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.progression.listener.casedocument.NewCaseDocumentReceivedListener;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@ExtendWith(MockitoExtension.class)
public class NewCaseDocumentReceivedListenerTest {

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Mock
    private  Function<Object, JsonEnvelope> objectJsonEnvelopeFunction;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @InjectMocks
    private NewCaseDocumentReceivedListener newCaseDocumentReceivedListener;


    @Test
    public void shouldProcessEvent() {
        // given
        final UUID randomId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final UUID clientCorrelationId = UUID.randomUUID();
        final UUID cppCaseId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final String mimeType = randomString();
        final String metaField = randomString();
        final String fileName = randomString();

        final JsonEnvelope inputEnvelope = JsonEnvelopeBuilder.envelope()
                .with(metadataOf(randomId, metaField)
                        .withUserId(userId.toString())
                        .withSessionId(sessionId.toString())
                        .withClientCorrelationId(clientCorrelationId.toString()))
                .withPayloadOf(cppCaseId, "cppCaseId")
                .withPayloadOf(fileId, "fileId")
                .withPayloadOf(mimeType, "fileMimeType")
                .withPayloadOf(fileName, "fileName")
                .build();

        final JsonEnvelope envelopeBeingPosted = JsonEnvelopeBuilder.envelope()
                .with(metadataOf(randomId, "public.progression.case-document-added")
                        .withUserId(userId.toString())
                        .withSessionId(sessionId.toString())
                        .withClientCorrelationId(clientCorrelationId.toString()))
                .withPayloadOf(cppCaseId, "cppCaseId")
                .withPayloadOf(fileId, "fileId")
                .withPayloadOf(mimeType, "fileMimeType")
                .withPayloadOf(fileName, "fileName")
                .build();

        // when
        when(enveloper.withMetadataFrom(inputEnvelope,"public.progression.case-document-added")).thenReturn(objectJsonEnvelopeFunction);
        when(objectJsonEnvelopeFunction.apply(inputEnvelope.payload())).thenReturn(envelopeBeingPosted);
        newCaseDocumentReceivedListener.processEvent(inputEnvelope);

        // then

        verify(sender, times(1)).send(envelopeCaptor.capture());
        final List<JsonEnvelope> envelopes = envelopeCaptor.getAllValues();



        final JsonEnvelope envelope2 = envelopes.get(0);
        Assert.assertThat(envelope2.metadata().name(), equalTo("public.progression.case-document-added"));
        final JsonObject payload2 = envelope2.payloadAsJsonObject();
        assertThat(payload2, JsonEnvelopePayloadMatcher.payloadIsJson(allOf(
                withJsonPath("$.fileName", equalTo(fileName)),
                withJsonPath("$.fileId", equalTo(fileId.toString())),
                withJsonPath("$.cppCaseId", equalTo(cppCaseId.toString())),
                withJsonPath("$.fileMimeType", equalTo(mimeType)))));

    }

    private String randomString() {
        return RandomGenerator.string(3).next();
    }
}
