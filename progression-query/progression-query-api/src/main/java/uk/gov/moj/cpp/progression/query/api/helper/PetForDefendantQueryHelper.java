package uk.gov.moj.cpp.progression.query.api.helper;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.PetQueryView;
import uk.gov.moj.cpp.progression.query.api.service.MaterialService;
import uk.gov.moj.cpp.progression.query.api.service.ProgressionService;

import java.util.Optional;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class PetForDefendantQueryHelper {
    public static final String DEFENDANT_ID = "defendantId";
    public static final String PET_ID = "petId";
    public static final String FORM_ID = "formId";
    public static final String DATA = "data";
    public static final String ID = "id";
    public static final String LAST_UPDATED = "lastUpdated";
    public static final String CASE_ID = "caseId";
    public static final String OFFENCES = "offences";
    public static final String DEFENCE = "defence";
    public static final String DEFENDANTS = "defendants";
    public static final String PETS = "pets";

    @Inject
    private ProgressionService progressionService;

    @Inject
    private MaterialService materialService;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private PetQueryView petQueryView;

    public JsonObject buildPetForDefendant(final Requester requester, final JsonEnvelope query){
        final String caseId = query.payloadAsJsonObject().getString(CASE_ID);

        final JsonArray petsJsonArray = progressionService.getPetsForCase(petQueryView, query, caseId).getJsonArray(PETS);
        final JsonArrayBuilder petsArrayBuilder = createArrayBuilder();
        final String defendantId = query.payloadAsJsonObject().getString(DEFENDANT_ID);
        petsJsonArray.stream()
                .map(JsonObject.class::cast)
                .filter(pet -> pet.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream()
                        .anyMatch(petDefendant -> petDefendant.getString(DEFENDANT_ID).equals(defendantId)))
                .forEach(obj -> {
                    final JsonObject pet = convertToPetForDefendant(requester, obj, query);
                    if(nonNull(pet)) {
                        petsArrayBuilder.add(pet);
                    }
                });


        return createObjectBuilder().add(PETS, petsArrayBuilder).build();

    }

    public JsonObject buildPetForDefendant(final Requester requester, final JsonEnvelope query, final String caseId, final String defendantId) {

        final JsonArray petsJsonArray = progressionService.getPetsForCase(petQueryView, query, caseId).getJsonArray(PETS);
        final JsonArrayBuilder petsArrayBuilder = createArrayBuilder();

        petsJsonArray.stream()
                .map(JsonObject.class::cast)
                .forEach(obj -> {
                    final JsonObject pet = convertToPetForDefendant(requester, obj, query, defendantId);
                    if(nonNull(pet)) {
                        petsArrayBuilder.add(pet);
                    }
                });


        return createObjectBuilder().add(PETS, petsArrayBuilder).build();

    }

    private JsonObject convertToPetForDefendant(final Requester requester, final JsonObject petDefendantOffence, final JsonEnvelope query) {
        final String petId = petDefendantOffence.getString(PET_ID);
        final String defendantId = query.payloadAsJsonObject().getString(DEFENDANT_ID);

        final JsonObject petFormPayload = materialService.getPet(requester, query, petId);
        final JsonObject petData = stringToJsonObjectConverter.convert(petFormPayload.getString(DATA));
        final JsonObject defendantPetData = buildDefendantPetData(petData, defendantId);

        final JsonArray offences = getOffencesOfDefendant(petDefendantOffence, defendantId);
        final JsonObjectBuilder jsonObjectBuilder = JsonObjects.createObjectBuilder();
        if (!defendantPetData.isEmpty()) {
            jsonObjectBuilder
                    .add(PET_ID, petId)
                    .add(FORM_ID, petFormPayload.getString(FORM_ID))
                    .add(DATA, defendantPetData.toString())
                    .add(LAST_UPDATED, petFormPayload.getString(LAST_UPDATED));
        }
        if (!offences.isEmpty()) {
            jsonObjectBuilder.add(OFFENCES, offences);
        }
        return jsonObjectBuilder.build();
    }

    private JsonObject convertToPetForDefendant(final Requester requester, final JsonObject petDefendantOffence, final JsonEnvelope query, final String defendantId) {
        final String petId = petDefendantOffence.getString(PET_ID);

        final JsonObject petFormPayload = materialService.getPet(requester, query, petId);
        final JsonObject petData = stringToJsonObjectConverter.convert(petFormPayload.getString(DATA));
        final JsonObject defendantPetData = buildDefendantPetData(petData, defendantId);

        final JsonArray offences = getOffencesOfDefendant(petDefendantOffence, defendantId);

        if (!offences.isEmpty()) {
            return createObjectBuilder()
                    .add(PET_ID, petId)
                    .add(FORM_ID, petFormPayload.getString(FORM_ID))
                    .add(OFFENCES, offences)
                    .add(DATA, defendantPetData.toString())
                    .add(LAST_UPDATED, petFormPayload.getString(LAST_UPDATED))
                    .build();
        } else {
            return null;
        }

    }


    private JsonArray getOffencesOfDefendant(final JsonObject petDefendantOffence, final String defendantId){
        final JsonArray defendants = petDefendantOffence.getJsonArray(DEFENDANTS);
        final Optional<JsonObject> defendant = defendants.stream()
                .map(JsonObject.class::cast)
                .filter(o -> o.getString(DEFENDANT_ID).equals(defendantId))
                .findFirst();

        return defendant.map(def -> def.getJsonArray(OFFENCES)).orElse(createArrayBuilder().build());
    }

    private JsonObject buildDefendantPetData(final JsonObject petData, final String defendantId) {
        final Optional<JsonObject> defendantData = petData.getJsonObject(DATA).getJsonObject(DEFENCE).getJsonArray(DEFENDANTS).stream()
                .map(JsonObject.class::cast)
                .filter(o -> o.getString(ID).equals(defendantId))
                .findFirst();

        final JsonArrayBuilder defendantsBuilder = createArrayBuilder();
        defendantData.ifPresent(defendantsBuilder::add);

        final JsonObject defenceSection = createObjectBuilder()
                .add(DEFENDANTS, defendantsBuilder)
                .build();

        final JsonObjectBuilder builder = createObjectBuilder();
        petData.getJsonObject(DATA).forEach((key, value)-> {
            if (key.equals(DEFENCE)){
                builder.add(DEFENCE, defenceSection);
            } else {
                builder.add(key, value);
            }
        });

        return createObjectBuilder().add(DATA, builder.build()).build();
    }

}
