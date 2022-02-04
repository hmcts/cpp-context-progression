package uk.gov.moj.cpp.progression.query.api.helper;

import com.google.common.io.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.service.MaterialService;
import uk.gov.moj.cpp.progression.query.api.service.ProgressionService;

import javax.json.JsonObject;

import java.io.IOException;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.CASE_ID;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.DATA;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.DEFENCE;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.DEFENDANTS;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.FORM_ID;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.OFFENCES;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.PETS;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.PET_ID;
import static uk.gov.moj.cpp.progression.query.api.PetQueryApi.PROSECUTION;

@RunWith(MockitoJUnitRunner.class)
public class PetForDefendantQueryHelperTest {
    @Mock
    private Requester requester;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private MaterialService materialService;

    @InjectMocks
    private PetForDefendantQueryHelper petForDefendantQueryHelper;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Test
    public void buildPetForDefendant() {
        final String petId = randomUUID().toString();
        final String formId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String offenceId1 = randomUUID().toString();
        final String offenceId2 = randomUUID().toString();

        final JsonEnvelope query = createEnvelope("progression.query.pet-for-defendant", createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(DEFENDANT_ID, defendantId)
                .build());

        final JsonObject petsForCasePayload = stringToJsonObjectConverter.convert(getPayload("json/progression.query.pets-for-case.json")
                .replaceAll("PET_ID", petId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("OFFENCE_ID1", offenceId1)
                .replaceAll("OFFENCE_ID2", offenceId2));

        final JsonObject materialPetPayload = stringToJsonObjectConverter.convert(getPayload("json/material.query.structured-form.json")
                .replaceAll("PET_ID", petId)
                .replaceAll("FORM_ID", formId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("OFFENCE_ID1", offenceId1)
                .replaceAll("OFFENCE_ID2", offenceId2));

        when(progressionService.getPetsForCase(requester, query, caseId)).thenReturn(petsForCasePayload);
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
        assertThat(data.getJsonObject(DATA).getJsonObject(DEFENCE).getJsonArray(DEFENDANTS).size(), is(2));
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
}
