package uk.gov.moj.cpp.progression.query.api;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.PetQueryView;
import uk.gov.moj.cpp.progression.query.api.helper.PetForDefendantQueryHelper;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_API)
public class PetQueryApi {

    public static final String STRUCTURED_FORM_ID = "structuredFormId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String PET_ID = "petId";
    public static final String FORM_ID = "formId";
    public static final String DATA = "data";
    public static final String ID = "id";
    public static final String DATE = "date";
    public static final String UPDATED_BY = "updatedBy";
    public static final String STATUS = "status";
    public static final String MATERIAL_ID = "materialId";
    public static final String LAST_UPDATED = "lastUpdated";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String CASE_ID = "caseId";
    public static final String OFFENCES = "offences";
    public static final String DEFENCE = "defence";
    public static final String PROSECUTION = "prosecution";
    public static final String DEFENDANTS = "defendants";
    public static final String PETS = "pets";
    public static final String NAME = "name";


    @Inject
    private Requester requester;

    @Inject
    private PetQueryView petQueryView;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private PetForDefendantQueryHelper petForDefendantQueryHelper;

    @Handles("progression.query.pet")
    public JsonEnvelope getPet(final JsonEnvelope query) {
        final JsonEnvelope materialResponse = requester.request(envelop(createObjectBuilder().add(STRUCTURED_FORM_ID, query.payloadAsJsonObject().getString(PET_ID)).build()).withName("material.query.structured-form").withMetadataFrom(query));

        final JsonObject response = createObjectBuilder()
                .add(PET_ID, materialResponse.payloadAsJsonObject().getString(STRUCTURED_FORM_ID))
                .add(FORM_ID, materialResponse.payloadAsJsonObject().getString(FORM_ID))
                .add(DATA, materialResponse.payloadAsJsonObject().getString(DATA))
                .add(LAST_UPDATED, materialResponse.payloadAsJsonObject().getString(LAST_UPDATED))
                .build();

        return JsonEnvelope.envelopeFrom(query.metadata(), response);
    }

    @Handles("progression.query.pets-for-case")
    public JsonEnvelope getPetsForCase(final JsonEnvelope query) {
        return petQueryView.getPetsForCase(query);
    }

    @Handles("progression.query.pet-change-history")
    public JsonEnvelope getPetChangeHistory(final JsonEnvelope query) {
        final JsonEnvelope materialResponse = requester.request(envelop(createObjectBuilder().add(STRUCTURED_FORM_ID, query.payloadAsJsonObject().getString(PET_ID)).build()).withName("material.query.structured-form-change-history").withMetadataFrom(query));
        final JsonArrayBuilder materialArrayBuilder = createArrayBuilder();

        materialResponse.payloadAsJsonObject().getJsonArray("structuredFormChangeHistory").forEach(
                ch -> materialArrayBuilder.add(convertChangeHistory((JsonObject) ch))
        );

        return JsonEnvelope.envelopeFrom(query.metadata(), createObjectBuilder().add("petChangeHistory", materialArrayBuilder).build());

    }

    private JsonObject convertChangeHistory(final JsonObject changeHistory) {
        final JsonObjectBuilder historyBuilder = createObjectBuilder()
                .add(ID, changeHistory.getString(ID))
                .add(PET_ID, changeHistory.getString(STRUCTURED_FORM_ID))
                .add(FORM_ID, changeHistory.getString(FORM_ID))
                .add(DATE, changeHistory.getString(DATE))
                .add(UPDATED_BY, buildUpdatedResponse(changeHistory))
                .add(DATA, changeHistory.getString(DATA))
                .add(STATUS, changeHistory.getString(STATUS));

        if (changeHistory.containsKey(MATERIAL_ID)) {
            historyBuilder.add(MATERIAL_ID, changeHistory.getString(MATERIAL_ID));
        }
        return historyBuilder.build();
    }

    @Handles("progression.query.pet-for-defendant")
    public JsonEnvelope getPetForDefendant(final JsonEnvelope query) {
        final JsonObject payload = petForDefendantQueryHelper.buildPetForDefendant(requester, query);
        return JsonEnvelope.envelopeFrom(query.metadata(), payload);
    }

    private JsonObject buildUpdatedResponse(final JsonObject changeHistoryFromMaterialQuery) {
        final JsonObjectBuilder updatedUserBuilder = createObjectBuilder();
        final JsonObject updatedByFromMaterial = changeHistoryFromMaterialQuery.getJsonObject(UPDATED_BY);

        if (updatedByFromMaterial.containsKey(NAME)) {
            updatedUserBuilder.add(NAME, updatedByFromMaterial.getString(NAME));
        } else {
            updatedUserBuilder
                    .add(ID, updatedByFromMaterial.getString(ID))
                    .add(FIRST_NAME, updatedByFromMaterial.getString(FIRST_NAME))
                    .add(LAST_NAME, updatedByFromMaterial.getString(LAST_NAME));
        }
        return updatedUserBuilder.build();
    }
}
