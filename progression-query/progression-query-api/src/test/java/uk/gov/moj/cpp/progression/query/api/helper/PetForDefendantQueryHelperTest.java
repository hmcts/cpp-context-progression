package uk.gov.moj.cpp.progression.query.api.helper;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.CASE_ID;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.DATA;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.DEFENCE;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.DEFENDANTS;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.FORM_ID;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.ID;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.OFFENCES;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.PETS;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.PET_ID;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.PROSECUTION;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.PetQueryView;
import uk.gov.moj.cpp.progression.query.api.service.MaterialService;
import uk.gov.moj.cpp.progression.query.api.service.ProgressionService;

import java.io.IOException;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PetForDefendantQueryHelperTest {
    @Mock
    private Requester requester;

    @Mock
    private PetQueryView petQueryView;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private MaterialService materialService;

    @InjectMocks
    private PetForDefendantQueryHelper petForDefendantQueryHelper;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Test
    public void buildPetForDefendant_WhenDefendantIdMatched() {
        final String petId = randomUUID().toString();
        final String formId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();
        final String offenceId1 = randomUUID().toString();
        final String offenceId2 = randomUUID().toString();

        final JsonEnvelope query = createEnvelope("progression.query.pet-for-defendant", createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(DEFENDANT_ID, defendantId1)
                .build());

        final JsonObject petsForCasePayload = fetchPetsForCasePayload(petId, defendantId1, caseId, offenceId1,offenceId2);

        final JsonObject materialPetPayload = fetchMaterialPetPayload(petId, formId, defendantId1, defendantId2, defendantId3, caseId, offenceId1, offenceId2);

        when(progressionService.getPetsForCase(petQueryView, query, caseId)).thenReturn(petsForCasePayload);
        when(materialService.getPet(requester, query, petId)).thenReturn(materialPetPayload);

        final JsonObject payload = petForDefendantQueryHelper.buildPetForDefendant(requester, query);

        assertThat(payload.containsKey(PETS), is(true));
        assertThat(payload.getJsonArray(PETS).size(), is(1));
        final JsonObject defendantObj = payload.getJsonArray(PETS).getJsonObject(0);
        assertThat(defendantObj.getString(PET_ID), is(petId));
        assertThat(defendantObj.getString(FORM_ID), is(formId));
        assertThat(defendantObj.containsKey(OFFENCES), is(true));
        assertThat(defendantObj.getJsonArray(OFFENCES).size(), is(2));
        assertThat(defendantObj.getJsonArray(OFFENCES).getString(0), anyOf(is(offenceId1), is(offenceId2)));
        assertThat(defendantObj.getJsonArray(OFFENCES).getString(1), anyOf(is(offenceId1), is(offenceId2)));
        assertThat(defendantObj.containsKey(DATA), is(true));
        final JsonObject data = stringToJsonObjectConverter.convert(defendantObj.getString(DATA));
        assertThat(data.containsKey(DATA), is(true));
        assertThat(data.getJsonObject(DATA).containsKey(PROSECUTION), is(true));
        assertThat(data.getJsonObject(DATA).containsKey(DEFENCE), is(true));
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).containsKey(DEFENDANTS), is(true));
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).getJsonArray(DEFENDANTS).size(), is(1));
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).get(0).getString(ID), is(defendantId1));
    }

    @Test
    public void buildPetForDefendant_WhenMultiplePetExists_DefendantIdMatched() {
        final String petId = randomUUID().toString();
        final String formId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();
        final String offenceId1 = randomUUID().toString();
        final String offenceId2 = randomUUID().toString();

        final JsonEnvelope query = createEnvelope("progression.query.pet-for-defendant", createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(DEFENDANT_ID, defendantId1)
                .build());

        final JsonObject petsForCasePayload = fetchPetsForCasePayload(petId, defendantId1, caseId, offenceId1,offenceId2);

        final JsonObject materialPetPayload = fetchMaterialPetPayload(petId, formId, defendantId1, defendantId2, defendantId3, caseId, offenceId1, offenceId2);

        when(progressionService.getPetsForCase(petQueryView, query, caseId)).thenReturn(petsForCasePayload);
        when(materialService.getPet(requester, query, petId)).thenReturn(materialPetPayload);

        final JsonObject payload = petForDefendantQueryHelper.buildPetForDefendant(requester, query);

        assertThat(payload.containsKey(PETS), is(true));
        assertThat(payload.getJsonArray(PETS).size(), is(1));
        final JsonObject defendantObj = payload.getJsonArray(PETS).getJsonObject(0);
        assertThat(defendantObj.getString(PET_ID), is(petId));
        assertThat(defendantObj.getString(FORM_ID), is(formId));
        assertThat(defendantObj.containsKey(OFFENCES), is(true));
        assertThat(defendantObj.getJsonArray(OFFENCES).size(), is(2));
        assertThat(defendantObj.getJsonArray(OFFENCES).getString(0), anyOf(is(offenceId1), is(offenceId2)));
        assertThat(defendantObj.getJsonArray(OFFENCES).getString(1), anyOf(is(offenceId1), is(offenceId2)));
        assertThat(defendantObj.containsKey(DATA), is(true));
        final JsonObject data = stringToJsonObjectConverter.convert(defendantObj.getString(DATA));
        assertThat(data.containsKey(DATA), is(true));
        assertThat(data.getJsonObject(DATA).containsKey(PROSECUTION), is(true));
        assertThat(data.getJsonObject(DATA).containsKey(DEFENCE), is(true));
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).containsKey(DEFENDANTS), is(true));
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).getJsonArray(DEFENDANTS).size(), is(1));
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).get(0).getString(ID), is(defendantId1));
    }


    @Test
    public void buildPetForDefendant_WhenMultiplePetExists_DefendantIdNotMatched() {
        final String petId = randomUUID().toString();
        final String formId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();
        final String offenceId1 = randomUUID().toString();
        final String offenceId2 = randomUUID().toString();

        final JsonEnvelope query = createEnvelope("progression.query.pet-for-defendant", createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(DEFENDANT_ID, randomUUID().toString())
                .build());

        final JsonObject petsForCasePayload = fetchPetsForCasePayload(petId, defendantId1, caseId, offenceId1,offenceId2);

        final JsonObject materialPetPayload = fetchMaterialPetPayload(petId, formId, defendantId1, defendantId2, defendantId3, caseId, offenceId1, offenceId2);

        when(progressionService.getPetsForCase(petQueryView, query, caseId)).thenReturn(petsForCasePayload);

        final JsonObject payload = petForDefendantQueryHelper.buildPetForDefendant(requester, query);

        assertThat(payload.containsKey(PETS), is(true));
        assertThat(payload.getJsonArray(PETS).size(), is(0));
    }


    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), defaultCharset());
        } catch (final IOException e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    @Test
    public void buildPetForDefendantWhenDefendantIdMatched() {
        final String petId = randomUUID().toString();
        final String formId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();
        final String offenceId1 = randomUUID().toString();
        final String offenceId2 = randomUUID().toString();

        final JsonEnvelope query = createEnvelope("progression.query.pet-for-defendant", createObjectBuilder()
                .build());

        final JsonObject petsForCasePayload = fetchPetsForCasePayload(petId, defendantId1, caseId, offenceId1,offenceId2);

        final JsonObject materialPetPayload = fetchMaterialPetPayload(petId, formId, defendantId1, defendantId2, defendantId3, caseId, offenceId1, offenceId2);

        when(progressionService.getPetsForCase(petQueryView, query, caseId)).thenReturn(petsForCasePayload);
        when(materialService.getPet(requester, query, petId)).thenReturn(materialPetPayload);

        final JsonObject payload = petForDefendantQueryHelper.buildPetForDefendant(requester, query,caseId, defendantId1);

        assertThat(payload.containsKey(PETS), is(true));
        assertThat(payload.getJsonArray(PETS).size(), is(1));
        final JsonObject defendantObj = payload.getJsonArray(PETS).getJsonObject(0);
        assertThat(defendantObj.getString(PET_ID), is(petId));
        assertThat(defendantObj.getString(FORM_ID), is(formId));
        assertThat(defendantObj.containsKey(OFFENCES), is(true));
        assertThat(defendantObj.getJsonArray(OFFENCES).size(), is(2));
        assertThat(defendantObj.getJsonArray(OFFENCES).getString(0), anyOf(is(offenceId1), is(offenceId2)));
        assertThat(defendantObj.getJsonArray(OFFENCES).getString(1), anyOf(is(offenceId1), is(offenceId2)));
        assertThat(defendantObj.containsKey(DATA), is(true));
        final JsonObject data = stringToJsonObjectConverter.convert(defendantObj.getString(DATA));
        assertThat(data.containsKey(DATA), is(true));
        assertThat(data.getJsonObject(DATA).containsKey(PROSECUTION), is(true));
        assertThat(data.getJsonObject(DATA).containsKey(DEFENCE), is(true));
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).containsKey(DEFENDANTS), is(true));
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).getJsonArray(DEFENDANTS).size(), is(1));
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).get(0).getString(ID), is(defendantId1));
    }

    private JsonObject fetchPetsForCasePayload(final String petId, final String defendantId, final String caseId, final String offenceId1, final String offenceId2){
        return stringToJsonObjectConverter.convert(getPayload("json/progression.query.pets-for-case-one-defendant-only.json")
                .replaceAll("PET_ID", petId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("OFFENCE_ID1", offenceId1)
                .replaceAll("OFFENCE_ID2", offenceId2));
    }

    private JsonObject fetchMaterialPetPayload(final String petId, final String formId,final String defendantId1, final String defendantId2, final String defendantId3,final String caseId, final String offenceId1, final String offenceId2){
        return stringToJsonObjectConverter.convert(getPayload("json/material.query.structured-form.json")
                .replaceAll("PET_ID", petId)
                .replaceAll("FORM_ID", formId)
                .replaceAll("DEFENDANT_ID_1", defendantId1)
                .replaceAll("DEFENDANT_ID", defendantId1)
                .replaceAll("DEFENDANT_ID_2", defendantId2)
                .replaceAll("DEFENDANT_ID_3", defendantId3)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("OFFENCE_ID1", offenceId1)
                .replaceAll("OFFENCE_ID2", offenceId2));
    }
}
