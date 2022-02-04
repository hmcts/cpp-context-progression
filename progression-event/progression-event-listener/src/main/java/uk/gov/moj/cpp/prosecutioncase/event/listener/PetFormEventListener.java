package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.PetDetailReceived;
import uk.gov.justice.core.courts.PetDetailUpdated;
import uk.gov.justice.core.courts.PetFormCreated;
import uk.gov.justice.core.courts.PetFormReceived;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffenceKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PetCaseDefendantOffenceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(EVENT_LISTENER)
public class PetFormEventListener {

    private static final Logger LOGGER = getLogger(PetFormEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private PetCaseDefendantOffenceRepository petCaseDefendantOffenceRepository;


    @Handles("progression.event.pet-form-created")
    public void petFormCreated(final JsonEnvelope event) {
        final PetFormCreated petFormCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetFormCreated.class);

        LOGGER.info("progression.event.pet-form-created event received with petId: {} for case: {}", petFormCreated.getPetId(), petFormCreated.getCaseId());

        final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = new ArrayList<>();

        petFormCreated.getPetDefendants().forEach(petDefendants ->
                petDefendants.getOffenceIds().forEach(
                        offenceId -> petCaseDefendantOffenceList.add(buildPetCaseDefendantOffenceEntity(petDefendants.getDefendantId(), offenceId, petFormCreated.getCaseId(), petFormCreated.getPetId()))
                )
        );

        petCaseDefendantOffenceList.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.save(petCaseDefendantOffence));
    }

    @Handles("progression.event.pet-form-received")
    public void petFormReceived(final JsonEnvelope event) {
        final PetFormReceived petFormReceived = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetFormReceived.class);

        LOGGER.info("progression.event.pet-form-received event received with petId: {} for case: {}", petFormReceived.getPetId(), petFormReceived.getCaseId());

        final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = new ArrayList<>();

        petFormReceived.getPetDefendants().forEach(petDefendants ->
                petDefendants.getOffenceIds().forEach(
                        offenceId -> petCaseDefendantOffenceList.add(buildPetCaseDefendantOffenceEntity(petDefendants.getDefendantId(), offenceId, petFormReceived.getCaseId(), petFormReceived.getPetId()))
                )
        );

        petCaseDefendantOffenceList.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.save(petCaseDefendantOffence));
    }

    @Handles("progression.event.pet-detail-updated")
    public void petDetailsUpdated(final JsonEnvelope event) {
        final PetDetailUpdated petDetailUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetDetailUpdated.class);

        LOGGER.info("progression.event.pet-detail-updated event received with petId: {} for case: {}", petDetailUpdated.getPetId(), petDetailUpdated.getCaseId());

        List<PetCaseDefendantOffence> petCaseDefendantOffences = petCaseDefendantOffenceRepository.findByPetId(petDetailUpdated.getPetId());
        petCaseDefendantOffences.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.remove(petCaseDefendantOffence));

        final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = new ArrayList<>();


        petDetailUpdated.getPetDefendants().forEach(petDefendants ->
                petDefendants.getOffenceIds().forEach(
                        offenceId -> petCaseDefendantOffenceList.add(buildPetCaseDefendantOffenceEntity(petDefendants.getDefendantId(), offenceId, petDetailUpdated.getCaseId(), petDetailUpdated.getPetId()))
                )
        );

        petCaseDefendantOffenceList.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.save(petCaseDefendantOffence));

    }

    @Handles("progression.event.pet-detail-received")
    public void petDetailReceived(final JsonEnvelope event) {
        final PetDetailReceived petDetailReceived = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetDetailReceived.class);

        LOGGER.info("progression.event.pet-details-received event received with petId: {} for case: {}", petDetailReceived.getPetId(), petDetailReceived.getCaseId());

        List<PetCaseDefendantOffence> petCaseDefendantOffences = petCaseDefendantOffenceRepository.findByPetId(petDetailReceived.getPetId());
        petCaseDefendantOffences.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.remove(petCaseDefendantOffence));

        final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = new ArrayList<>();


        petDetailReceived.getPetDefendants().forEach(petDefendants ->
                petDefendants.getOffenceIds().forEach(
                        offenceId -> petCaseDefendantOffenceList.add(buildPetCaseDefendantOffenceEntity(petDefendants.getDefendantId(), offenceId, petDetailReceived.getCaseId(), petDetailReceived.getPetId()))
                )
        );

        petCaseDefendantOffenceList.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.save(petCaseDefendantOffence));

    }

    private PetCaseDefendantOffence buildPetCaseDefendantOffenceEntity(final UUID defendantId, final UUID offenceId, final UUID caseId, final UUID petId) {
        return PetCaseDefendantOffence.builder()
                .withPetkey(new PetCaseDefendantOffenceKey(defendantId, offenceId))
                .withCaseId(caseId)
                .withPetId(petId)
                .build();
    }


}
