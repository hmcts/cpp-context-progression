package uk.gov.justice.api.resource;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.api.resource.utils.ApplicationExtractTransformer;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.progression.courts.exract.ApplicationCourtExtractRequested;
import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.exception.DocumentGeneratorException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
@Stateless
@Adapter(Component.QUERY_API)
public class DefaultQueryApiApplicationsApplicationIdExtractResource implements QueryApiApplicationsApplicationIdExtractResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryApiApplicationsApplicationIdExtractResource.class);
    private static final String APPLICATION_AT_A_GLANCE_QUERY_NAME = "progression.query.application";
    public static final String STANDALONE_APPLICATION = "CourtExtractApplication";
    @Inject
    @Named("DefaultQueryApiApplicationsApplicationIdExtractResourceActionMapper")
    ActionMapper actionMapper;
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
    private ApplicationExtractTransformer applicationExtractTransformer;

    private UUID userId;

    @Override
    public Response getApplicationExtractByApplicationIdContent(String applicationId, String hearingIds, UUID userId) {
        this.userId = userId;
        final JsonEnvelope documentQuery = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(APPLICATION_AT_A_GLANCE_QUERY_NAME)
                        .withUserId(userId.toString())
                        .build(),

                createObjectBuilder()
                        .add("applicationId", applicationId)
                        .build());
        final List<String> hearingIdList = isNotEmpty(hearingIds) ? asList(hearingIds.trim().split(",")) : emptyList();

        final JsonEnvelope document = interceptorChainProcessor.process(interceptorContextWithInput(documentQuery)).get();
        return getDocumentContent(document, hearingIdList, applicationId);
    }

    private Response getDocumentContent(final JsonEnvelope document, final List<String> hearingIdList, final String applicationId) {

        if (JsonValue.NULL.equals(document.payload()) || document.payloadAsJsonObject().isEmpty()) {
            LOGGER.info(" ### No record found for the application id: {} ### ", applicationId);
            return null;
        } else {
            final UUID systemUser = serviceContextSystemUserProvider.getContextSystemUserId()
                    .orElseThrow(() -> new WebApplicationException("System user for progression context not found"));
            final String pdfMimeType = "application/pdf";
            final InputStream documentInputStream = getPdfDocument(document, hearingIdList, systemUser);
            final Response.ResponseBuilder responseBuilder = status(OK).entity(documentInputStream);
            return responseBuilder
                    .header(CONTENT_TYPE, pdfMimeType)
                    .build();

        }
    }

    private InputStream getPdfDocument(final JsonEnvelope document, final List<String> hearingIdList, final UUID systemUser) {
        final byte[] resultOrderAsByteArray;
        final InputStream documentInputStream;
        try {

            final JsonObject payload = transformToTemplateConvert(document.payloadAsJsonObject(), hearingIdList);
            LOGGER.info("create application extract with payload : {}", payload);
            resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(payload, STANDALONE_APPLICATION, systemUser);
            documentInputStream = new ByteArrayInputStream(resultOrderAsByteArray);
        } catch (IOException e) {
            LOGGER.error("application extract generate Pdf document failed ", e);
            throw new DocumentGeneratorException();
        }
        return documentInputStream;
    }

    private JsonObject transformToTemplateConvert(JsonObject jsonObject, final List<String> hearingIdList) {
        final CourtApplication courtApplication = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("courtApplication"), CourtApplication.class);
        final List<Hearing> hearingsForApplication = applicationExtractTransformer.getHearingsForApplication(jsonObject.getJsonArray("hearings"), hearingIdList);
        final ApplicationCourtExtractRequested applicationCourtExtractRequested = applicationExtractTransformer.getApplicationCourtExtractRequested(courtApplication, hearingsForApplication, STANDALONE_APPLICATION, userId);
        return objectToJsonObjectConverter.convert(applicationCourtExtractRequested);
    }

}
