package uk.gov.moj.cpp.progression.query;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PetCaseDefendantOffenceRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;

import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

@ServiceComponent(Component.QUERY_VIEW)
public class PetQueryView {
    public static final String PET_ID = "petId";
    public static final String CASE_ID = "caseId";
    public static final String DEFENDANTS = "defendants";
    public static final String PETS = "pets";
    public static final String OFFENCES = "offences";
    public static final String DEFENDANT_ID = "defendantId";

    @Inject
    private PetCaseDefendantOffenceRepository petCaseDefendantOffenceRepository;

    @Handles("progression.query.pets-for-case")
    public JsonEnvelope getPetsForCase(JsonEnvelope envelope) {
        final UUID caseId = fromString(envelope.asJsonObject().getString(CASE_ID));
        final List<PetCaseDefendantOffence> petCaseDefendantOffenceListForCase = petCaseDefendantOffenceRepository.findByCaseId(caseId);
        final List<UUID> petIds = petCaseDefendantOffenceListForCase.stream().map(PetCaseDefendantOffence::getPetId)
                .distinct()
                .collect(Collectors.toList());

        final JsonArrayBuilder petsArrayBuilder = createArrayBuilder();

        petIds.stream().map( petId -> {
                    final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = petCaseDefendantOffenceListForCase.stream()
                            .filter(item -> item.getPetId().equals(petId))
                            .collect(Collectors.toList());

                    return petsArrayBuilder.add(buildPet(petId, petCaseDefendantOffenceList));
                }).collect(Collectors.toList());

        final JsonObject resultJson = createObjectBuilder().add(PETS, petsArrayBuilder).build();
        return envelopeFrom(envelope.metadata(), resultJson);
    }

    private JsonObject buildPet(final UUID petId, final List<PetCaseDefendantOffence> petCaseDefendantOffenceList){
        final Map<UUID, List<PetCaseDefendantOffence>> itemsGroupedByDefendant = groupByDefendantId(petCaseDefendantOffenceList);

        final JsonArrayBuilder defendants = createArrayBuilder();
        itemsGroupedByDefendant.values().forEach(list -> defendants.add(buildPetDefendant(list)));

        return createObjectBuilder()
                .add(PET_ID, petId.toString())
                .add(DEFENDANTS, defendants)
                .build();
    }

    private JsonObject buildPetDefendant(final List<PetCaseDefendantOffence> list) {
        final PetCaseDefendantOffence first = list.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("The PetCaseDefendantOffence list is empty."));

        final JsonArrayBuilder offences = createArrayBuilder();
        list.forEach(each -> offences.add(each.getId().getOffenceId().toString()));


        return createObjectBuilder()
                .add(DEFENDANT_ID, first.getId().getDefendantId().toString())
                .add(CASE_ID, first.getCaseId().toString())
                .add(OFFENCES, offences)
                .build();
    }

    private Map<UUID, List<PetCaseDefendantOffence>> groupByDefendantId(final List<PetCaseDefendantOffence> petCaseDefendantOffenceList) {
        final Map<UUID, List<PetCaseDefendantOffence>> petCaseDefendantOffenceMapByDefendant = new HashMap<>();
        petCaseDefendantOffenceList.forEach(each -> {
            if (!petCaseDefendantOffenceMapByDefendant.containsKey(each.getId().getDefendantId())){
                petCaseDefendantOffenceMapByDefendant.put(each.getId().getDefendantId(), new ArrayList<>());
            }

            petCaseDefendantOffenceMapByDefendant.get(each.getId().getDefendantId()).add(each);
        });
        return petCaseDefendantOffenceMapByDefendant;
    }
}
