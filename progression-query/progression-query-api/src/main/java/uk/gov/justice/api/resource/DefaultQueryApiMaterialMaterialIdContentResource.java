package uk.gov.justice.api.resource;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.multipart.FileInputDetailsFactory;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.progression.query.view.UserDetailsLoader;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexRepository;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryApiMaterialMaterialIdContentResource.class);
    public static final String PROGRESSION_QUERY_MATERIAL_CONTENT = "progression.query.material-content";
    public static final String PROGRESSION_QUERY_MATERIAL_CONTENT_DEFENCE = "progression.query.material-content-for-defence";
    public static final String PROGRESSION_QUERY_MATERIAL_CONTENT_PROSECUTION = "progression.query.material-content-for-prosecution";
    private static final String MATERIAL_ID = "materialId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String JSON_MIME_TYPE = "application/json";
    private static final String CASE_ID = "caseId";
    public static final String APPLICATION_ID = "applicationId";
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
    private CourtDocumentIndexRepository courtDocumentIndexRepository;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private MaterialClient materialClient;

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
    public Response getMaterialForDefenceByMaterialIdContent(final String materialId, final String defendantId, final String applicationId, final UUID userId) {
        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add(MATERIAL_ID, materialId);
        ofNullable(defendantId).ifPresent(param -> builder.add(DEFENDANT_ID, param));
        ofNullable(applicationId).ifPresent(param -> builder.add(APPLICATION_ID, param));

        final JsonEnvelope documentQuery = envelopeFrom(metadataBuilder()
                        .withId(randomUUID())
                        .withName(PROGRESSION_QUERY_MATERIAL_CONTENT_DEFENCE)
                        .withUserId(userId.toString())
                        .build(),
                builder.build()
        );

        return processInterceptor(documentQuery);
    }

    @Override
    public Response getMaterialForProsecutionByMaterialIdContent(final String materialId, final String caseId, final String applicationId, final UUID userId) {

        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add(MATERIAL_ID, materialId);
        ofNullable(caseId).ifPresent(param -> builder.add(CASE_ID, param));
        ofNullable(applicationId).ifPresent(param -> builder.add(APPLICATION_ID, param));

        final JsonEnvelope documentQuery = envelopeFrom(metadataBuilder()
                        .withId(randomUUID())
                        .withName(PROGRESSION_QUERY_MATERIAL_CONTENT_PROSECUTION)
                        .withUserId(userId.toString())
                        .build(),
                builder.build()
        );

        return processInterceptor(documentQuery);
    }


    private JsonEnvelope getMaterialQueryEnvelope(final String materialId, final UUID userId, final String actionName, final Optional<Pair<String, String>> queryParam) {

        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add(MATERIAL_ID, materialId);
        queryParam.ifPresent(param -> builder.add(param.getKey(), param.getValue()));

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

            if (isNotAuthorisedToViewMaterial(document, materialId)) {
                return Response
                        .status(FORBIDDEN)
                        .entity(createObjectBuilder().build())
                        .header(CONTENT_TYPE, JSON_MIME_TYPE)
                        .build();
            }


            final Response documentContentResponse = materialClient.getMaterial(fromString(materialId), systemUser);

            final Response.Status documentContentResponseStatus = fromStatusCode(documentContentResponse.getStatus());
            if (OK.equals(documentContentResponseStatus)) {
                final String url = documentContentResponse.readEntity(String.class);
                final JsonObject jsonObject = createObjectBuilder()
                        .add("url", url)
                        .build();

                return Response
                        .status(OK)
                        .entity(jsonObject)
                        .header(CONTENT_TYPE, JSON_MIME_TYPE)
                        .build();
            } else {
                return Response.fromResponse(documentContentResponse).build();
            }
        }
    }

    private boolean isNotAuthorisedToViewMaterial(final JsonEnvelope document, final String materialId) {
        try {
            final List<CourtDocumentIndexEntity> courtDocumentIndexEntities = courtDocumentIndexRepository.findByMaterialId(fromString(materialId));
            if (isEmpty(courtDocumentIndexEntities)) {
                return false;
            }

            return courtDocumentIndexEntities.stream()
                    .map(CourtDocumentIndexEntity::getApplicationId)
                    .filter(Objects::nonNull)
                    .map(this::getCourtApplication)
                    .anyMatch(courtApplication -> !userDetailsLoader.isUserHasPermissionForApplicationTypeCode(document.metadata(), courtApplication.getType().getCode()));
        } catch (final Exception e) {
            LOGGER.error(format("Error checking authorisation for materialId: %s", materialId), e);
            return false;
        }
    }

    private CourtApplication getCourtApplication(final UUID applicationId) {
        final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(applicationId);
        final JsonObject application = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
        return jsonObjectToObjectConverter.convert(application, CourtApplication.class);
    }
}
