package uk.gov.moj.cpp.progression.query.api;

import static java.time.LocalTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.FormQueryView;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FormQueryApiTest {

    @Mock
    private Requester requester;

    @Mock
    private FormQueryView formQueryView;

    @InjectMocks
    private FormQueryApi formQueryApi;

    @Test
    public void shouldReturnFormsForCase() {
        final String caseId = randomUUID().toString();

        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.form-for-case"), createObjectBuilder().build());

        final JsonEnvelope query = createEnvelope("progression.query.forms-for-case", createObjectBuilder()
                .add("caseId", caseId)
                .build());

        when(formQueryView.getFormsForCase(query)).thenReturn(envelope);

        final JsonEnvelope result = formQueryApi.getFormsForCase(query);
        assertThat(result, is(envelope));
    }

    @Test
    public void shouldReturnFormForGivenFormId() {
        final String caseId = randomUUID().toString();
        final String courtFormId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        final JsonEnvelope envelope = createEnvelope("progression.query.form", createObjectBuilder()
                .add("caseId", caseId)
                .add("courtFormId", courtFormId)
                .build());

        final JsonEnvelope materialResponse = envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("material.query.structured-form"),
                createObjectBuilder()
                        .add("structuredFormId", courtFormId)
                        .add("data", "this is form data as a string")
                        .add("lastUpdated", "12/10/2021")
                        .build());

        when(requester.request(any(Envelope.class)))
                .thenReturn(materialResponse);

        final JsonArray defendantsArray = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("defendantId", defendantId)
                        .build())
                .build();

        final JsonEnvelope defendantsResponse = envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("progression.query.form"),
                createObjectBuilder()
                        .add("caseId",  caseId)
                        .add("defendants", defendantsArray)
                        .add("formType", "BCM")
                        .build());

        when(formQueryView.getForm(envelope)).thenReturn(defendantsResponse);

        final JsonEnvelope result = formQueryApi.getForm(envelope);

        final JsonObject responsePayload = result.payloadAsJsonObject();
        assertThat(responsePayload.getString("courtFormId"), is(courtFormId));
        assertThat(responsePayload.getString("lastUpdated"), is("12/10/2021"));
        assertThat(responsePayload.getString("formData"), is("this is form data as a string"));
        assertThat(responsePayload.getString("formType"), is("BCM"));

        final JsonArray actualDefendants = responsePayload.getJsonArray("defendants");
        assertThat(actualDefendants, hasSize(1));
        assertThat(actualDefendants.getJsonObject(0).getString("defendantId"), is(defendantId));
    }

    @Test
    public void shouldReturnFormHistoryForGivenFormId() {
        final String caseId = randomUUID().toString();
        final String courtFormId = randomUUID().toString();
        final String firstName = "firstName";
        final String lastName = "Chapman";

        final JsonEnvelope envelope = createEnvelope("progression.query.form-change-history", createObjectBuilder()
                .add("caseId", caseId)
                .add("courtFormId", courtFormId)
                .build());

        final JsonArrayBuilder changeHistory = prepareChangeHistoryResponse(courtFormId, firstName, lastName);

        final JsonEnvelope materialResponse = envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("material.query.structured-form-change-history"),
                createObjectBuilder()
                        .add("structuredFormChangeHistory", changeHistory)
                        .build());

        when(requester.request(any(Envelope.class))).thenReturn(materialResponse);

        final JsonEnvelope result = formQueryApi.getFormChangeHistory(envelope);
        final JsonArray structuredFormChangeHistoryArray = result.payloadAsJsonObject().getJsonArray("formChangeHistory");

        assertThat(structuredFormChangeHistoryArray.size(), is(3));
        final JsonObject structuredFormChangeHistory1 = structuredFormChangeHistoryArray.getJsonObject(0);
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy"), is(notNullValue()));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy").getString("firstName"), is(firstName));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy").getString("lastName"), is(lastName));
        assertThat(structuredFormChangeHistory1.getString("status"), is("CREATED"));

        final JsonObject structuredFormChangeHistory2 = structuredFormChangeHistoryArray.getJsonObject(1);
        assertThat(structuredFormChangeHistory2.getString("status"), is("UPDATED"));

        final JsonObject structuredFormChangeHistory3 = structuredFormChangeHistoryArray.getJsonObject(2);
        assertThat(structuredFormChangeHistory3.getString("status"), is("FINALISED"));
    }

    private JsonArrayBuilder prepareChangeHistoryResponse(final String courtFormId, final String firstName, final String lastName) {
        final JsonArrayBuilder changeHistory = createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("structuredFormId", courtFormId)
                                .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135302")
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
                                .add("structuredFormId", courtFormId)
                                .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135302")
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
                                .add("structuredFormId", courtFormId)
                                .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135302")
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
