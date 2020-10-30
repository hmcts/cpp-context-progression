package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CpsProsecutorUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseCpsProsecutorEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseCpsProsecutorRepository;

import java.util.Objects;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class UpdateCpsProsecutorEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCpsProsecutorEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CaseCpsProsecutorRepository caseCpsProsecutorRepository;

    @Handles("progression.event.cps-prosecutor-updated")
    public void updateCpsProsecutor(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.cps-prosecutor-updated {} ", event.toObfuscatedDebugString());
        }
        final CpsProsecutorUpdated cpsProsecutorUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CpsProsecutorUpdated.class);
        final CaseCpsProsecutorEntity caseCpsProsecutorEntity = caseCpsProsecutorRepository.findBy(cpsProsecutorUpdated.getProsecutionCaseId());

        if (Objects.isNull(caseCpsProsecutorEntity)) {
            final CaseCpsProsecutorEntity entity = new CaseCpsProsecutorEntity();
            entity.setCaseId(cpsProsecutorUpdated.getProsecutionCaseId());
            entity.setCpsProsecutor(cpsProsecutorUpdated.getProsecutionAuthorityCode());
            entity.setOldCpsProsecutor(cpsProsecutorUpdated.getOldCpsProsecutor());
            caseCpsProsecutorRepository.save(entity);
        } else {
            if (StringUtils.isNotBlank(cpsProsecutorUpdated.getOldCpsProsecutor())) {
                caseCpsProsecutorEntity.setCpsProsecutor(cpsProsecutorUpdated.getProsecutionAuthorityCode());
                caseCpsProsecutorEntity.setOldCpsProsecutor(cpsProsecutorUpdated.getOldCpsProsecutor());
                caseCpsProsecutorRepository.save(caseCpsProsecutorEntity);
            }
        }
    }
}
