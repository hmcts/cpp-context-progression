package uk.gov.moj.cpp.progression.listener.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.progression.listener.progression.ProgressionDocumentAddedListener;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionDocumentAddedListenerTest {

    private static final String MATERIAL_COMMAND_ADD_MATERIAL = "material.add-material";


    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(JsonObject.class);

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;


    private ProgressionDocumentAddedListener progressionDocumentAddedListener;

    @Before
    public void setUp() {
        progressionDocumentAddedListener = new ProgressionDocumentAddedListener(sender, enveloper);
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


        JsonEnvelope envelope = JsonEnvelopeBuilder.envelope()
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
        progressionDocumentAddedListener.processEvent(envelope);

        // then
        verify(enveloper).withMetadataFrom(eq(envelope), eq(MATERIAL_COMMAND_ADD_MATERIAL));
        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope publicEventPayload = envelopeCaptor.getValue();
        assertThat(publicEventPayload.metadata(), withMetadataEnvelopedFrom(envelope).withName(MATERIAL_COMMAND_ADD_MATERIAL));

        // and
        final JsonObject payload = publicEventPayload.payloadAsJsonObject();
        assertThat(payload, JsonEnvelopePayloadMatcher.payloadIsJson(allOf(
                withJsonPath("$.fileName", CoreMatchers.equalTo(fileName)),
                withJsonPath("$.materialId", CoreMatchers.equalTo(fileId.toString())),
                withJsonPath("$.document.fileReference", CoreMatchers.equalTo(fileId.toString())),
                withJsonPath("$.document.mimeType", CoreMatchers.equalTo(mimeType)))));

    }

    private String randomString() {
        return RandomGenerator.string(3).next();
    }
}
