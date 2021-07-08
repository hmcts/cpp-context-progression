package uk.gov.moj.cpp.progression.processor.document;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.helper.JsonHelper;
import uk.gov.moj.cpp.progression.service.DefenceNotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.service.payloads.UserGroupDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentAddedProcessor {

    public static final String PUBLIC_COURT_DOCUMENT_ADDED = "public.progression.court-document-added";
    protected static final String PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED = "public.progression.idpc-document-received";
    public static final UUID IDPC_DOCUMENT_TYPE_ID = fromString("41be14e8-9df5-4b08-80b0-1e670bc80a5b");
    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    protected static final String PROGRESSION_COMMAND_UPDATE_CASE_FOR_CPS_PROSECUTOR = "progression.command.update-case-for-cps-prosecutor";
    private static final String IS_CPS_CASE = "isCpsCase";
    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String IS_NOTIFY_DEFENCE = "notifyDefence";
    public static final String SECTION = "section";

    @Inject
    private Sender sender;

    @Inject
    private UsersGroupService usersGroupService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private DefenceNotificationService defenceNotificationService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private Requester requester;


    private boolean isProsecutorExists(final JsonEnvelope envelope, UUID caseId) {
        final Optional<JsonObject> prosecutionCaseDetailById = progressionService.getProsecutionCaseDetailById(envelope, caseId.toString());
        return prosecutionCaseDetailById.isPresent() && nonNull(prosecutionCaseDetailById.get().getJsonObject(PROSECUTION_CASE).getJsonObject("prosecutor"));
    }

    @Handles("progression.event.court-document-added")
    public void handleCourtDocumentAddEvent(final JsonEnvelope envelope) {
        final CourtsDocumentAdded courtsDocumentAdded = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtsDocumentAdded.class);
        final CourtDocument courtDocument = courtsDocumentAdded.getCourtDocument();
        JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.get(IS_CPS_CASE) != null && "true".equals(payload.get(IS_CPS_CASE).toString())) {
            final Optional<UUID> caseId = getCaseIdFromDocuments(courtDocument.getDocumentCategory());
            caseId.ifPresent(id -> {
                if (!isProsecutorExists(envelope, id)) {
                    sender.send(Enveloper.envelop(createObjectBuilder().add("caseId", id.toString()).build()).withName(PROGRESSION_COMMAND_UPDATE_CASE_FOR_CPS_PROSECUTOR).withMetadataFrom(envelope));
                }
            });
        }
        if (envelope.payloadAsJsonObject().get(IS_CPS_CASE) != null) {
            payload = JsonHelper.removeProperty(payload, IS_CPS_CASE);
        }
        if (nonNull(envelope.payloadAsJsonObject().get(IS_UNBUNDLED_DOCUMENT))) {
            payload = JsonHelper.removeProperty(payload, IS_UNBUNDLED_DOCUMENT);
        }
        sender.send(Enveloper.envelop(payload).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).withMetadataFrom(envelope));
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_COURT_DOCUMENT_ADDED).build();
        sender.send(uk.gov.justice.services.messaging.Envelope.envelopeFrom(metadata, payload));
        if (requiresEmailNotification(envelope, courtDocument)) {
            final JsonObject documentTypeData = getDocumentTypeData(envelope, courtDocument.getDocumentTypeId());
            if (isNotifyDefence(documentTypeData) && isBundled(envelope)) {
                defenceNotificationService.prepareNotificationsForCourtDocument(envelope, courtDocument, getDocumentSection(documentTypeData),courtDocument.getName());
            }
        }

        if (courtsDocumentAdded.getCourtDocument().getDocumentTypeId().equals(IDPC_DOCUMENT_TYPE_ID)) {
            final DefendantDocument defendantDocument = courtDocument.getDocumentCategory().getDefendantDocument();
            final List<UUID> defendantIds = defendantDocument.getDefendants();
            final List<Material> materials = courtDocument.getMaterials();
            final Metadata idpcMetadata = metadataFrom(envelope.metadata())
                    .withName(PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED)
                    .build();
            defendantIds
                    .forEach(defendantId ->
                            materials
                                    .forEach(material ->
                                            sender.send(envelopeFrom(idpcMetadata,
                                                    createIDPCReceivedBody(material, defendantDocument.getProsecutionCaseId(), defendantId)))));
        }
    }

    private String getDocumentSection(final JsonObject documentTypeData) {
        return documentTypeData.getString(SECTION);
    }

    private boolean isNotifyDefence(final JsonObject documentTypeData) {
        return documentTypeData.getBoolean(IS_NOTIFY_DEFENCE, false);
    }

    private boolean isBundled(final JsonEnvelope envelope) {
        return !envelope.payloadAsJsonObject().getBoolean(IS_UNBUNDLED_DOCUMENT, false);

    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private boolean requiresEmailNotification(final JsonEnvelope envelope, final CourtDocument courtDocument) {
        return isBundled(envelope) &&
                isCaseDocumentOrDefendantDocument(courtDocument.getDocumentCategory())
                &&
                isNonDefenceUser(envelope)
                &&
                hasReadPermissionForDefenceLawyers(courtDocument.getDocumentTypeRBAC());
    }

    private JsonObject getDocumentTypeData(final JsonEnvelope envelope, final UUID documentTypeId) {
        return referenceDataService.getDocumentTypeAccessData(documentTypeId, envelope, requester)
                .orElseThrow(() -> new RuntimeException("Unable to retrieve document type details for '" + documentTypeId + "'"));
    }

    private boolean isNonDefenceUser(final JsonEnvelope envelope) {
        final List<String> userGroups = usersGroupService.getUserGroupsForUser(envelope).stream().map(UserGroupDetails::getGroupName).collect(Collectors.toList());
        return !userGroups.contains("Defence Lawyers") && !userGroups.contains("Advocates") && !userGroups.contains("Chambers Admin") && !userGroups.contains("Chambers Clerk");
    }

    private boolean hasReadPermissionForDefenceLawyers(final DocumentTypeRBAC documentTypeRBAC) {
        final List<String> readAccessGroups = documentTypeRBAC.getReadUserGroups();
        return isNotEmpty(readAccessGroups) && readAccessGroups.contains("Defence Lawyers");

    }

    private boolean isCaseDocumentOrDefendantDocument(final DocumentCategory documentCategory) {
        return (null != documentCategory.getCaseDocument()) || (null != documentCategory.getDefendantDocument());
    }

    private JsonObject createIDPCReceivedBody(final Material material, final UUID caseId, UUID defendantId) {
        return createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("materialId", material.getId().toString())
                .add("defendantId", defendantId.toString())
                .add("publishedDate", material.getReceivedDateTime().toLocalDate().toString())
                .build();
    }

    private Optional<UUID> getCaseIdFromDocuments(DocumentCategory documentCategory) {
        final CaseDocument caseDocument = documentCategory.getCaseDocument();
        if (caseDocument != null) {
            return Optional.of(caseDocument.getProsecutionCaseId());
        }
        final DefendantDocument defendantDocument = documentCategory.getDefendantDocument();
        if (defendantDocument != null) {
            return Optional.of(defendantDocument.getProsecutionCaseId());
        }
        return Optional.empty();
    }
}





