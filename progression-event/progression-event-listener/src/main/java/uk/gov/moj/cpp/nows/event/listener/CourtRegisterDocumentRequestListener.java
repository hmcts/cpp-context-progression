package uk.gov.moj.cpp.nows.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDocumentRequest;
import uk.gov.justice.progression.courts.CourtRegisterGenerated;
import uk.gov.justice.progression.courts.CourtRegisterNotified;
import uk.gov.justice.progression.courts.CourtRegisterNotifiedV2;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtRegisterRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtRegisterRequestRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.BooleanUtils;

@ServiceComponent(EVENT_LISTENER)
public class CourtRegisterDocumentRequestListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CourtRegisterRequestRepository repository;

    @Handles("progression.event.court-register-recorded")
    public void saveCourtRegisterRequest(final JsonEnvelope event) {
        final JsonObject courtRegisterDocumentRequestJson = event.payloadAsJsonObject().getJsonObject("courtRegister");

        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = jsonObjectConverter.convert(courtRegisterDocumentRequestJson, CourtRegisterDocumentRequest.class);
        final CourtRegisterRequestEntity courtRegisterRequestEntity = new CourtRegisterRequestEntity();
        courtRegisterRequestEntity.setCourtRegisterRequestId(UUID.randomUUID());
        courtRegisterRequestEntity.setCourtCentreId(courtRegisterDocumentRequest.getCourtCentreId());
        courtRegisterRequestEntity.setRegisterDate(courtRegisterDocumentRequest.getRegisterDate().toLocalDate());
        courtRegisterRequestEntity.setRegisterTime(courtRegisterDocumentRequest.getRegisterDate());
        courtRegisterRequestEntity.setHearingId(courtRegisterDocumentRequest.getHearingId());
        courtRegisterRequestEntity.setPayload(courtRegisterDocumentRequestJson.toString());
        courtRegisterRequestEntity.setStatus(RegisterStatus.RECORDED);
        courtRegisterRequestEntity.setCourtHouse(courtRegisterDocumentRequest.getHearingVenue().getCourtHouse());
        repository.save(courtRegisterRequestEntity);
    }

    @Handles("progression.event.court-register-generated")
    public void generateCourtRegister(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final CourtRegisterGenerated courtRegisterGenerated = jsonObjectConverter.convert(payload, CourtRegisterGenerated.class);
        final ZonedDateTime currentDateTime = new UtcClock().now();

        final List<CourtRegisterRequestEntity> courtRegisters = repository.findByCourtCenterIdAndStatusRecorded(courtRegisterGenerated.getCourtRegisterDocumentRequests().get(0).getCourtCentreId());
        courtRegisters.forEach(courtRegisterRequestEntity -> {
            courtRegisterRequestEntity.setStatus(RegisterStatus.GENERATED);
            courtRegisterRequestEntity.setProcessedOn(currentDateTime);
            if(BooleanUtils.isTrue(courtRegisterGenerated.getSystemGenerated())) {
                courtRegisterRequestEntity.setGeneratedDate(currentDateTime.toLocalDate());
                courtRegisterRequestEntity.setGeneratedTime(currentDateTime);
            }
        });

        courtRegisterGenerated.getCourtRegisterDocumentRequests().stream().map(CourtRegisterDocumentRequest::getHearingId).forEach(hearingId -> {
            final List<CourtRegisterRequestEntity> courtRegisterRequestEntities = repository.findByHearingIdAndStatusRecorded(hearingId);
            courtRegisterRequestEntities.forEach(courtRegisterRequestEntity -> courtRegisterRequestEntity.setProcessedOn(currentDateTime));
        });
    }

    @Handles("progression.event.court-register-notified")
    public void notifyCourtRegister(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final CourtRegisterNotified registerNotified = jsonObjectConverter.convert(payload, CourtRegisterNotified.class);
        final List<CourtRegisterRequestEntity> courtRegisters = repository.findByCourtCenterIdAndStatusGenerated(registerNotified.getCourtCentreId());
        courtRegisters.forEach(courtRegisterRequestEntity -> {
                    courtRegisterRequestEntity.setStatus(RegisterStatus.NOTIFIED);
                    courtRegisterRequestEntity.setSystemDocGeneratorId(registerNotified.getSystemDocGeneratorId());
                    courtRegisterRequestEntity.setProcessedOn(ZonedDateTime.now());
                }
        );
    }

    @Handles("progression.event.court-register-notified-v2")
    public void notifyCourtRegisterV2(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final CourtRegisterNotifiedV2 registerNotified = jsonObjectConverter.convert(payload, CourtRegisterNotifiedV2.class);
        final List<CourtRegisterRequestEntity> courtRegisters = repository.findByCourtCenterIdForRegisterDateAndStatusGenerated(registerNotified.getCourtCentreId(), registerNotified.getRegisterDate());
        courtRegisters.forEach(courtRegisterRequestEntity -> {
                    courtRegisterRequestEntity.setStatus(RegisterStatus.NOTIFIED);
                    courtRegisterRequestEntity.setSystemDocGeneratorId(registerNotified.getSystemDocGeneratorId());
                    courtRegisterRequestEntity.setProcessedOn(ZonedDateTime.now());
                }
        );
    }
}
