package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.lang.String.format;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.RefDataDefinitionException;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

public class DocumentTypeAccessProvider {

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private DocumentTypeAccessConverter documentTypeAccessConverter;

    @Inject
    private Logger logger;

    public DocumentTypeAccess getDocumentTypeAccess(final CourtDocument courtDocument, final JsonEnvelope defaultCourtDocumentEnvelope) {
        logger.info("document type access {}", courtDocument);
        final UUID documentTypeId = courtDocument.getDocumentTypeId();
        logger.info("document type id ", documentTypeId);
        final JsonObject documentTypeData = referenceDataService.getDocumentTypeAccessData(
                documentTypeId,
                defaultCourtDocumentEnvelope,
                requester)
                .orElseThrow(() -> new RefDataDefinitionException(format("No DocumentTypeAccess with id '%s' found in referencedata context", documentTypeId)));

        return documentTypeAccessConverter.toDocumentTypeAccess(documentTypeData);
    }
}
