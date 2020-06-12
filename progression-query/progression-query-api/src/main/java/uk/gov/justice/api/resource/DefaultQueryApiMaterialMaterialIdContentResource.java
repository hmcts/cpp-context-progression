package uk.gov.justice.api.resource;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
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
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.progression.query.api.UserDetailsLoader;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * http endpoint adapter which overrides default framework adapter. It handles transfer of files
 * binaries between material and progression context. Name of this class is after raml definition of
 * case-document-content query and need to be changed when raml definition changes. Class invoke
 * standard interceptor chain. Thanks to that all standard cross-cutting concerns like
 * authorisation, audit, performance metrics, feature toggles handling are handled in standard way.
 * At the end of interceptor chain, regular query handler is invoked and returns documents details
 * (materialId among others)
 */

@Stateless
@Adapter(Component.QUERY_API)
public class DefaultQueryApiMaterialMaterialIdContentResource implements QueryApiMaterialMaterialIdContentResource {
    public static final String PROGRESSION_QUERY_MATERIAL_CONTENT = "progression.query.material-content";
    public static final String PROGRESSION_QUERY_MATERIAL_CONTENT_DEFENCE = "progression.query.material-content-for-defence";
    private static final String MATERIAL_ID = "materialId";
    private static final String DEFENDANT_ID = "defendantId";
    @Inject
    RestProcessor restProcessor;

    @Inject
    @Named("DefaultQueryApiMaterialMaterialIdContentResourceActionMapper")
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

    @Inject
    private Requester requester;

    @Inject
    private UserDetailsLoader userDetailsLoader;


    @Override
    public Response getMaterialByMaterialIdContent(final String materialId, final UUID userId) {

        final JsonEnvelope documentQuery = getMaterialQueryEnvelope(materialId, userId, PROGRESSION_QUERY_MATERIAL_CONTENT, empty());

        return processInterceptor(documentQuery);

    }

    private Response processInterceptor(final JsonEnvelope documentQuery) {
        return interceptorChainProcessor.process(interceptorContextWithInput(documentQuery))
                .map(this::getDocumentContent)
                .orElse(status(NOT_FOUND).build());
    }

    @Override
    public Response getMaterialForDefenceByMaterialIdContent(final String materialId, final String defendantId, final UUID userId) {

        final JsonEnvelope documentQuery = getMaterialQueryEnvelope(materialId, userId, PROGRESSION_QUERY_MATERIAL_CONTENT_DEFENCE, of(defendantId));

        return processInterceptor(documentQuery);

    }

    private JsonEnvelope getMaterialQueryEnvelope(final String materialId, final UUID userId, final String actionName, final Optional<String> defendantId) {

        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add(MATERIAL_ID, materialId);
        defendantId.ifPresent(id->builder.add(DEFENDANT_ID,id));

        return envelopeFrom(metadataBuilder()
                        .withId(randomUUID())
                        .withName(actionName)
                        .withUserId(userId.toString())
                        .build(),
                builder.build()
        );
    }


    private Response getDocumentContent(final JsonEnvelope document) {
        if (JsonValue.NULL.equals(document.payload())) {
            return null;
        } else {
            final UUID systemUser = serviceContextSystemUserProvider.getContextSystemUserId()
                    .orElseThrow(() -> new WebApplicationException("System user for progression context not found"));

            final String materialId = document.payloadAsJsonObject().getString(MATERIAL_ID);

            final Response documentContentResponse = materialClient.getMaterial(fromString(materialId), systemUser);
            return Response.fromResponse(documentContentResponse).entity(documentContentResponse.readEntity(InputStream.class)).build();
        }
    }


}
