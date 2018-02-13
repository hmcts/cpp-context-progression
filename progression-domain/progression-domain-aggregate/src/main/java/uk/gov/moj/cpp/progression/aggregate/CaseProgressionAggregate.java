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
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantDefenceSolicitorFirm;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantInterpreter;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CaseAlreadyExistsInCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetInvalidated;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetPreviouslyCompleted;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailStatusUpdatedForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantNotFound;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

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
    public static final String CANNOT_ADD_ADDITIONAL_INFO = "Cannot add additional information without defendant %s";
    private static final long serialVersionUID = 6L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProgressionAggregate.class);
    public static final String CASE_ID = "caseId";
    private Set<UUID> caseIdsWithCompletedSendingSheet = new HashSet<>();
    private boolean isAllDefendantReviewed;
    private boolean isAnyDefendantPending;

    private String courtCentreId;
    private Set<String> policeDefendantIds = new HashSet<>();
    private Set<Defendant> defendants = new HashSet<>();
    private Map<UUID, List<OffenceForDefendant>> offenceForDefendants = new HashMap<>();
    private Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
    private Map<UUID, BailDocument> defendantsBailDocuments = new HashMap<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DefendantAdded.class).apply(e -> {
                            this.defendants.add(new Defendant(e.getDefendantId()));
                            this.policeDefendantIds.add(e.getPoliceDefendantId());
                            this.offenceIdsByDefendantId.put(
                                    e.getDefendantId(),
                                    e.getOffences().stream().map(Offence::getId).collect(Collectors.toSet()));
                        }
                ),
                when(OffencesForDefendantUpdated.class).apply(e ->
                        e.getOffences().forEach(o -> {
                            this.offenceForDefendants.put(e.getDefendantId(), e.getOffences());
                            this.offenceIdsByDefendantId.put(
                                    e.getDefendantId(),
                                    e.getOffences().stream().map(OffenceForDefendant::getId).collect(Collectors.toSet()));
                        })
                ),
                when(BailStatusUpdatedForDefendant.class)
                        .apply(e -> this.defendantsBailDocuments.put(e.getDefendantId(), e.getBailDocument()
                        )),
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
                        }),
                when(SendingSheetCompleted.class)
                        .apply(e -> caseIdsWithCompletedSendingSheet.add(e.getHearing().getCaseId())),
                otherwiseDoNothing());

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

    public Stream<Object> addCaseToCrownCourt(JsonEnvelope jsonEnvelope) {
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


    public Stream<Object> prepareForSentenceHearing(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createCaseReadyForSentenceHearing(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> requestPsrForDefendant(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createPsrForDefendantsRequested(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> sendingHearingCommittal(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createSendingCommittalHearingInformationAdded(jsonEnvelope));
        return apply(streamBuilder.build());
    }


    public Stream<Object> caseToBeAssigned(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createCaseToBeAssignedUpdated(jsonEnvelope));
        return apply(streamBuilder.build());
    }


    public Stream<Object> addSentenceHearingDate(final UUID caseId, LocalDate sentenceHearingDate) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(new SentenceHearingDateAdded(sentenceHearingDate, caseId));
        return apply(streamBuilder.build());
    }

    private Map<UUID, Set<UUID>> sendingSheetToDefendantOffenceMap(JsonObject payload) {
        JsonArray jsonDefendants = payload.getJsonObject(HEARING_PAYLOAD_PROPERTY).getJsonArray("defendants");
        Map<UUID, Set<UUID>> incomingDefendantId2OffenceIds = new HashMap<>();
        if (jsonDefendants != null) {
            jsonDefendants.forEach(
                    jd -> {
                        JsonObject jdo = (JsonObject) jd;
                        UUID defendantId = UUID.fromString(jdo.getString("id"));
                        JsonArray jsonOffences = ((JsonObject) jd).getJsonArray("offences");
                        Set<UUID> offenceIds = jsonOffences.stream().map(jo -> (UUID.fromString(((JsonObject) jo).getString("id")))).collect(Collectors.toSet());
                        incomingDefendantId2OffenceIds.put(defendantId, offenceIds);
                    }
            );
        }
        return incomingDefendantId2OffenceIds;
    }

    public Stream<Object> completeSendingSheet(JsonEnvelope jsonEnvelope) {
        JsonObject payload = jsonEnvelope.payloadAsJsonObject();
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

        String incomingCourtCentreId = payload.getJsonObject(CROWN_COURT_HEARING_PROPERTY).getString("courtCentreId");

        if (incomingCourtCentreId == null || !incomingCourtCentreId.equals(courtCentreId)) {
            LOGGER.error("CourtCentreId mismatch for case with id: {}. courtCentreId:{}, incomingCourtCentreId:{}", caseId, courtCentreId, incomingCourtCentreId);
            return apply(Stream.of(new SendingSheetInvalidated(caseId, String.format("CourtCentreId mismatch. courtCentreId:%s, incomingCourtCentreId:%s", courtCentreId, incomingCourtCentreId))));
        }

        Map<UUID, Set<UUID>> incomingDefendantId2OffenceIds = sendingSheetToDefendantOffenceMap(payload);

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        Set<UUID> defendantUUIDs = defendants.stream().map(Defendant::getId).collect(Collectors.toSet());
        Function<Set<UUID>, String> uuidsToString = uuids ->
                uuids == null ? null : uuids.stream().map(UUID::toString).collect(Collectors.joining(","));

        if (!incomingDefendantId2OffenceIds.keySet().equals(defendantUUIDs)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("CaseId: {}: invalid sending sheet defendant ids specified do not match history: ({}) / ({}) ", caseId,
                        uuidsToString.apply(incomingDefendantId2OffenceIds.keySet()), uuidsToString.apply(defendantUUIDs));
            }
            return apply(Stream.of(new SendingSheetInvalidated(caseId, String.format("defendant ids specified do not match history: (%s) / (%s) ",
                    uuidsToString.apply(incomingDefendantId2OffenceIds.keySet()), uuidsToString.apply(defendantUUIDs)))));

        }
        for (UUID defendantUUID : defendantUUIDs) {
            Set<UUID> incomingOffenceIds = incomingDefendantId2OffenceIds.get(defendantUUID);
            Set<UUID> offenceIds = offenceIdsByDefendantId.get(defendantUUID);
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


    private void updateDefendantInfo(boolean isAdditionalInfo, UUID defendantId) {
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

        updateDefendantInfo(defendant.getAdditionalInformation() != null ? true : false, defendantId);

        // check if all defendant's reviewed
        checkAllDefendant();
        updateCaseStatus(streamBuilder, defendant.getCaseId());

        streamBuilder.add(addDefendantEvent(defendant));
        return apply(streamBuilder.build());
    }


    public Stream<Object> updateOffencesForDefendant(OffencesForDefendantUpdated offencesForDefendantUpdated) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(offencesForDefendantUpdated);
        return apply(streamBuilder.build());
    }

    public Stream<Object> updateDefendantBailStatus(UUID caseId, UUID defendantId, String bailStatus, Optional<BailDocument> bailDocument, Optional<LocalDate> custodyTimeLimitDate) {
        if (!this.offenceIdsByDefendantId.containsKey(defendantId)) {
            LOGGER.warn("Cannot update bail status without defendant ", defendantId);
            return apply(Stream.of(new DefendantNotFound(defendantId.toString(), "Update Defendant Bail Status")));
        }

        return apply(Stream.of(new BailStatusUpdatedForDefendant(
                caseId,
                defendantId,
                bailStatus,
                bailDocument.orElse(null),
                custodyTimeLimitDate.orElse(null))));
    }

    public Stream<Object> updateDefenceSolicitorFirm(
            UpdateDefendantDefenceSolicitorFirm updateDefenceSolicitorFirm) {
        final UUID defendantId = updateDefenceSolicitorFirm.getDefendantId();
        if (!offenceIdsByDefendantId.containsKey(defendantId)) {
            LOGGER.error("Cannot set defence solicitor firm status without defendant {}", defendantId);
            return apply(Stream.of(new DefendantNotFound(defendantId.toString(), "Update Defence Solicitor Firm")));
        }
        return apply(Stream.of(ProgressionEventFactory.asSolicitorFirmUpdatedForDefendant(updateDefenceSolicitorFirm)));
    }

    public Stream<Object> addDefendant(AddDefendant addDefendantCommand) {
        final UUID defendantId = addDefendantCommand.getDefendantId();
        final UUID caseId = addDefendantCommand.getCaseId();
        final String caseUrn = addDefendantCommand.getCaseUrn();
        final String policeDefendantId = addDefendantCommand.getPoliceDefendantId();

        if (offenceIdsByDefendantId.containsKey(defendantId) || policeDefendantIds.contains(policeDefendantId)) {
            LOGGER.error("Defendant already exists with ID: {} or PoliceDefendantId: {}", defendantId, policeDefendantId);
            return apply(Stream.of(new DefendantAdditionFailed(caseId.toString(), defendantId.toString(), "Add Defendant failed as defendant already exists")));
        }

        return apply(Stream.of(new DefendantAdded(
                addDefendantCommand.getCaseId(), addDefendantCommand.getDefendantId(), addDefendantCommand.getPersonId(),
                addDefendantCommand.getPoliceDefendantId(), addDefendantCommand.getOffences(), caseUrn)));
    }

    public Stream<Object> updateDefendantInterpreter(final UpdateDefendantInterpreter updateDefendantInterpreter) {
        final UUID defendantId = updateDefendantInterpreter.getDefendantId();

        if (!offenceIdsByDefendantId.containsKey(defendantId)) {
            LOGGER.warn("Cannot set defendant interpreter without defendant ", defendantId);
            return apply(Stream.of(new DefendantNotFound(defendantId.toString(), "Update Defendant Interpreter")));
        }
        return apply(Stream.of(ProgressionEventFactory.asInterpreterUpdatedForDefendant(updateDefendantInterpreter)));
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


    private void updateCaseStatus(Stream.Builder<Object> streamBuilder, final UUID caseId) {
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
}
