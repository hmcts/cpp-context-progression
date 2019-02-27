package uk.gov.moj.cpp.progression.listener;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.listener.material.MaterialAddedListener;

import javax.json.JsonObject;
import java.util.UUID;
import java.util.function.Function;

@RunWith(MockitoJUnitRunner.class)
public class MaterialAddedListenerTest {

    //@Spy
    //private final Enveloper enveloper = createEnveloper();

    @Mock
    private Enveloper enveloper;

    @Mock
    private Sender sender;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private MaterialAddedListener target;

    @Mock
    private JsonEnvelope event;

    @Mock
    private Metadata metadata;

    @Mock
    private JsonObject metaDataJson;

    @Mock
    private JsonObject payloadJson;

    @Mock
    JsonEnvelope outEnvelope;

    @Captor
    ArgumentCaptor<JsonEnvelope> sentEnvelopes;

    @Test
    public void shouldFowardCourtOriginUpdate() {
        when(event.metadata()).thenReturn(metadata);
        when(metadata.asJsonObject()).thenReturn(metaDataJson);

        when(metaDataJson.containsKey(MaterialAddedListener.ORIGINATOR)).thenReturn(true);
        when(metaDataJson.getString(MaterialAddedListener.ORIGINATOR)).thenReturn(MaterialAddedListener.ORIGINATOR_VALUE);
        when(event.payloadAsJsonObject()).thenReturn(payloadJson);
        final UUID materialId = UUID.randomUUID();
        when(payloadJson.getString(MaterialAddedListener.MATERIAL_ID)).thenReturn(materialId.toString());

        Function<Object, JsonEnvelope> factory = (o) -> {
            return outEnvelope;
        };
        when(enveloper.withMetadataFrom(event, MaterialAddedListener.PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS))
                .thenReturn(factory);

        target.processEvent(event);

        verify(sender, times(1)).send(sentEnvelopes.capture());
        assertThat(sentEnvelopes.getValue(), CoreMatchers.is(outEnvelope));
    }

    @Test
    public void shouldNotFowardNonCourtOriginUpdate() {
        when(event.metadata()).thenReturn(metadata);
        when(metadata.asJsonObject()).thenReturn(metaDataJson);

        when(metaDataJson.containsKey(MaterialAddedListener.ORIGINATOR)).thenReturn(true);
        when(metaDataJson.getString(MaterialAddedListener.ORIGINATOR)).thenReturn("xxx" + MaterialAddedListener.ORIGINATOR_VALUE);
        when(event.payloadAsJsonObject()).thenReturn(payloadJson);
        target.processEvent(event);

        verify(sender, times(0)).send(sentEnvelopes.capture());
    }


}
