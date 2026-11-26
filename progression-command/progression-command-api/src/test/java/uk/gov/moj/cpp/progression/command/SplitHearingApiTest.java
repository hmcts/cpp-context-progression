package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.adapter.rest.exception.ConflictedResourceException;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Subset validation is what stops a split corrupting the source hearing, so it is covered here for
 * the multi-defendant and multi-case shapes as well as the simple one.
 */
@ExtendWith(MockitoExtension.class)
class SplitHearingApiTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID CASE_A = randomUUID();
    private static final UUID CASE_B = randomUUID();
    private static final UUID DEFENDANT_1 = randomUUID();
    private static final UUID DEFENDANT_2 = randomUUID();
    private static final UUID OFFENCE_1 = randomUUID();
    private static final UUID OFFENCE_2 = randomUUID();
    private static final UUID OFFENCE_3 = randomUUID();
    private static final UUID OFFENCE_4 = randomUUID();

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @InjectMocks
    private SplitHearingApi splitHearingApi;

    // ── stored hearing (the viewstore's view) ────────────────────────────

    private static JsonObject offence(final UUID id) {
        return createObjectBuilder().add("id", id.toString()).build();
    }

    private static JsonObject defendant(final UUID id, final UUID... offenceIds) {
        final JsonArrayBuilder offences = createArrayBuilder();
        for (final UUID offenceId : offenceIds) {
            offences.add(offence(offenceId));
        }
        return createObjectBuilder().add("id", id.toString()).add("offences", offences).build();
    }

    private static JsonObject prosecutionCase(final UUID caseId, final JsonObject... defendants) {
        final JsonArrayBuilder built = createArrayBuilder();
        for (final JsonObject defendant : defendants) {
            built.add(defendant);
        }
        return createObjectBuilder().add("id", caseId.toString()).add("defendants", built).build();
    }

    private void givenStoredHearing(final String hearingStatus, final JsonObject... cases) {
        final JsonArrayBuilder built = createArrayBuilder();
        for (final JsonObject prosecutionCase : cases) {
            built.add(prosecutionCase);
        }
        final javax.json.JsonObjectBuilder hearing = createObjectBuilder()
                .add("id", HEARING_ID.toString())
                .add("prosecutionCases", built);
        // hearingListingStatus is a sibling of hearing in the query response, not a field on it.
        final javax.json.JsonObjectBuilder response = createObjectBuilder().add("hearing", hearing);
        if (hearingStatus != null) {
            response.add("hearingListingStatus", hearingStatus);
        }
        when(requester.request(any(JsonEnvelope.class))).thenReturn(
                JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName().build(), response.build()));
    }

    private void givenNoStoredHearing() {
        when(requester.request(any(JsonEnvelope.class))).thenReturn(
                JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName().build(),
                        createObjectBuilder().build()));
    }

    // ── the split request ────────────────────────────────────────────────

    private static JsonObject defendantRequest(final UUID caseId, final UUID defendantId, final UUID... offenceIds) {
        final JsonArrayBuilder offences = createArrayBuilder();
        for (final UUID offenceId : offenceIds) {
            offences.add(offenceId.toString());
        }
        return createObjectBuilder()
                .add("prosecutionCaseId", caseId.toString())
                .add("defendantId", defendantId.toString())
                .add("defendantOffences", offences)
                .build();
    }

    private static JsonEnvelope splitEnvelope(final JsonObject... defendantRequests) {
        final JsonArrayBuilder requests = createArrayBuilder();
        for (final JsonObject request : defendantRequests) {
            requests.add(request);
        }
        return JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName().build(),
                createObjectBuilder()
                        .add("hearingId", HEARING_ID.toString())
                        .add("listNewHearing", createObjectBuilder()
                                .add("estimatedMinutes", 1080)
                                .add("listDefendantRequests", requests))
                        .add("sendNotificationToParties", false)
                        .build());
    }

    private JsonObject capturedCommand() {
        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).send(captor.capture());
        return (JsonObject) captor.getValue().payload();
    }

    // ── tests ────────────────────────────────────────────────────────────

    @Test
    void shouldSendTheCommandWithTheOffencesToRemoveWhenTheRequestIsAStrictSubset() {
        givenStoredHearing(null, prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2)));

        splitHearingApi.handle(splitEnvelope(defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_1)));

        final JsonObject command = capturedCommand();
        assertThat(command.getString("hearingId"), is(HEARING_ID.toString()));

        final JsonArray cases = command.getJsonArray("prosecutionCasesToRemove");
        assertThat(cases, hasSize(1));
        assertThat(cases.getJsonObject(0).getString("caseId"), is(CASE_A.toString()));

        final JsonArray defendants = cases.getJsonObject(0).getJsonArray("defendantsToRemove");
        assertThat(defendants, hasSize(1));
        assertThat(defendants.getJsonObject(0).getString("defendantId"), is(DEFENDANT_1.toString()));

        final JsonArray offences = defendants.getJsonObject(0).getJsonArray("offencesToRemove");
        assertThat(offences, hasSize(1));
        assertThat(offences.getJsonObject(0).getString("offenceId"), is(OFFENCE_1.toString()));
    }

    @Test
    void shouldGroupOffencesPerDefendantForAMultiDefendantSplit() {
        givenStoredHearing(null, prosecutionCase(CASE_A,
                defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2),
                defendant(DEFENDANT_2, OFFENCE_3, OFFENCE_4)));

        splitHearingApi.handle(splitEnvelope(
                defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_1),
                defendantRequest(CASE_A, DEFENDANT_2, OFFENCE_3)));

        final JsonArray cases = capturedCommand().getJsonArray("prosecutionCasesToRemove");
        assertThat("both defendants belong to the one case", cases, hasSize(1));
        assertThat(cases.getJsonObject(0).getJsonArray("defendantsToRemove"), hasSize(2));
    }

    @Test
    void shouldKeepCasesSeparateForAMultiCaseSplit() {
        givenStoredHearing(null,
                prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2)),
                prosecutionCase(CASE_B, defendant(DEFENDANT_2, OFFENCE_3, OFFENCE_4)));

        splitHearingApi.handle(splitEnvelope(
                defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_1),
                defendantRequest(CASE_B, DEFENDANT_2, OFFENCE_3)));

        final JsonArray cases = capturedCommand().getJsonArray("prosecutionCasesToRemove");
        assertThat(cases, hasSize(2));
    }

    // Moving every offence is not a split — it would leave the source hearing empty.
    @Test
    void shouldRejectARequestForTheWholeOffenceSet() {
        givenStoredHearing(null, prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2)));

        final BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> splitHearingApi.handle(splitEnvelope(
                        defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_1, OFFENCE_2))));

        assertThat(thrown.getMessage(), containsString("would be emptied"));
        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    void shouldRejectAnOffenceThatIsNotOnTheHearing() {
        givenStoredHearing(null, prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2)));

        final BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> splitHearingApi.handle(splitEnvelope(
                        defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_1, OFFENCE_3))));

        assertThat(thrown.getMessage(), containsString("not on hearing"));
        verify(sender, never()).send(any(Envelope.class));
    }

    /**
     * The offence exists on the hearing, but under a different defendant. Comparing flattened id
     * sets would accept this: the new hearing would list OFFENCE_3 against DEFENDANT_1, and the
     * removal computed from the same request would take nothing off DEFENDANT_2, who actually
     * holds it.
     */
    @Test
    void shouldRejectAnOffenceRequestedUnderTheWrongDefendant() {
        givenStoredHearing(null, prosecutionCase(CASE_A,
                defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2),
                defendant(DEFENDANT_2, OFFENCE_3)));

        final BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> splitHearingApi.handle(splitEnvelope(
                        defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_3))));

        assertThat(thrown.getMessage(), containsString(OFFENCE_3.toString()));
        verify(sender, never()).send(any(Envelope.class));
    }

    /**
     * Same trap one level up: the offence is on the hearing under the same defendant but a
     * different prosecution case.
     */
    @Test
    void shouldRejectAnOffenceRequestedUnderTheWrongCase() {
        givenStoredHearing(null,
                prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2)),
                prosecutionCase(CASE_B, defendant(DEFENDANT_1, OFFENCE_3)));

        final BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> splitHearingApi.handle(splitEnvelope(
                        defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_3))));

        assertThat(thrown.getMessage(), containsString(OFFENCE_3.toString()));
        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    void shouldRejectAnEmptyOffenceSelection() {
        givenStoredHearing(null, prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1)));

        assertThrows(BadRequestException.class,
                () -> splitHearingApi.handle(splitEnvelope(defendantRequest(CASE_A, DEFENDANT_1))));

        verify(sender, never()).send(any(Envelope.class));
    }

    /**
     * A malformed offence id has to read as the caller's mistake. Unguarded it escapes as an
     * unchecked {@code IllegalArgumentException} and reaches the caller as a 500.
     */
    @Test
    void shouldRejectAnOffenceIdThatIsNotAUuid() {
        givenStoredHearing(null, prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2)));

        final JsonObject malformed = createObjectBuilder()
                .add("prosecutionCaseId", CASE_A.toString())
                .add("defendantId", DEFENDANT_1.toString())
                .add("defendantOffences", createArrayBuilder().add("not-a-uuid"))
                .build();

        final BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> splitHearingApi.handle(splitEnvelope(malformed)));

        assertThat(thrown.getMessage(), containsString("not-a-uuid"));
        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    void shouldReturnNotFoundForAnUnknownHearing() {
        givenNoStoredHearing();

        assertThrows(NotFoundException.class,
                () -> splitHearingApi.handle(splitEnvelope(defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_1))));

        verify(sender, never()).send(any(Envelope.class));
    }

    // A resulted hearing is a concluded record; splitting it would rewrite history.
    @Test
    void shouldConflictOnAResultedHearing() {
        givenStoredHearing("HEARING_RESULTED", prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2)));

        assertThrows(ConflictedResourceException.class,
                () -> splitHearingApi.handle(splitEnvelope(defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_1))));

        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    void shouldRejectAMissingHearingId() {
        final JsonEnvelope noHearingId = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName().build(),
                createObjectBuilder().add("listNewHearing", createObjectBuilder()).build());

        assertThrows(BadRequestException.class, () -> splitHearingApi.handle(noHearingId));
        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    void shouldRejectAMalformedHearingId() {
        final JsonEnvelope badId = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName().build(),
                createObjectBuilder()
                        .add("hearingId", "not-a-uuid")
                        .add("listNewHearing", createObjectBuilder())
                        .build());

        assertThrows(BadRequestException.class, () -> splitHearingApi.handle(badId));
        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    void shouldRejectAMissingListDefendantRequests() {
        givenStoredHearing(null, prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1)));

        final JsonEnvelope noRequests = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName().build(),
                createObjectBuilder()
                        .add("hearingId", HEARING_ID.toString())
                        .add("listNewHearing", createObjectBuilder().add("estimatedMinutes", 60))
                        .build());

        assertThrows(BadRequestException.class, () -> splitHearingApi.handle(noRequests));
        verify(sender, never()).send(any(Envelope.class));
    }

    @Test
    void shouldForwardTheOriginalRequestAlongsideTheComputedRemoval() {
        givenStoredHearing(null, prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_2)));

        splitHearingApi.handle(splitEnvelope(defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_1)));

        final JsonObject command = capturedCommand();
        assertThat("the listNewHearing the caller sent must survive untouched",
                command.getJsonObject("listNewHearing").getInt("estimatedMinutes"), is(1080));
        assertThat(command.getBoolean("sendNotificationToParties"), is(false));
    }

    @Test
    void shouldValidateAgainstOffencesStillOnTheHearingAfterAnEarlierSplit() {
        // OFFENCE_2 was moved off by a previous split, so the viewstore no longer lists it.
        givenStoredHearing(null, prosecutionCase(CASE_A, defendant(DEFENDANT_1, OFFENCE_1, OFFENCE_3)));

        final BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> splitHearingApi.handle(splitEnvelope(
                        defendantRequest(CASE_A, DEFENDANT_1, OFFENCE_2))));

        assertThat(thrown.getMessage(), containsString("not on hearing"));
    }
}
