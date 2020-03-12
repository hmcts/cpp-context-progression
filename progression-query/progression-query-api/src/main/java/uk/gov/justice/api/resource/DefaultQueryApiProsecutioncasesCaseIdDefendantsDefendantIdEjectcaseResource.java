package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.api.resource.utils.CourtExtractTransformer;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.multipart.FileInputDetailsFactory;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;
import uk.gov.moj.cpp.progression.query.api.exception.DocumentGeneratorException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@SuppressWarnings({"squid:S3655", "squid:S1166"})
@Stateless
@Adapter(Component.QUERY_API)
public class DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdEjectcaseResource implements QueryApiProsecutioncasesCaseIdDefendantsDefendantIdEjectcaseResource {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdEjectcaseResource.class);

    private static final String PROGRESSION_QUERY_PROSECUTION_CASE = "progression.query.prosecutioncase";
    private static final String MIME_TYPE = "application/pdf";
    private static final String CROWN_COURT_EXTRACT = "CrownCourtExtract";

    @Inject
    RestProcessor restProcessor;

    @Inject
    @Named("DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdEjectcaseResourceActionMapper")
    ActionMapper actionMapper;

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
    InterceptorChainProcessor interceptorChainProcessor;

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    CourtExtractTransformer courtExtractTransformer;

    private UUID userId;

    @Override
    public Response getProsecutioncasesByCaseIdDefendantsByDefendantIdEjectcase(final String caseId, final String defendantId, final UUID userId) {
        this.userId = userId;
        final JsonEnvelope documentQuery = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PROGRESSION_QUERY_PROSECUTION_CASE)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .build()
        );

        final JsonEnvelope document = interceptorChainProcessor.process(interceptorContextWithInput(documentQuery)).get();

        return getDocumentContent(document, defendantId);
    }

    private Response getDocumentContent(final JsonEnvelope document, final String defendantId) {
        if (JsonValue.NULL.equals(document.payload())) {
            return null;
        } else {
            final UUID systemUser = serviceContextSystemUserProvider.getContextSystemUserId()
                    .orElseThrow(() -> new WebApplicationException("System user for progression context not found"));

            final InputStream documentInputStream = getPdfDocument(document, defendantId, systemUser);

            final Response.ResponseBuilder responseBuilder = status(OK).entity(documentInputStream);

            return responseBuilder
                    .header(CONTENT_TYPE, MIME_TYPE)
                    .build();
        }
    }

    private InputStream getPdfDocument(final JsonEnvelope document, final String defendantId, final UUID systemUser) {
        final byte[] resultOrderAsByteArray;
        final InputStream documentInputStream;
        try {
            final JsonObject payload = transformToTemplateConvert(document.payloadAsJsonObject(), defendantId);
            LOGGER.info("Eject case transformed payload : {}", payload);
            resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(payload, CROWN_COURT_EXTRACT, systemUser);
            documentInputStream = new ByteArrayInputStream(resultOrderAsByteArray);
        } catch (IOException e) {
            LOGGER.error("Eject case PDF generation failed ", e);
            throw new DocumentGeneratorException();
        }
        return documentInputStream;
    }

    private JsonObject transformToTemplateConvert(JsonObject jsonObject, final String defendantId) {
        final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("hearingsAtAGlance"), GetHearingsAtAGlance.class);
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class);
        final CourtExtractRequested courtExtractRequested = courtExtractTransformer.ejectCase(prosecutionCase, hearingsAtAGlance, defendantId, userId);
        return objectToJsonObjectConverter.convert(courtExtractRequested);
    }

}
