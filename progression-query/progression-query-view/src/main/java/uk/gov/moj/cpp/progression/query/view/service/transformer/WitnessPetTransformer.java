package uk.gov.moj.cpp.progression.query.view.service.transformer;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.LinkedHashMap;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

public class WitnessPetTransformer {

    public static final String PROSECUTION = "prosecution";
    public static final String WITNESSES = "witnesses";
    public static final String DEFENCE = "defence";
    public static final String DEFENDANTS = "defendants";
    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    public Map<UUID, String> transform(final JsonObject payload) {
        final Map<UUID, String> witnesses = new HashMap<>();
        if (nonNull(payload.getString("data", null))) {
            final JsonObject jsonObject = stringToJsonObjectConverter.convert(payload.getString("data"));
            final JsonObject petForm = jsonObject.getJsonObject("data");
            if (nonNull(petForm.getJsonObject(PROSECUTION))) {
                final JsonArray prosecutionWitnesses = ofNullable(petForm.getJsonObject(PROSECUTION).getJsonArray(WITNESSES)).orElse(JsonObjects.createArrayBuilder().build());
                IntStream.range(0, prosecutionWitnesses.size()).mapToObj(prosecutionWitnesses::getJsonObject).forEach(prosecutionWitnesse ->
                        mapWitness(witnesses, prosecutionWitnesse)
                );
            }
            if (nonNull(petForm.getJsonObject(DEFENCE)) && nonNull(petForm.getJsonObject(DEFENCE).getJsonArray(DEFENDANTS))) {
                final JsonArray defendants = petForm.getJsonObject(DEFENCE).getJsonArray(DEFENDANTS);
                IntStream.range(0, defendants.size()).mapToObj(defendants::getJsonObject).forEach(defendant ->
                        {
                            final JsonArray prosecutionWitnesses = ofNullable(defendant.getJsonArray(WITNESSES)).orElse(JsonObjects.createArrayBuilder().build());
                            IntStream.range(0, prosecutionWitnesses.size()).mapToObj(prosecutionWitnesses::getJsonObject).forEach(prosecutionWitnesse ->
                                    mapWitness(witnesses, prosecutionWitnesse)
                            );
                        }
                );
            }
        }
        return witnesses.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    private String mapWitness(final Map<UUID, String> witnesses, final JsonObject prosecutionWitnesse) {
        return witnesses.putIfAbsent(UUID.fromString(prosecutionWitnesse.getString("id")),
                prosecutionWitnesse.getString("firstName", "")
                        .concat(" ")
                        .concat(prosecutionWitnesse.getString("lastName", ""))
        );
    }
}
