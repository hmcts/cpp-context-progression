package uk.gov.moj.cpp.progression.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.service.MaterialService.MATERIAL_METADETA_QUERY;

import uk.gov.justice.core.courts.CourtsDocumentUploaded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.material.client.MaterialClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialServiceTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Mock
    Metadata metadata;

    @InjectMocks
    private MaterialService service;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject courtDocumentUploadJson;

    @Mock
    private CourtsDocumentUploaded courtsDocumentUploaded;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private MaterialClient materialClient;

    @Mock
    private Response response;

    @Test
    public void shouldCallMaterialUpload() {
        final String userId = UUID.randomUUID().toString();
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(Optional.of(userId));
        //when
        service.uploadMaterial(randomUUID(), randomUUID(), envelope);
        //Then
        verify(sender).send(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldGetMaterialMetadata() {

        //given
        final UUID materialId = UUID.randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileName", "abc.txt")
                .add("mimeType", "text")
                .add("materialAddedDate", "2016-05-03")
                .build();
        when(requester.requestAsAdmin(any()))
                .thenReturn(envelopeFrom(metadataWithRandomUUID(MATERIAL_METADETA_QUERY), payload));

        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(MATERIAL_METADETA_QUERY))
                .build();
        final Optional<JsonObject> result = service.getMaterialMetadata(envelope, materialId);

        //then
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope)
                        .withName(MATERIAL_METADETA_QUERY),
                payloadIsJson(
                        withJsonPath("$.materialId", equalTo(materialId.toString()))
                ))
        ));
        assertThat(result.get().getString("fileName"), is("abc.txt"));
        assertThat(result.get().getString("mimeType"), is("text"));
        assertThat(result.get().getString("materialAddedDate"), is("2016-05-03"));
        verifyNoMoreInteractions(requester);
    }


    @Test
    public void shouldGetMaterialMetadataV2() {

        //given
        final UUID materialId = UUID.randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileName", "abc.txt")
                .add("mimeType", "text")
                .add("materialAddedDate", "2016-05-03")
                .build();
        when(requester.requestAsAdmin(any(JsonEnvelope.class), any(Class.class))).thenReturn(finalEnvelope);
        when(finalEnvelope.payload()).thenReturn(payload);

        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(MATERIAL_METADETA_QUERY))
                .build();
        final String result = service.getMaterialMetadataV2(envelope, materialId);

        //then
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture(), any(Class.class));
        MatcherAssert.assertThat(result, is("abc.txt"));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldThrowcConditionTimeoutExceptionWhenMaterialMetadataV2ReturnsPayloadAsNull() {

        //given
        final UUID materialId = UUID.randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileName", "abc.txt")
                .add("mimeType", "text")
                .add("materialAddedDate", "2016-05-03")
                .build();
        when(requester.requestAsAdmin(any(JsonEnvelope.class), any(Class.class))).thenReturn(finalEnvelope);
        when(finalEnvelope.payload()).thenReturn(null);

        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(MATERIAL_METADETA_QUERY))
                .build();
        final ConditionTimeoutException conditionTimeoutException = Assert.assertThrows(ConditionTimeoutException.class, () -> service.getMaterialMetadataV2(envelope, materialId));

        //then
        MatcherAssert.assertThat(conditionTimeoutException, notNullValue());
    }


    @Test
    public void shouldTestGetDocumentFromMaterial() {
        when(materialClient.getMaterial(any(UUID.class), any(UUID.class))).thenReturn(response);
        final InputStream inputStream = new ByteArrayInputStream("initialString".getBytes());
        when(response.readEntity(InputStream.class)).thenReturn(inputStream);
        final byte[] documentContent = service.getDocumentContent(randomUUID(), randomUUID());
        assertEquals(13, documentContent.length, "Material stream size not as expected");
    }
}
