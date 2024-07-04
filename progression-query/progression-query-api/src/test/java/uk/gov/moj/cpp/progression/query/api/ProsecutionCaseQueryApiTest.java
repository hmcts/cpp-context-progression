package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.QueryClientTestBase.readJson;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.moj.cpp.progression.query.api.CourtDocumentQueryApi.CASE_ID;
import static uk.gov.moj.cpp.progression.query.api.CourtDocumentQueryApi.COURT_DOCUMENTS_SEARCH_NAME;
import static uk.gov.moj.cpp.progression.query.api.CourtDocumentQueryApi.COURT_DOCUMENTS_SEARCH_PROSECUTION;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.api.service.CourtOrderService;
import uk.gov.moj.cpp.progression.query.api.service.OrganisationService;
import uk.gov.moj.cpp.progression.query.api.service.UsersGroupQueryService;

import java.util.Optional;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseQueryApiTest {

    private static final String JSON_CAAG_RESPONSE_JSON = "json/caagQueryResponse.json";
    private static final String JSON_CAAG_NO_DEFENDANTS_RESPONSE_JSON = "json/caagQueryNoDefendantsResponse.json";
    private static final String JSON_CAAG_EXPECTED_RESPONSE_JSON = "json/caagQueryResponse_expected.json";
    private static final String JSON_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_JSON = "json/associatedCaseDefendantsOrganisation.json";
    private static final String CAAG_PROSECUTION_QUERY = "progression.query.prosecutioncase.caag";

    private static final String PROSECUTION_CASE_QUERY_VIEW_JSON = "json/prosecutionCaseQueryResponse.json";
    private static final String PROSECUTION_CASE_QUERY_VIEW_MULTIPLE_DEFENDANTS_JSON = "json/prosecutionCaseQueryMultipleDefendantsResponse.json";
    private static final String DEFENDANT_WITH_COURT_ORDERS_JSON = "json/defendantWithCourtOrders.json";
    private static final String PROSECUTION_CASE_QUERY_API_EXPECTED_WITH_COURT_ORDERS_JSON = "json/caseQueryApiWithCourtOrdersExpectedResponse.json";
    private static final String PROSECUTION_CASE_QUERY_API_EXPECTED_WITH_COURT_ORDERS_MULTIPLE_DEFENDANTS_JSON = "json/caseQueryApiWithCourtOrdersMultipleDefendantsExpectedResponse.json";
    private static final String PROSECUTION_CASE_QUERY_API_EXPECTED_WIT_NO_COURT_ORDERS_JSON = "json/caseQueryApiWithNoCourtOrdersExpectedResponse.json";
    private static final String CASE_QUERY_VIEW_JSON = "json/caseQueryResponse.json";
    private static final String COTR_QUERY_VIEW_JSON = "json/cotrQueryResponse.json";
    private static final String CASE_QUERY_API_EXPECTED_JSON = "json/caseQueryExpectedResponse.json";
    private static final String COTR_QUERY_API_EXPECTED_JSON = "json/cotrQueryExpectedResponse.json";

    private static final String PROSECUTION_CASE_QUERY = "progression.query.prosecutioncase";
    private static final String COTR_CASE_QUERY = "progression.query.cotr.details.prosecutioncase";


    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;

    @Mock
    private Requester requester;

    @Mock
    private ProsecutionCaseQuery prosecutionCaseQuery;

    @InjectMocks
    private ProsecutionCaseQueryApi prosecutionCaseQueryApi;

    @Mock
    private OrganisationService organisationService;

    @Mock
    private CourtOrderService courtOrderService;

    @Mock
    UsersGroupQueryService usersGroupQueryService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleGetCaseHearings() {
        when(prosecutionCaseQuery.getCaseHearings(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.getCaseHearings(query), equalTo(response));
    }

    @Test
    public void shouldHandleGetCaseDefendantHearings() {
        when(prosecutionCaseQuery.getCaseDefendantHearings(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.getCaseDefendantHearings(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchProsecutionCaseQuery() {
        when(prosecutionCaseQuery.searchCase(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchCaseProsecutionCase(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchProsecutorIdProsecutionAuthorityIdByCaseId() {
        when(prosecutionCaseQuery.searchProsecutorIdProsecutionAuthorityId(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchProsecutorIdProsecutionAuthorityIdByCaseId(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchForUserGroupsByMaterialId() {
        when(prosecutionCaseQuery.searchByMaterialId(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchForUserGroupsByMaterialId(query), equalTo(response));
    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithNoRecord() {

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonObject emptyPayload = createObjectBuilder().build();
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, emptyPayload);

        final String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final JsonEnvelope queryEnvelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(prosecutionCaseQuery.getProsecutionCase(queryEnvelope)).thenReturn(envelope);

        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(queryEnvelope);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(emptyPayload));
    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithCourtOrders() {
        final JsonObject prosecutionCasePayload = readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = readJson(DEFENDANT_WITH_COURT_ORDERS_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        final String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final JsonEnvelope queryEnvelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(prosecutionCaseQuery.getProsecutionCase(queryEnvelope)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());

        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(queryEnvelope);

        final JsonObject expectedProsecutionCaseResponse = readJson(PROSECUTION_CASE_QUERY_API_EXPECTED_WITH_COURT_ORDERS_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithCourtOrdersMultipleDefendantIds() {
        final JsonObject prosecutionCasePayload = readJson(PROSECUTION_CASE_QUERY_VIEW_MULTIPLE_DEFENDANTS_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = readJson(DEFENDANT_WITH_COURT_ORDERS_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        final String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final JsonEnvelope queryEnvelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(prosecutionCaseQuery.getProsecutionCase(queryEnvelope)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());

        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(queryEnvelope);

        final JsonObject expectedProsecutionCaseResponse = readJson(PROSECUTION_CASE_QUERY_API_EXPECTED_WITH_COURT_ORDERS_MULTIPLE_DEFENDANTS_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithNoCourtOrders() {
        final JsonObject prosecutionCasePayload = readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = createObjectBuilder().build();

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        final String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final JsonEnvelope queryEnvelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(prosecutionCaseQuery.getProsecutionCase(queryEnvelope)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());


        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(queryEnvelope);

        final JsonObject expectedProsecutionCaseResponse = readJson(PROSECUTION_CASE_QUERY_API_EXPECTED_WIT_NO_COURT_ORDERS_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithEmptyCourtOrders() {
        final JsonObject prosecutionCasePayload = readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = createObjectBuilder().add("courtOrders", Json.createArrayBuilder().build()).build();

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        final String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final JsonEnvelope queryEnvelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(prosecutionCaseQuery.getProsecutionCase(queryEnvelope)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());

        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(queryEnvelope);

        final JsonObject expectedProsecutionCaseResponse = readJson(PROSECUTION_CASE_QUERY_API_EXPECTED_WIT_NO_COURT_ORDERS_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }


    @Test
    public void shouldHandleProsecutionCaseAtAGlanceWithRepresentation() {
        final JsonObject caagResponse = readJson(JSON_CAAG_RESPONSE_JSON, JsonObject.class);
        final JsonObject jsonObjectPayload = readJson(JSON_ASSOCIATED_CASE_DEFENDANTS_ORGANISATION_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(CAAG_PROSECUTION_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, caagResponse);

        when(prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(query)).thenReturn(envelope);
        when(organisationService.getAssociatedCaseDefendantsWithOrganisationAddress(any(), any(), any())).thenReturn(jsonObjectPayload.getJsonObject("caseDefendantOrganisation"));
        final JsonEnvelope prosecutionCaseForCaseAtAGlance = prosecutionCaseQueryApi.getProsecutionCaseForCaseAtAGlance(query);

        final JsonObject expectedCaagResponse = readJson(JSON_CAAG_EXPECTED_RESPONSE_JSON, JsonObject.class);

        assertThat(prosecutionCaseForCaseAtAGlance.payloadAsJsonObject(), equalTo(expectedCaagResponse));
    }


    @Test
    public void shouldHandleProsecutionCaseAtAGlanceWithOutRepresentation() {
        final JsonObject caagResponse = readJson(JSON_CAAG_RESPONSE_JSON, JsonObject.class);
        final JsonObject jsonObjectPayload = createObjectBuilder().add("defendants", Json.createArrayBuilder().build()).build();

        final Metadata metadata = QueryClientTestBase.metadataFor(CAAG_PROSECUTION_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, caagResponse);

        when(prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(query)).thenReturn(envelope);
        when(organisationService.getAssociatedCaseDefendantsWithOrganisationAddress(any(), any(), any())).thenReturn(jsonObjectPayload);
        final JsonEnvelope prosecutionCaseForCaseAtAGlance = prosecutionCaseQueryApi.getProsecutionCaseForCaseAtAGlance(query);

        assertThat(prosecutionCaseForCaseAtAGlance.payloadAsJsonObject(), equalTo(caagResponse));
    }

    @Test
    public void shouldHandleProsecutionCaseAtAGlanceWithNoDefendant() {
        final JsonObject caagResponse = readJson(JSON_CAAG_NO_DEFENDANTS_RESPONSE_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(CAAG_PROSECUTION_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, caagResponse);

        when(prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(query)).thenReturn(envelope);
        final JsonEnvelope prosecutionCaseForCaseAtAGlance = prosecutionCaseQueryApi.getProsecutionCaseForCaseAtAGlance(query);

        assertThat(prosecutionCaseForCaseAtAGlance.payloadAsJsonObject(), equalTo(caagResponse));
    }

    @Test
    public void shouldHandleCaseQuery() {
        final JsonObject prosecutionCasePayload = readJson(CASE_QUERY_VIEW_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        when(prosecutionCaseQuery.getCase(query)).thenReturn(envelope);
        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getProsecutionCase(query);

        final JsonObject expectedProsecutionCaseResponse = readJson(CASE_QUERY_API_EXPECTED_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }

    @Test
    public void shouldHandleCotrDetailsProsecutionCase() {
        final JsonObject cotrPayload = readJson(COTR_QUERY_VIEW_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(COTR_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, cotrPayload);

        when(prosecutionCaseQuery.getCotrDetailsByCaseId(query)).thenReturn(envelope);
        final JsonEnvelope actualCotrCaseResponse = prosecutionCaseQueryApi.getCotrDetailsProsecutionCase(query);

        final JsonObject expectedCotrCaseResponse = readJson(COTR_QUERY_API_EXPECTED_JSON, JsonObject.class);

        assertThat(actualCotrCaseResponse.payloadAsJsonObject(), equalTo(expectedCotrCaseResponse));
    }

    @Test
    public void shouldHandleSearchProsecutionAuthorityIdByCaseId() {
        when(prosecutionCaseQuery.searchProsecutionAuthorityId(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchProsecutionAuthorityIdByCaseId(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchCaseByUrn() {
        when(prosecutionCaseQuery.searchCaseByCaseUrn(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchCaseByUrn(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchCaseExistsByCaseUrn() {
        when(prosecutionCaseQuery.caseExistsByCaseUrn(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchCaseExistsByCaseUrn(query), equalTo(response));
    }

    @Test(expected = ForbiddenRequestException.class)
    public void shouldThrowForbiddenExceptionWhenNonCPSProsecutorAndOrganisationMismatch() {
        final JsonObject prosecutionCasePayload = readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = readJson(DEFENDANT_WITH_COURT_ORDERS_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        final String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final JsonEnvelope queryEnvelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(prosecutionCaseQuery.getProsecutionCase(queryEnvelope)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.of("OrganisationMisMatch"));

        prosecutionCaseQueryApi.getCaseProsecutionCase(queryEnvelope);

    }

    @Test
    public void shouldHandleProsecutionCaseQueryWithCourtOrdersForNonCPSProsecutors() {
        final JsonObject prosecutionCasePayload = readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final JsonObject courtOrdersPayload = readJson(DEFENDANT_WITH_COURT_ORDERS_JSON, JsonObject.class);

        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);

        final String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final JsonEnvelope queryEnvelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(prosecutionCaseQuery.getProsecutionCase(queryEnvelope)).thenReturn(envelope);
        when(courtOrderService.getCourtOrdersByDefendant(any(), any(), any())).thenReturn(courtOrdersPayload);
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.of("OrganisationMatch"));

        final JsonEnvelope actualProsecutionCaseResponse = prosecutionCaseQueryApi.getCaseProsecutionCase(queryEnvelope);

        final JsonObject expectedProsecutionCaseResponse = readJson(PROSECUTION_CASE_QUERY_API_EXPECTED_WITH_COURT_ORDERS_JSON, JsonObject.class);

        assertThat(actualProsecutionCaseResponse.payloadAsJsonObject(), equalTo(expectedProsecutionCaseResponse));
    }
}
