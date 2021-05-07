package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CaseCpsProsecutorUpdated;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_LISTENER)
public class UpdateProsecutionCaseCpsProsecutorEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateProsecutionCaseCpsProsecutorEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private SearchProsecutionCase searchCase;


    @Handles("progression.event.case-cps-prosecutor-updated")
    public void handleUpdateCaseCpsProsecutor(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.case-cps-prosecutor-updated {} ", event.toObfuscatedDebugString());
        }
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseCpsProsecutorUpdated.class);
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(caseCpsProsecutorUpdated.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase persistentProsecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final ProsecutionCase updatedProsecutionCase = updateProsecutionCase(persistentProsecutionCase, caseCpsProsecutorUpdated);
        repository.save(getProsecutionCaseEntity(updatedProsecutionCase));
        searchCase.updateSearchable(updatedProsecutionCase);
    }

    private ProsecutionCase updateProsecutionCase(final ProsecutionCase prosecutionCase, final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated) {
        Prosecutor prosecutor = null;
        if (isNull(caseCpsProsecutorUpdated.getIsCpsOrgVerifyError()) || !caseCpsProsecutorUpdated.getIsCpsOrgVerifyError()) {
            prosecutor = Prosecutor.prosecutor()
                    .withAddress(caseCpsProsecutorUpdated.getAddress())
                    .withProsecutorCode(caseCpsProsecutorUpdated.getProsecutionAuthorityCode())
                    .withProsecutorId(caseCpsProsecutorUpdated.getProsecutionAuthorityId())
                    .withProsecutorName(caseCpsProsecutorUpdated.getProsecutionAuthorityName())
                    .build();
        }

        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withProsecutor(prosecutor)
                .withIsCpsOrgVerifyError(caseCpsProsecutorUpdated.getIsCpsOrgVerifyError())
                .build();
    }


    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }
}
