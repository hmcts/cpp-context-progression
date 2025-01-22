package uk.gov.moj.cpp.progression.processor.document;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.DocumentWithProsecutionCaseIdAdded;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.AddDocumentWithProsecutionCaseId;
import uk.gov.moj.cpp.progression.domain.helper.JsonHelper;
import uk.gov.moj.cpp.progression.service.CpsRestNotificationService;
import uk.gov.moj.cpp.progression.service.DefenceNotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.service.payloads.UserGroupDetails;
import uk.gov.moj.cpp.progression.transformer.CourtDocumentTransformer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentAddedProcessor {

    public static final String DOCUMENT_ADDED_WITH_PROSECUTION_CASE_ID_COMMAND = "progression.command.add-document-with-prosecution-case-id";
    public static final String PUBLIC_COURT_DOCUMENT_ADDED = "public.progression.court-document-added";
    public static final String PUBLIC_DOCUMENT_ADDED = "public.progression.document-added";
    public static final UUID IDPC_DOCUMENT_TYPE_ID = fromString("41be14e8-9df5-4b08-80b0-1e670bc80a5b");
    public static final String SECTION = "section";
    protected static final String PUBLIC_IDPC_COURT_DOCUMENT_RECEIVED = "public.progression.idpc-document-received";
    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    protected static final String PROGRESSION_COMMAND_UPDATE_CASE_FOR_CPS_PROSECUTOR = "progression.command.update-case-for-cps-prosecutor";
    private static final String IS_CPS_CASE = "isCpsCase";
    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String IS_NOTIFY_DEFENCE = "notifyDefence";
    private static final String CASE_ID = "caseId";
    static final String FEATURE_DEFENCE_DISCLOSURE = "defenceDisclosure";
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
    private RefDataService referenceDataService;

    @Inject
    private Requester requester;

    @Inject
    private CourtDocumentTransformer courtDocumentTransformer;

    @Inject
    private CpsRestNotificationService cpsRestNotificationService;

    @Inject
    private FeatureControlGuard featureControlGuard;

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentAddedProcessor.class);

    @Handles("progression.event.court-document-added")
    public void handleCourtDocumentAddEvent(final JsonEnvelope envelope) {
        final CourtsDocumentAdded courtsDocumentAdded = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtsDocumentAdded.class);
        final CourtDocument courtDocument = courtsDocumentAdded.getCourtDocument();
        JsonObject payload = envelope.payloadAsJsonObject();
        Optional<JsonObject> prosecutor = Optional.empty();

        final Optional<UUID> caseId = getCaseIdFromDocuments(courtDocument.getDocumentCategory());
        if (caseId.isPresent()) {
            prosecutor = getProsecutor(envelope, caseId.get());
            LOGGER.info("prosecutor - {}", prosecutor);
            sendUpdateCaseForCpsProsecutionCommandWhenProsecutorIsAbsent(envelope, payload, prosecutor, caseId.get());
        }

        if (nonNull(payload.get(IS_UNBUNDLED_DOCUMENT))) {
            payload = JsonHelper.removeProperty(payload, IS_UNBUNDLED_DOCUMENT);
        }

        payload = sendCreateCourtDocumentCommandAndRemoveCpsCaseFlag(envelope, payload, prosecutor);

        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_COURT_DOCUMENT_ADDED).build();
        sender.send(uk.gov.justice.services.messaging.Envelope.envelopeFrom(metadata, payload));

        if (requiresEmailNotification(envelope, courtDocument)) {
            final JsonObject documentTypeData = getDocumentTypeData(envelope, courtDocument.getDocumentTypeId());
            if (isNotifyDefence(documentTypeData) && isBundled(envelope)) {
                defenceNotificationService.prepareNotificationsForCourtDocument(envelope, courtDocument, getDocumentSection(documentTypeData), courtDocument.getName());
            }
        }

        sendIdpcCourtDocumentPublicEvent(envelope, courtsDocumentAdded, courtDocument);

        if (caseId.isPresent()) {
            final Metadata metadataPrivate = Envelope.metadataFrom(envelope.metadata()).withName(DOCUMENT_ADDED_WITH_PROSECUTION_CASE_ID_COMMAND).build();
            sender.send(uk.gov.justice.services.messaging.Envelope.envelopeFrom(metadataPrivate, createAddDocumentWithProsecutionCaseId(caseId.get(), courtDocument)));
        }

        if (featureControlGuard.isFeatureEnabled(FEATURE_DEFENCE_DISCLOSURE) &&
                nonNull(courtDocument.getDocumentCategory()) &&
                nonNull(courtDocument.getDocumentCategory().getApplicationDocument())) {

            final UUID applicationId = courtDocument.getDocumentCategory().getApplicationDocument().getApplicationId();
            caseId.ifPresent(caseID -> sendNotificationToCpsIfApplicantOrRespondentIsCps(envelope, courtDocument, applicationId, caseID));

        }
    }

    @Handles("progression.event.document-with-prosecution-case-id-added")
    public void handleAddDocumentWithProsecutionCaseId(final JsonEnvelope envelope) {
        final DocumentWithProsecutionCaseIdAdded documentAdded = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), DocumentWithProsecutionCaseIdAdded.class);
        final ProsecutionCase prosecutionCase = documentAdded.getProsecutionCase();

        if (prosecutionCase == null) {
            LOGGER.info("Prosecution case is null. courtDocumentId:{}", documentAdded.getCourtDocument() != null ? documentAdded.getCourtDocument().getCourtDocumentId() : "");
            return;
        }

        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(PUBLIC_DOCUMENT_ADDED).build();

        sender.send(envelopeFrom(metadata, createPayloadFromCourtDocumentAddedToCasePublicEvent (prosecutionCase, documentAdded.getCourtDocument())));

    }

    private void sendNotificationToCpsIfApplicantOrRespondentIsCps(final JsonEnvelope envelope, final CourtDocument courtDocument, final UUID applicationId, final UUID prosecutionCaseId) {
        final Optional<CourtApplication> courtApplicationOptional = progressionService.getCourtApplicationById(envelope, applicationId.toString())
                .map(caJson -> jsonObjectConverter.convert(caJson.getJsonObject("courtApplication"), CourtApplication.class));

        if (courtApplicationOptional.isPresent()) {
            final Optional<JsonArray> cpsProsecutors = referenceDataService.getCPSProsecutors(envelope, requester);
            if (cpsProsecutors.isPresent() && isApplicantOrRespondentCps(courtApplicationOptional.get(), toProsecutorIdList(cpsProsecutors.get()))) {

                final Optional<JsonObject> prosecutionCaseOptional = nonNull(prosecutionCaseId) ? progressionService.getProsecutionCaseDetailById(envelope, prosecutionCaseId.toString()) : Optional.empty();

                final Optional<String> transformedJsonPayload = courtDocumentTransformer.transform(courtDocument, prosecutionCaseOptional, envelope, "application-document");
                transformedJsonPayload.ifPresent(s -> cpsRestNotificationService.sendMaterialWithCourtDocument(s,courtDocument.getCourtDocumentId(),envelope));
            }
        }
    }

    private boolean isApplicantOrRespondentCps(final CourtApplication courtApplication, final List<UUID> cpsProsecutorIdList) {

        //check applicant CPS
        if (nonNull(courtApplication.getApplicant())
                && nonNull(courtApplication.getApplicant().getProsecutingAuthority())
                && nonNull(courtApplication.getApplicant().getProsecutingAuthority().getProsecutionAuthorityId())) {

            return cpsProsecutorIdList.contains(courtApplication.getApplicant().getProsecutingAuthority().getProsecutionAuthorityId());
        }
        //check applicant CPS
        if (nonNull(courtApplication.getRespondents()) && !courtApplication.getRespondents().isEmpty()) {
            return courtApplication.getRespondents().stream()
                    .filter(resp -> nonNull(resp.getProsecutingAuthority()))
                    .anyMatch(resp -> cpsProsecutorIdList.contains(resp.getProsecutingAuthority().getProsecutionAuthorityId()));
        }
        return false;
    }

    private List<UUID> toProsecutorIdList(JsonArray cpsProsecutorsJsonArray) {
        return cpsProsecutorsJsonArray.getValuesAs(JsonObject.class).stream()
                .map(p -> (p.getString("id")))
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    private void sendIdpcCourtDocumentPublicEvent(final JsonEnvelope envelope, final CourtsDocumentAdded courtsDocumentAdded, final CourtDocument courtDocument) {
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

    private void sendUpdateCaseForCpsProsecutionCommandWhenProsecutorIsAbsent(final JsonEnvelope envelope, final JsonObject payload, final Optional<JsonObject> prosecutor, final UUID caseId) {
        if (payload.get(IS_CPS_CASE) != null && "true".equals(payload.get(IS_CPS_CASE).toString()) && !prosecutor.isPresent()) {
            sender.send(Enveloper.envelop(createObjectBuilder().add(CASE_ID, caseId.toString()).build()).withName(PROGRESSION_COMMAND_UPDATE_CASE_FOR_CPS_PROSECUTOR).withMetadataFrom(envelope));
        }
    }

    private JsonObject sendCreateCourtDocumentCommandAndRemoveCpsCaseFlag(final JsonEnvelope envelope, final JsonObject payload, final Optional<JsonObject> prosecutor) {
        boolean isCpsCaseFlag = false;
        if (prosecutor.isPresent()) {
            final Optional<JsonObject> prosecutorJsonObject = referenceDataService.getProsecutorV2(envelope, fromString(prosecutor.get().getString("prosecutorId")), requester);
            if (prosecutorJsonObject.isPresent() && prosecutorJsonObject.get().getBoolean("cpsFlag", false)) {
                isCpsCaseFlag = true;
            }
        }

        final JsonObject updatedPayload = JsonHelper.addProperty(payload, IS_CPS_CASE, isCpsCaseFlag);
        sender.send(Enveloper.envelop(updatedPayload).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).withMetadataFrom(envelope));

        return JsonHelper.removeProperty(payload, IS_CPS_CASE);
    }

    private Optional<JsonObject> getProsecutor(final JsonEnvelope envelope, UUID caseId) {
        final Optional<JsonObject> prosecutionCaseDetailById = progressionService.getProsecutionCaseDetailById(envelope, caseId.toString());
        return prosecutionCaseDetailById.map(jsonObject -> jsonObject.getJsonObject(PROSECUTION_CASE).getJsonObject("prosecutor"));
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
                .add(CASE_ID, caseId.toString())
                .add("materialId", material.getId().toString())
                .add("defendantId", defendantId.toString())
                .add("publishedDate", material.getReceivedDateTime().toLocalDate().toString())
                .build();
    }

    private JsonObject createPayloadFromCourtDocumentAddedToCasePublicEvent(final ProsecutionCase prosecutionCase, final CourtDocument courtDocument) {
        final StringBuilder defendantName = new StringBuilder();
        final StringBuilder defendantId = new StringBuilder();
        final StringBuilder caseUrn = new StringBuilder();

        caseUrn.append(prosecutionCase.getProsecutionCaseIdentifier() != null ? prosecutionCase.getProsecutionCaseIdentifier().getCaseURN() : "");
        final List<Defendant> defendants = prosecutionCase.getDefendants();
        if (defendants != null) {
            for (final Defendant defendant : defendants) {
                defendantId.append(defendant.getId()).append(",");
                if (defendant.getPersonDefendant() != null && defendant.getPersonDefendant().getPersonDetails() != null) {
                    defendantName.append(defendant.getPersonDefendant().getPersonDetails().getFirstName()).append(" ").append(defendant.getPersonDefendant().getPersonDetails().getLastName()).append(",");
                }
            }
        }

        return createObjectBuilder()
                .add("caseURN", caseUrn.toString())
                .add(CASE_ID, prosecutionCase.getId().toString())
                .add("defendantId", defendantId.toString())
                .add("defendantName", defendantName.toString())
                .add("documentTypeId", courtDocument.getDocumentTypeId().toString())
                .build();
    }

    private AddDocumentWithProsecutionCaseId createAddDocumentWithProsecutionCaseId(final UUID caseId, final CourtDocument courtDocument) {
        return AddDocumentWithProsecutionCaseId.
                addDocumentWithProsecutionCaseId()
                .withCaseId(caseId)
                .withCourtDocument(courtDocument)
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
        return empty();
    }
}





