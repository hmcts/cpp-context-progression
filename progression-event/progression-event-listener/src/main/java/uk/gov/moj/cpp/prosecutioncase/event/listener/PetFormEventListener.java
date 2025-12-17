package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.time.ZonedDateTime.now;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.PetDetailReceived;
import uk.gov.justice.core.courts.PetDetailUpdated;
import uk.gov.justice.core.courts.PetFormCreated;
import uk.gov.justice.core.courts.PetFormDefendantUpdated;
import uk.gov.justice.core.courts.PetFormFinalised;
import uk.gov.justice.core.courts.PetFormReceived;
import uk.gov.justice.core.courts.PetFormUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;
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

        LOGGER.info("progression.event.pet-form-created event received with petId: {} for case: {} with isYouth flag: {}", petFormCreated.getPetId(), petFormCreated.getCaseId(), petFormCreated.getIsYouth());

        final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = new ArrayList<>();

        petFormCreated.getPetDefendants().forEach(petDefendants ->
                petCaseDefendantOffenceList.add(buildPetCaseDefendantOffenceEntity(petDefendants.getDefendantId(), petFormCreated.getCaseId(), petFormCreated.getPetId(), petFormCreated.getIsYouth()))
        );

        petCaseDefendantOffenceList.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.save(petCaseDefendantOffence));
    }

    @Handles("progression.event.pet-form-updated")
    public void petFormUpdated(final JsonEnvelope event) {
        final PetFormUpdated petFormUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetFormUpdated.class);

        LOGGER.info("progression.event.pet-form-updated event received with petId: {} for case: {}", petFormUpdated.getPetId(), petFormUpdated.getCaseId());

        final List<PetCaseDefendantOffence> petCaseDefendantOffences = petCaseDefendantOffenceRepository.findByPetId(petFormUpdated.getPetId());

        petCaseDefendantOffences.forEach(petCaseDefendantOffence -> {
            petCaseDefendantOffence.setLastUpdated(now());
            petCaseDefendantOffenceRepository.save(petCaseDefendantOffence);
        });
    }

    @Handles("progression.event.pet-form-finalised")
    public void petFormFinalised(final JsonEnvelope event) {
        final PetFormFinalised petFormFinalised = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetFormFinalised.class);

        LOGGER.info("progression.event.pet-form-finalised event received with petId: {} for case: {}", petFormFinalised.getPetId(), petFormFinalised.getCaseId());

        final List<PetCaseDefendantOffence> petCaseDefendantOffences = petCaseDefendantOffenceRepository.findByPetId(petFormFinalised.getPetId());

        petCaseDefendantOffences.forEach(petCaseDefendantOffence -> {
            petCaseDefendantOffence.setLastUpdated(now());
            petCaseDefendantOffenceRepository.save(petCaseDefendantOffence);
        });
    }

    @Handles("progression.event.pet-form-received")
    public void petFormReceived(final JsonEnvelope event) {
        final PetFormReceived petFormReceived = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetFormReceived.class);

        LOGGER.info("progression.event.pet-form-received event received with petId: {} for case: {}", petFormReceived.getPetId(), petFormReceived.getCaseId());

        final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = new ArrayList<>();

        petFormReceived.getPetDefendants().forEach(petDefendants ->
               petCaseDefendantOffenceList.add(buildPetCaseDefendantOffenceEntity(petDefendants.getDefendantId(), petFormReceived.getCaseId(), petFormReceived.getPetId(), false))
        );

        petCaseDefendantOffenceList.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.save(petCaseDefendantOffence));
    }

    @Handles("progression.event.pet-detail-updated")
    public void petDetailsUpdated(final JsonEnvelope event) {
        final PetDetailUpdated petDetailUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetDetailUpdated.class);

        LOGGER.info("progression.event.pet-detail-updated event received with petId: {} for case: {} with isYouth flag: {}", petDetailUpdated.getPetId(), petDetailUpdated.getCaseId(), petDetailUpdated.getIsYouth());

        List<PetCaseDefendantOffence> petCaseDefendantOffences = petCaseDefendantOffenceRepository.findByPetId(petDetailUpdated.getPetId());
        petCaseDefendantOffences.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.remove(petCaseDefendantOffence));

        final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = new ArrayList<>();


        petDetailUpdated.getPetDefendants().forEach(petDefendants ->
                petCaseDefendantOffenceList.add(buildPetCaseDefendantOffenceEntity(petDefendants.getDefendantId(), petDetailUpdated.getCaseId(), petDetailUpdated.getPetId(), petDetailUpdated.getIsYouth()))

        );

        petCaseDefendantOffenceList.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.save(petCaseDefendantOffence));

    }


    @Handles("progression.event.pet-form-defendant-updated")
    public void petFormDefendantUpdated(final JsonEnvelope event) {
        final PetFormDefendantUpdated petFormDefendantUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetFormDefendantUpdated.class);

        LOGGER.info("progression.event.pet-form-defendant-updated event received with petId: {} for case: {}", petFormDefendantUpdated.getPetId(), petFormDefendantUpdated.getCaseId());

        final List<PetCaseDefendantOffence> petCaseDefendantOffences = petCaseDefendantOffenceRepository.findByPetId(petFormDefendantUpdated.getPetId());

        petCaseDefendantOffences.forEach(petCaseDefendantOffence -> {
            petCaseDefendantOffence.setLastUpdated(now());
            petCaseDefendantOffenceRepository.save(petCaseDefendantOffence);
        });
    }

    @Handles("progression.event.pet-detail-received")
    public void petDetailReceived(final JsonEnvelope event) {
        final PetDetailReceived petDetailReceived = jsonObjectConverter.convert(event.payloadAsJsonObject(), PetDetailReceived.class);

        LOGGER.info("progression.event.pet-details-received event received with petId: {} for case: {}", petDetailReceived.getPetId(), petDetailReceived.getCaseId());

        List<PetCaseDefendantOffence> petCaseDefendantOffences = petCaseDefendantOffenceRepository.findByPetId(petDetailReceived.getPetId());
        petCaseDefendantOffences.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.remove(petCaseDefendantOffence));

        final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = new ArrayList<>();


        petDetailReceived.getPetDefendants().forEach(petDefendants ->
                petCaseDefendantOffenceList.add(buildPetCaseDefendantOffenceEntity(petDefendants.getDefendantId(), petDetailReceived.getCaseId(), petDetailReceived.getPetId(), false))
        );

        petCaseDefendantOffenceList.forEach(petCaseDefendantOffence -> petCaseDefendantOffenceRepository.save(petCaseDefendantOffence));

    }

    private PetCaseDefendantOffence buildPetCaseDefendantOffenceEntity(final UUID defendantId, final UUID caseId, final UUID petId, final Boolean isYouth) {

        final boolean youth = isYouth != null && isYouth;

        return PetCaseDefendantOffence.builder()
                .withPetkey(UUID.randomUUID())
                .withDefendantId(defendantId)
                .withCaseId(caseId)
                .withPetId(petId)
                .withIsYouth(youth)
                .withLastUpdated(now())
                .build();
    }


}
