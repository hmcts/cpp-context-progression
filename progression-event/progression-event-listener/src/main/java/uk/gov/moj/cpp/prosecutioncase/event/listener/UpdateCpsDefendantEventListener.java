package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CaseCpsDetailsUpdatedFromCourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.CpsDefendantIdUpdated;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class UpdateCpsDefendantEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCpsDefendantEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Handles("progression.events.cps-defendant-id-updated")
    public void cpsDefendantIdUpdated(final Envelope<CpsDefendantIdUpdated> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.events.cps-defendant-id-updated {} ", event.payload());
        }
        final CpsDefendantIdUpdated cpsDefendantIdUpdated = event.payload();
        try {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(cpsDefendantIdUpdated.getCaseId());
            if (nonNull(prosecutionCaseEntity)) {
                final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);

                final List<Defendant> updatedDefendants = updateCpsDefendantId(prosecutionCase, cpsDefendantIdUpdated.getDefendantId(), cpsDefendantIdUpdated.getCpsDefendantId());

                updateProsecutionCaseForCpsCaseIdUpdate(updatedDefendants, prosecutionCaseEntity, prosecutionCase);
            }
        } catch (NoResultException exception) {
            LOGGER.info(" No record found with case id {}. EVENT_NAME: progression.events.cps-defendant-id-updated", cpsDefendantIdUpdated.getCaseId(), exception);
        }
    }

    @Handles("progression.event.case-cps-details-updated-from-court-document")
    public void updateCpsDefendant(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.event.case-cps-details-updated-from-court-document {} ", event.toObfuscatedDebugString());
        }
        final CaseCpsDetailsUpdatedFromCourtDocument caseCpsDetailsUpdatedFromCourtDocument = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CaseCpsDetailsUpdatedFromCourtDocument.class);
        try {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseCpsDetailsUpdatedFromCourtDocument.getCaseId());
            if (nonNull(prosecutionCaseEntity)) {
                final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);

                final List<Defendant> updatedDefendants = updateCpsDefendantId(prosecutionCase, caseCpsDetailsUpdatedFromCourtDocument.getDefendantId(), caseCpsDetailsUpdatedFromCourtDocument.getCpsDefendantId());

                updateProsecutionCase(updatedDefendants, prosecutionCaseEntity, prosecutionCase, caseCpsDetailsUpdatedFromCourtDocument.getCpsOrganisation());
            }
        } catch (NoResultException exception) {
            LOGGER.info(" No record found with case id {}. EVENT_NAME: progression.event.case-cps-details-updated-from-court-document", caseCpsDetailsUpdatedFromCourtDocument.getCaseId(), exception);
        }
    }

    public void updateProsecutionCase(final List<Defendant> updatedDefendants, final ProsecutionCaseEntity prosecutionCaseEntity,
                                      final ProsecutionCase prosecutionCase, final String cpsOrganisation) {

        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase).withDefendants(updatedDefendants)
                .withCpsOrganisation(isNull(prosecutionCase.getCpsOrganisation()) ? cpsOrganisation : prosecutionCase.getCpsOrganisation()).build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(updatedProsecutionCase).toString());
        prosecutionCaseRepository.save(prosecutionCaseEntity);

    }

    private List<Defendant> updateCpsDefendantId(ProsecutionCase prosecutionCase, final UUID defendantId, final String cpsDefendantId) {
        return prosecutionCase.getDefendants().stream()
                .map(defendant -> updateCpsDefendantId(defendant, defendantId, cpsDefendantId))
                .collect(Collectors.toList());
    }

    private Defendant updateCpsDefendantId(final Defendant defendant, final UUID defendantId, final String cpsDefendantId) {
        final Defendant.Builder updatedDefendant = Defendant.defendant();
        if (defendant.getId().equals(defendantId) && (isNull(defendant.getCpsDefendantId()) || !cpsDefendantId.equals(defendant.getCpsDefendantId()))) {
            updatedDefendant.withValuesFrom(defendant).withCpsDefendantId(cpsDefendantId);
        } else {
            updatedDefendant.withValuesFrom(defendant);
        }
        return updatedDefendant.build();
    }

    private void updateProsecutionCaseForCpsCaseIdUpdate(final List<Defendant> updatedDefendants, final ProsecutionCaseEntity prosecutionCaseEntity,
                                       final ProsecutionCase prosecutionCase) {

        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withDefendants(updatedDefendants)
                .build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(updatedProsecutionCase).toString());
        prosecutionCaseRepository.save(prosecutionCaseEntity);

    }
}
