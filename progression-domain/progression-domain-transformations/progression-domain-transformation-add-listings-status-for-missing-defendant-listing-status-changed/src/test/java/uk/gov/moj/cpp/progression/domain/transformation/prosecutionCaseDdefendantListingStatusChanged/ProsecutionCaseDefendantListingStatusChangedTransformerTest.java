package uk.gov.moj.cpp.progression.domain.transformation.prosecutionCaseDdefendantListingStatusChanged;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.progression.domain.transformation.prosecutionCaseDdefendantListingStatusChanged.ProsecutionCaseDefendantListingStatusChangedTransformer.HEARING_LISTING_STATUS_VALUE;
import static uk.gov.moj.cpp.progression.domain.transformation.prosecutionCaseDdefendantListingStatusChanged.ProsecutionCaseDefendantListingStatusChangedTransformer.PROGRESSION_CASE_DEFENDANT_LISTING_STATUS_CHANGED;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseDefendantListingStatusChangedTransformerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    private static final String SOME_OTHER_EVENT = "progression.event.some-other-event";
    private static final String PROCECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_WITH_LISTING_STATUS_KNOWN = "progression.event.prosecutionCase-defendant-listing-status-changed-Known.json";
    private static final String PROCECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_WITH_LISTING_STATUS_UNKNOWN = "progression.event.prosecutionCase-defendant-listing-status-changed-Unknown.json";
    private static final String PROCECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_WITHOUT_LISTING_STATUS_KNOWN = "progression.event.prosecutionCase-defendant-listing-status-changed-without-listing.json";

    @InjectMocks
    private ProsecutionCaseDefendantListingStatusChangedTransformer prosecutionCaseDefendantListingStatusChangedTransformer;

    private JsonEnvelope jsonEnvelope;

    @BeforeEach
    public void onceBeforeEachTest() {
        jsonEnvelope = envelopeFrom(metadataWithRandomUUID(PROGRESSION_CASE_DEFENDANT_LISTING_STATUS_CHANGED)
                        .withUserId(randomUUID().toString())
                        .createdAt(ZonedDateTime.now())
                        .withClientCorrelationId(randomUUID().toString())
                        .withCausation(randomUUID(), randomUUID())
                        .withStreamId(UUID.fromString("cf73207f-3ced-488a-82a0-3fba79c2ce04"))
                        .build(),
                createObjectBuilder()
                        .build());
    }

    @Test
    public void shouldNotRaiseTransformActionForAnyOtherEvent() {
        final JsonEnvelope someOtherEventEnvelope = envelopeFrom(metadataWithRandomUUID(SOME_OTHER_EVENT).build(),
                createObjectBuilder().add("title", "title").build());
        assertThat(prosecutionCaseDefendantListingStatusChangedTransformer.actionFor(someOtherEventEnvelope), is(NO_ACTION));
    }

    @Test
    public void shouldEnrichEventWithHearingListingStatusIfNotExistsForKnownEvent() {
        final JsonEnvelope hearingListingStatusEnvelope = envelopeFrom(
                Envelope.metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_CASE_DEFENDANT_LISTING_STATUS_CHANGED),
                readJson(PROCECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_WITHOUT_LISTING_STATUS_KNOWN, JsonValue.class));
        final List<JsonEnvelope> envelopeList = prosecutionCaseDefendantListingStatusChangedTransformer.apply(hearingListingStatusEnvelope).collect(toList());
        thenOutputStreamHasOneEvent(envelopeList);
        verifyProsecutionCaseDefendantListingStatusChanged(PROCECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_WITHOUT_LISTING_STATUS_KNOWN, envelopeList);
        assertThat(prosecutionCaseDefendantListingStatusChangedTransformer.actionFor(hearingListingStatusEnvelope), is(TRANSFORM));
    }

    @Test
    public void shouldNotEnrichEventWithHearingListingStatusForKnownEventWithListingStatus() {
        final JsonValue payload = readJson(PROCECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_WITH_LISTING_STATUS_KNOWN, JsonValue.class);
        final JsonEnvelope hearingListingStatusEnvelope = envelopeFrom(
                Envelope.metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_CASE_DEFENDANT_LISTING_STATUS_CHANGED), payload);
        final List<JsonEnvelope> envelopeList = prosecutionCaseDefendantListingStatusChangedTransformer.apply(hearingListingStatusEnvelope).collect(toList());
        thenOutputStreamHasOneEvent(envelopeList);
        verifyProsecutionCaseDefendantListingStatusChanged(PROCECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_WITH_LISTING_STATUS_KNOWN, envelopeList);
        assertThat(prosecutionCaseDefendantListingStatusChangedTransformer.actionFor(hearingListingStatusEnvelope), is(NO_ACTION));
    }

    @Test
    public void shouldNotEnrichEventWithHearingListingStatusForUnknownEvent() {
        final JsonValue payload = readJson(PROCECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_WITH_LISTING_STATUS_UNKNOWN, JsonValue.class);
        final JsonEnvelope hearingListingStatusEnvelope = envelopeFrom(
                Envelope.metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_CASE_DEFENDANT_LISTING_STATUS_CHANGED), payload);
        final List<JsonEnvelope> envelopeList = prosecutionCaseDefendantListingStatusChangedTransformer.apply(hearingListingStatusEnvelope).collect(toList());
        thenOutputStreamHasOneEvent(envelopeList);
        verifyProsecutionCaseDefendantListingStatusChanged(PROCECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_WITH_LISTING_STATUS_UNKNOWN, envelopeList);
        assertThat(prosecutionCaseDefendantListingStatusChangedTransformer.actionFor(hearingListingStatusEnvelope), is(NO_ACTION));
    }

    private void thenOutputStreamHasOneEvent(final List<JsonEnvelope> envelopeList) {
        assertThat(envelopeList, hasSize(1));
    }

    private void verifyProsecutionCaseDefendantListingStatusChanged(final String expectedFileName,
                                                                    final List<JsonEnvelope> envelopeList) {
        final JsonEnvelope expectedEnvelope = envelopeFrom(
                Envelope.metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_CASE_DEFENDANT_LISTING_STATUS_CHANGED),
                readJson(expectedFileName, JsonValue.class));

        final JsonEnvelope actualJsonEnvelope = envelopeList.get(0);
        final JsonObject actualPayload = actualJsonEnvelope.payloadAsJsonObject();

        final Metadata metadata = actualJsonEnvelope.metadata();
        assertThat(metadata.name(), is(expectedEnvelope.metadata().name()));
        assertThat(metadata.userId(), is(expectedEnvelope.metadata().userId()));
        assertThat(metadata.createdAt(), is(expectedEnvelope.metadata().createdAt()));
        assertThat(metadata.causation(), is(expectedEnvelope.metadata().causation()));
        assertThat(metadata.streamId(), is(expectedEnvelope.metadata().streamId()));
        assertThat(metadata.id(), is((expectedEnvelope.metadata().id())));

        final JsonObject actualHearing = actualPayload.getJsonObject("hearing");
        final JsonObject expectedHearing = expectedEnvelope.payloadAsJsonObject().getJsonObject("hearing");

        assertThat(actualJsonEnvelope.payloadAsJsonObject().getString("hearingListingStatus"), is(HEARING_LISTING_STATUS_VALUE));


        final JsonObject actualCourtCentre = actualHearing.getJsonObject("courtCentre");
        final JsonObject expectedCourtCentre = expectedHearing.getJsonObject("courtCentre");

        final JsonObject actualCourtCentreAddress = actualCourtCentre.getJsonObject("address");
        final JsonObject expectedCourtCentreAddress = expectedCourtCentre.getJsonObject("address");

        assertThat(actualCourtCentreAddress.getString("address1"), is(expectedCourtCentreAddress.getString("address1")));
        assertThat(actualCourtCentreAddress.getString("address2"), is(expectedCourtCentreAddress.getString("address2")));
        assertThat(actualCourtCentreAddress.getString("address3"), is(expectedCourtCentreAddress.getString("address3")));
        assertThat(actualCourtCentreAddress.getString("address4"), is(expectedCourtCentreAddress.getString("address4")));
        assertThat(actualCourtCentreAddress.getString("address5"), is(expectedCourtCentreAddress.getString("address5")));
        assertThat(actualCourtCentreAddress.getString("postcode"), is(expectedCourtCentreAddress.getString("postcode")));

        assertThat(actualCourtCentre.getString("id"), is(expectedCourtCentre.getString("id")));
        assertThat(actualCourtCentre.getString("name"), is(expectedCourtCentre.getString("name")));
        assertThat(actualCourtCentre.getString("roomId"), is(expectedCourtCentre.getString("roomId")));
        assertThat(actualCourtCentre.getString("roomName"), is(expectedCourtCentre.getString("roomName")));

        final JsonObject acutualHearingDay = actualHearing.getJsonArray("hearingDays").getJsonObject(0);
        final JsonObject expectedHearingDay = actualHearing.getJsonArray("hearingDays").getJsonObject(0);
        assertThat(acutualHearingDay.getJsonNumber("listedDurationMinutes"), is(expectedHearingDay.getJsonNumber("listedDurationMinutes")));
        assertThat(acutualHearingDay.getJsonNumber("listingSequence"), is(expectedHearingDay.getJsonNumber("listingSequence")));
        assertThat(acutualHearingDay.getString("sittingDay"), is(expectedHearingDay.getString("sittingDay")));

        assertThat(actualHearing.getString("hearingLanguage"), is(expectedHearing.getString("hearingLanguage")));
        assertThat(actualHearing.getString("id"), is(expectedHearing.getString("id")));
        assertThat(actualHearing.getString("jurisdictionType"), is(expectedHearing.getString("jurisdictionType")));

        final JsonArray actualJudiciary = actualHearing.getJsonArray("judiciary");
        final JsonArray expectedJudiciary = expectedHearing.getJsonArray("judiciary");
        assertThat(actualJudiciary, is(expectedJudiciary));

        final JsonArray actualProsecutionCases = actualHearing.getJsonArray("prosecutionCases");
        final JsonArray expectedProsecutionCases = expectedHearing.getJsonArray("prosecutionCases");
        assertThat(actualProsecutionCases, is(expectedProsecutionCases));

        final JsonObject actualType = actualHearing.getJsonObject("type");
        final JsonObject expectedType = expectedHearing.getJsonObject("type");

        assertThat(actualType.getString("description"), is(expectedType.getString("description")));
        assertThat(actualType.getString("id"), is(expectedType.getString("id")));
    }

    private static <T> T readJson(final String jsonPath, final Class<T> clazz, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(jsonPath)) {
            return OBJECT_MAPPER.readValue(format(IOUtils.toString(systemResourceAsStream), placeholders), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible ", e);
        }
    }
}