package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.api.resource.utils.CourtExtractTransformer;
import uk.gov.justice.api.resource.utils.payload.PleaValueDescriptionBuilder;
import uk.gov.justice.api.resource.utils.payload.ResultTextFlagBuilder;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * http endpoint adapter which overrides default framework adapter. It handles transfer of files
 * binaries between domosis and progression context. Class invoke
 * standard interceptor chain. Thanks to that all standard cross-cutting concerns like
 * authorisation, audit, performance metrics, feature toggles handling are handled in standard way.
 * At the end of interceptor chain, regular query handler is invoked and returns documents details
 */
@SuppressWarnings({"squid:S3655", "squid:S1166"})
@Stateless
@Adapter(Component.QUERY_API)
public class DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource implements QueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.class);

    public static final String PROGRESSION_QUERY_PROSECUTION_CASE = "progression.query.prosecutioncase";
    public static final String COURT_EXTRACT = "CrownCourtExtract";
    public static final String CERTIFICATE_OF_ACQUITTAL_CONVICTION = "CertificateOfAcquittalConviction";

    @Inject
    RestProcessor restProcessor;

    @Inject
    @Named("DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResourceActionMapper")
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
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtExtractTransformer courtExtractTransformer;

    @Inject
    private PleaValueDescriptionBuilder pleaValueDescriptionBuilder;

    @Inject
    private ResultTextFlagBuilder resultTextFlagBuilder;

    private UUID userId;

    @Override
    public Response getCourtExtractByCaseIdContent(final String caseId, final String defendantId, final String template, final String hearingIds, final UUID userId) {

        this.userId = userId;
        final JsonEnvelope documentQuery = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PROGRESSION_QUERY_PROSECUTION_CASE)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .add("template", template)
                        .build()
        );

        List<String> hearingIdList = null;
        if (COURT_EXTRACT.equals(template)) {
            if(hearingIds == null || hearingIds.isEmpty()){
                throw new IllegalArgumentException("hearingIds query parameter cannot be empty or null for the template CrownCourtExtract");
            }
            hearingIdList = Arrays.asList(hearingIds.trim().split(","));
        }

        final JsonEnvelope document = interceptorChainProcessor.process(interceptorContextWithInput(documentQuery)).get();
        return getDocumentContent(document, defendantId, template, hearingIdList);
    }

    private Response getDocumentContent(final JsonEnvelope document, final String defendantId, final String extractType, final List<String> hearingIdList) {
        if (NULL.equals(document.payload())) {
            return null;
        } else {
            final UUID systemUser = serviceContextSystemUserProvider.getContextSystemUserId()
                    .orElseThrow(() -> new WebApplicationException("System user for progression context not found"));
            final String pdfMimeType = "application/pdf";
            final InputStream documentInputStream = getPdfDocument(document, defendantId, extractType, hearingIdList, systemUser);
            final Response.ResponseBuilder responseBuilder = status(OK).entity(documentInputStream);
            return responseBuilder
                    .header(CONTENT_TYPE, pdfMimeType)
                    .build();
        }
    }

    private InputStream getPdfDocument(final JsonEnvelope document, final String defendantId, final String extractType, final List<String> hearingIdList, final UUID systemUser) {
        final byte[] resultOrderAsByteArray;
        final InputStream documentInputStream;
        try {
            LOGGER.info("transform court extract payload : {}", document.toObfuscatedDebugString());
            final JsonObject payload = transformToTemplateConvert(document.payloadAsJsonObject(), defendantId, extractType, hearingIdList);
            LOGGER.info("create court extract with payload : {}", payload);
            JsonObject newPayload = pleaValueDescriptionBuilder.rebuildPleaWithDescription(payload);
            newPayload = resultTextFlagBuilder.rebuildWithResultTextFlag(newPayload);
            resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(newPayload, getTemplateName(extractType), systemUser);
            documentInputStream = new ByteArrayInputStream(resultOrderAsByteArray);
        } catch (IOException e) {
            LOGGER.error("Court extract generate Pdf document failed ", e);
            throw new DocumentGeneratorException();
        }
        return documentInputStream;
    }

    public String getTemplateName(String template) {
        if(COURT_EXTRACT.equals(template)) {
            return COURT_EXTRACT;
        }

        return CERTIFICATE_OF_ACQUITTAL_CONVICTION;
    }

    private JsonObject transformToTemplateConvert(JsonObject jsonObject, final String defendantId, final String extractType, final List<String> hearingIdList) {
        final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("hearingsAtAGlance"), GetHearingsAtAGlance.class);
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class);
        final CourtExtractRequested courtExtractRequested = courtExtractTransformer.getCourtExtractRequested(hearingsAtAGlance, defendantId, extractType, hearingIdList, userId, prosecutionCase);
        return objectToJsonObjectConverter.convert(courtExtractRequested);
    }
}
