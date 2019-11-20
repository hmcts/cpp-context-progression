package uk.gov.moj.cpp.progression.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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
import static uk.gov.moj.cpp.progression.service.MaterialService.UPLOAD_MATERIAL;

import uk.gov.justice.core.courts.CourtsDocumentUploaded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MaterialServiceTest {

    @InjectMocks
    private MaterialService service;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Spy
    private final Enveloper enveloper = createEnveloper();

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
    Metadata metadata;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldCallMaterialUpload() {
        final String userId = UUID.randomUUID().toString();
        when(enveloper.withMetadataFrom(envelope, UPLOAD_MATERIAL)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
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
}
