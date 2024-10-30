package uk.gov.moj.cpp.progression.query.api;

import static java.time.LocalTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.PetQueryView;
import uk.gov.moj.cpp.progression.query.api.helper.PetForDefendantQueryHelper;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PetQueryApiTest {

    @Mock
    private Requester requester;

    @Mock
    private PetQueryView petQueryView;

    @InjectMocks
    private PetQueryApi petQueryApi;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private PetForDefendantQueryHelper petForDefendantQueryHelper;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Test
    public void shouldReturnPetsForCase() {
        final String caseId = randomUUID().toString();

        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.pets-for-case"), createObjectBuilder().build());

        final JsonEnvelope query = createEnvelope("progression.query.pets-for-case", createObjectBuilder()
                .add("caseId", caseId)
                .build());

        when(petQueryView.getPetsForCase(query)).thenReturn(envelope);

        final JsonEnvelope result = petQueryApi.getPetsForCase(query);
        assertThat(result, is(envelope));
    }

    @Test
    public void shouldReturnPetForDefendant(){
        final JsonObject payload = createObjectBuilder().build();

        final JsonEnvelope query = createEnvelope("progression.query.pet-for-defendant", createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("defendantId", randomUUID().toString())
                .build());

        when(petForDefendantQueryHelper.buildPetForDefendant(eq(requester), any())).thenReturn(payload);
        final JsonEnvelope result = petQueryApi.getPetForDefendant(query);
        assertThat(result.payloadAsJsonObject(), is(payload));
    }

    @Test
    public void shouldReturnPet(){
        final String petId = randomUUID().toString();
        final String formId = randomUUID().toString();

        final JsonEnvelope query = createEnvelope("progression.query.pet", createObjectBuilder()
                .add("petId", petId)
                .add("formId", formId)
                .add("data", "test")
                .add("lastUpdated", "2023-01-13T00:00Z[UTC]")
                .build());

        final JsonEnvelope materialResponse = envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("material.query.structured-form"),
                createObjectBuilder()
                        .add("structuredFormId", petId)
                        .add("formId", formId)
                        .add("data", "test")
                        .add("lastUpdated", "2023-01-13T00:00Z[UTC]")
                        .build());

        when(requester.request(any(Envelope.class))).thenReturn(materialResponse);

        final JsonEnvelope result = petQueryApi.getPet(query);
        assertThat(result.payloadAsJsonObject(), is(query.payloadAsJsonObject()));
    }

    @Test
    public void shouldGetPetChangeHistory(){
        final String petId = randomUUID().toString();
        final String formId = randomUUID().toString();
        final String firstName = "firstName";
        final String lastName = "Chapman";

        final JsonEnvelope query = createEnvelope("progression.query.pet-change-history", createObjectBuilder()
                .add("petId", petId)
                .build());

        final JsonArrayBuilder changeHistory = prepareChangeHistoryResponse(petId, firstName, lastName);

        final JsonEnvelope materialResponse = envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("material.query.structured-form-change-history"),
                createObjectBuilder()
                        .add("structuredFormChangeHistory", changeHistory)
                        .build());

        when(requester.request(any(Envelope.class))).thenReturn(materialResponse);

        final JsonEnvelope result = petQueryApi.getPetChangeHistory(query);
        assertThat(result.payloadAsJsonObject().getJsonArray("petChangeHistory").size(), is(3));
    }

    private JsonArrayBuilder prepareChangeHistoryResponse(final String petId, final String firstName, final String lastName) {
        final JsonArrayBuilder changeHistory = createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("structuredFormId", petId)
                                .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135302")
                                .add("formId", randomUUID().toString())
                                .add("date", now().toString())
                                .add("data", "this is form data")
                                .add("status", "CREATED")
                                .add("updatedBy", createObjectBuilder()
                                        .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135303")
                                        .add("firstName", firstName)
                                        .add("lastName", lastName)
                                        .build()
                                )

                ).add(
                        createObjectBuilder()
                                .add("structuredFormId", petId)
                                .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135302")
                                .add("formId", randomUUID().toString())
                                .add("date", now().toString())
                                .add("data", "this is form data")
                                .add("status", "UPDATED")
                                .add("updatedBy", createObjectBuilder()
                                        .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135303")
                                        .add("firstName", firstName)
                                        .add("lastName", lastName)
                                        .build()
                                )

                ).add(
                        createObjectBuilder()
                                .add("structuredFormId", petId)
                                .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135302")
                                .add("formId", randomUUID().toString())
                                .add("date", now().toString())
                                .add("data", "this is form data")
                                .add("status", "FINALISED")
                                .add("updatedBy", createObjectBuilder()
                                        .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135303")
                                        .add("firstName", firstName)
                                        .add("lastName", lastName)
                                        .build()
                                )
                );
        return changeHistory;
    }
}
