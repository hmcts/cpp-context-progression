package uk.gov;

import static com.google.common.io.Resources.getResource;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

    public static Metadata metadataFor(final String commandName) {
        return metadataFrom(metadataFor(commandName, randomUUID()))
                .build();
    }

    public static Metadata metadataFor(final String commandName, final UUID commandId) {
        return metadataBuilder()
                .withName(commandName)
                .withId(commandId)
                .withUserId(randomUUID().toString())
                .build();
    }

    public static <T> T readJson(final String jsonPath, final Class<T> clazz) {
        try {
            final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

            return OBJECT_MAPPER.readValue(getResource(jsonPath), clazz);
        } catch (final IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible: " + e.getMessage());
        }
    }

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    protected void mockQuery(final String queryId, final Object result, final boolean asAdmin) {
        final JsonEnvelope resultEnvelope = Mockito.mock(JsonEnvelope.class);
        final JsonEnvelope requestEnvelope = Mockito.mock(JsonEnvelope.class);
        final Function<Object, JsonEnvelope> enveloperBuilder = (o) -> {
            return requestEnvelope;
        };
        when(enveloper.withMetadataFrom(context, queryId)).thenReturn(enveloperBuilder);
        final JsonObject organisationalUnitJson = objectToJsonObjectConverter.convert(result);
        when(resultEnvelope.payloadAsJsonObject()).thenReturn(organisationalUnitJson);
        if (asAdmin) {
            when(requester.requestAsAdmin(requestEnvelope)).thenReturn(resultEnvelope);
        } else {
            when(requester.request(requestEnvelope)).thenReturn(resultEnvelope);
        }
    }


}
