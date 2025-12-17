package uk.gov.moj.cpp.nows.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
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
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class NowNotificationGeneratedEventListener {

    private static final String MATERIAL_ID = "materialId";
    private static final String STATUS = "status";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Inject
    private CourtDocumentRepository courtDocumentRepository;
    @Inject
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @Transactional
    @Handles("progression.event.now-notification-generated")
    public void nowNotificationGenerated(final JsonEnvelope event) {
        final JsonObject nowNotificationGeneratedPayload = event.payloadAsJsonObject();
        final UUID materialId = UUID.fromString(nowNotificationGeneratedPayload.getString(MATERIAL_ID));
        final CourtDocumentMaterialEntity courtDocumentMaterialEntity = courtDocumentMaterialRepository.findBy(materialId);
        if (courtDocumentMaterialEntity != null) {
            final CourtDocumentEntity courtDocumentEntity = courtDocumentRepository.findBy(courtDocumentMaterialEntity.getCourtDocumentId());
            CourtDocument courtDocument = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(courtDocumentEntity.getPayload()), CourtDocument.class);
            final List<Material> modifiedMaterials = courtDocument.getMaterials().stream()
                    .map(m -> (m.getId().equals(materialId)) ? updateMaterialGenerationStatus(m, nowNotificationGeneratedPayload.getString(STATUS)) : m
                    ).collect(Collectors.toList());
            courtDocument = updateCourtDocumentMaterials(courtDocument, modifiedMaterials);
            courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(courtDocument).toString());
            courtDocumentRepository.save(courtDocumentEntity);
        }
    }

    final Material updateMaterialGenerationStatus(final Material material, final String status) {
        return Material.material()
                .withValuesFrom(material)
                .withGenerationStatus(status)
                .build();
    }

    final CourtDocument updateCourtDocumentMaterials(final CourtDocument courtDocument, List<Material> materials) {
        return CourtDocument.courtDocument()
                .withValuesFrom(courtDocument)
                .withMaterials(materials)
                .build();
    }
}
