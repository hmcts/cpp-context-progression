package uk.gov.moj.cpp.nows.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NowsMaterialStatusUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

@SuppressWarnings("squid:S1188")
@ServiceComponent(EVENT_LISTENER)
public class NowsRequestedEventListener {

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private CourtDocumentRepository repository;

    @Inject
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    final Material cloneWithStatus(final Material material, final String status) {
        return Material.material()
                .withGenerationStatus(status)
                .withId(material.getId())
                .withUploadDateTime(material.getUploadDateTime())
                .withReceivedDateTime(material.getReceivedDateTime())
                .withName(material.getName())
                .withUserGroups(material.getUserGroups())
                .build();
    }

    final CourtDocument cloneWithMaterials(final CourtDocument courtDocument, List<Material> materials) {
        return CourtDocument.courtDocument()
                .withMaterials(materials)
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withName(courtDocument.getName())
                .withMimeType(courtDocument.getMimeType())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withContainsFinancialMeans(courtDocument.getContainsFinancialMeans())
                .withAmendmentDate(courtDocument.getAmendmentDate())
                .withSeqNum(courtDocument.getSeqNum())
                .withDocumentTypeRBAC(courtDocument.getDocumentTypeRBAC())
                .build();
    }

    @Transactional
    @Handles("progression.event.nows-material-status-updated")
    public void nowsMaterialStatusUpdated(final JsonEnvelope event) {
        final NowsMaterialStatusUpdated update = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), NowsMaterialStatusUpdated.class);
        final UUID materialId = update.getDetails().getMaterialId();
        final CourtDocumentMaterialEntity courtDocumentMaterialEntity = courtDocumentMaterialRepository.findBy(materialId);
        if (courtDocumentMaterialEntity != null) {
            final CourtDocumentEntity courtDocumentEntity = repository.findBy(courtDocumentMaterialEntity.getCourtDocumentId());
            CourtDocument courtDocument = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(courtDocumentEntity.getPayload()), CourtDocument.class);
            final List<Material> modifiedMaterials = courtDocument.getMaterials().stream()
                    .map(m -> (m.getId().equals(materialId)) ? cloneWithStatus(m, update.getStatus()) : m
                    ).collect(Collectors.toList());
            courtDocument = cloneWithMaterials(courtDocument, modifiedMaterials);
            courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(courtDocument).toString());
            repository.save(courtDocumentEntity);
        }
    }


}
