package uk.gov.moj.cpp.progression.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.listener.material.MaterialAddedProcessor;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.function.Function;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    public void shouldForwardCourtOriginUpdateWhenOriginatorIsCourt() {
        final JsonObject metaDataJson = JsonObjects.createObjectBuilder()
                .add(MaterialAddedProcessor.ORIGINATOR, MaterialAddedProcessor.ORIGINATOR_VALUE)
                .add("id", UUID.randomUUID().toString()).build();
        when(metadata.asJsonObject()).thenReturn(metaDataJson);
        when(event.metadata()).thenReturn(metadata);
        when(event.payloadAsJsonObject()).thenReturn(payloadJson);
        final UUID materialId = UUID.randomUUID();
        when(payloadJson.getString(MaterialAddedProcessor.MATERIAL_ID)).thenReturn(materialId.toString());
        Function<Object, JsonEnvelope> factory = (o) -> outEnvelope;
        when(enveloper.withMetadataFrom(event, MaterialAddedProcessor.PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS))
                .thenReturn(factory);

        target.processEvent(event);

        verify(sender, times(1)).send(sentEnvelopes.capture());
        assertThat(sentEnvelopes.getValue(), is(outEnvelope));
    }

    @Test
    public void shouldNotForwardUpdateWhenOriginatorIsNotCourt() {
        when(event.metadata()).thenReturn(metadata);
        when(metadata.asJsonObject()).thenReturn(metaDataJson);

        when(metaDataJson.containsKey(MaterialAddedProcessor.ORIGINATOR)).thenReturn(true);
        when(metaDataJson.getString(MaterialAddedProcessor.ORIGINATOR)).thenReturn("NOWS_DOCUMENTS");
        target.processEvent(event);

        verify(sender, times(0)).send(sentEnvelopes.capture());
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
