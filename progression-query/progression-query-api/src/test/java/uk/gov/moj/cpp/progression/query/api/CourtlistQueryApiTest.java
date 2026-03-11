package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.service.CourtlistQueryService;

import java.lang.reflect.Field;
import java.util.UUID;

import javax.json.Json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtlistQueryApiTest {

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Mock
    private CourtlistQueryService courtlistQueryService;

    private StubCourtlistQueryView stubCourtlistQueryView;
    private CourtlistQueryApi courtListQueryApi;

    @BeforeEach
    public void setUp() throws Exception {
        stubCourtlistQueryView = new StubCourtlistQueryView();
        courtListQueryApi = new CourtlistQueryApi();
        setField(courtListQueryApi, "courtlistQueryView", stubCourtlistQueryView);
        setField(courtListQueryApi, "courtlistQueryService", courtlistQueryService);
    }

    @Test
    public void shouldHandleApplicationQuery() {
        stubCourtlistQueryView.setSearchCourtlistResponse(response);
        assertThat(courtListQueryApi.searchCourtlist(query), equalTo(response));
    }

    @Test
    public void shouldHandlePrisonCourtListQuery() {
        stubCourtlistQueryView.setSearchPrisonCourtlistResponse(response);
        assertThat(courtListQueryApi.searchPrisonCourtlist(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchCourtlistDataQuery() {
        stubCourtlistQueryView.setSearchCourtlistResponse(response);
        var enrichedPayload = Json.createObjectBuilder().build();
        when(courtlistQueryService.buildEnrichedPayload(response)).thenReturn(enrichedPayload);
        var metadata = metadataBuilder().withId(UUID.randomUUID()).withName("test").build();
        when(query.metadata()).thenReturn(metadata);
        when(query.payloadAsJsonObject()).thenReturn(Json.createObjectBuilder().build());
        var result = courtListQueryApi.searchCourtlistData(query);
        assertThat(result.metadata().name(), equalTo(metadata.name()));
        assertThat(result.payloadAsJsonObject(), equalTo(enrichedPayload));
    }

    @Test
    public void shouldHandleSearchPrisonCourtlistDataQuery() {
        stubCourtlistQueryView.setSearchPrisonCourtlistResponse(response);
        var enrichedPayload = Json.createObjectBuilder().build();
        when(courtlistQueryService.buildEnrichedPayload(response)).thenReturn(enrichedPayload);
        var metadata = metadataBuilder().withId(UUID.randomUUID()).withName("test").build();
        when(query.metadata()).thenReturn(metadata);
        when(query.payloadAsJsonObject()).thenReturn(Json.createObjectBuilder().build());
        var result = courtListQueryApi.searchPrisonCourtlistData(query);
        assertThat(result.metadata().name(), equalTo(metadata.name()));
        assertThat(result.payloadAsJsonObject(), equalTo(enrichedPayload));
    }

    @Test
    public void searchCourtlistData_shouldPassIncludeApplicationsTrueWhenPresentInPayload() {
        stubCourtlistQueryView.setSearchCourtlistResponse(response);
        when(courtlistQueryService.buildEnrichedPayload(response)).thenReturn(Json.createObjectBuilder().build());
        when(query.metadata()).thenReturn(metadataBuilder().withId(UUID.randomUUID()).withName("progression.search.court.list.data").build());
        when(query.payloadAsJsonObject()).thenReturn(Json.createObjectBuilder().add("includeApplications", true).build());

        courtListQueryApi.searchCourtlistData(query);

        var envelopePassedToView = stubCourtlistQueryView.getLastSearchCourtlistQuery();
        assertThat(envelopePassedToView.payloadAsJsonObject().getBoolean("includeApplications"), equalTo(true));
    }

    @Test
    public void searchCourtlistData_shouldPassIncludeApplicationsFalseWhenPresentInPayload() {
        stubCourtlistQueryView.setSearchCourtlistResponse(response);
        when(courtlistQueryService.buildEnrichedPayload(response)).thenReturn(Json.createObjectBuilder().build());
        when(query.metadata()).thenReturn(metadataBuilder().withId(UUID.randomUUID()).withName("progression.search.court.list.data").build());
        when(query.payloadAsJsonObject()).thenReturn(Json.createObjectBuilder().add("includeApplications", false).build());

        courtListQueryApi.searchCourtlistData(query);

        var envelopePassedToView = stubCourtlistQueryView.getLastSearchCourtlistQuery();
        assertThat(envelopePassedToView.payloadAsJsonObject().getBoolean("includeApplications"), equalTo(false));
    }

    @Test
    public void searchCourtlistData_shouldDefaultIncludeApplicationsToFalseWhenMissingFromPayload() {
        stubCourtlistQueryView.setSearchCourtlistResponse(response);
        when(courtlistQueryService.buildEnrichedPayload(response)).thenReturn(Json.createObjectBuilder().build());
        when(query.metadata()).thenReturn(metadataBuilder().withId(UUID.randomUUID()).withName("progression.search.court.list.data").build());
        when(query.payloadAsJsonObject()).thenReturn(Json.createObjectBuilder().build());

        courtListQueryApi.searchCourtlistData(query);

        var envelopePassedToView = stubCourtlistQueryView.getLastSearchCourtlistQuery();
        assertThat(envelopePassedToView.payloadAsJsonObject().getBoolean("includeApplications"), equalTo(false));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
