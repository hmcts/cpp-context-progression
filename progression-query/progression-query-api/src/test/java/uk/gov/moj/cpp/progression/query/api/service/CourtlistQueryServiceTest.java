package uk.gov.moj.cpp.progression.query.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtlistQueryServiceTest {

    private static final String COURT_CENTRE_ID = "court-centre-123";
    private static final String COURT_ROOM_ID = "court-room-456";
    private static final String LIST_ID = "PUBLIC";
    private static final String START_DATE = "2025-01-01";
    private static final String END_DATE = "2025-01-31";
    private static final String COURT_LIST_ACTION = "progression.search.court.list.data";

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private CourtlistQueryService courtlistQueryService;

    @Test
    void buildCourtlistQueryEnvelope_shouldIncludeAllPayloadFieldsAndUserIdWhenProvided() {
        final UUID userId = UUID.randomUUID();

        final JsonEnvelope result = courtlistQueryService.buildCourtlistQueryEnvelope(
                COURT_CENTRE_ID, COURT_ROOM_ID, LIST_ID, START_DATE, END_DATE, false, userId, COURT_LIST_ACTION);

        assertThat(result, notNullValue());
        final JsonObject payload = result.payloadAsJsonObject();
        assertThat(payload.getString("courtCentreId"), is(COURT_CENTRE_ID));
        assertThat(payload.getString("courtRoomId"), is(COURT_ROOM_ID));
        assertThat(payload.getString("listId"), is(LIST_ID));
        assertThat(payload.getString("startDate"), is(START_DATE));
        assertThat(payload.getString("endDate"), is(END_DATE));
        assertThat(payload.getBoolean("restricted"), is(false));
        assertThat(payload.getBoolean("includeApplications"), is(true));
        assertThat(result.metadata().userId(), is(Optional.of(userId.toString())));
        assertThat(result.metadata().name(), is(COURT_LIST_ACTION));
    }

    @Test
    void buildCourtlistQueryEnvelope_shouldIncludeIncludeApplicationsTrueForListing() {
        final JsonEnvelope result = courtlistQueryService.buildCourtlistQueryEnvelope(
                COURT_CENTRE_ID, null, LIST_ID, START_DATE, END_DATE, false, null, COURT_LIST_ACTION);

        assertThat(result.payloadAsJsonObject().getBoolean("includeApplications"), is(true));
    }

    @Test
    void buildCourtlistQueryEnvelope_shouldOmitCourtRoomIdWhenNull() {
        final JsonEnvelope result = courtlistQueryService.buildCourtlistQueryEnvelope(
                COURT_CENTRE_ID, null, LIST_ID, START_DATE, END_DATE, true, null, COURT_LIST_ACTION);

        assertThat(result, notNullValue());
        final JsonObject payload = result.payloadAsJsonObject();
        assertThat(payload.containsKey("courtRoomId"), is(false));
        assertThat(result.metadata().userId(), is(Optional.empty()));
    }

    @Test
    void buildCourtlistQueryEnvelope_shouldOmitUserIdWhenNull() {
        final JsonEnvelope result = courtlistQueryService.buildCourtlistQueryEnvelope(
                COURT_CENTRE_ID, COURT_ROOM_ID, LIST_ID, START_DATE, END_DATE, false, null, COURT_LIST_ACTION);

        assertThat(result, notNullValue());
        assertThat(result.metadata().userId(), is(Optional.empty()));
    }

    @Test
    void buildEnrichedPayload_shouldCopyPayloadWhenCourtCentreNameIsNull() {
        final JsonObject originalPayload = Json.createObjectBuilder()
                .add("listType", "public")
                .build();
        final JsonEnvelope document = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("test"),
                originalPayload);

        final JsonObject result = courtlistQueryService.buildEnrichedPayload(document);

        assertThat(result, notNullValue());
        assertThat(result.getString("listType"), is("public"));
        assertThat(result.containsKey("ouCode"), is(false));
        assertThat(result.containsKey("courtId"), is(false));
        assertThat(result.containsKey("courtIdNumeric"), is(false));
    }

    @Test
    void buildEnrichedPayload_shouldEnrichWithCourtCentreDataWhenCourtCentreNamePresentAndReferenceDataReturnsData() {
        final String courtCentreName = "Test Court";
        final JsonObject originalPayload = Json.createObjectBuilder()
                .add("courtCentreName", courtCentreName)
                .add("listType", "public")
                .build();
        final JsonEnvelope document = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("test"),
                originalPayload);

        final JsonObject courtCentreData = Json.createObjectBuilder()
                .add("oucode", "OU123")
                .add("id", "court-id-1")
                .add("courtId", "456")
                .build();
        when(referenceDataService.getCourtCenterDataByCourtName(document, courtCentreName))
                .thenReturn(Optional.of(courtCentreData));

        final JsonObject result = courtlistQueryService.buildEnrichedPayload(document);

        assertThat(result, notNullValue());
        assertThat(result.getString("courtCentreName"), is(courtCentreName));
        assertThat(result.getJsonString("ouCode").getString(), is("OU123"));
        assertThat(result.getJsonString("courtId").getString(), is("court-id-1"));
        assertThat(result.getString("courtIdNumeric"), is("456"));
        verify(referenceDataService).getCourtCenterDataByCourtName(document, courtCentreName);
    }

    @Test
    void buildEnrichedPayload_shouldUseDefaultCourtIdNumericWhenNotInReferenceData() {
        final String courtCentreName = "Other Court";
        final JsonObject originalPayload = Json.createObjectBuilder()
                .add("courtCentreName", courtCentreName)
                .build();
        final JsonEnvelope document = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("test"),
                originalPayload);

        final JsonObject courtCentreData = Json.createObjectBuilder()
                .add("oucode", "OU456")
                .add("id", "court-id-2")
                .build();
        when(referenceDataService.getCourtCenterDataByCourtName(any(), eq(courtCentreName)))
                .thenReturn(Optional.of(courtCentreData));

        final JsonObject result = courtlistQueryService.buildEnrichedPayload(document);

        assertThat(result.getString("courtIdNumeric"), is("0"));
    }

    @Test
    void buildEnrichedPayload_shouldNotAddEnrichmentWhenReferenceDataReturnsEmpty() {
        final String courtCentreName = "Unknown Court";
        final JsonObject originalPayload = Json.createObjectBuilder()
                .add("courtCentreName", courtCentreName)
                .build();
        final JsonEnvelope document = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("test"),
                originalPayload);

        when(referenceDataService.getCourtCenterDataByCourtName(any(), eq(courtCentreName)))
                .thenReturn(Optional.empty());

        final JsonObject result = courtlistQueryService.buildEnrichedPayload(document);

        assertThat(result.getString("courtCentreName"), is(courtCentreName));
        assertThat(result.containsKey("ouCode"), is(false));
        assertThat(result.containsKey("courtId"), is(false));
        assertThat(result.containsKey("courtIdNumeric"), is(false));
    }
}
