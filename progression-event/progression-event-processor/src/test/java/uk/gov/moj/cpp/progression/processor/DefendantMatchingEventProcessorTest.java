package uk.gov.moj.cpp.progression.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdatedIntoHearings;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdatedV2;
import uk.gov.moj.cpp.progression.events.MatchedDefendants;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantMatchingEventProcessorTest {

    @Mock
    private ProgressionService progressionService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Sender sender;

    @InjectMocks
    private DefendantMatchingEventProcessor defendantMatchingEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void handleDefendantMatchedEventWhenMatchedForTheFirstTime() {
        defendantMatchingEventProcessor.handleDefendantMatchedEvent(getJsonEnvelope(false));
        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().payload().getBoolean("hasDefendantAlreadyBeenDeleted"), is(false));
    }

    @Test
    public void handleDefendantUnmatchedEvent() {
        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(createDefendants(defendantId))
                .build();

        final JsonObject incomingProsecutionCaseJson = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(incomingProsecutionCase))
                .build();
        final Optional<JsonObject> incomingProsecutionCaseJsonOptional = Optional.of(incomingProsecutionCaseJson);


        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(incomingProsecutionCaseJsonOptional);
        defendantMatchingEventProcessor.handleDefendantUnmatchedEvent(getJsonEnvelopeForUnmatching(prosecutionCaseId.toString(), defendantId.toString()));
        verify(sender, times(2)).send(envelopeCaptor.capture());
        envelopeCaptor.getAllValues().forEach(
                v -> {
                    if (v.metadata().name().equalsIgnoreCase("public.progression.defendant-unmatched")) {
                        assertThat(v.payload().getString("defendantId"), is(defendantId.toString()));
                    } else {
                        assertThat(v.payload().getJsonObject("defendant").getString("id"), is(defendantId.toString()));
                        assertThat(v.payload().getJsonObject("defendant").getString("masterDefendantId"), is(defendantId.toString()));
                    }
                }
        );
    }

    @Test
    public void handleDefendantUnmatchedV2Event() {
        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(createDefendants(defendantId))
                .build();

        final JsonObject incomingProsecutionCaseJson = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(incomingProsecutionCase))
                .build();
        final Optional<JsonObject> incomingProsecutionCaseJsonOptional = Optional.of(incomingProsecutionCaseJson);

        defendantMatchingEventProcessor.handleDefendantUnmatchedV2Event(
                buildDefendantUnmatchedV2EventEnvelope(prosecutionCaseId.toString(), defendantId.toString(), defendantId.toString()));

        verify(sender, times(2)).send(envelopeCaptor.capture());

        envelopeCaptor.getAllValues().forEach(
                v -> {
                    if (v.metadata().name().equalsIgnoreCase("public.progression.defendant-unmatched")) {
                        assertThat(v.payload().getString("defendantId"), is(defendantId.toString()));
                    } else {
                        assertThat(v.payload().getJsonObject("defendant").getString("id"), is(defendantId.toString()));
                        assertThat(v.payload().getJsonObject("defendant").getString("masterDefendantId"), is(defendantId.toString()));
                    }
                }
        );
        verifyNoMoreInteractions(progressionService);
    }

    @Test
    public void handleDefendantMatchedEventWhenMatchedAlready() {
        defendantMatchingEventProcessor.handleDefendantMatchedEvent(getJsonEnvelope(true));
        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().payload().getBoolean("hasDefendantAlreadyBeenDeleted"), is(true));
    }

    @Test
    public void handleMasterDefendantIdUpdatedEvent() {
        final UUID incomingDefendantId = randomUUID();
        final UUID incomingProsecutionCaseId = randomUUID();
        final UUID matchedDefendantId = randomUUID();
        final UUID matchedProsecutionCaseId = randomUUID();
        final UUID matchedMasterDefendantId = randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");

        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();

        final JsonObject incomingProsecutionCaseJson = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(incomingProsecutionCase))
                .build();
        final Optional<JsonObject> incomingProsecutionCaseJsonOptional = Optional.of(incomingProsecutionCaseJson);

        final MasterDefendantIdUpdated masterDefendantIdUpdated = MasterDefendantIdUpdated.masterDefendantIdUpdated()
                .withDefendantId(incomingDefendantId)
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withMatchedDefendants(asList(MatchedDefendants.matchedDefendants()
                        .withDefendantId(matchedDefendantId)
                        .withProsecutionCaseId(matchedProsecutionCaseId)
                        .withMasterDefendantId(matchedMasterDefendantId)
                        .withCourtProceedingsInitiated(courtProceedingsInitiatedDate)
                        .build()))
                .build();

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(incomingProsecutionCaseJsonOptional);

        defendantMatchingEventProcessor.handleMasterDefendantIdUpdatedEvent(getJsonEnvelope(masterDefendantIdUpdated));
        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().payload().getJsonObject("defendant").getString("masterDefendantId"), is(matchedMasterDefendantId.toString()));
    }

    @Test
    public void handleMasterDefendantIdUpdatedEventWithNoCourtProceedingsInitiatedDateOnOneMatchedDefendant() {
        final UUID incomingDefendantId = randomUUID();
        final UUID incomingProsecutionCaseId = randomUUID();
        final UUID matchedDefendantId = randomUUID();
        final UUID matchedDefendantId2 = randomUUID();
        final UUID matchedProsecutionCaseId = randomUUID();
        final UUID matchedMasterDefendantId = randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");

        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();

        final JsonObject incomingProsecutionCaseJson = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(incomingProsecutionCase))
                .build();
        final Optional<JsonObject> incomingProsecutionCaseJsonOptional = Optional.of(incomingProsecutionCaseJson);

        final MasterDefendantIdUpdated masterDefendantIdUpdated = MasterDefendantIdUpdated.masterDefendantIdUpdated()
                .withDefendantId(incomingDefendantId)
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withMatchedDefendants(asList(MatchedDefendants.matchedDefendants()
                                .withDefendantId(matchedDefendantId)
                                .withProsecutionCaseId(matchedProsecutionCaseId)
                                .withMasterDefendantId(matchedMasterDefendantId)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate)
                                .build(),
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(matchedDefendantId2)
                                .withProsecutionCaseId(matchedProsecutionCaseId)
                                .withMasterDefendantId(matchedMasterDefendantId)
                                .build()))
                .build();

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(incomingProsecutionCaseJsonOptional);

        defendantMatchingEventProcessor.handleMasterDefendantIdUpdatedEvent(getJsonEnvelope(masterDefendantIdUpdated));
        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().payload().getJsonObject("defendant").getString("masterDefendantId"), is(matchedMasterDefendantId.toString()));
    }

    private List<Defendant> createDefendants(final UUID matchedDefendantId) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withId(matchedDefendantId)
                .build());
        return defendants;
    }

    private JsonEnvelope getJsonEnvelope(final boolean isDeleted) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-matched"),
                Json.createObjectBuilder()
                        .add("defendantId", randomUUID().toString())
                        .add("hasDefendantAlreadyBeenDeleted", isDeleted)
                        .build());
    }

    private JsonEnvelope getJsonEnvelopeForUnmatching(final String prosecutionCaseId, final String defendantId) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-unmatched"),
                Json.createObjectBuilder()
                        .add("defendantId", defendantId)
                        .add("prosecutionCaseId", prosecutionCaseId)
                        .build());
    }

    private JsonEnvelope buildDefendantUnmatchedV2EventEnvelope(final String prosecutionCaseId, final String defendantId, final String masterDefendantId) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-unmatched-v2"),
                Json.createObjectBuilder()
                        .add("defendantId", defendantId)
                        .add("prosecutionCaseId", prosecutionCaseId)
                        .add("defendant", Json.createObjectBuilder()
                                .add("id", defendantId)
                                .add("masterDefendantId", masterDefendantId)
                                .build())
                        .build());
    }

    private JsonEnvelope getJsonEnvelope(final MasterDefendantIdUpdated masterDefendantIdUpdated) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-matched"),
                objectToJsonObjectConverter.convert(masterDefendantIdUpdated));
    }

    @Test
    public void handleMasterDefendantIdUpdatedEventV2() {
        final UUID incomingProsecutionCaseId = randomUUID();
        final UUID matchedDefendantId = randomUUID();
        final UUID matchedProsecutionCaseId = randomUUID();
        final UUID matchedMasterDefendantId = randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");

        final MasterDefendantIdUpdatedV2 masterDefendantIdUpdated = MasterDefendantIdUpdatedV2.masterDefendantIdUpdatedV2()
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withDefendant(Defendant.defendant()
                        .withId(randomUUID())
                        .build())
                .withMatchedDefendants(asList(MatchedDefendants.matchedDefendants()
                        .withDefendantId(matchedDefendantId)
                        .withProsecutionCaseId(matchedProsecutionCaseId)
                        .withMasterDefendantId(matchedMasterDefendantId)
                        .withCourtProceedingsInitiated(courtProceedingsInitiatedDate)
                        .build()))
                .build();

        defendantMatchingEventProcessor.handleMasterDefendantIdUpdatedEventV2(JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-matched"),
                objectToJsonObjectConverter.convert(masterDefendantIdUpdated)));

        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().payload().getJsonObject("defendant").getString("masterDefendantId"), is(matchedMasterDefendantId.toString()));
    }

    @Test
    public void shouldHandleMasterDefendantIdUpdatedEventForHearingAndShouldRaiseCommandForUniqueHearingIds() {
        final UUID incomingProsecutionCaseId = randomUUID();
        final UUID matchedDefendantId = randomUUID();
        final UUID matchedProsecutionCaseId = randomUUID();
        final UUID matchedMasterDefendantId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID hearingId3 = randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");


        final MasterDefendantIdUpdatedIntoHearings masterDefendantIdUpdatedIntoHearings = MasterDefendantIdUpdatedIntoHearings.masterDefendantIdUpdatedIntoHearings()
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withDefendant(Defendant.defendant()
                        .withId(randomUUID())
                        .build())
                .withMatchedDefendants(asList(MatchedDefendants.matchedDefendants()
                        .withDefendantId(matchedDefendantId)
                        .withProsecutionCaseId(matchedProsecutionCaseId)
                        .withMasterDefendantId(matchedMasterDefendantId)
                        .withCourtProceedingsInitiated(courtProceedingsInitiatedDate)
                        .build()))
                .withHearingIds(asList(hearingId1, hearingId2, hearingId1, hearingId3, hearingId2))
                .build();

        defendantMatchingEventProcessor.handleMasterDefendantIdUpdatedEventForHearing(JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.master-defendant-id-updated-into-hearings"),
                objectToJsonObjectConverter.convert(masterDefendantIdUpdatedIntoHearings)));

        verify(sender, times(3)).send(envelopeCaptor.capture());
        envelopeCaptor.getAllValues().forEach(envelope -> assertThat(envelope.metadata().name(), is("progression.command.update-defendant-for-hearing")));

    }

    @Test
    public void shouldHandleMasterDefendantIdUpdatedEventForHearingAndShouldNotRaiseCommandWhenHearingIdsIsEmpty() {
        final UUID incomingProsecutionCaseId = randomUUID();
        final UUID matchedDefendantId = randomUUID();
        final UUID matchedProsecutionCaseId = randomUUID();
        final UUID matchedMasterDefendantId = randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");


        final MasterDefendantIdUpdatedIntoHearings masterDefendantIdUpdatedIntoHearings = MasterDefendantIdUpdatedIntoHearings.masterDefendantIdUpdatedIntoHearings()
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withDefendant(Defendant.defendant()
                        .withId(randomUUID())
                        .build())
                .withMatchedDefendants(asList(MatchedDefendants.matchedDefendants()
                        .withDefendantId(matchedDefendantId)
                        .withProsecutionCaseId(matchedProsecutionCaseId)
                        .withMasterDefendantId(matchedMasterDefendantId)
                        .withCourtProceedingsInitiated(courtProceedingsInitiatedDate)
                        .build()))
                .withHearingIds(emptyList())
                .build();

        defendantMatchingEventProcessor.handleMasterDefendantIdUpdatedEventForHearing(JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.master-defendant-id-updated-into-hearings"),
                objectToJsonObjectConverter.convert(masterDefendantIdUpdatedIntoHearings)));

        verify(sender, times(0)).send(envelopeCaptor.capture());

    }

}
