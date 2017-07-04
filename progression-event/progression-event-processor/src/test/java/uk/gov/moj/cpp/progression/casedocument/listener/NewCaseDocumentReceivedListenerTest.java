package uk.gov.moj.cpp.progression.casedocument.listener;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.progression.casedocument.listener.NewCaseDocumentReceivedListener.PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT;
import static uk.gov.moj.cpp.progression.casedocument.listener.NewCaseDocumentReceivedListener.STRUCTURE_COMMAND_ADD_DOCUMENT;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NewCaseDocumentReceivedListenerTest {

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(AssociateNewCaseDocumentCommand.class);

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    private NewCaseDocumentReceivedListener newCaseDocumentReceivedListener;

    @Before
    public void setUp() {
        newCaseDocumentReceivedListener = new NewCaseDocumentReceivedListener(sender, enveloper);
    }


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

        // when
        newCaseDocumentReceivedListener.processEvent(inputEnvelope);

        // then
        verify(enveloper).withMetadataFrom(eq(inputEnvelope), eq(STRUCTURE_COMMAND_ADD_DOCUMENT));

        verify(sender, times(2)).send(envelopeCaptor.capture());
        List<JsonEnvelope> envelopes = envelopeCaptor.getAllValues();

        JsonEnvelope envelope1 = envelopes.get(0);
        Assert.assertThat(envelope1.metadata(), withMetadataEnvelopedFrom(inputEnvelope).withName(STRUCTURE_COMMAND_ADD_DOCUMENT));
        final JsonObject payload = envelope1.payloadAsJsonObject();
        Assert.assertThat(payload, JsonEnvelopePayloadMatcher.payloadIsJson(allOf(
                withJsonPath("$.caseId", CoreMatchers.equalTo(cppCaseId.toString())),
                withJsonPath("$.materialId", CoreMatchers.equalTo(fileId.toString())),
                withJsonPath("$.documentType", CoreMatchers.equalTo("PLEA")))));

        JsonEnvelope envelope2 = envelopes.get(1);
        Assert.assertThat(envelope2.metadata(), withMetadataEnvelopedFrom(inputEnvelope).withName(PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT));
        final JsonObject payload2 = envelope2.payloadAsJsonObject();
        assertThat(payload2, JsonEnvelopePayloadMatcher.payloadIsJson(allOf(
                withJsonPath("$.fileName", CoreMatchers.equalTo(fileName)),
                withJsonPath("$.fileId", CoreMatchers.equalTo(fileId.toString())),
                withJsonPath("$.cppCaseId", CoreMatchers.equalTo(cppCaseId.toString())),
                withJsonPath("$.fileMimeType", CoreMatchers.equalTo(mimeType)))));

    }

    private String randomString() {
        return RandomGenerator.string(3).next();
    }
}
