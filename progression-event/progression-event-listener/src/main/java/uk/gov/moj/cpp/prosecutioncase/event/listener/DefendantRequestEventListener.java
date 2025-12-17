package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.DefendantRequest;
import uk.gov.justice.core.courts.DefendantRequestCreated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantRequestRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class DefendantRequestEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DefendantRequestRepository repository;


    @Handles("progression.event.defendant-request-created")
    public void processProsecutionCaseCreated(final JsonEnvelope event) {
        final DefendantRequestCreated defendantRequestCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), DefendantRequestCreated.class);
        final DefendantRequest defendantRequest = defendantRequestCreated.getDefendantRequest();
        repository.save(getDefendantRequestEntity(defendantRequest));
    }

    private DefendantRequestEntity getDefendantRequestEntity(final DefendantRequest defendantRequest) {
        final DefendantRequestEntity defendantRequestEntity = new DefendantRequestEntity();
        defendantRequestEntity.setDefendantId(defendantRequest.getDefendantId());
        defendantRequestEntity.setProsecutionCaseId(defendantRequest.getProsecutionCaseId());
        defendantRequestEntity.setPayload(objectToJsonObjectConverter.convert(defendantRequest).toString());
        return defendantRequestEntity;
    }


}
