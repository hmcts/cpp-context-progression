package uk.gov.moj.cpp.progression.query.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.helper.PetForDefendantQueryHelper;

import javax.json.JsonObject;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

@RunWith(MockitoJUnitRunner.class)
public class PetQueryApiTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private PetQueryApi petQueryApi;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private PetForDefendantQueryHelper petForDefendantQueryHelper;

    @Test
    public void shouldReturnPetsForCase() {
        final String caseId = randomUUID().toString();

        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.pets-for-case"), createObjectBuilder().build());

        final JsonEnvelope query = createEnvelope("progression.query.pets-for-case", createObjectBuilder()
                .add("caseId", caseId)
                .build());

        when(requester.request(query)).thenReturn(envelope);

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



}
