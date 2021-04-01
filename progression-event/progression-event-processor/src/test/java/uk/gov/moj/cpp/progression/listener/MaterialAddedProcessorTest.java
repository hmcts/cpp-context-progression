package uk.gov.moj.cpp.progression.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.listener.material.MaterialAddedProcessor;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MaterialAddedProcessorTest {

    @Mock
    private Enveloper enveloper;

    @Mock
    private Sender sender;

    @InjectMocks
    private MaterialAddedProcessor target;

    @Mock
    private JsonEnvelope event;

    @Mock
    private Metadata metadata;

    @Mock
    private Requester requester;

    @Mock
    private JsonObject metaDataJson;

    @Mock
    private JsonObject payloadJson;

    @Mock
    private JsonEnvelope outEnvelope;

    @Captor
    private ArgumentCaptor<JsonEnvelope> sentEnvelopes;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldForwardCourtOriginUpdate() {
        final JsonObject metaDataJson = Json.createObjectBuilder()
                .add(MaterialAddedProcessor.ORIGINATOR, MaterialAddedProcessor.ORIGINATOR_VALUE)
                .add("id", UUID.randomUUID().toString()).build();
        when(metadata.asJsonObject()).thenReturn(metaDataJson);
        when(event.metadata()).thenReturn(metadata);
        when(event.payloadAsJsonObject()).thenReturn(payloadJson);
        final UUID materialId = UUID.randomUUID();
        when(payloadJson.getString(MaterialAddedProcessor.MATERIAL_ID)).thenReturn(materialId.toString());
        when(materialUrlGenerator.pdfFileStreamUrlFor(materialId)).thenReturn(RandomGenerator.STRING.next());
        Function<Object, JsonEnvelope> factory = (o) -> outEnvelope;
        when(enveloper.withMetadataFrom(event, MaterialAddedProcessor.PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS))
                .thenReturn(factory);
        final Envelope<JsonObject> envelope = mock(Envelope.class);
        when(requester.requestAsAdmin(any(Envelope.class), eq(JsonObject.class))).thenReturn(envelope);
        when(envelope.payload()).thenReturn(Json.createObjectBuilder().build());

        target.processEvent(event);

        verify(sender, times(1)).send(sentEnvelopes.capture());
        assertThat(sentEnvelopes.getValue(), is(outEnvelope));
    }

    @Test
    public void shouldNotForwardNonCourtOriginUpdate() {
        when(event.metadata()).thenReturn(metadata);
        when(metadata.asJsonObject()).thenReturn(metaDataJson);

        when(metaDataJson.containsKey(MaterialAddedProcessor.ORIGINATOR)).thenReturn(true);
        when(metaDataJson.getString(MaterialAddedProcessor.ORIGINATOR)).thenReturn("xxx" + MaterialAddedProcessor.ORIGINATOR_VALUE);
        when(event.payloadAsJsonObject()).thenReturn(payloadJson);
        target.processEvent(event);

        verify(sender, times(0)).send(sentEnvelopes.capture());
    }

    private JsonObject getAggregateCourtRegisterDocumentRequestPayload() {
        return getPayload("progression.add-court-register-document.json");
    }

    public static JsonObject getPayload(final String path) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return new StringToJsonObjectConverter().convert(response);
    }

}
