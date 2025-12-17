package uk.gov.moj.cpp.progression.processor.summons;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SystemIdMapperService;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishSummonsDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishSummonsDocumentService.class);

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private CourtDocumentObjectService courtDocumentObjectService;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    public void generateCaseSummonsCourtDocument(final JsonEnvelope jsonEnvelope,
                                                 final UUID defendantId,
                                                 final UUID caseId,
                                                 final SummonsDocumentContent summonsDocumentContent,
                                                 final String templateName,
                                                 final boolean sendForRemotePrinting,
                                                 final EmailChannel emailChannel,
                                                 final UUID materialId) {
        LOGGER.info("Generating '{}' summons for defendant '{}' for case '{}' and sendForRemotePrinting status is '{}'", templateName, defendantId, caseId, sendForRemotePrinting);
        logEmailNotificationStatusForCaseSummons(emailChannel, defendantId);
        generateSummonsDocument(jsonEnvelope, caseId, null, summonsDocumentContent, templateName, sendForRemotePrinting, emailChannel, materialId);

        final CourtDocument courtDocument = courtDocumentObjectService.buildCaseSummonsCourtDocument(caseId, defendantId, materialId, jsonEnvelope);
        progressionService.createCourtDocument(jsonEnvelope, singletonList(courtDocument));

        mapMaterialIdToDocumentId(courtDocument.getCourtDocumentId(), materialId);
    }

    public void generateApplicationSummonsCourtDocument(final JsonEnvelope jsonEnvelope,
                                                        final UUID applicationId,
                                                        final SummonsDocumentContent summonsDocumentContent,
                                                        final String templateName,
                                                        final boolean sendForRemotePrinting,
                                                        final EmailChannel emailChannel,
                                                        final UUID materialId) {
        LOGGER.info("Generating '{}' summons for subject for application '{}' and sendForRemotePrinting status is '{}'", templateName, applicationId, sendForRemotePrinting);
        logEmailNotificationStatusForApplicationSummons(emailChannel, applicationId);
        generateSummonsDocument(jsonEnvelope, null, applicationId, summonsDocumentContent, templateName, sendForRemotePrinting, emailChannel, materialId);

        final CourtDocument courtDocument = courtDocumentObjectService.buildApplicationSummonsCourtDocument(applicationId, materialId, jsonEnvelope);
        progressionService.createCourtDocument(jsonEnvelope, singletonList(courtDocument));

        mapMaterialIdToDocumentId(courtDocument.getCourtDocumentId(), materialId);
    }

    private void generateSummonsDocument(final JsonEnvelope jsonEnvelope,
                                         final UUID caseId,
                                         final UUID applicationId,
                                         final SummonsDocumentContent summonsDocumentContent,
                                         final String templateName,
                                         final boolean sendForRemotePrinting,
                                         final EmailChannel emailChannel,
                                         final UUID materialId) {
        documentGeneratorService.generateSummonsDocument(jsonEnvelope,
                objectToJsonObjectConverter.convert(summonsDocumentContent),
                templateName,
                sender,
                caseId,
                applicationId,
                sendForRemotePrinting,
                emailChannel,
                materialId);
    }

    private void logEmailNotificationStatusForCaseSummons(final EmailChannel emailChannel, final UUID defendantId) {
        if (isNull(emailChannel)) {
            LOGGER.info("Not generating email notification for defendant '{}'", defendantId);
            return;
        }
        if (isNull(emailChannel.getMaterialUrl())) {
            LOGGER.info("Generating email notification for defendant '{}' without attachment", defendantId);
            return;
        }
        LOGGER.info("Generating email notification for defendant '{}' with attachment", defendantId);
    }

    private void logEmailNotificationStatusForApplicationSummons(final EmailChannel emailChannel, final UUID applicationId) {
        if (isNull(emailChannel)) {
            LOGGER.info("Not generating email notification for application '{}'", applicationId);
            return;
        }
        if (isNull(emailChannel.getMaterialUrl())) {
            LOGGER.info("Generating email notification for application '{}' without attachment", applicationId);
            return;
        }
        LOGGER.info("Generating email notification for application '{}' with attachment", applicationId);
    }

    private void mapMaterialIdToDocumentId(final UUID documentId, final UUID materialId) {
        systemIdMapperService.mapMaterialIdToDocumentId(documentId, materialId);
    }
}
