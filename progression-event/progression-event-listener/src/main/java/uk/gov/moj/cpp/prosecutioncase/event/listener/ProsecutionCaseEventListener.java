package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import javax.inject.Inject;


@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private SearchProsecutionCase searchCase;

    @Handles("progression.event.prosecution-case-created")
    public void processProsecutionCaseCreated(final JsonEnvelope event) {
        final ProsecutionCaseCreated prosecutionCaseCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseCreated.class);
        final ProsecutionCase prosecutionCase = prosecutionCaseCreated.getProsecutionCase();
        repository.save(getProsecutionCaseEntity(prosecutionCase));
        makeSearchable(prosecutionCase);
    }

    private void makeSearchable(final ProsecutionCase prosecutionCase) {
        prosecutionCase.getDefendants().forEach(defendant ->
                searchCase.makeSearchable(prosecutionCase, defendant));
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }
}
