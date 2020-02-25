package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentQueryApiTest {

    public static final String JSON_COURT_DOCUMENT_WITH_RBAC_JSON = "json/courtDocumentWithRBAC.json";
    public static final String PROGRESSION_QUERY_COURTDOCUMENT = "progression.query.courtdocument";
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

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private CourtDocumentApi courtDocumentApi;

    @InjectMocks
    private CourtDocumentQueryApi courtDocumentQueryApi;


    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCaseDocumentMetadataQuery() {

        final JsonObject jsonObjectMaterial = createObjectBuilder().add("materialId", randomUUID().toString()).build();
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

    @Test
    public void shouldHandleCourtDocumentNotificationStatusQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(courtDocumentQueryApi.getCaseNotificationStatus(query), equalTo(response));
    }

    @Test
    public void shouldHandleCourtDocumentQuery() {

        final JsonObject jsonObjectPayload = QueryClientTestBase.readJson(JSON_COURT_DOCUMENT_WITH_RBAC_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(PROGRESSION_QUERY_COURTDOCUMENT);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.request(any())).thenReturn(envelope);
        courtDocumentQueryApi.getCourtDocument(envelope);
        assertThat(courtDocumentQueryApi.getCourtDocument(envelope), equalTo(envelope));
    }


}
