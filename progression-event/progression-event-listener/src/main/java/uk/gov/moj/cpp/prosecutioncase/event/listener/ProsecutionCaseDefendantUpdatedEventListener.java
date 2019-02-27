package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.io.StringReader;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseDefendantUpdatedEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private SearchProsecutionCase searchCase;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseDefendantUpdatedEventListener.class);

    @Handles("progression.event.prosecution-case-defendant-updated")
    public void processProsecutionCaseDefendantUpdated(final JsonEnvelope event) {
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("received event  progression.event.prosecution-case-defendant-updated {} ", event.toObfuscatedDebugString());
        }
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantUpdated.class);
        final Defendant defendant = prosecutionCaseDefendantUpdated.getDefendant();
        ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendant.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        Optional<Defendant> originDefendant = prosecutionCase.getDefendants().stream().filter(d -> d.getId().equals(defendant.getId())).findFirst();
        if (originDefendant.isPresent()) {
            Defendant updatedDefendant = updateDefendant(originDefendant.get(), defendant);
            prosecutionCase.getDefendants().remove(originDefendant.get());
            prosecutionCase.getDefendants().add(updatedDefendant);
        }
        repository.save(getProsecutionCaseEntity(prosecutionCase));

        updateSearchable(prosecutionCase);
    }

    private void updateSearchable(final ProsecutionCase prosecutionCase) {
        prosecutionCase.getDefendants().forEach(caseDefendant ->
                searchCase.makeSearchable(prosecutionCase, caseDefendant));
    }

    private Defendant updateDefendant(final Defendant originDefendant, final Defendant defendant) {

        return Defendant.defendant().withOffences(originDefendant.getOffences())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withLegalEntityDefendant(originDefendant.getLegalEntityDefendant())
                .withAssociatedPersons(originDefendant.getAssociatedPersons())
                .withId(defendant.getId())
                .withMitigation(originDefendant.getMitigation())
                .withMitigationWelsh(originDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(originDefendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withWitnessStatement(originDefendant.getWitnessStatement())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .build();
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }


}
