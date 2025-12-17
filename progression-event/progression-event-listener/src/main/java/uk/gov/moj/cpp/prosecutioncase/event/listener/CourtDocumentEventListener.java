package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static uk.gov.justice.core.courts.CourtDocument.courtDocument;
import static uk.gov.justice.core.courts.Material.material;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentPrintTimeUpdated;
import uk.gov.justice.core.courts.CourtDocumentUpdated;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.CourtApplicationDocumentUpdated;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentTypeRBAC;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CpsSendNotificationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CpsSendNotificationRepository;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S2789", "squid:S1612"})
@ServiceComponent(EVENT_LISTENER)
public class CourtDocumentEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private CourtDocumentRepository repository;
    @Inject
    private CourtApplicationRepository applicationRepository;

    @Inject
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @Inject
    private CpsSendNotificationRepository cpsSendNotificationRepository;

    @Inject
    private CourtDocumentIndexRepository courtDocumentIndexRepository;

    @Handles("progression.event.court-document-created")
    public void processCourtDocumentCreated(final JsonEnvelope event) {
        final CourtsDocumentCreated courtsDocumentCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtsDocumentCreated.class);
        final CourtDocument courtDocument = courtsDocumentCreated.getCourtDocument();

        repository.save(getCourtDocumentEntity(courtDocument));

        final List<Material> materials = courtDocument.getMaterials();
        if (materials != null && !materials.isEmpty()) {
            materials.forEach(material ->
                    courtDocumentMaterialRepository
                            .save(getCourtDocumentMaterialEntity(material, courtDocument.getCourtDocumentId()))
            );
        }
    }

    @Handles("progression.event.send-to-cps-flag-updated")
    public void processSendToCpsFlagUpdated(final JsonEnvelope event){
        final JsonObject jsonObject = event.payloadAsJsonObject();
        LOGGER.info("progression.event.send-to-cps-flag-updated - {}", jsonObject);

        final CpsSendNotificationEntity cpsSendNotificationEntity = new CpsSendNotificationEntity();
        cpsSendNotificationEntity.setCourtDocumentId(UUID.fromString(jsonObject.getJsonString("courtDocumentId").getString()));
        cpsSendNotificationEntity.setSendToCps(jsonObject.getBoolean("sendToCps"));
        cpsSendNotificationRepository.save(cpsSendNotificationEntity);
    }

    @Handles("progression.event.court-document-updated")
    public void processCourtDocumentUpdated(final JsonEnvelope event) {
        final CourtDocumentUpdated courtDocumentUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtDocumentUpdated.class);
        repository.save(getCourtDocumentEntity(courtDocumentUpdated.getCourtDocument()));
    }

    @Handles("progression.event.court-document-print-time-updated")
    public void processCourtDocumentPrinted(final Envelope<CourtDocumentPrintTimeUpdated> event) {
        final CourtDocumentPrintTimeUpdated courtDocumentPrintTimeUpdated = event.payload();
        final CourtDocumentEntity courtDocumentEntity = repository.findBy(courtDocumentPrintTimeUpdated.getCourtDocumentId());

        if (nonNull(courtDocumentEntity)) {
            final UUID materialId = courtDocumentPrintTimeUpdated.getMaterialId();
            final CourtDocument courtDocument = jsonObjectConverter.convert(stringToJsonObjectConverter.convert(courtDocumentEntity.getPayload()), CourtDocument.class);
            final ZonedDateTime printedAt = courtDocumentPrintTimeUpdated.getPrintedAt();
            final List<Material> materialList = courtDocument.getMaterials().stream()
                    .map(material -> material.getId().equals(materialId) ?
                            material().withValuesFrom(material).withPrintedDateTime(printedAt).build() : material)
                    .collect(toList());
            final CourtDocument updatedCourtDocument = courtDocument()
                    .withValuesFrom(courtDocument)
                    .withMaterials(materialList)
                    .build();
            courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(updatedCourtDocument).toString());
            repository.save(courtDocumentEntity);
        }
    }

    @Handles("progression.event.court-application-document-updated")
    public void processCourApplicationDocumentUpdated(final Envelope<CourtApplicationDocumentUpdated> event){
       final CourtApplicationDocumentUpdated courtApplicationDocumentUpdated = event.payload();
       courtDocumentIndexRepository.updateApplicationIdByApplicationId(courtApplicationDocumentUpdated.getApplicationId(), courtApplicationDocumentUpdated.getOldApplicationId());
    }

    private CourtDocumentEntity getCourtDocumentEntity(final CourtDocument courtDocument) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocument.getCourtDocumentId());
        courtDocumentEntity.setIndices(new HashSet<>());
        final DocumentCategory documentCategory = courtDocument.getDocumentCategory();
        final List<UUID> linkedCaseIds = getLinkedCaseIds(documentCategory);
        if (!linkedCaseIds.isEmpty()) {
            if(isStandaloneApplicationDocument(documentCategory)){
                //standalone applications produce only NowDocument and there will be only one applicationId that is passed over prosecutionCases array.
                final UUID applicationId = documentCategory.getNowDocument().getProsecutionCases().get(0);
                addCourtDocumentIndexEntity(courtDocument, courtDocumentEntity, null, applicationId);
            } else {
                linkedCaseIds.forEach(caseId -> addCourtDocumentIndexEntity(courtDocument, courtDocumentEntity, caseId, null));
            }
        }
        if (nonNull(documentCategory.getApplicationDocument())) {
            if (!linkedCaseIds.isEmpty()) {
                courtDocumentEntity
                        .getIndices()
                        .forEach(index -> index.setApplicationId(documentCategory.getApplicationDocument().getApplicationId()));
            } else {
                addCourtDocumentIndexEntity(courtDocument, courtDocumentEntity, null, documentCategory.getApplicationDocument().getApplicationId());
            }
        }
        courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(courtDocument).toString());
        courtDocumentEntity.setContainsFinancialMeans(toBooleanDefaultIfNull(courtDocument.getContainsFinancialMeans(), false));
        final DocumentTypeRBAC documentTypeRBAC = courtDocument.getDocumentTypeRBAC();
        if (documentTypeRBAC != null) {
            courtDocumentEntity.setCourtDocumentTypeRBAC(getCourtDocumentRBACEntity(documentTypeRBAC));
        }
        courtDocumentEntity.setSeqNum(courtDocument.getSeqNum());
        courtDocumentEntity.setIsRemoved(false);
        return courtDocumentEntity;
    }

    private boolean isStandaloneApplicationDocument(final DocumentCategory documentCategory) {
        if(nonNull(documentCategory.getNowDocument()) && nonNull(documentCategory.getNowDocument().getProsecutionCases()) && documentCategory.getNowDocument().getProsecutionCases().size() == 1){
            return isApplication(documentCategory.getNowDocument().getProsecutionCases().get(0));
        }
        return false;
    }

    private boolean isApplication(final UUID uuid) {
        try {
            final CourtApplicationEntity courtApplicationEntity = applicationRepository.findByApplicationId(uuid);
            return nonNull(courtApplicationEntity);
        } catch (Exception e) {
            return false;
        }
    }

    private void addCourtDocumentIndexEntity(final CourtDocument courtDocument,
                                             final CourtDocumentEntity courtDocumentEntity,
                                             final UUID prosecutionCaseId,
                                             final UUID applicationDocumentId) {
        final CourtDocumentIndexEntity index = new CourtDocumentIndexEntity();
        index.setProsecutionCaseId(prosecutionCaseId);
        index.setApplicationId(applicationDocumentId);
        index.setCourtDocument(courtDocumentEntity);
        if (nonNull(courtDocument.getDocumentCategory().getNowDocument())) {
            index.setDefendantId(courtDocument.getDocumentCategory().getNowDocument().getDefendantId());
            index.setHearingId(courtDocument.getDocumentCategory().getNowDocument().getOrderHearingId());
        }
        if (nonNull(courtDocument.getDocumentCategory().getDefendantDocument())) {
            index.setDefendantId(courtDocument.getDocumentCategory().getDefendantDocument().getDefendants().get(0));
        }
        index.setDocumentCategory(courtDocumentEntity.getDocumentCategory());
        index.setId(UUID.randomUUID());
        courtDocumentEntity.getIndices().add(index);
    }

    private List<UUID> getLinkedCaseIds(final DocumentCategory documentCategory) {

        if (nonNull(documentCategory.getNowDocument())) {
            return documentCategory.getNowDocument().getProsecutionCases();
        } else if (nonNull(documentCategory.getCaseDocument())) {
            return asList(documentCategory.getCaseDocument().getProsecutionCaseId());
        } else if (nonNull(documentCategory.getDefendantDocument())) {
            return asList(documentCategory.getDefendantDocument().getProsecutionCaseId());
        } else if (nonNull(documentCategory.getApplicationDocument())) {
            if (null != documentCategory.getApplicationDocument().getProsecutionCaseId()) {
                return asList(documentCategory.getApplicationDocument().getProsecutionCaseId());
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }


    private CourtDocumentMaterialEntity getCourtDocumentMaterialEntity(final Material material, final UUID courtDocumentId) {
        final CourtDocumentMaterialEntity courtDocumentMaterialEntity = new CourtDocumentMaterialEntity();
        courtDocumentMaterialEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentMaterialEntity.setMaterialId(material.getId());
        courtDocumentMaterialEntity.setUserGroups(material.getUserGroups());
        return courtDocumentMaterialEntity;
    }


    private CourtDocumentTypeRBAC getCourtDocumentRBACEntity(final DocumentTypeRBAC documentTypeRBAC) {
        final CourtDocumentTypeRBAC courtDocumentTypeRBAC = new CourtDocumentTypeRBAC();
        courtDocumentTypeRBAC.setCreateUserGroups(documentTypeRBAC.getUploadUserGroups());
        courtDocumentTypeRBAC.setReadUserGroups(documentTypeRBAC.getReadUserGroups());
        courtDocumentTypeRBAC.setDownloadUserGroups(documentTypeRBAC.getDownloadUserGroups());
        return courtDocumentTypeRBAC;
    }
}
