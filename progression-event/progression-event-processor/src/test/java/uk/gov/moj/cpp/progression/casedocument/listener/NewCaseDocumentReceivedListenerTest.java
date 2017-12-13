package uk.gov.moj.cpp.progression.casedocument.listener;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.progression.casedocument.listener.NewCaseDocumentReceivedListener.PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT;

import org.mockito.InjectMocks;
import org.mockito.Spy;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
                .with(metadataOf(randomId, PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT)
                        .withUserId(userId.toString())
                        .withSessionId(sessionId.toString())
                        .withClientCorrelationId(clientCorrelationId.toString()))
                .withPayloadOf(cppCaseId, "cppCaseId")
                .withPayloadOf(fileId, "fileId")
                .withPayloadOf(mimeType, "fileMimeType")
                .withPayloadOf(fileName, "fileName")
                .build();

        // when
        when(enveloper.withMetadataFrom(inputEnvelope,PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT)).thenReturn(objectJsonEnvelopeFunction);
        when(objectJsonEnvelopeFunction.apply(inputEnvelope.payload())).thenReturn(envelopeBeingPosted);
        newCaseDocumentReceivedListener.processEvent(inputEnvelope);

        // then

        verify(sender, times(1)).send(envelopeCaptor.capture());
        List<JsonEnvelope> envelopes = envelopeCaptor.getAllValues();



        JsonEnvelope envelope2 = envelopes.get(0);
        Assert.assertThat(envelope2.metadata().name(), equalTo(PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT));
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
