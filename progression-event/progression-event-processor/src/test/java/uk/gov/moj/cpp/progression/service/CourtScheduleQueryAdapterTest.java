package uk.gov.moj.cpp.progression.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtScheduleQueryAdapterTest {

    private static final UUID SCHEDULE_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SCHEDULE_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Function<Object, JsonEnvelope> envelopeBuilder;

    @InjectMocks
    private CourtScheduleQueryAdapter adapter;

    @Test
    public void returnsFalseAndDoesNotCallListingQueryWhenInputIsNull() {
        assertThat(adapter.anySessionIsDraft(sourceEnvelope(), null), is(false));
        verify(requester, never()).requestAsAdmin(any());
    }

    @Test
    public void returnsFalseAndDoesNotCallListingQueryWhenInputIsEmpty() {
        assertThat(adapter.anySessionIsDraft(sourceEnvelope(), Collections.emptyList()), is(false));
        verify(requester, never()).requestAsAdmin(any());
    }

    @Test
    public void returnsTrueWhenListingResponseSaysAnyDraft() {
        givenListingReturns(true);
        assertThat(adapter.anySessionIsDraft(sourceEnvelope(), List.of(SCHEDULE_ID_1, SCHEDULE_ID_2)), is(true));
    }

    @Test
    public void sendsRequestAsFlatArrayOfUuidStringsNotObjectWrapped() {
        // Wire contract for the request body to listing.query.court.schedule.draft.status:
        //   { "courtScheduleIdList": ["<uuid>", "<uuid>"] }
        // NOT the older form {"courtScheduleIdList": [{"courtScheduleId":"<uuid>"}, ...]}.
        // This test captures the actual body built by the adapter so a regression to the
        // wrapped form would fail loudly here.
        givenListingReturns(false);
        final ArgumentCaptor<JsonObject> payloadCaptor = ArgumentCaptor.forClass(JsonObject.class);

        adapter.anySessionIsDraft(sourceEnvelope(), List.of(SCHEDULE_ID_1, SCHEDULE_ID_2));

        verify(envelopeBuilder).apply(payloadCaptor.capture());
        final JsonObject sent = payloadCaptor.getValue();
        final List<String> idsInPayload = new ArrayList<>();
        sent.getJsonArray("courtScheduleIdList").forEach(v -> idsInPayload.add(((javax.json.JsonString) v).getString()));
        assertThat(idsInPayload, containsInAnyOrder(SCHEDULE_ID_1.toString(), SCHEDULE_ID_2.toString()));
    }

    @Test
    public void sendsRequestPreservingScheduleIdOrder() {
        givenListingReturns(false);
        final ArgumentCaptor<JsonObject> payloadCaptor = ArgumentCaptor.forClass(JsonObject.class);

        adapter.anySessionIsDraft(sourceEnvelope(), List.of(SCHEDULE_ID_1, SCHEDULE_ID_2));

        verify(envelopeBuilder).apply(payloadCaptor.capture());
        final List<String> idsInPayload = new ArrayList<>();
        payloadCaptor.getValue().getJsonArray("courtScheduleIdList")
                .forEach(v -> idsInPayload.add(((javax.json.JsonString) v).getString()));
        assertThat(idsInPayload, contains(SCHEDULE_ID_1.toString(), SCHEDULE_ID_2.toString()));
    }

    @Test
    public void returnsFalseWhenListingResponseSaysNoDraft() {
        givenListingReturns(false);
        assertThat(adapter.anySessionIsDraft(sourceEnvelope(), List.of(SCHEDULE_ID_1)), is(false));
    }

    @Test
    public void failsOpenToFalseWhenListingResponseOmitsAnyDraftKey() {
        // Fail-open: if we cannot prove the session is draft, preserve the input (don't strip).
        // Allocated CROWN hearings keep their courtCentre.roomId during a listing outage;
        // the unallocated-leak guard is best-effort and resumes once listing is reachable.
        givenListingReturnsBody(Json.createObjectBuilder().build());
        assertThat(adapter.anySessionIsDraft(sourceEnvelope(), List.of(SCHEDULE_ID_1)), is(false));
    }

    @Test
    public void failsOpenToFalseWhenListingCallThrows() {
        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), any(String.class)))
                .thenThrow(new RuntimeException("simulated dispatch failure"));
        assertThat(adapter.anySessionIsDraft(sourceEnvelope(), List.of(SCHEDULE_ID_1)), is(false));
    }

    @Test
    public void failsOpenToFalseWhenListingReturnsNullEnvelope() {
        // The Requester contract allows null in some dispatch failures; treat the same way
        // as malformed - we cannot prove draft state, so preserve the input.
        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), any(String.class))).thenReturn(envelopeBuilder);
        when(envelopeBuilder.apply(any())).thenReturn(envelopeFrom(metadataWithRandomUUID("listing.query.court.schedule.draft.status"), Json.createObjectBuilder().build()));
        when(requester.requestAsAdmin(any())).thenReturn(null);

        assertThat(adapter.anySessionIsDraft(sourceEnvelope(), List.of(SCHEDULE_ID_1)), is(false));
    }

    private void givenListingReturns(final boolean anyDraft) {
        givenListingReturnsBody(Json.createObjectBuilder().add("anyDraft", anyDraft).build());
    }

    private void givenListingReturnsBody(final JsonObject body) {
        final JsonEnvelope response = envelopeFrom(metadataWithRandomUUID("listing.query.court.schedule.draft.status.response"), body);
        when(enveloper.withMetadataFrom(any(JsonEnvelope.class), any(String.class))).thenReturn(envelopeBuilder);
        when(envelopeBuilder.apply(any())).thenReturn(envelopeFrom(metadataWithRandomUUID("listing.query.court.schedule.draft.status"), Json.createObjectBuilder().build()));
        when(requester.requestAsAdmin(any())).thenReturn(response);
    }

    private static JsonEnvelope sourceEnvelope() {
        return envelopeFrom(metadataWithRandomUUID("progression.event.list-hearing-requested"),
                Json.createObjectBuilder().build());
    }
}
