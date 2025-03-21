package uk.gov.moj.cpp.nows.event.listener;

import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
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
import java.util.Optional;
import java.util.UUID;


import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class PrisonCourtRegisterEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrisonCourtRegisterEventListener.class);

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

        prisonCourtRegisterEntity.setId(isNull(prisonCourtRegisterRecorded.getId()) ? randomUUID() : prisonCourtRegisterRecorded.getId());
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
        if(isNull(prisonCourtRegisterGenerated.getId())) {
            // this is for old events , catch-up or replay DLQs
            final String defendantId = Optional.ofNullable(prisonCourtRegisterGenerated.getDefendant().getMasterDefendantId()).map(UUID::toString).orElse("");
            try{
                prisonCourtRegisterEntity = prisonCourtRegisterRepository.findByCourtCentreIdAndHearingIdAndDefendantId(prisonCourtRegisterGenerated.getCourtCentreId(), prisonCourtRegisterGenerated.getHearingId().toString(), defendantId);
                prisonCourtRegisterEntity.setFileId(prisonCourtRegisterGenerated.getFileId());
            } catch (Exception e) {
                // this update is not important for the old events
                LOGGER.error("Found courtCentreId {} and hearingId {} defendantId {}", prisonCourtRegisterGenerated.getCourtCentreId(), prisonCourtRegisterGenerated.getHearingId(), defendantId);
                LOGGER.error("Error generating prison court register " , e);
            }
        } else {
            prisonCourtRegisterEntity = prisonCourtRegisterRepository.findById(prisonCourtRegisterGenerated.getId());
            prisonCourtRegisterEntity.setFileId(prisonCourtRegisterGenerated.getFileId());
        }

    }
}
