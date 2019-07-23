package uk.gov.moj.cpp.prosecutioncase.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import javax.inject.Inject;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class DefendantsAddedToCourtProceedingsListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantsAddedToCourtProceedingsListener.class);

    @Handles("progression.event.defendants-added-to-court-proceedings")
    public void processDefendantsAddedToCourtProceedings(final JsonEnvelope event) {
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.event.defendants-added-to-court-proceedings {} ", event.toObfuscatedDebugString());
        }
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = jsonObjectConverter.convert(event.payloadAsJsonObject(), DefendantsAddedToCourtProceedings.class);

        for (final uk.gov.justice.core.courts.Defendant defendant: defendantsAddedToCourtProceedings.getDefendants()) {
            final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendant.getProsecutionCaseId());
            final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
            final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

            prosecutionCase.getDefendants().add(defendant);
            prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
            repository.save(prosecutionCaseEntity);
        }
    }

}
