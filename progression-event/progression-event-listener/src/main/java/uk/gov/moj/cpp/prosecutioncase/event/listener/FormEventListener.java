package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.FormCreated;
import uk.gov.justice.core.courts.FormDefendantsUpdated;
import uk.gov.justice.core.courts.FormFinalised;
import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.FormUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantOffenceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(EVENT_LISTENER)
public class FormEventListener {

    private static final Logger LOGGER = getLogger(FormEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CaseDefendantOffenceRepository caseDefendantOffenceRepository;


    @Handles("progression.event.form-created")
    public void formCreated(final JsonEnvelope event) {
        final FormCreated formCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), FormCreated.class);

        LOGGER.info("progression.event.form-created event received with form ref id: {} for case: {}", formCreated.getCourtFormId(), formCreated.getCaseId());

        final List<CaseDefendantOffence> caseDefendantOffenceList = new ArrayList<>();

        formCreated.getFormDefendants().forEach(formDefendants -> caseDefendantOffenceList.add(buildCaseDefendantOffenceEntity(formDefendants.getDefendantId(), formCreated.getCaseId(), formCreated.getCourtFormId(), formCreated.getFormType())));

        caseDefendantOffenceList.forEach(formCaseDefendantOffence -> caseDefendantOffenceRepository.save(formCaseDefendantOffence));
    }


    @Handles("progression.event.form-updated")
    public void formUpdated(final JsonEnvelope event) {
        final FormUpdated payload = jsonObjectConverter.convert(event.payloadAsJsonObject(), FormUpdated.class);

        LOGGER.info("progression.event.form-updated event received with court form id: {} for case: {}", payload.getCourtFormId(), payload.getCaseId());

        final List<CaseDefendantOffence> caseDefendantOffences = caseDefendantOffenceRepository.findByCourtFormId(payload.getCourtFormId());

        caseDefendantOffences.forEach(caseDefendantOffence -> {
            caseDefendantOffence.setLastUpdated(now());
            caseDefendantOffenceRepository.save(caseDefendantOffence);
        });
    }

    @Handles("progression.event.form-finalised")
    public void formFinalised(final JsonEnvelope event) {
        final FormFinalised payload = jsonObjectConverter.convert(event.payloadAsJsonObject(), FormFinalised.class);

        LOGGER.info("progression.event.form-finalised event received with court form id: {} for case: {}", payload.getCourtFormId(), payload.getCaseId());

        final List<CaseDefendantOffence> caseDefendantOffences = caseDefendantOffenceRepository.findByCourtFormId(payload.getCourtFormId());

        caseDefendantOffences.forEach(caseDefendantOffence -> {
            caseDefendantOffence.setLastUpdated(now());
            caseDefendantOffenceRepository.save(caseDefendantOffence);
        });
    }


    @Handles("progression.event.form-defendants-updated")
    public void formDefendantsUpdated(final JsonEnvelope event) {
        final FormDefendantsUpdated payload = jsonObjectConverter.convert(event.payloadAsJsonObject(), FormDefendantsUpdated.class);

        LOGGER.info("progression.event.form-defendants-updated event received with court form id: {} for case: {}", payload.getCourtFormId(), payload.getCaseId());

        final List<CaseDefendantOffence> caseDefendantOffences = caseDefendantOffenceRepository.findByCourtFormId(payload.getCourtFormId());
        caseDefendantOffences.forEach(caseDefendantOffence -> caseDefendantOffenceRepository.remove(caseDefendantOffence));

        final List<CaseDefendantOffence> caseDefendantOffenceList = new ArrayList<>();

        payload.getFormDefendants().forEach(formDefendants -> caseDefendantOffenceList.add(buildCaseDefendantOffenceEntity(formDefendants.getDefendantId(), payload.getCaseId(), payload.getCourtFormId(), payload.getFormType())));

        caseDefendantOffenceList.forEach(caseDefendantOffence -> caseDefendantOffenceRepository.save(caseDefendantOffence));
    }


    private CaseDefendantOffence buildCaseDefendantOffenceEntity(final UUID defendantId, final UUID caseId, final UUID courtFormId, final FormType formType) {
        return CaseDefendantOffence.builder()
                .withId(randomUUID())
                .withDefendantId(defendantId)
                .withCaseId(caseId)
                .withCourtFormId(courtFormId)
                .withFormType(FormType.valueOf(formType.name()))
                .withLastUpdated(now())
                .build();
    }
}
