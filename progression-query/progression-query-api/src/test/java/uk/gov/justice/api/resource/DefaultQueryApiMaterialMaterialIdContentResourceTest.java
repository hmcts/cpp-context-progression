package uk.gov.justice.api.resource;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.api.resource.DefaultQueryApiMaterialMaterialIdContentResource.PROGRESSION_QUERY_MATERIAL_CONTENT;
import static uk.gov.justice.api.resource.DefaultQueryApiMaterialMaterialIdContentResource.PROGRESSION_QUERY_MATERIAL_CONTENT_PROSECUTION;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.accesscontrol.AccessControlViolationException;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiMaterialMaterialIdContentResourceTest {

    private static final String JSON_CONTENT_TYPE = "application/json";

    @Mock
    private MaterialClient materialClient;

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Mock
    private Response documentContentResponse;

    @Captor
    private ArgumentCaptor<InterceptorContext> interceptorContextCaptor;

    @InjectMocks
    private DefaultQueryApiMaterialMaterialIdContentResource endpointHandler;

    private final UUID userId = randomUUID();
    private final UUID materialId = randomUUID();
    private final UUID systemUserId = randomUUID();
    private final String documentUrl = "http://filelocation.com/myfile.pdf";
    private final UUID caseId = randomUUID();

    private final InputStream documentStream = new ByteArrayInputStream("test".getBytes());


    @Test
    public void shouldRunAllInterceptorsAndFetchAndStreamDocument() {
        final JsonEnvelope documentDetails = documentDetails(materialId);

        final MultivaluedMap headers = new MultivaluedHashMap(ImmutableMap.of(CONTENT_TYPE, JSON_CONTENT_TYPE));

        final JsonObject json = Json.createObjectBuilder()
                .add("url", documentUrl)
                .build();

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(interceptorChainProcessor.process(argThat((any(InterceptorContext.class))))).thenReturn(Optional.ofNullable(documentDetails));
        when(materialClient.getMaterial(materialId, systemUserId)).thenReturn(documentContentResponse);
        when(documentContentResponse.readEntity(String.class)).thenReturn(documentUrl);
        when(documentContentResponse.getStatus()).thenReturn(SC_OK);

        final Response documentContentResponse = endpointHandler.getMaterialByMaterialIdContent(materialId.toString(), userId);

        assertThat(documentContentResponse.getStatus(), is(SC_OK));
        assertThat(documentContentResponse.getHeaders(), is(headers));
        assertThat(documentContentResponse.getEntity(), is(json));

        verifyInterceptorChainExecution();
    }

    @Test
    public void shouldRunNotFoundStatusWhenDocumentNotFound() {
        final JsonEnvelope documentDetails = missingDocumentDetails();

        when(interceptorChainProcessor.process(argThat((any(InterceptorContext.class))))).thenReturn(Optional.ofNullable(documentDetails));

        final Response documentContentResponse = endpointHandler.getMaterialByMaterialIdContent(materialId.toString(), userId);

        assertThat(documentContentResponse.getStatus(), is(SC_NOT_FOUND));

        verifyInterceptorChainExecution();

        verify(materialClient, never()).getMaterial(argThat(any(UUID.class)), argThat(any(UUID.class)));
    }

    @Test
    public void shouldRunNotFoundStatusWhenMaterialNotFound() {
        final JsonEnvelope documentDetails = documentDetails(materialId);

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(interceptorChainProcessor.process(argThat((any(InterceptorContext.class))))).thenReturn(Optional.ofNullable(documentDetails));
        when(materialClient.getMaterial(materialId, systemUserId)).thenReturn(documentContentResponse);
        when(documentContentResponse.getHeaders()).thenReturn(new MultivaluedHashMap());
        when(documentContentResponse.getStatus()).thenReturn(SC_NOT_FOUND);

        final Response documentContentResponse = endpointHandler.getMaterialByMaterialIdContent(materialId.toString(), userId);

        assertThat(documentContentResponse.getStatus(), is(SC_NOT_FOUND));

        verifyInterceptorChainExecution();
    }

    @Test
    public void shouldRethrowAnyInterceptorException() {
        final Exception interceptorException = new AccessControlViolationException("");

        when(interceptorChainProcessor.process(argThat((any(InterceptorContext.class))))).thenThrow(interceptorException);

        try {
            endpointHandler.getMaterialByMaterialIdContent(materialId.toString(), userId);
            fail("Interceptor exception expected");
        } catch (Exception e) {
            assertThat(e, is(interceptorException));
        }

        verifyInterceptorChainExecution();
        verify(materialClient, never()).getMaterial(argThat(any(UUID.class)), argThat(any(UUID.class)));
    }

    @Test
    public void shouldOverrideGeneratedDefaultAdapterClass() {
        assertThat(endpointHandler.getClass().getName(), is("uk.gov.justice.api.resource.DefaultQueryApiMaterialMaterialIdContentResource"));
    }

    private void verifyInterceptorChainExecution() {
        verify(interceptorChainProcessor).process(interceptorContextCaptor.capture());

        assertThat(interceptorContextCaptor.getValue().inputEnvelope(), jsonEnvelope(metadata().withName(PROGRESSION_QUERY_MATERIAL_CONTENT).withUserId(userId.toString()),
                payload().isJson(allOf(
                        withJsonPath("$.materialId", equalTo(materialId.toString()))
                ))
        ));
    }

    @Test
    public void shouldRunInterceptorsAndFetchDocumentWhenQueryMaterialByIdForProsecutor() {
        final JsonEnvelope documentDetails = documentDetails(materialId);

        final MultivaluedMap headers = new MultivaluedHashMap(ImmutableMap.of(CONTENT_TYPE, JSON_CONTENT_TYPE));

        final JsonObject json = Json.createObjectBuilder()
                .add("url", documentUrl)
                .build();

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(interceptorChainProcessor.process(argThat((any(InterceptorContext.class))))).thenReturn(Optional.ofNullable(documentDetails));
        when(materialClient.getMaterial(materialId, systemUserId)).thenReturn(documentContentResponse);
        when(documentContentResponse.readEntity(String.class)).thenReturn(documentUrl);
        when(documentContentResponse.getStatus()).thenReturn(SC_OK);

        final Response documentContentResponse = endpointHandler.getMaterialForProsecutionByMaterialIdContent(materialId.toString(), caseId.toString(), null, userId);

        assertThat(documentContentResponse.getStatus(), is(SC_OK));
        assertThat(documentContentResponse.getHeaders(), is(headers));
        assertThat(documentContentResponse.getEntity(), is(json));

        verify(interceptorChainProcessor).process(interceptorContextCaptor.capture());

        assertThat(interceptorContextCaptor.getValue().inputEnvelope(), jsonEnvelope(metadata().withName(PROGRESSION_QUERY_MATERIAL_CONTENT_PROSECUTION).withUserId(userId.toString()),
                payload().isJson(allOf(
                        withJsonPath("$.materialId", equalTo(materialId.toString()))
                ))
        ));
    }

    private JsonEnvelope documentDetails(final UUID materialId) {
        return documentDetails(createObjectBuilder().add("materialId", materialId.toString()).build());
    }

    private JsonEnvelope missingDocumentDetails() {
        return documentDetails(JsonValue.NULL);
    }

    private JsonEnvelope documentDetails(final JsonValue payload) {
        return envelopeFrom(metadataWithRandomUUID(PROGRESSION_QUERY_MATERIAL_CONTENT), payload);
    }
}
