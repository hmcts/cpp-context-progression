package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.progression.query.view.service.SjpQueryService.SJP_QUERY_CASE;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SjpQueryServiceTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final JsonEnvelope QUERY = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Mock
    private Requester requester;

    @InjectMocks
    private SjpQueryService sjpQueryService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> request;

    @Test
    void shouldRequestCaseAsAdminAndReturnTheMostRecentDecision() {
        final UUID decisionId = UUID.randomUUID();
        final UUID applicationDecisionId = UUID.randomUUID();
        final UUID nullApplicationDecisionId = UUID.randomUUID();
        when(requester.requestAsAdmin(request.capture())).thenReturn(response(createObjectBuilder()
                .add("id", CASE_ID.toString())
                .add("caseDecisions", createArrayBuilder()
                        .add(decision(decisionId, "2026-01-01T10:00:00.000Z"))
                        .add(decision(applicationDecisionId, "2026-02-01T10:00:00.000Z")
                                .add("applicationDecision", createObjectBuilder().add("granted", true)))
                        .add(decision(nullApplicationDecisionId, "2026-03-01T10:00:00.000Z")
                                .addNull("applicationDecision")))
                .build()));

        final Optional<SjpCaseDecision> decision = sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID);

        assertThat(request.getValue().metadata().name(), is(SJP_QUERY_CASE));
        assertThat(request.getValue().payloadAsJsonObject().getString("caseId"), is(CASE_ID.toString()));
        assertThat(decision, is(Optional.of(
                new SjpCaseDecision(nullApplicationDecisionId, ZonedDateTime.parse("2026-03-01T10:00:00.000Z")))));
    }

    @Test
    void shouldPickTheMostRecentDecisionBySavedAtWhateverOrderSjpReturnsThemIn() {
        final UUID adjournment = UUID.randomUUID();
        final UUID finalDecision = UUID.randomUUID();
        final UUID earlierAdjournment = UUID.randomUUID();
        when(requester.requestAsAdmin(request.capture())).thenReturn(response(createObjectBuilder()
                .add("id", CASE_ID.toString())
                .add("caseDecisions", createArrayBuilder()
                        .add(decision(adjournment, "2026-02-01T10:00:00.000Z"))
                        .add(decision(finalDecision, "2026-03-01T09:30:00.000Z"))
                        .add(decision(earlierAdjournment, "2026-01-01T10:00:00.000Z")))
                .build()));

        assertThat(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID), is(Optional.of(
                new SjpCaseDecision(finalDecision, ZonedDateTime.parse("2026-03-01T09:30:00.000Z")))));
    }

    @Test
    void shouldReturnTheDecisionBeforeTheSetAsideWhenTheSetAsideIsTheMostRecentDecision() {
        final UUID decisionId = UUID.randomUUID();
        final UUID setAsideId = UUID.randomUUID();
        when(requester.requestAsAdmin(request.capture())).thenReturn(response(createObjectBuilder()
                .add("id", CASE_ID.toString())
                .add("caseDecisions", createArrayBuilder()
                        .add(decision(decisionId, "2026-01-01T10:00:00.000Z"))
                        .add(decision(setAsideId, "2026-02-01T10:00:00.000Z")
                                .add("applicationDecision", createObjectBuilder().add("granted", true))))
                .build()));

        assertThat(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID), is(Optional.of(
                new SjpCaseDecision(decisionId, ZonedDateTime.parse("2026-01-01T10:00:00.000Z")))));
    }

    @Test
    void shouldReturnNoDecisionWhenTheCaseHasOnlySetAsides() {
        when(requester.requestAsAdmin(request.capture())).thenReturn(response(createObjectBuilder()
                .add("id", CASE_ID.toString())
                .add("caseDecisions", createArrayBuilder()
                        .add(decision(UUID.randomUUID(), "2026-02-01T10:00:00.000Z")
                                .add("applicationDecision", createObjectBuilder().add("granted", true))))
                .build()));

        assertThat(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID), is(Optional.empty()));
    }

    @Test
    void shouldReturnNoDecisionWhenSjpHasNoCase() {
        when(requester.requestAsAdmin(request.capture())).thenReturn(envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL));

        assertThat(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID), is(Optional.empty()));
    }

    @Test
    void shouldReturnNoDecisionWhenCaseHasNone() {
        when(requester.requestAsAdmin(request.capture())).thenReturn(response(createObjectBuilder().add("id", CASE_ID.toString()).build()));

        assertThat(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID), is(Optional.empty()));
    }

    @Test
    void shouldReturnNoDecisionWhenCaseDecisionsIsEmpty() {
        when(requester.requestAsAdmin(request.capture())).thenReturn(response(createObjectBuilder()
                .add("id", CASE_ID.toString())
                .add("caseDecisions", createArrayBuilder())
                .build()));

        assertThat(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID), is(Optional.empty()));
    }

    private static javax.json.JsonObjectBuilder decision(final UUID id, final String savedAt) {
        return createObjectBuilder().add("id", id.toString()).add("savedAt", savedAt);
    }

    private static JsonEnvelope response(final JsonObject payload) {
        return envelopeFrom(metadataWithRandomUUIDAndName(), payload);
    }
}
