package uk.gov.moj.cpp.nows.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrisonCourtRegisterRepository;

import java.time.LocalDate;
import java.util.Objects;
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

        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), PrisonCourtRegisterRecorded.class);
        final JsonObject prisonCourtRegisterJson = event.payloadAsJsonObject().getJsonObject("prisonCourtRegister");

        final PrisonCourtRegisterEntity prisonCourtRegisterEntity = new PrisonCourtRegisterEntity();

        prisonCourtRegisterEntity.setId(Objects.isNull(prisonCourtRegisterRecorded.getId()) ? UUID.randomUUID() : prisonCourtRegisterRecorded.getId());
        prisonCourtRegisterEntity.setRecordedDate(LocalDate.now());
        prisonCourtRegisterEntity.setCourtCentreId(prisonCourtRegisterRecorded.getCourtCentreId());
        prisonCourtRegisterEntity.setPayload(prisonCourtRegisterJson.toString());

        prisonCourtRegisterRepository.save(prisonCourtRegisterEntity);
    }

    @Handles("progression.event.prison-court-register-generated")
    public void generatePrisonCourtRegister(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = jsonObjectToObjectConverter.convert(payload, PrisonCourtRegisterGenerated.class);
        final PrisonCourtRegisterEntity prisonCourtRegisterEntity;
        if(Objects.isNull(prisonCourtRegisterGenerated.getId())) {
            // this is for old events , catch-up or replay DLQs
            prisonCourtRegisterEntity = prisonCourtRegisterRepository.findByCourtCentreIdAndHearingId(prisonCourtRegisterGenerated.getCourtCentreId(), prisonCourtRegisterGenerated.getHearingId().toString());
        } else {
            prisonCourtRegisterEntity = prisonCourtRegisterRepository.findById(prisonCourtRegisterGenerated.getId());
        }
        prisonCourtRegisterEntity.setFileId(prisonCourtRegisterGenerated.getFileId());
    }
}
