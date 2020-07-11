package uk.gov.moj.cpp.nows.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrisonCourtRegisterRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class PrisonCourtRegisterEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private PrisonCourtRegisterRepository prisonCourtRegisterRepository;

    @Transactional
    @Handles("progression.event.prison-court-register-recorded")
    public void savePrisonCourtRegister(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();
        final JsonObject prisonCourtRegisterJson = payload.getJsonObject("prisonCourtRegister");

        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = jsonObjectToObjectConverter.convert(prisonCourtRegisterJson, PrisonCourtRegisterDocumentRequest.class);
        final PrisonCourtRegisterEntity prisonCourtRegisterEntity = new PrisonCourtRegisterEntity();

        prisonCourtRegisterEntity.setId(UUID.randomUUID());
        prisonCourtRegisterEntity.setRecordedDate(LocalDate.now());
        prisonCourtRegisterEntity.setCourtCentreId(prisonCourtRegisterDocumentRequest.getCourtCentreId());
        prisonCourtRegisterEntity.setPayload(prisonCourtRegisterJson.toString());

        prisonCourtRegisterRepository.save(prisonCourtRegisterEntity);
    }

    @Handles("progression.event.prison-court-register-generated")
    public void generatePrisonCourtRegister(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = jsonObjectToObjectConverter.convert(payload, PrisonCourtRegisterGenerated.class);
        final List<PrisonCourtRegisterEntity> prisonCourtRegisterEntities = prisonCourtRegisterRepository.findByCourtCentreId(prisonCourtRegisterGenerated.getCourtCentreId());
        prisonCourtRegisterEntities.forEach(prisonCourtRegisterEntity -> prisonCourtRegisterEntity.setFileId(prisonCourtRegisterGenerated.getFileId()));
    }
}
