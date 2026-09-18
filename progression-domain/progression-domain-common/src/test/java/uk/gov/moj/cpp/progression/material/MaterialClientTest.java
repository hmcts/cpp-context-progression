package uk.gov.moj.cpp.progression.material;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;

import java.util.UUID;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MaterialClient replaced the material-client jar on the promise that the requests it makes are the
 * same ones the jar made - same URI, query parameters, header and media type. Nothing in the
 * compiler checks that promise, so these tests pin each request down argument by argument. A mocked
 * JAX-RS chain is used rather than a stub server because the subject here is the request that gets
 * built, not what material would reply.
 */
@ExtendWith(MockitoExtension.class)
class MaterialClientTest {

    private static final String QUERY_BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    private static final String COMMAND_BASE_URI = "http://localhost:8080/material-command-api/command/api/rest/material";
    private static final String MATERIAL_MEDIA_TYPE = "application/vnd.material.query.material+json";
    private static final String REMOVE_MATERIAL_MEDIA_TYPE = "application/vnd.material.command.delete-material+json";

    private static final String STREAM = "stream";
    private static final String REQUEST_PDF = "requestPdf";
    private static final String ADD_INLINE_CONTENT_DISPOSITION_HEADER = "addInlineContentDispositionHeader";

    private static final UUID MATERIAL_ID = fromString("4b0e3a9c-9d1e-4f2a-8a35-1c7c0a6d8e21");
    private static final UUID USER_UUID = fromString("7c5d1e88-2a44-4b66-8c99-5e0f1a2b3c4d");

    @Mock
    private Client client;

    @Mock
    private WebTarget webTarget;

    @Mock
    private Invocation.Builder builder;

    @Mock
    private Response response;

    /** Only ever passed through and asserted by identity, so it needs no JSON provider. */
    @Mock
    private JsonObject body;

    private MaterialClient materialClient;

    @BeforeEach
    void stubTheJaxRsChainAndSubstituteTheClient() {
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.path(anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.header(anyString(), any())).thenReturn(builder);
        when(builder.accept(anyString())).thenReturn(builder);

        materialClient = new MaterialClient() {
            @Override
            Client getClient() {
                return client;
            }
        };
    }

    @Test
    void shouldRequestTheMaterialAsAStreamWithoutAskingForPdfOrAnInlineHeader() {
        when(webTarget.queryParam(anyString(), any())).thenReturn(webTarget);
        when(builder.get()).thenReturn(response);

        assertThat(materialClient.getMaterial(MATERIAL_ID, USER_UUID), is(response));

        verify(client).target(QUERY_BASE_URI);
        verify(webTarget).path("/material/" + MATERIAL_ID);
        verify(webTarget).queryParam(STREAM, true);
        verify(webTarget).queryParam(REQUEST_PDF, false);
        verify(webTarget).queryParam(ADD_INLINE_CONTENT_DISPOSITION_HEADER, false);
        verify(builder).header(USER_ID, USER_UUID.toString());
        verify(builder).accept(MATERIAL_MEDIA_TYPE);
        verify(builder).get();
    }

    @Test
    void shouldAskForPdfAndStreamWhenFetchingAPdfByUuid() {
        when(webTarget.queryParam(anyString(), any())).thenReturn(webTarget);
        when(builder.get()).thenReturn(response);

        assertThat(materialClient.getMaterialAsPdf(MATERIAL_ID, USER_UUID), is(response));

        verify(webTarget).queryParam(STREAM, true);
        verify(webTarget).queryParam(REQUEST_PDF, true);
        verify(webTarget).queryParam(ADD_INLINE_CONTENT_DISPOSITION_HEADER, false);
    }

    /**
     * The String form is deliberately not an overload of the UUID one: it turns streaming off and the
     * inline content-disposition header on, which is what makes a document render in the browser
     * rather than download. Collapsing the two would silently change that behaviour.
     */
    @Test
    void shouldTurnStreamingOffAndTheInlineHeaderOnWhenFetchingAPdfByString() {
        when(webTarget.queryParam(anyString(), any())).thenReturn(webTarget);
        when(builder.get()).thenReturn(response);

        assertThat(materialClient.getMaterialAsPdf(MATERIAL_ID.toString(), USER_UUID.toString()), is(response));

        verify(webTarget).queryParam(STREAM, false);
        verify(webTarget).queryParam(REQUEST_PDF, true);
        verify(webTarget).queryParam(ADD_INLINE_CONTENT_DISPOSITION_HEADER, true);
    }

    @Test
    void shouldDifferFromTheUuidFormSoTheInlineDocumentBehaviourIsNotLost() {
        when(webTarget.queryParam(anyString(), any())).thenReturn(webTarget);
        when(builder.get()).thenReturn(response);

        materialClient.getMaterialAsPdf(MATERIAL_ID, USER_UUID);
        materialClient.getMaterialAsPdf(MATERIAL_ID.toString(), USER_UUID.toString());

        verify(webTarget).queryParam(STREAM, false);
        verify(webTarget).queryParam(ADD_INLINE_CONTENT_DISPOSITION_HEADER, true);
        verify(webTarget).queryParam(ADD_INLINE_CONTENT_DISPOSITION_HEADER, false);
    }

    @Test
    void shouldPostToTheCommandApiWithTheDeleteMediaTypeWhenRemovingAMaterial() {
        when(builder.post(any(Entity.class))).thenReturn(response);

        assertThat(materialClient.removeMaterial(MATERIAL_ID, USER_UUID, body), is(response));

        verify(client).target(COMMAND_BASE_URI);
        verify(webTarget).path("/material/" + MATERIAL_ID);
        verify(builder).header(USER_ID, USER_UUID.toString());
        verify(builder).accept(REMOVE_MATERIAL_MEDIA_TYPE);

        final ArgumentCaptor<Entity<?>> entityCaptor = ArgumentCaptor.captor();
        verify(builder).post(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEntity(), is(body));
        assertThat(entityCaptor.getValue().getMediaType().toString(), is(REMOVE_MATERIAL_MEDIA_TYPE));
    }

    @Test
    void shouldNotSendTheRemoveRequestToTheQueryApi() {
        when(builder.post(any(Entity.class))).thenReturn(response);

        materialClient.removeMaterial(MATERIAL_ID, USER_UUID, body);

        verify(client).target(COMMAND_BASE_URI);
    }
}
