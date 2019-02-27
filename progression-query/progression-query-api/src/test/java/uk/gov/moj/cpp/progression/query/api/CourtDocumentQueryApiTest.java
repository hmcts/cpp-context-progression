package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentQueryApiTest {

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope queryForMaterial;

    @Mock
    private JsonEnvelope response;

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Function<Object, JsonEnvelope> objectJsonEnvelopeFunction;

    @InjectMocks
    private CourtDocumentApi courtDocumentApi;

    @Test
    public void shouldHandleCaseDocumentMetadataQuery() {

        JsonObject jsonObjectMaterial = createObjectBuilder().add("materialId", randomUUID().toString()).build();
        when(enveloper.withMetadataFrom(query, "material.query.material-metadata")).thenReturn(objectJsonEnvelopeFunction);
        when(objectJsonEnvelopeFunction.apply(jsonObjectMaterial)).thenReturn(queryForMaterial);
        when(requester.requestAsAdmin(queryForMaterial)).thenReturn(response);
        when(query.payloadAsJsonObject()).thenReturn(jsonObjectMaterial);
        assertThat(courtDocumentApi.getCaseDocumentMetadata(query), equalTo(response));
    }

    @Test
    public void shouldHandleCaseDocumentDetailsQuery() {
        assertThat(courtDocumentApi.getCaseDocumentDetails(query), equalTo(query));
    }


}
