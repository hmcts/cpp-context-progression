package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S3655","squid:S2789", "squid:S1612"})
@ServiceComponent(EVENT_LISTENER)
public class CourtDocumentEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtDocumentRepository repository;

    @Inject
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

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

    private CourtDocumentEntity getCourtDocumentEntity(final CourtDocument courtDocument) {
        CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocument.getCourtDocumentId());
        courtDocumentEntity.setIndices(new HashSet<>());
        getLinkedCaseIds(courtDocument.getDocumentCategory()).forEach(
                caseId -> {
                    final CourtDocumentIndexEntity index = new CourtDocumentIndexEntity();
                    index.setProsecutionCaseId(caseId);
                    index.setCourtDocument(courtDocumentEntity);
                    index.setId(UUID.randomUUID());
                    courtDocumentEntity.getIndices().add(index);
                }
        );
        courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(courtDocument).toString());
        return courtDocumentEntity;
    }


    private List<UUID> getLinkedCaseIds(final DocumentCategory documentCategory) {

        if (nonNull(documentCategory.getNowDocument())) {
            return documentCategory.getNowDocument().getProsecutionCases();
        } else if (nonNull(documentCategory.getCaseDocument())) {
            return asList(documentCategory.getCaseDocument().getProsecutionCaseId());
        } else if (nonNull(documentCategory.getDefendantDocument())) {
            return asList(documentCategory.getDefendantDocument().getProsecutionCaseId());
        } else {
            return nonNull(documentCategory.getApplicationDocument().getProsecutionCaseId()) ?
                    asList(documentCategory.getApplicationDocument().getProsecutionCaseId()) : Collections.emptyList();

        }
    }


    private CourtDocumentMaterialEntity getCourtDocumentMaterialEntity(final Material material, final UUID courtDocumentId) {
        CourtDocumentMaterialEntity courtDocumentMaterialEntity = new CourtDocumentMaterialEntity();
        courtDocumentMaterialEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentMaterialEntity.setMaterialId(material.getId());
        courtDocumentMaterialEntity.setUserGroups(material.getUserGroups());
        return courtDocumentMaterialEntity;
    }
}
