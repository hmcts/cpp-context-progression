package uk.gov.justice.api.resource;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
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
import java.util.Optional;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http endpoint adapter which overrides default framework adapter. It handles transfer of files
 * binaries between docmosis and progression context. Class invoke standard interceptor chain. At
 * the end of interceptor chain, regular query handler is invoked and returns documents details
 */

@Stateless
@SuppressWarnings({"squid:S3655"})
@Adapter(Component.QUERY_API)
public class DefaultQueryApiCourtlistResource implements QueryApiCourtlistResource {

    protected static final String COURT_LIST_QUERY_NAME = "progression.search.court.list";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryApiCourtlistResource.class);
    private static final String EXTRACT_FILE_NAME = "CourtList.pdf";
    protected static final String DISPOSITION = "attachment; filename=\"" + EXTRACT_FILE_NAME + "\"";
    private static final String MIME_TYPE = "application/pdf";

    @Context
    HttpHeaders headers;

    @Inject
    @Named("DefaultQueryApiCourtlistResourceActionMapper")
    ActionMapper actionMapper;

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Inject
    private InterceptorChainProcessor interceptorChainProcessor;

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Override
    public Response getCourtlist(final String courtCentreId, final String courtRoomId, final String listId,
                                 final String startDate, final String endDate, final boolean restricted, final UUID userId) {
        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("listId", listId)
                .add("startDate", startDate)
                .add("endDate", endDate)
                .add("restricted", restricted);

        if (nonNull(courtRoomId)) {
            payloadBuilder.add("courtRoomId", courtRoomId);
        }

        final JsonEnvelope documentQuery = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(COURT_LIST_QUERY_NAME)
                        .withUserId(userId.toString())
                        .build(),
                payloadBuilder.build());

        final JsonEnvelope document = interceptorChainProcessor.process(interceptorContextWithInput(documentQuery)).get();

        return getDocumentContent(document);
    }


    private Response getDocumentContent(final JsonEnvelope document) {
        if (JsonValue.NULL.equals(document.payload())) {
            return null;
        } else {
            final UUID systemUser = serviceContextSystemUserProvider.getContextSystemUserId()
                    .orElseThrow(() -> new WebApplicationException("System user for progression context not found"));

            final Optional<InputStream> documentInputStream = getPdfDocument(document, systemUser);
            if (documentInputStream.isPresent()) {
                final Response.ResponseBuilder responseBuilder = status(OK)
                        .entity(documentInputStream.get())
                        .header(CONTENT_TYPE, MIME_TYPE)
                        .header(CONTENT_DISPOSITION, DISPOSITION);
                return responseBuilder.build();
            }

            return status(OK).build();
        }
    }

    private Optional<InputStream> getPdfDocument(final JsonEnvelope document, final UUID systemUser) {
        final byte[] resultOrderAsByteArray;
        try {
            final JsonObject payload = document.payloadAsJsonObject();
            if (payload.containsKey("templateName")) {
                final String templateName = payload.getString("templateName");
                LOGGER.info("Calling document generation with Courtlist payload: {}, systemUser: {}", payload, systemUser);
                resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(payload, templateName, systemUser);
                return of(new ByteArrayInputStream(resultOrderAsByteArray));
            }
        } catch (IOException e) {
            LOGGER.error("Courtlist PDF generation failed ", e);
            throw new DocumentGeneratorException();
        }
        return empty();
    }
}