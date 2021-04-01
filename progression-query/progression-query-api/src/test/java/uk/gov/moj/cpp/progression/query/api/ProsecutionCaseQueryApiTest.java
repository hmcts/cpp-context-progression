package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.query.api.service.CourtOrderService;
import uk.gov.moj.cpp.progression.query.api.service.OrganisationService;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseQueryApiTest {

    private static final String JSON_CAAG_RESPONSE_JSON = "json/caagQueryResponse.json";
    private static final String JSON_CAAG_NO_DEFENDANTS_RESPONSE_JSON = "json/caagQueryNoDefendantsResponse.json";
    private static final String JSON_CAAG_EXPECTED_RESPONSE_JSON = "json/caagQueryResponse_expected.json";
    private static final String JSON_ASSOCIATED_ORGANISATION_JSON = "json/associatedOrganisation_response.json";
    private static final String CAAG_PROSECUTION_QUERY = "progression.query.prosecutioncase.caag";

    private static final String PROSECUTION_CASE_QUERY_VIEW_JSON = "json/prosecutionCaseQueryResponse.json";
    private static final String PROSECUTION_CASE_QUERY_VIEW_MULTIPLE_DEFENDANTS_JSON = "json/prosecutionCaseQueryMultipleDefendantsResponse.json";
    private static final String DEFENDANT_WITH_COURT_ORDERS_JSON = "json/defendantWithCourtOrders.json";
    private static final String DEFENDANT_WITH_NO_COURT_ORDERS_JSON = "json/defendantWithNoCourtOrders.json";
    private static final String PROSECUTION_CASE_QUERY_API_EXPECTED_WITH_COURT_ORDERS_JSON = "json/caseQueryApiWithCourtOrdersExpectedResponse.json";
    private static final String PROSECUTION_CASE_QUERY_API_EXPECTED_WITH_COURT_ORDERS_MULTIPLE_DEFENDANTS_JSON = "json/caseQueryApiWithCourtOrdersMultipleDefendantsExpectedResponse.json";
    private static final String PROSECUTION_CASE_QUERY_API_EXPECTED_WIT_NO_COURT_ORDERS_JSON = "json/caseQueryApiWithNoCourtOrdersExpectedResponse.json";
    private static final String PROSECUTION_CASE_QUERY = "progression.query.prosecutioncase";

    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;

    @Mock
    private Requester requester;

    @InjectMocks
    private ProsecutionCaseQueryApi prosecutionCaseQueryApi;

    @Mock
    private OrganisationService organisationService;

    @Mock
    private CourtOrderService courtOrderService;

    @Test
    public void shouldHandleGetCaseHearings() {
        when(requester.request(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.getCaseHearings(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchProsecutionCaseQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchCaseProsecutionCase(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchForUserGroupsByMaterialId() {
        when(requester.request(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchForUserGroupsByMaterialId(query), equalTo(response));
    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithCourtOrders() {
        final JsonObject prosecutionCasePayload = QueryClientTestBase.readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = QueryClientTestBase.readJson(DEFENDANT_WITH_COURT_ORDERS_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        when(requester.request(query)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(query);

        final JsonObject expectedProsecutionCaseResponse = QueryClientTestBase.readJson(PROSECUTION_CASE_QUERY_API_EXPECTED_WITH_COURT_ORDERS_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithCourtOrdersMultipleDefendantIds() {
        final JsonObject prosecutionCasePayload = QueryClientTestBase.readJson(PROSECUTION_CASE_QUERY_VIEW_MULTIPLE_DEFENDANTS_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = QueryClientTestBase.readJson(DEFENDANT_WITH_COURT_ORDERS_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        when(requester.request(query)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(query);

        final JsonObject expectedProsecutionCaseResponse = QueryClientTestBase.readJson(PROSECUTION_CASE_QUERY_API_EXPECTED_WITH_COURT_ORDERS_MULTIPLE_DEFENDANTS_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithNoCourtOrders() {
        final JsonObject prosecutionCasePayload = QueryClientTestBase.readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = Json.createObjectBuilder().build();

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        when(requester.request(query)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(query);

        final JsonObject expectedProsecutionCaseResponse = QueryClientTestBase.readJson(PROSECUTION_CASE_QUERY_API_EXPECTED_WIT_NO_COURT_ORDERS_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithEmptyCourtOrders() {
        final JsonObject prosecutionCasePayload = QueryClientTestBase.readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = Json.createObjectBuilder().add("courtOrders", Json.createArrayBuilder().build()).build();

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        when(requester.request(query)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(query);

        final JsonObject expectedProsecutionCaseResponse = QueryClientTestBase.readJson(PROSECUTION_CASE_QUERY_API_EXPECTED_WIT_NO_COURT_ORDERS_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }


    @Test
    public void shouldHandleProsecutionCaseAtAGlanceWithRepresentation() {
        final JsonObject caagResponse = QueryClientTestBase.readJson(JSON_CAAG_RESPONSE_JSON, JsonObject.class);
        final JsonObject jsonObjectPayload = QueryClientTestBase.readJson(JSON_ASSOCIATED_ORGANISATION_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(CAAG_PROSECUTION_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, caagResponse);

        when(requester.request(query)).thenReturn(envelope);
        when(organisationService.getAssociatedOrganisation(any(), any(), any())).thenReturn(jsonObjectPayload);
        final JsonEnvelope prosecutionCaseForCaseAtAGlance = prosecutionCaseQueryApi.getProsecutionCaseForCaseAtAGlance(query);

        final JsonObject expectedCaagResponse = QueryClientTestBase.readJson(JSON_CAAG_EXPECTED_RESPONSE_JSON, JsonObject.class);

        assertThat(prosecutionCaseForCaseAtAGlance.payloadAsJsonObject(), equalTo(expectedCaagResponse));
    }


    @Test
    public void shouldHandleProsecutionCaseAtAGlanceWithOutRepresentation() {
        final JsonObject caagResponse = QueryClientTestBase.readJson(JSON_CAAG_RESPONSE_JSON, JsonObject.class);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().build();

        final Metadata metadata = QueryClientTestBase.metadataFor(CAAG_PROSECUTION_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, caagResponse);

        when(requester.request(query)).thenReturn(envelope);
        when(organisationService.getAssociatedOrganisation(any(), any(), any())).thenReturn(jsonObjectPayload);
        final JsonEnvelope prosecutionCaseForCaseAtAGlance = prosecutionCaseQueryApi.getProsecutionCaseForCaseAtAGlance(query);

        assertThat(prosecutionCaseForCaseAtAGlance.payloadAsJsonObject(), equalTo(caagResponse));
    }

    @Test
    public void shouldHandleProsecutionCaseAtAGlanceWithNoDefendant() {
        final JsonObject caagResponse = QueryClientTestBase.readJson(JSON_CAAG_NO_DEFENDANTS_RESPONSE_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(CAAG_PROSECUTION_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, caagResponse);

        when(requester.request(query)).thenReturn(envelope);
        final JsonEnvelope prosecutionCaseForCaseAtAGlance = prosecutionCaseQueryApi.getProsecutionCaseForCaseAtAGlance(query);

        assertThat(prosecutionCaseForCaseAtAGlance.payloadAsJsonObject(), equalTo(caagResponse));
    }
}
