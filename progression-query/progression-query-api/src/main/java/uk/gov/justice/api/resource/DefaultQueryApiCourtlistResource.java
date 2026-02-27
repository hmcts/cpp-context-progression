package uk.gov.justice.api.resource;

import static java.util.Optional.of;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;

import uk.gov.moj.cpp.progression.query.api.service.CourtlistQueryService;
import uk.gov.justice.api.resource.service.StagingPubHubService;
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
import javax.json.JsonObject;
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

    public static final String USHERS_CROWN = "USHERS_CROWN";
    public static final String USHERS_MAGISTRATE = "USHERS_MAGISTRATE";
    public static final String PRISON_COURT_LIST = "PRISON";
    protected static final String COURT_LIST_QUERY_NAME = "progression.search.court.list";
    protected static final String PRISON_COURT_LIST_QUERY_NAME = "progression.search.prison.court.list";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryApiCourtlistResource.class);
    private static final String EXTRACT_FILE_NAME = "CourtList.pdf";
    private static final String EXTRACT_WORD_FILE_NAME = "UshersList.docx";
    protected static final String PDF_DISPOSITION = "attachment; filename=\"" + EXTRACT_FILE_NAME + "\"";
    protected static final String WORD_DISPOSITION = "attachment; filename=\"" + EXTRACT_WORD_FILE_NAME + "\"";
    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String WORD_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";


    @Context
    HttpHeaders headers;

    @Inject
    @Named("DefaultQueryApiCourtlistResourceActionMapper")
    ActionMapper actionMapper;

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Inject
    private CourtlistQueryService courtlistQueryService;

    @Inject
    private InterceptorChainProcessor interceptorChainProcessor;

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private StagingPubHubService stagingPubHubService;

    @Override
    public Response getCourtlist(final String courtCentreId, final String courtRoomId, final String listId,
                                 final String startDate, final String endDate, final boolean restricted, final UUID userId) {
        if (PRISON_COURT_LIST.equals(listId)) {
            return Response.status(FORBIDDEN).build();
        }
        return getCourtListInternal(courtCentreId, courtRoomId, listId, startDate, endDate, restricted, userId, COURT_LIST_QUERY_NAME);
    }

    @Override
    public Response getPrisonCourtlist(final String courtCentreId, final String courtRoomId, final String startDate, final String endDate, final UUID userId) {
        return getCourtListInternal(courtCentreId, courtRoomId, PRISON_COURT_LIST, startDate, endDate, false, userId, PRISON_COURT_LIST_QUERY_NAME);
    }

    private Response getCourtListInternal(final String courtCentreId, final String courtRoomId, final String listId, final String startDate, final String endDate, final boolean restricted, final UUID userId, final String courtListAction) {
        final JsonEnvelope queryEnvelope = courtlistQueryService.buildCourtlistQueryEnvelope(
                courtCentreId, courtRoomId, listId, startDate, endDate, restricted, userId, courtListAction);
        final JsonEnvelope document = interceptorChainProcessor.process(interceptorContextWithInput(queryEnvelope)).get();

        final JsonObject enrichedPayload = courtlistQueryService.buildEnrichedPayload(document);

        if (!PRISON_COURT_LIST.equalsIgnoreCase(listId)) {
            stagingPubHubService.publishStandardList(enrichedPayload, userId);
        }

        return getDocumentContent(document);
    }


    private Response getDocumentContent(final JsonEnvelope document) {
        if (JsonValue.NULL.equals(document.payload())) {
            return null;
        } else {
            final UUID systemUser = serviceContextSystemUserProvider.getContextSystemUserId()
                    .orElseThrow(() -> new WebApplicationException("System user for progression context not found"));
            final JsonObject payload = document.payloadAsJsonObject();
            final String templateName = payload.getString("templateName");
            final String listType = payload.getString("listType");
            final Optional<InputStream> documentInputStream;
            Response.ResponseBuilder responseBuilder;
            if (USHERS_CROWN.equalsIgnoreCase(listType) || USHERS_MAGISTRATE.equalsIgnoreCase(listType)) {
                documentInputStream = getWordDocument(payload, templateName, systemUser);
                responseBuilder = status(OK)
                        .entity(documentInputStream.get())
                        .header(CONTENT_TYPE, WORD_MIME_TYPE)
                        .header(CONTENT_DISPOSITION, WORD_DISPOSITION);
            } else {
                documentInputStream = getPdfDocument(payload, templateName, systemUser);
                responseBuilder = status(OK)
                        .entity(documentInputStream.get())
                        .header(CONTENT_TYPE, PDF_MIME_TYPE)
                        .header(CONTENT_DISPOSITION, PDF_DISPOSITION);
            }
            return responseBuilder.build();
        }
    }


    private Optional<InputStream> getPdfDocument(final JsonObject payload, final String templateName, final UUID systemUser) {
        final byte[] resultOrderAsByteArray;
        try {
            LOGGER.info("Calling document generation with Court List payload: {}, systemUser: {}", payload, systemUser);
            resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(payload, templateName, systemUser);
            return of(new ByteArrayInputStream(resultOrderAsByteArray));

        } catch (IOException e) {
            LOGGER.error("Court List PDF generation failed ", e);
            throw new DocumentGeneratorException();
        }
    }

    private Optional<InputStream> getWordDocument(final JsonObject payload, final String templateName, final UUID systemUser) {
        final byte[] resultOrderAsByteArray;
        try {
            LOGGER.info("Calling document generation with UsersList payload: {}, systemUser: {}", payload, systemUser);
            resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generateWordDocument(payload, templateName, systemUser);
            return of(new ByteArrayInputStream(resultOrderAsByteArray));

        } catch (IOException e) {
            LOGGER.error("UshersList docx generation failed ", e);
            throw new DocumentGeneratorException();
        }
    }
}