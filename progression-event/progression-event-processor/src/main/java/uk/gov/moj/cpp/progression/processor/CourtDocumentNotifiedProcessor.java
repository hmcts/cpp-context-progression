package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.CpsEmailNotificationService;
import uk.gov.moj.cpp.progression.service.CpsRestNotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.CourtDocumentTransformer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentNotifiedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentNotifiedProcessor.class.getCanonicalName());
    private static final String FEATURE_DEFENCE_DISCLOSURE = "defenceDisclosure";
    private static final String COTR_FORM_SERVED_NOTIFICATION = "cotr-form-served";
    private static final String OPA_FORM_SUBMITTED ="opa-form-submitted";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private CourtDocumentTransformer courtDocumentTransformer;

    @Inject
    private CpsEmailNotificationService cpsEmailNotificationService;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Inject
    private CpsRestNotificationService cpsRestNotificationService;

    @Handles("progression.event.court-document-send-to-cps")
    public void processCourtDocumentSendToCPS(final JsonEnvelope envelope) {
        LOGGER.info("processCourtDocumentSendToCPS sendToCPS is called and payload is {}", envelope);
        final JsonObject event = envelope.payloadAsJsonObject();
        final CourtDocument courtDocument = jsonObjectConverter.convert(event.getJsonObject("courtDocument"), CourtDocument.class);
        final String notificationType = event.getString("notificationType", null);
        final List<UUID> caseIds = getLinkedCaseIds(courtDocument.getDocumentCategory());
        final UUID prosecutionCaseId = caseIds.get(0);

        final Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(envelope, prosecutionCaseId.toString());

        if (prosecutionCaseOptional.isPresent()) {

            if (shouldNotifyCPS(courtDocument)) {
                final Optional<String> transformedJsonPayload = courtDocumentTransformer.transform(courtDocument, prosecutionCaseOptional, envelope,notificationType);
                if (transformedJsonPayload.isPresent()) {
                    LOGGER.info("Event court-document-send-to-cps triggered and API-M notification is enabled");
                    cpsRestNotificationService.sendMaterial(transformedJsonPayload.get(), courtDocument.getCourtDocumentId(), envelope);
                } else {
                    LOGGER.info("Event court-document-send-to-cps triggered but no payload available");
                }

            } else {
                cpsEmailNotificationService.sendEmailToCps(envelope, courtDocument, prosecutionCaseId, prosecutionCaseOptional.get());
            }
        }
    }

    private List<UUID> getLinkedCaseIds(final DocumentCategory documentCategory) {

        if (nonNull(documentCategory.getNowDocument())) {
            return documentCategory.getNowDocument().getProsecutionCases();
        } else if (nonNull(documentCategory.getCaseDocument())) {
            return singletonList(documentCategory.getCaseDocument().getProsecutionCaseId());
        } else if (nonNull(documentCategory.getDefendantDocument())) {
            return singletonList(documentCategory.getDefendantDocument().getProsecutionCaseId());
        } else if (nonNull(documentCategory.getApplicationDocument())) {
            if (null != documentCategory.getApplicationDocument().getProsecutionCaseId()) {
                return singletonList(documentCategory.getApplicationDocument().getProsecutionCaseId());
            } else {
                return emptyList();
            }
        } else {
            return emptyList();
        }
    }

    private boolean shouldNotifyCPS(final  CourtDocument courtDocument) {
        if(nonNull(courtDocument.getNotificationType()) &&
                (courtDocument.getNotificationType().equalsIgnoreCase(COTR_FORM_SERVED_NOTIFICATION)||
                        courtDocument.getNotificationType().equalsIgnoreCase(OPA_FORM_SUBMITTED))) {
            return true;
        }
        return  featureControlGuard.isFeatureEnabled(FEATURE_DEFENCE_DISCLOSURE);
    }
}