package uk.gov.moj.cpp.progression.query.view.service.transformer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

@SuppressWarnings("squid:S1612")
public class WitnessPtphTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WitnessPtphTransformer.class);

    public static final String FORM_DATA = "formData";

    public static final String DATA = "data";

    public static final String WITNESSES = "witnesses";

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    public Map<UUID, String> transform(final JsonObject payload) {
        final Map<UUID, String> witnesses = new HashMap<>();


        if(nonNull(payload.getString(FORM_DATA, null))) {
            final JsonObject jsonObject = stringToJsonObjectConverter.convert(payload.getString(FORM_DATA));

            final JsonObject petForm = jsonObject.getJsonObject(DATA);

            if (nonNull(petForm.getJsonArray(WITNESSES))) {
                final JsonArray cpsParticipantsWitnesses = ofNullable(petForm.getJsonArray(WITNESSES)).orElse(Json.createArrayBuilder().build());

                LOGGER.info("cpsParticipantsWitnesses >> {}", cpsParticipantsWitnesses);

                IntStream.range(0, cpsParticipantsWitnesses.size()).mapToObj(
                        cpsCounter -> cpsParticipantsWitnesses.getJsonObject(cpsCounter))
                        .forEach(prosecutionWitnesse -> mapCpsParticipantWitness(witnesses, prosecutionWitnesse));
            }
        }
        return witnesses.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (lastValue, currentValue) -> lastValue, LinkedHashMap::new));
    }

    private String mapCpsParticipantWitness(final Map<UUID, String> witness, final JsonObject prosecutionWitnesse) {
        final String witnessFirstName = prosecutionWitnesse.getString("firstName", "");
        final String witnessLastName = prosecutionWitnesse.getString("lastName", "");

        return witness.putIfAbsent(nonNull(prosecutionWitnesse.getString("id")) ? UUID.fromString(prosecutionWitnesse.getString("id")) : UUID.randomUUID(),
                generateWitnessName(witnessFirstName, witnessLastName)
        );
    }

    private String generateWitnessName(final String firstName, final String lastName){
        if(StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName)){
            return firstName + " " + lastName;
        }else if(StringUtils.isNotEmpty(firstName)){
            return firstName;
        }
        return StringUtils.isNotEmpty(lastName) ? lastName : "";
    }
}
