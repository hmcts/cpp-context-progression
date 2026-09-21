package uk.gov.moj.cpp.progression.query;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import org.apache.commons.lang3.tuple.Pair;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PetCaseDefendantOffenceRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_VIEW)
public class PetQueryView {
    public static final String PET_ID = "petId";
    public static final String IS_YOUTH = "isYouth";
    public static final String CASE_ID = "caseId";
    public static final String DEFENDANTS = "defendants";
    public static final String PETS = "pets";
    public static final String OFFENCES = "offences";
    public static final String DEFENDANT_ID = "defendantId";
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String LAST_UPDATED = "lastUpdated";

    @Inject
    private PetCaseDefendantOffenceRepository petCaseDefendantOffenceRepository;

    @Handles("progression.query.pets-for-case")
    public JsonEnvelope getPetsForCase(JsonEnvelope envelope) {
        final UUID caseId = fromString(envelope.asJsonObject().getString(CASE_ID));
        final List<PetCaseDefendantOffence> petCaseDefendantOffenceListForCase = petCaseDefendantOffenceRepository.findByCaseId(caseId);
        final List<Pair<UUID, Boolean>> petIdPairs = petCaseDefendantOffenceListForCase.stream().map(def -> Pair.of(def.getPetId(), def.getIsYouth()))
                .distinct()
                .collect(Collectors.toList());

        final JsonArrayBuilder petsArrayBuilder = createArrayBuilder();

        petIdPairs.stream().map(petIdPair -> {
            final List<PetCaseDefendantOffence> petCaseDefendantOffenceList = petCaseDefendantOffenceListForCase.stream()
                    .filter(item -> item.getPetId().equals(petIdPair.getLeft()))
                    .collect(Collectors.toList());

            return petsArrayBuilder.add(buildPet(petIdPair.getLeft(), petIdPair.getRight(), petCaseDefendantOffenceList));
        }).collect(Collectors.toList());

        final JsonObject resultJson = createObjectBuilder().add(PETS, petsArrayBuilder).build();
        return envelopeFrom(envelope.metadata(), resultJson);
    }

    private JsonObject buildPet(final UUID petId, final boolean isYouth, final List<PetCaseDefendantOffence> petCaseDefendantOffenceList) {
        final Map<UUID, List<PetCaseDefendantOffence>> itemsGroupedByDefendant = groupByDefendantId(petCaseDefendantOffenceList);

        final JsonArrayBuilder defendants = createArrayBuilder();
        itemsGroupedByDefendant.values().forEach(list -> defendants.add(buildPetDefendant(list)));

        final JsonObjectBuilder petBuilder =  createObjectBuilder()
                .add(PET_ID, petId.toString())
                .add(IS_YOUTH, isYouth)
                .add(DEFENDANTS, defendants);

        final Optional<String> lastUpdated = petCaseDefendantOffenceList.stream()
                .filter(petCaseDefendantOffence -> nonNull(petCaseDefendantOffence.getLastUpdated()))
                .map(petOffence -> petOffence.getLastUpdated().format(ZONE_DATETIME_FORMATTER))
                .findFirst();

        lastUpdated.ifPresent(date -> petBuilder.add(LAST_UPDATED,date));

        return petBuilder.build();
    }

    private JsonObject buildPetDefendant(final List<PetCaseDefendantOffence> list) {
        final PetCaseDefendantOffence first = list.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("The PetCaseDefendantOffence list is empty."));


        return createObjectBuilder()
                .add(DEFENDANT_ID, first.getDefendantId().toString())
                .add(CASE_ID, first.getCaseId().toString())
                .build();
    }

    private Map<UUID, List<PetCaseDefendantOffence>> groupByDefendantId(final List<PetCaseDefendantOffence> petCaseDefendantOffenceList) {
        final Map<UUID, List<PetCaseDefendantOffence>> petCaseDefendantOffenceMapByDefendant = new HashMap<>();
        petCaseDefendantOffenceList.forEach(each -> {
            if (!petCaseDefendantOffenceMapByDefendant.containsKey(each.getDefendantId())) {
                petCaseDefendantOffenceMapByDefendant.put(each.getDefendantId(), new ArrayList<>());
            }

            petCaseDefendantOffenceMapByDefendant.get(each.getDefendantId()).add(each);
        });
        return petCaseDefendantOffenceMapByDefendant;
    }
}
