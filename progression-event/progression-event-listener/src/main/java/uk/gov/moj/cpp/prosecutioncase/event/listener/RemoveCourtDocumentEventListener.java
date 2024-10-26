package uk.gov.moj.cpp.prosecutioncase.event.listener;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentRemoved;
import uk.gov.justice.core.courts.CourtsDocumentRemovedBdf;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class RemoveCourtDocumentEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtDocumentRepository repository;

    @Inject
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @Handles("progression.event.court-document-removed")
    public void processCourtDocumentRemoved(final JsonEnvelope event) {
        final CourtsDocumentRemoved courtsDocumentRemoved = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtsDocumentRemoved.class);
        final CourtDocumentEntity courtDocumentEntity = repository.findBy(courtsDocumentRemoved.getCourtDocumentId());

        final CourtDocument courtDocument = jsonObjectConverter.convert(stringToJsonObjectConverter.convert(courtDocumentEntity.getPayload()), CourtDocument.class);

        final CourtDocument courtDocumentUpdated = CourtDocument.courtDocument()
                .withName(courtDocument.getName())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withMaterials(courtDocument.getMaterials())
                .withMimeType(courtDocument.getMimeType())
                .build();
        courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(courtDocumentUpdated).toString());
        courtDocumentEntity.setIsRemoved(true);

    }

    @Handles("progression.event.court-document-removed-bdf")
    public void processCourtDocumentRemovedBdf(final JsonEnvelope event) {
        final CourtsDocumentRemovedBdf courtsDocumentRemovedBdf = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtsDocumentRemovedBdf.class);
        final CourtDocumentEntity courtDocumentEntity = repository.findBy(courtsDocumentRemovedBdf.getCourtDocumentId());

        final CourtDocument courtDocument = jsonObjectConverter.convert(stringToJsonObjectConverter.convert(courtDocumentEntity.getPayload()), CourtDocument.class);

        final CourtDocument courtDocumentUpdated = CourtDocument.courtDocument()
                .withName(courtDocument.getName())
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withDocumentTypeDescription(courtDocument.getDocumentTypeDescription())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withMaterials(courtDocument.getMaterials())
                .withMimeType(courtDocument.getMimeType())
                .build();
        courtDocumentEntity.setPayload(objectToJsonObjectConverter.convert(courtDocumentUpdated).toString());
        courtDocumentEntity.setIsRemoved(true);

    }

}
