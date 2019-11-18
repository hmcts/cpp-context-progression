package uk.gov;

import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

public class QueryClientTestBase {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents();
    @Mock
    protected Requester requester;

    @Mock
    protected JsonEnvelope context;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    protected void mockQuery(final String queryId, final Object result, boolean asAdmin) {
        final JsonEnvelope resultEnvelope = Mockito.mock(JsonEnvelope.class);
        final JsonEnvelope requestEnvelope = Mockito.mock(JsonEnvelope.class);
        final Function<Object, JsonEnvelope> enveloperBuilder = (o) -> {
            return requestEnvelope;
        };
        when(enveloper.withMetadataFrom(context, queryId)).thenReturn(enveloperBuilder);
        JsonObject organisationalUnitJson = objectToJsonObjectConverter.convert(result);
        when(resultEnvelope.payloadAsJsonObject()).thenReturn(organisationalUnitJson);
        if (asAdmin) {
            when(requester.requestAsAdmin(requestEnvelope)).thenReturn(resultEnvelope);
        } else {
            when(requester.request(requestEnvelope)).thenReturn(resultEnvelope);
        }
    }


}
