package uk.gov.moj.cpp.progression.processor.document;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import java.util.Optional;
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
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.helper.JsonHelper;
import uk.gov.moj.cpp.progression.service.DefenceNotificationService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.service.payloads.UserGroupDetails;

import java.util.List;
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
    protected static final String PROGRESSION_COMMAND_UPDATE_CASE_FOR_CPS = "progression.command.update-case-for-cps-prosecutor";
    private static final String IS_CPS_CASE = "isCpsCase";

    @Inject
    private Sender sender;

    @Inject
    private UsersGroupService usersGroupService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private DefenceNotificationService defenceNotificationService;


    @Handles("progression.event.court-document-added")
    public void handleCourtDocumentAddEvent(final JsonEnvelope envelope) {
        final CourtsDocumentAdded courtsDocumentAdded = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtsDocumentAdded.class);
        JsonObject payload = envelope.payloadAsJsonObject();
        if(payload.get(IS_CPS_CASE) != null && "true".equals(payload.get(IS_CPS_CASE).toString())){
            final Optional<UUID> caseId = getCaseIdFromDocuments(courtsDocumentAdded.getCourtDocument().getDocumentCategory());
            caseId.ifPresent(id -> sender.send(Enveloper.envelop(createObjectBuilder().add("caseId", id.toString()).build()).withName(PROGRESSION_COMMAND_UPDATE_CASE_FOR_CPS).withMetadataFrom(envelope)));
        }
        if (envelope.payloadAsJsonObject().get(IS_CPS_CASE) != null) {
            payload = JsonHelper.removeProperty( payload , IS_CPS_CASE);
        }
        sender.send(Enveloper.envelop(payload).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).withMetadataFrom(envelope));
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_COURT_DOCUMENT_ADDED).build();
        sender.send(uk.gov.justice.services.messaging.Envelope.envelopeFrom(metadata, payload));
       if(requiresEmailNotification(envelope, courtsDocumentAdded.getCourtDocument())) {
           defenceNotificationService.prepareNotificationsForCourtDocument(envelope, courtsDocumentAdded.getCourtDocument());
       }

        if(courtsDocumentAdded.getCourtDocument().getDocumentTypeId().equals(IDPC_DOCUMENT_TYPE_ID)) {
            final DefendantDocument courtDocument = courtsDocumentAdded.getCourtDocument().getDocumentCategory().getDefendantDocument();
            final List<UUID> defendantIds = courtDocument.getDefendants();
            final List<Material> materials = courtsDocumentAdded.getCourtDocument().getMaterials();
            final Metadata idpcMetadata = metadataFrom(envelope.metadata())
                    .withName(PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED)
                    .build();
            defendantIds
                    .forEach(defendantId ->
                            materials
                                    .forEach(material ->
                                            sender.send(envelopeFrom(idpcMetadata,
                                                    createIDPCReceivedBody(material, courtDocument.getProsecutionCaseId(), defendantId)))));
        }
    }

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private boolean requiresEmailNotification(final JsonEnvelope envelope, final CourtDocument courtDocument) {
        return isCaseDocumentOrDefendantDocument(
                courtDocument.getDocumentCategory())
                &&
                isNonDefenceUser(envelope)
                &&
                hasReadPermissionForDefenceLawyers(courtDocument.getDocumentTypeRBAC());

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

    private Optional<UUID> getCaseIdFromDocuments(DocumentCategory documentCategory){
        final CaseDocument caseDocument = documentCategory.getCaseDocument();
        if(caseDocument != null ){
            return Optional.of(caseDocument.getProsecutionCaseId());
        }
        final DefendantDocument defendantDocument = documentCategory.getDefendantDocument();
        if(defendantDocument != null){
            return Optional.of(defendantDocument.getProsecutionCaseId());
        }
        return Optional.empty();
    }
}





