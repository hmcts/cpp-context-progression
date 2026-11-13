package uk.gov.moj.cpp.progression.material.client;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.progression.material.client.MaterialClient.REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class MaterialClientTest {

    private static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    private static final String COMMAND_BASE_URI = "http://localhost:8080/material-command-api/command/api/rest/material";
    private static final String GET_MATERIAL_AS_PDF = "application/vnd.material.query.material+json";
    private static final String REMOVE_MATERIAL = "application/vnd.material.command.delete-material+json";
    private static final String REQUEST_PARAM_STREAM = "stream";
    private static final String REQUEST_PARAM_REQUEST_PDF = "requestPdf";
    private static final String MATERIAL_ID_VALUE = randomUUID().toString();
    private static final String USER_ID_VALUE = randomUUID().toString();
    private static final String LOG_MATERIAL_CALL = "Invoking call to material context";

    @Mock
    private Logger logger;

    private final Client client = mock(Client.class);
    private final WebTarget webTarget = mock(WebTarget.class);
    private final Invocation.Builder builder = mock(Invocation.Builder.class);

    private final MaterialClient materialClient = new MaterialClient() {
        @Override
        Client getClient() {
            return client;
        }
    };

    @BeforeEach
    public void init() {
        materialClient.logger = logger;
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.path(anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.accept(anyString())).thenReturn(builder);
    }

    @Test
    public void shouldGetMaterial() {
        stubQueryParams();
        materialClient.getMaterial(fromString(MATERIAL_ID_VALUE), fromString(USER_ID_VALUE));
        verifyGetRequest(true, false, false);
        verify(logger).info(LOG_MATERIAL_CALL);
    }

    @Test
    public void shouldGetMaterialAsPdf() {
        stubQueryParams();
        materialClient.getMaterialAsPdf(MATERIAL_ID_VALUE, USER_ID_VALUE);
        verifyGetRequest(false, true, true);
        verify(logger).info(LOG_MATERIAL_CALL);
    }

    @Test
    public void shouldRemoveMaterial() {
        materialClient.removeMaterial(fromString(MATERIAL_ID_VALUE), fromString(USER_ID_VALUE), null);

        verify(client).target(COMMAND_BASE_URI);
        verify(webTarget).path(anyString());
        verify(webTarget).request();
        verify(builder).header(eq(USER_ID), anyString());
        verify(builder).accept(REMOVE_MATERIAL);
        verify(builder).post(any(Entity.class));
        verify(logger).info(LOG_MATERIAL_CALL);
    }

    @Test
    public void shouldReturnTheJaxRsClientFromGetClient() {
        try (final MockedStatic<ClientBuilder> clientBuilder = mockStatic(ClientBuilder.class)) {
            clientBuilder.when(ClientBuilder::newClient).thenReturn(client);

            assertThat(new MaterialClient().getClient(), is(client));
        }
    }

    private void stubQueryParams() {
        when(webTarget.queryParam(anyString(), any())).thenReturn(webTarget);
    }

    private void verifyGetRequest(final boolean asStream, final boolean asPdf, final boolean addInlineContentDispositionHeader) {
        verify(client).target(BASE_URI);
        verify(webTarget).path(anyString());
        verify(webTarget).queryParam(REQUEST_PARAM_STREAM, asStream);
        verify(webTarget).queryParam(REQUEST_PARAM_REQUEST_PDF, asPdf);
        verify(webTarget).queryParam(REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER, addInlineContentDispositionHeader);
        verify(webTarget).request();
        verify(builder).header(eq(USER_ID), anyString());
        verify(builder).accept(GET_MATERIAL_AS_PDF);
        verify(builder).get();
    }
}
