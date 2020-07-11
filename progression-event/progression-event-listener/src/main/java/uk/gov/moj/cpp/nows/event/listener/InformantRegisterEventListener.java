package uk.gov.moj.cpp.nows.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.progression.courts.InformantRegisterGenerated;
import uk.gov.justice.progression.courts.InformantRegisterNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InformantRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InformantRegisterRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.BooleanUtils;

@ServiceComponent(EVENT_LISTENER)
public class InformantRegisterEventListener {

    private static final String INFORMANT_REGISTER_REQUEST_PARAM = "informantRegister";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private InformantRegisterRepository informantRegisterRepository;

    @Transactional
    @Handles("progression.event.informant-register-recorded")
    public void saveInformantRegister(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();
        final JsonObject informantRegisterDocumentRequestJson = payload.getJsonObject(INFORMANT_REGISTER_REQUEST_PARAM);

        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = jsonObjectToObjectConverter.convert(informantRegisterDocumentRequestJson, InformantRegisterDocumentRequest.class);
        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();

        informantRegisterEntity.setId(UUID.randomUUID());
        informantRegisterEntity.setRegisterDate(informantRegisterDocumentRequest.getRegisterDate().toLocalDate());
        informantRegisterEntity.setRegisterTime(informantRegisterDocumentRequest.getRegisterDate());
        informantRegisterEntity.setHearingId(informantRegisterDocumentRequest.getHearingId());
        informantRegisterEntity.setProsecutionAuthorityId(informantRegisterDocumentRequest.getProsecutionAuthorityId());
        informantRegisterEntity.setProsecutionAuthorityCode(informantRegisterDocumentRequest.getProsecutionAuthorityCode());
        informantRegisterEntity.setPayload(informantRegisterDocumentRequestJson.toString());
        informantRegisterEntity.setStatus(RegisterStatus.RECORDED);

        informantRegisterRepository.save(informantRegisterEntity);
    }

    @Handles("progression.event.informant-register-generated")
    public void generateInformantRegister(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final InformantRegisterGenerated informantRegisterGenerated = jsonObjectToObjectConverter.convert(payload, InformantRegisterGenerated.class);
        final List<InformantRegisterEntity> informantRegisters = informantRegisterRepository.findByProsecutionAuthorityIdAndStatusRecorded(informantRegisterGenerated.getInformantRegisterDocumentRequests().get(0).getProsecutionAuthorityId());
        informantRegisters.forEach(informantRegisterEntity -> {
            final ZonedDateTime currentDateTime = ZonedDateTime.now();
            informantRegisterEntity.setStatus(RegisterStatus.GENERATED);
            informantRegisterEntity.setProcessedOn(currentDateTime);
            if(BooleanUtils.isTrue(informantRegisterGenerated.getSystemGenerated())) {
                informantRegisterEntity.setGeneratedDate(currentDateTime.toLocalDate());
                informantRegisterEntity.setGeneratedTime(currentDateTime);
            }
        });
    }

    @Handles("progression.event.informant-register-notified")
    public void notifyInformantRegister(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final InformantRegisterNotified informantRegisterNotified = jsonObjectToObjectConverter.convert(payload, InformantRegisterNotified.class);
        final List<InformantRegisterEntity> informantRegisters = informantRegisterRepository.findByProsecutionAuthorityIdAndStatusGenerated(informantRegisterNotified.getProsecutionAuthorityId());
        informantRegisters.forEach(informantRegisterEntity -> {
                    informantRegisterEntity.setStatus(RegisterStatus.NOTIFIED);
                    informantRegisterEntity.setFileId(informantRegisterNotified.getFileId());
                    informantRegisterEntity.setProcessedOn(ZonedDateTime.now());
                }
        );
    }
}
