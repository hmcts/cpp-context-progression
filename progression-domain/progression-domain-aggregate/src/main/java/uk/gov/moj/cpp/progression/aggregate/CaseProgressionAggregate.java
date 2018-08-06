package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.addDefendantEvent;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createCaseAddedToCrownCourt;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createCaseReadyForSentenceHearing;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createCaseToBeAssignedUpdated;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createPsrForDefendantsRequested;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createSendingCommittalHearingInformationAdded;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantOffenceHelper;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantUpdateHelper;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CaseAlreadyExistsInCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateRemoved;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetInvalidated;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetPreviouslyCompleted;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffencesChanged;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdateFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseProgressionAggregate implements Aggregate {

    private static final String HEARING_PAYLOAD_PROPERTY = "hearing";
    private static final String CROWN_COURT_HEARING_PROPERTY = "crownCourtHearing";
    private static final String CANNOT_ADD_ADDITIONAL_INFO =
            "Cannot add additional information without defendant %s";
    private static final long serialVersionUID = 7L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProgressionAggregate.class);
    private static final String CASE_ID = "caseId";
    private final Set<UUID> caseIdsWithCompletedSendingSheet = new HashSet<>();
    private boolean isAllDefendantReviewed;
    private boolean isAnyDefendantPending;

    private String courtCentreId;
    private final Set<String> policeDefendantIds = new HashSet<>();
    private final Set<Defendant> defendants = new HashSet<>();
    private final Map<UUID, List<OffenceForDefendant>> offenceForDefendants = new HashMap<>();
    private final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
    private final Map<UUID, BailDocument> defendantsBailDocuments = new HashMap<>();
    private boolean caseInReview;
    private final Map<UUID, Person> personOnDefendantAdded = new HashMap<>();
    private final Map<UUID, Person> personForDefendant = new HashMap<>();
    private final Map<UUID, Interpreter> interpreterForDefendant = new HashMap<>();
    private final Map<UUID, String> bailStatusForDefendant = new HashMap<>();
    private final Map<UUID, String> solicitorFirmForDefendant = new HashMap<>();
    private final Map<UUID, LocalDate> custodyTimeLimitForDefendant = new HashMap<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DefendantAdded.class).apply(e -> {
                            this.defendants.add(new Defendant(e.getDefendantId()));
                            this.policeDefendantIds.add(e.getPoliceDefendantId());
                            this.offenceIdsByDefendantId.put(
                                    e.getDefendantId(),
                                    e.getOffences().stream().map(Offence::getId).collect(Collectors.toSet()));
                            if (e.getPerson() != null) {
                                this.personOnDefendantAdded.put(e.getDefendantId(), e.getPerson());
                                this.personForDefendant.put(e.getDefendantId(), e.getPerson());
                            }
                            this.offenceForDefendants.put(e.getDefendantId(), e
                                    .getOffences().stream()
                                    .map(o -> new OffenceForDefendant(o.getId(), o.getCjsCode(), null,
                                            o.getWording(), o.getStartDate(), o.getEndDate(),
                                            0, null, null,
                                            null, o.getChargeDate()))
                                    .collect(Collectors.toList()));
                        }
                ), when(DefendantUpdated.class).apply(this::updateDefendantRelatedMaps),
                when(OffencesForDefendantUpdated.class).apply(e ->
                        e.getOffences().forEach(o -> {
                            this.offenceForDefendants.put(e.getDefendantId(), e.getOffences());
                            this.offenceIdsByDefendantId.put(
                                    e.getDefendantId(),
                                    e.getOffences().stream().map(OffenceForDefendant::getId).collect(Collectors.toSet()));
                        })
                ),
                when(uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt.class)
                        .apply(e ->
                                this.courtCentreId = e.getCourtCentreId()
                        ),
                when(uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded.class)
                        .apply(e -> {
                            final Defendant defendant = defendants.stream()
                                    .filter(d -> d.getId()
                                            .equals(e.getDefendantId()))
                                    .findAny().get();
                            defendant.setSentenceHearingReviewDecision(true);
                            if (e.getAdditionalInformationEvent() != null) {
                                defendant.setIsAdditionalInfoAvilable(true);
                            }
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent.class)
                        .apply(e -> {
                            final Defendant defendant = defendants.stream()
                                    .filter(d -> d.getId()
                                            .equals(e.getDefendantId()))
                                    .findAny().get();
                            defendant.setSentenceHearingReviewDecision(true);
                            defendant.setIsAdditionalInfoAvilable(false);
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(SendingSheetPreviouslyCompleted.class)
                        .apply(e -> {
                            // Do Nothng
                        }), when(DefendantUpdated.class).apply(e ->
                        this.defendantsBailDocuments.put(e.getDefendantId(),
                                e.getBailDocument())
                ), when(SendingSheetCompleted.class).apply(e -> {
                    caseIdsWithCompletedSendingSheet.add(e.getHearing().getCaseId());
                    caseInReview = true;
                }), otherwiseDoNothing());

    }

    private void checkAllDefendant() {

        // check if all defendant is reviewed
        final Defendant defReviewRequire = defendants.stream()
                .filter(d -> d.getSentenceHearingReviewDecision() == null
                        || (!d.getSentenceHearingReviewDecision().booleanValue()))
                .findFirst().orElse(null);
        if (defReviewRequire == null) {
            isAllDefendantReviewed = true;
        }

        // check if any defendant additional information is required
        final Defendant def = defendants.stream()
                .filter(d -> d.getIsAdditionalInfoAvilable() != null
                        && d.getIsAdditionalInfoAvilable().booleanValue())
                .findFirst().orElse(null);

        if (def != null) {
            isAnyDefendantPending = true;
        }

    }

    public Stream<Object> noMoreInformationForDefendant(final UUID defendantId, final UUID caseId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (!offenceIdsByDefendantId.keySet().contains(defendantId)) {
            LOGGER.warn(CANNOT_ADD_ADDITIONAL_INFO, defendantId);
            return Stream.empty();
        }

        updateDefendantInfo(false, defendantId);
        // check if all defendant's reviewed
        checkAllDefendant();

        updateCaseStatus(streamBuilder, caseId);

        streamBuilder.add(new NoMoreInformationRequiredEvent(caseId, defendantId));
        return apply(streamBuilder.build());
    }

    public Stream<Object> addCaseToCrownCourt(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = UUID.fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
        final String centreId = jsonEnvelope.payloadAsJsonObject().getString("courtCentreId");
        if (centreId.equals(this.courtCentreId)) {
            LOGGER.warn("Case already exists in crown court with Id %s", caseId);
            return apply(Stream.of(new CaseAlreadyExistsInCrownCourt(caseId, "Case already exists in crown court with Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createCaseAddedToCrownCourt(jsonEnvelope));
        return apply(streamBuilder.build());
    }


    public Stream<Object> prepareForSentenceHearing(final JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createCaseReadyForSentenceHearing(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> requestPsrForDefendant(final JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createPsrForDefendantsRequested(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> sendingHearingCommittal(final JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createSendingCommittalHearingInformationAdded(jsonEnvelope));
        return apply(streamBuilder.build());
    }


    public Stream<Object> caseToBeAssigned(final JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createCaseToBeAssignedUpdated(jsonEnvelope));
        return apply(streamBuilder.build());
    }


    public Stream<Object> addSentenceHearingDate(final UUID caseId, final LocalDate sentenceHearingDate) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(new SentenceHearingDateAdded(sentenceHearingDate, caseId));
        return apply(streamBuilder.build());
    }

    public Stream<Object> addConvictionDateToOffence(final UUID caseId,
                                                     final UUID offenceId, final LocalDate convictionDate) {
        return apply(Stream.of(ConvictionDateAdded.builder()
                .withCaseId(caseId)
                .withOffenceId(offenceId)
                .withConvictionDate(convictionDate)
                .build()));
    }

    public Stream<Object> removeConvictionDateFromOffence(final UUID caseId, final UUID offenceId) {
        return apply(Stream.of(ConvictionDateRemoved.builder()
                .withCaseId(caseId)
                .withOffenceId(offenceId)
                .build()));
    }

    private Map<UUID, Set<UUID>> sendingSheetToDefendantOffenceMap(final JsonObject payload) {
        final JsonArray jsonDefendants = payload.getJsonObject(HEARING_PAYLOAD_PROPERTY).getJsonArray("defendants");
        final Map<UUID, Set<UUID>> incomingDefendantId2OffenceIds = new HashMap<>();
        if (jsonDefendants != null) {
            jsonDefendants.forEach(
                    jd -> {
                        final JsonObject jdo = (JsonObject) jd;
                        final UUID defendantId = UUID.fromString(jdo.getString("id"));
                        final JsonArray jsonOffences = ((JsonObject) jd).getJsonArray("offences");
                        final Set<UUID> offenceIds = jsonOffences.stream().map(jo -> (UUID.fromString(((JsonObject) jo).getString("id")))).collect(Collectors.toSet());
                        incomingDefendantId2OffenceIds.put(defendantId, offenceIds);
                    }
            );
        }
        return incomingDefendantId2OffenceIds;
    }

    public Stream<Object> completeSendingSheet(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final UUID caseId =
                UUID.fromString(payload
                        .getJsonObject(HEARING_PAYLOAD_PROPERTY)
                        .getString(CASE_ID));
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.error("Sending sheet already completed for case with id: {}", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "Sending sheet already completed for case with id " + caseId)));
        }

        if (defendants.isEmpty()) {
            LOGGER.error("CaseId:{} doesn't exist for sending sheet.", caseId);
            return apply(Stream.of(new SendingSheetInvalidated(caseId, "Invalid Case Id")));
        }

        final String incomingCourtCentreId = payload.getJsonObject(CROWN_COURT_HEARING_PROPERTY).getString("courtCentreId");

        if (incomingCourtCentreId == null || !incomingCourtCentreId.equals(courtCentreId)) {
            LOGGER.error("CourtCentreId mismatch for case with id: {}. courtCentreId:{}, incomingCourtCentreId:{}", caseId, courtCentreId, incomingCourtCentreId);
            return apply(Stream.of(new SendingSheetInvalidated(caseId, String.format("CourtCentreId mismatch. courtCentreId:%s, incomingCourtCentreId:%s", courtCentreId, incomingCourtCentreId))));
        }

        final Map<UUID, Set<UUID>> incomingDefendantId2OffenceIds = sendingSheetToDefendantOffenceMap(payload);

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final Set<UUID> defendantUUIDs = defendants.stream().map(Defendant::getId).collect(Collectors.toSet());
        final Function<Set<UUID>, String> uuidsToString = uuids ->
                uuids == null ? null : uuids.stream().map(UUID::toString).collect(Collectors.joining(","));

        if (!incomingDefendantId2OffenceIds.keySet().equals(defendantUUIDs)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("CaseId: {}: invalid sending sheet defendant ids specified do not match history: ({}) / ({}) ", caseId,
                        uuidsToString.apply(incomingDefendantId2OffenceIds.keySet()), uuidsToString.apply(defendantUUIDs));
            }
            return apply(Stream.of(new SendingSheetInvalidated(caseId, String.format("defendant ids specified do not match history: (%s) / (%s) ",
                    uuidsToString.apply(incomingDefendantId2OffenceIds.keySet()), uuidsToString.apply(defendantUUIDs)))));

        }
        for (final UUID defendantUUID : defendantUUIDs) {
            final Set<UUID> incomingOffenceIds = incomingDefendantId2OffenceIds.get(defendantUUID);
            final Set<UUID> offenceIds = offenceIdsByDefendantId.get(defendantUUID);
            if (!incomingOffenceIds.equals(offenceIds)) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("CaseId: %s: invalid sending sheet, offence ids for DefendantId: %s ids specified do not match history: ({}) / ({}) ", caseId, defendantUUID,
                            uuidsToString.apply(incomingDefendantId2OffenceIds.keySet()), uuidsToString.apply(defendantUUIDs));
                }
                return apply(Stream.of(new SendingSheetInvalidated(caseId, String.format("invalid sending sheet, offence ids for DefendantId: %s ids specified do not match history: (%s) / (%s) ",
                        defendantUUID, uuidsToString.apply(incomingOffenceIds), uuidsToString.apply(offenceIds)))));
            }
        }

        streamBuilder.add(ProgressionEventFactory.completedSendingSheet(jsonEnvelope));
        return apply(streamBuilder.build());
    }


    private void updateDefendantInfo(final boolean isAdditionalInfo, final UUID defendantId) {
        final Defendant def = defendants.stream().filter(d -> d.getId().equals(defendantId)).findAny().orElse(null);
        if (def != null) {
            def.setSentenceHearingReviewDecision(true);
            def.setIsAdditionalInfoAvilable(isAdditionalInfo);
        }
    }

    public Stream<Object> addAdditionalInformationForDefendant(final DefendantCommand defendant) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final UUID defendantId = defendant.getDefendantId();

        if (!offenceIdsByDefendantId.keySet().contains(defendantId)) {
            LOGGER.warn(CANNOT_ADD_ADDITIONAL_INFO, defendantId);
            return Stream.empty();
        }

        updateDefendantInfo(defendant.getAdditionalInformation() != null ? true : false,
                defendantId);

        // check if all defendant's reviewed
        checkAllDefendant();
        updateCaseStatus(streamBuilder, defendant.getCaseId());

        streamBuilder.add(addDefendantEvent(defendant));
        return apply(streamBuilder.build());
    }


    public Stream<Object> updateOffencesForDefendant(final OffencesForDefendantUpdated offencesForDefendantUpdated) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(offencesForDefendantUpdated);
        if (this.offenceForDefendants.containsKey(offencesForDefendantUpdated.getDefendantId())) {
            final Optional<DefendantOffencesChanged> defendantOffencesChanged =
                    DefendantOffenceHelper.getDefendantOffencesChanged(
                            offencesForDefendantUpdated.getCaseId(),
                            offencesForDefendantUpdated.getDefendantId(),
                            offencesForDefendantUpdated.getOffences(),
                            this.offenceForDefendants
                                    .get(offencesForDefendantUpdated
                                            .getDefendantId()));
            if (defendantOffencesChanged.isPresent()) {
                streamBuilder.add(defendantOffencesChanged.get());
            }
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> addDefendant(final AddDefendant addDefendantCommand) {
        final UUID defendantId = addDefendantCommand.getDefendantId();
        final UUID caseId = addDefendantCommand.getCaseId();
        final String caseUrn = addDefendantCommand.getCaseUrn();
        final String policeDefendantId = addDefendantCommand.getPoliceDefendantId();

        if (offenceIdsByDefendantId.containsKey(defendantId)
                || policeDefendantIds.contains(policeDefendantId)) {
            LOGGER.error("Defendant already exists with ID: {} or PoliceDefendantId: {}",
                    defendantId, policeDefendantId);
            return apply(Stream.of(
                    new DefendantAdditionFailed(caseId.toString(), defendantId.toString(),
                            "Add Defendant failed as defendant already exists")));
        }

        return apply(Stream.of(new DefendantAdded(addDefendantCommand.getCaseId(),
                addDefendantCommand.getDefendantId(), addDefendantCommand.getPerson(),
                addDefendantCommand.getPoliceDefendantId(),
                addDefendantCommand.getOffences(), caseUrn)));
    }


    public Map<UUID, List<OffenceForDefendant>> getOffenceForDefendants() {
        return offenceForDefendants;
    }

    public Map<UUID, Set<UUID>> getOffenceIdsByDefendantId() {
        return offenceIdsByDefendantId;
    }

    public Map<UUID, BailDocument> getDefendantsBailDocuments() {
        return defendantsBailDocuments;
    }


    private void updateCaseStatus(final Stream.Builder<Object> streamBuilder, final UUID caseId) {
        if (isAllDefendantReviewed) {
            if (isAnyDefendantPending) {
                streamBuilder.add(new CasePendingForSentenceHearing(caseId,
                        CaseStatusEnum.PENDING_FOR_SENTENCING_HEARING, ZonedDateTime.now(ZoneOffset.UTC)));
            } else {
                streamBuilder.add(new CaseReadyForSentenceHearing(caseId,
                        CaseStatusEnum.READY_FOR_SENTENCING_HEARING, ZonedDateTime.now(ZoneOffset.UTC)));
            }
        }
    }

    public Stream<Object> updateDefendant(
            final UpdateDefendantCommand updateDefendantCommandCommand) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (!this.offenceIdsByDefendantId
                .containsKey(updateDefendantCommandCommand.getDefendantId())) {
            streamBuilder.add(new DefendantUpdateFailed(
                    updateDefendantCommandCommand.getCaseId().toString(),
                    updateDefendantCommandCommand.getDefendantId().toString(),
                    "Defendant not foind for the case"));
        } else {
            final BailDocument bailDocument = (updateDefendantCommandCommand.getDocumentId() == null
                    ? null
                    : new BailDocument(UUID.randomUUID(),
                    updateDefendantCommandCommand.getDocumentId()));
            if (this.caseInReview && isDefendantUpdated(updateDefendantCommandCommand)) {
                streamBuilder.add(DefendantUpdateHelper.createDefendantUpdateConfirmedEvent(
                        updateDefendantCommandCommand,
                        this.personOnDefendantAdded.get(
                                updateDefendantCommandCommand.getDefendantId())));
            }
            streamBuilder.add(new DefendantUpdated(updateDefendantCommandCommand.getCaseId(),
                    updateDefendantCommandCommand.getDefendantId(),
                    updateDefendantCommandCommand.getPerson(), bailDocument,
                    updateDefendantCommandCommand.getInterpreter(),
                    updateDefendantCommandCommand.getBailStatus(),
                    updateDefendantCommandCommand.getCustodyTimeLimitDate(),
                    updateDefendantCommandCommand.getDefenceSolicitorFirm()));
        }
        return apply(streamBuilder.build());

    }

    private boolean isDefendantUpdated(final UpdateDefendantCommand updateDefendantCommand) {
        final UUID defendantId = updateDefendantCommand.getDefendantId();
        return DefendantUpdateHelper.isDefendantUpdated(updateDefendantCommand,
                this.personForDefendant.get(defendantId),
                this.bailStatusForDefendant.get(defendantId),
                this.interpreterForDefendant.get(defendantId),
                this.custodyTimeLimitForDefendant.get(defendantId),
                this.solicitorFirmForDefendant.get(defendantId));
    }


    private void updateDefendantRelatedMaps(final DefendantUpdated e) {
        if (e.getPerson() != null) {
            this.personForDefendant.put(e.getDefendantId(), e.getPerson());
        }
        if (e.getInterpreter() != null) {
            this.interpreterForDefendant.put(e.getDefendantId(), e.getInterpreter());
        }
        if (e.getBailDocument() != null) {
            this.defendantsBailDocuments.put(e.getDefendantId(), new BailDocument(
                    e.getBailDocument().getId(), e.getBailDocument().getMaterialId()));
        }
        if (e.getBailStatus() != null) {
            this.bailStatusForDefendant.put(e.getDefendantId(), e.getBailStatus());
        }
        if (e.getCustodyTimeLimitDate() != null) {
            this.custodyTimeLimitForDefendant.put(e.getDefendantId(), e.getCustodyTimeLimitDate());
        }
        if (e.getDefenceSolicitorFirm() != null) {
            this.solicitorFirmForDefendant.put(e.getDefendantId(), e.getDefenceSolicitorFirm());
        }
    }


}
