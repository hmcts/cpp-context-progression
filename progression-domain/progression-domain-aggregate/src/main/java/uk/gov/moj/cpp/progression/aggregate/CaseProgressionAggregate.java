package uk.gov.moj.cpp.progression.aggregate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantDefenceSolicitorFirm;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantInterpreter;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CaseAlreadyExistsInCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailStatusUpdatedForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAllocationDecisionRemoved;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAllocationDecisionUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantNotFound;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffencesDoesNotHaveRequiredModeOfTrial;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceNotFound;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.PleaUpdated;

import javax.json.JsonObject;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

public class CaseProgressionAggregate implements Aggregate {


    public static final String CANNOT_ADD_ADDITIONAL_INFO = "Cannot add additional information without defendant ";
    private static final long serialVersionUID = 5L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProgressionAggregate.class);
    private transient ProgressionEventFactory progressionEventFactory = new ProgressionEventFactory();
    private boolean isAllDefendantReviewed;
    private boolean isAnyDefendantPending;
    private LocalDate sentenceHearingDate;
    private UUID sentenceHearingId;

    private UUID caseIdForProgression;
    private String courtCentreId;
    private Set<String> policeDefendantIds = new HashSet<>();
    private Set<Defendant> defendants = new HashSet<>();
    private Map<UUID, List<OffenceForDefendant>> offenceForDefendants = new HashMap<>();
    private Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
    private Map<UUID, BailDocument> defendantsBailDocuments = new HashMap<>();
    private Set<UUID> allocationDecisionForDefendant = new HashSet<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DefendantAdded.class).apply(e -> {
                            this.caseIdForProgression = e.getCaseId();
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
                        .apply(e ->
                                this.sentenceHearingDate = e.getSentenceHearingDate()
                        ),
                when(uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateUpdated.class)
                        .apply(e ->
                                this.sentenceHearingDate = e.getSentenceHearingDate()
                        ),
                when(uk.gov.moj.cpp.progression.domain.event.SentenceHearingAdded.class)
                        .apply(e ->
                                this.sentenceHearingId = e.getSentenceHearingId()
                        ),
                when(DefendantAllocationDecisionUpdated.class).apply(e ->
                        allocationDecisionForDefendant.add(e.getDefendantId())
                ),
                when(DefendantAllocationDecisionRemoved.class).apply(e ->
                        allocationDecisionForDefendant.remove(e.getDefendantId())
                ),
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

    public Stream<Object> noMoreInformationForDefendant(final UUID defendantId, final UUID caseId, final UUID caseProgressionId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (!offenceIdsByDefendantId.keySet().contains(defendantId)) {
            LOGGER.warn(CANNOT_ADD_ADDITIONAL_INFO + defendantId);
            return Stream.empty();
        }

        updateDefendantInfo(false, defendantId);
        // check if all defendant's reviewed
        checkAllDefendant();

        updateCaseStatus(streamBuilder);

        streamBuilder.add(new NoMoreInformationRequiredEvent(caseId, defendantId, caseProgressionId));
        return apply(streamBuilder.build());
    }

    public Stream<Object> addCaseToCrownCourt(JsonEnvelope jsonEnvelope) {
        final UUID caseId = UUID.fromString(jsonEnvelope.payloadAsJsonObject().getString("caseId"));
        final String centreId = jsonEnvelope.payloadAsJsonObject().getString("courtCentreId");
        if (centreId.equals(this.courtCentreId)) {
            LOGGER.warn("Case already exists in crown court with Id " + caseId);
            return apply(Stream.of(new CaseAlreadyExistsInCrownCourt(caseId, "Case already exists in crown court with Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(progressionEventFactory.createCaseAddedToCrownCourt(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> uploadCaseDocument(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(progressionEventFactory.newCaseDocumentReceivedEvent(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> uploadDefendantDocument(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(progressionEventFactory.newCaseDocumentReceivedEvent(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> prepareForSentenceHearing(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(progressionEventFactory.createCaseReadyForSentenceHearing(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> requestPsrForDefendant(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(progressionEventFactory.createPsrForDefendantsRequested(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> sendingHearingCommittal(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(progressionEventFactory.createSendingCommittalHearingInformationAdded(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> caseAssignedForReview(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(progressionEventFactory.createCaseAssignedForReviewUpdated(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> caseToBeAssigned(JsonEnvelope jsonEnvelope) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(progressionEventFactory.createCaseToBeAssignedUpdated(jsonEnvelope));
        return apply(streamBuilder.build());
    }


    public Stream<Object> addSentenceHearingDate(final UUID caseId, final UUID caseProgressionId, LocalDate sentenceHearingDate) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.sentenceHearingDate == null) {
            streamBuilder.add(new SentenceHearingDateAdded(caseProgressionId, sentenceHearingDate, caseId));
        } else {
            if (this.sentenceHearingId != null) {
                streamBuilder.add(new SentenceHearingDateUpdated(caseProgressionId, sentenceHearingDate, caseId));
            } else {
                LOGGER.warn("Cannot update sentence hearing date without hearing id " + caseId);
                return Stream.empty();
            }
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> addSentenceHearing(final UUID caseId, final UUID caseProgressionId, UUID sentenceHearingId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (sentenceHearingDate == null) {
            LOGGER.warn("Cannot add sentence heading id without date for " + caseId);
            return Stream.empty();
        }
        streamBuilder.add(new SentenceHearingAdded(caseProgressionId, sentenceHearingId, caseId));
        return apply(streamBuilder.build());
    }

    private void updateDefendantInfo(boolean isAdditionalInfo, UUID defendantId) {
        final Defendant def = defendants.stream().filter(d -> d.getId().equals(defendantId)).findAny().get();
        def.setSentenceHearingReviewDecision(true);
        def.setIsAdditionalInfoAvilable(isAdditionalInfo);
    }

    public Stream<Object> addAdditionalInformationForDefendant(final DefendantCommand defendant) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final UUID defendantId = defendant.getDefendantId();

        if (!offenceIdsByDefendantId.keySet().contains(defendantId)) {
            LOGGER.warn("Cannot add additional information without defendant " + defendantId);
            return Stream.empty();
        }

        updateDefendantInfo(defendant.getAdditionalInformation() != null ? true : false, defendantId);

        // check if all defendant's reviewed
        checkAllDefendant();
        updateCaseStatus(streamBuilder);

        streamBuilder.add(progressionEventFactory.addDefendantEvent(defendant));
        return apply(streamBuilder.build());
    }


    public Stream<Object> updateOffencesForDefendant(OffencesForDefendantUpdated offencesForDefendantUpdated) {
        Stream.Builder<Object> streamBuilder = Stream.builder();
        if (allocationDecisionForDefendant.contains(offencesForDefendantUpdated.getDefendantId())) {
            Long countEWAY = offencesForDefendantUpdated.getOffences().stream().filter(o -> o.getModeOfTrial() != null && "EWAY".equals(o.getModeOfTrial())).count();
            if ( 0 == countEWAY) {
                streamBuilder.add(new DefendantAllocationDecisionRemoved(offencesForDefendantUpdated.getCaseId(), offencesForDefendantUpdated.getDefendantId()));
            }
        }
        streamBuilder.add(offencesForDefendantUpdated);
        return apply(streamBuilder.build());
    }



    public Stream<Object> updateOffencesPleaForDefendant(JsonEnvelope jsonEnvelope) {
        JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        Optional<UUID> offenceId = JsonObjects.getUUID(payload, "offenceId");
        String caseId = JsonObjects.getString(payload, "caseId").orElse(null);
        String plea = JsonObjects.getString(payload, "plea").orElse(null);

        if(offenceId.isPresent() && !offenceExists(offenceId.get())){
            LOGGER.warn("Cannot update plea for offence which doesn't exist, ID:" + offenceId);
            return apply(Stream.of(new OffenceNotFound(offenceId.toString(), "Update Plea")));
        }

        return apply(Stream.of(new PleaUpdated(
                caseId,
                offenceId.get().toString(),
                plea)));


    }

    public Stream<Object> updateDefendantBailStatus(UUID defendantId, String bailStatus, Optional<BailDocument> bailDocument, Optional<LocalDate> custodyTimeLimitDate) {
        if (!this.offenceIdsByDefendantId.containsKey(defendantId)) {
            LOGGER.warn("Cannot update bail status without defendant " + defendantId);
            return apply(Stream.of(new DefendantNotFound(defendantId.toString(), "Update Defendant Bail Status")));
        }

        return apply(Stream.of(new BailStatusUpdatedForDefendant(
                caseIdForProgression,
                defendantId,
                bailStatus,
                bailDocument.orElse(null),
                custodyTimeLimitDate.orElse(null))));
    }

    public Stream<Object> updateDefenceSolicitorFirm(
            UpdateDefendantDefenceSolicitorFirm updateDefenceSolicitorFirm) {
        UUID defendantId = updateDefenceSolicitorFirm.getDefendantId();
        if (!offenceIdsByDefendantId.containsKey(defendantId)) {
            LOGGER.error("Cannot set defence solicitor firm status without defendant {}", defendantId);
            return apply(Stream.of(new DefendantNotFound(defendantId.toString(), "Update Defence Solicitor Firm")));
        }
        return apply(Stream.of(ProgressionEventFactory.asSolicitorFirmUpdatedForDefendant(updateDefenceSolicitorFirm)));
    }

    public Stream<Object> updateDefendantAllocationDecision(JsonEnvelope command) {
        JsonObject payload = command.payloadAsJsonObject();
        UUID caseId = JsonObjects.getUUID(payload, "caseId").orElse(null);
        UUID defendantId = JsonObjects.getUUID(payload, "defendantId").orElse(null);
        String allocationDecision = JsonObjects.getString(payload, "allocationDecision").orElse(null);
        if (!offenceForDefendants.containsKey(defendantId) || offenceForDefendants.get(defendantId).stream().noneMatch(o -> o.getModeOfTrial() != null && "EWAY".equals(o.getModeOfTrial()))) {
            return apply(Stream.of(new DefendantOffencesDoesNotHaveRequiredModeOfTrial(caseId, defendantId)));
        }

        return apply(Stream.of(new DefendantAllocationDecisionUpdated(caseId, defendantId, allocationDecision)));

    }


    public Stream<Object> addDefendant(AddDefendant addDefendantCommand) {
        UUID defendantId = addDefendantCommand.getDefendantId();
        UUID caseId = addDefendantCommand.getCaseId();
        String caseUrn=addDefendantCommand.getCaseUrn();
        String policeDefendantId = addDefendantCommand.getPoliceDefendantId();

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
            LOGGER.warn("Cannot set defendant interpreter without defendant " + defendantId);
            return apply(Stream.of(new DefendantNotFound(defendantId.toString(), "Update Defendant Interpreter")));
        }
        return apply(Stream.of(ProgressionEventFactory.asInterpreterUpdatedForDefendant(updateDefendantInterpreter)));
    }

    public Map<UUID, List<OffenceForDefendant>> getOffenceForDefendants() {
        return offenceForDefendants;
    }

    public Set<UUID> getAllocationDecisionForDefendant() {
        return allocationDecisionForDefendant;
    }

    public Map<UUID, Set<UUID>> getOffenceIdsByDefendantId() {
        return offenceIdsByDefendantId;
    }

    public Map<UUID, BailDocument> getDefendantsBailDocuments() {
        return defendantsBailDocuments;
    }


    private void updateCaseStatus(Stream.Builder<Object> streamBuilder) {
        if (isAllDefendantReviewed) {
            if (isAnyDefendantPending) {
                streamBuilder.add(new CasePendingForSentenceHearing(caseIdForProgression,
                        CaseStatusEnum.PENDING_FOR_SENTENCING_HEARING, ZonedDateTime.now(ZoneOffset.UTC)));
            } else {
                streamBuilder.add(new CaseReadyForSentenceHearing(caseIdForProgression,
                        CaseStatusEnum.READY_FOR_SENTENCING_HEARING, ZonedDateTime.now(ZoneOffset.UTC)));
            }
        }
    }

    private boolean offenceExists(final UUID offenceId) {
        return offenceIdsByDefendantId.values().stream()
                .anyMatch(offenceIds -> offenceIds.contains(offenceId));
    }


}
