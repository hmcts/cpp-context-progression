package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class DefendantDefenceAssociationLockedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantDefenceAssociationLockedListener.class);
    private static final String DEFENDANT_ID = "defendantId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String LOCKED_BY_REP_ORDER = "lockedByRepOrder";

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Handles("progression.event.defendant-defence-association-locked")
    public void processDefendantAssociationLock(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event  progression.event.defendant-defence-association-locked {} ", event.payloadAsJsonObject());
        }
        final JsonObject payload = event.payloadAsJsonObject();
        final UUID caseId = fromString(payload.getString(PROSECUTION_CASE_ID));
        final UUID defendantId = fromString(payload.getString(DEFENDANT_ID));
        final boolean lockedByRepOrder = payload.getBoolean(LOCKED_BY_REP_ORDER);
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(caseId);
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final Optional<Defendant> originDefendant = prosecutionCase.getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst();
        if (originDefendant.isPresent()) {
            final Defendant updatedDefendant = updateDefendant(originDefendant.get(), lockedByRepOrder);
            prosecutionCase.getDefendants().remove(originDefendant.get());
            prosecutionCase.getDefendants().add(updatedDefendant);

        }
        repository.save(getProsecutionCaseEntity(prosecutionCase));
    }

    private Defendant updateDefendant(final Defendant originDefendant, final boolean  lockedByRepOrder) {

        return Defendant.defendant()
                .withOffences(originDefendant.getOffences())
                .withCpsDefendantId(originDefendant.getCpsDefendantId())
                .withPersonDefendant(originDefendant.getPersonDefendant())
                .withLegalEntityDefendant(originDefendant.getLegalEntityDefendant())
                .withAssociatedPersons(originDefendant.getAssociatedPersons())
                .withId(originDefendant.getId())
                .withMasterDefendantId(originDefendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(originDefendant.getCourtProceedingsInitiated())
                .withMitigation(originDefendant.getMitigation())
                .withMitigationWelsh(originDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(originDefendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(originDefendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(originDefendant.getProsecutionCaseId())
                .withWitnessStatement(originDefendant.getWitnessStatement())
                .withWitnessStatementWelsh(originDefendant.getWitnessStatementWelsh())
                .withDefenceOrganisation(originDefendant.getDefenceOrganisation())
                .withPncId(originDefendant.getPncId())
                .withAliases(originDefendant.getAliases())
                .withIsYouth(originDefendant.getIsYouth())
                .withLegalAidStatus(originDefendant.getLegalAidStatus())
                .withAssociatedDefenceOrganisation(originDefendant.getAssociatedDefenceOrganisation())
                .withProceedingsConcluded(originDefendant.getProceedingsConcluded())
                .withDefendantCaseJudicialResults(originDefendant.getDefendantCaseJudicialResults())
                .withCroNumber(originDefendant.getCroNumber())
                .withAssociationLockedByRepOrder(lockedByRepOrder)
                .build();

    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        if (nonNull(prosecutionCase.getGroupId())) {
            pCaseEntity.setGroupId(prosecutionCase.getGroupId());
        }
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }
}
