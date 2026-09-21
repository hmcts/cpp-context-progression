package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.multipart.FileInputDetailsFactory;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Adapter(Component.QUERY_API)
public class DefaultQueryApiMaterialNowsMaterialIdContentResource implements QueryApiMaterialNowsMaterialIdContentResource {
    public static final String PROGRESSION_QUERY_MATERIAL_NOWS_CONTENT = "progression.query.material-nows-content";

    @Inject
    RestProcessor restProcessor;

    @Inject
    @Named("DefaultQueryApiMaterialNowsMaterialIdContentResourceActionMapper")
    ActionMapper actionMapper;

    @Inject
    InterceptorChainProcessor interceptorChainProcessor;

    @Context
    HttpHeaders headers;

    @Inject
    FileInputDetailsFactory fileInputDetailsFactory;

    @Inject
    ParameterCollectionBuilderFactory validParameterCollectionBuilderFactory;

    @Inject
    TraceLogger traceLogger;

    @Inject
    HttpTraceLoggerHelper httpTraceLoggerHelper;

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Inject
    private MaterialClient materialClient;


    @Override
    public Response getMaterialNowsByMaterialIdContent(final String materialId, final UUID userId) {
        final JsonEnvelope documentQuery = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PROGRESSION_QUERY_MATERIAL_NOWS_CONTENT)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("materialId", materialId)
                        .build()
        );

        return interceptorChainProcessor.process(interceptorContextWithInput(documentQuery))
                .map(this::getDocumentContent)
                .orElse(status(NOT_FOUND).build());
    }

    private Response getDocumentContent(final JsonEnvelope document) {
        if (JsonValue.NULL.equals(document.payload())) {
            return null;
        } else {
            final UUID systemUser = serviceContextSystemUserProvider.getContextSystemUserId()
                    .orElseThrow(() -> new WebApplicationException("System user for progression context not found"));

            final String materialId = document.payloadAsJsonObject().getString("materialId");

            final Response documentContentResponse = materialClient.getMaterialAsPdf(materialId, systemUser.toString());
            return Response.fromResponse(documentContentResponse).entity(documentContentResponse.readEntity(InputStream.class)).build();
        }
    }
}
