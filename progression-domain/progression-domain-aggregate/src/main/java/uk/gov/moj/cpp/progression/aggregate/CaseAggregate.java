package uk.gov.moj.cpp.progression.aggregate;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createCaseAddedToCrownCourt;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createPsrForDefendantsRequested;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createSendingCommittalHearingInformationAdded;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationRejected;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.DefendantsNotAddedToCourtProceedings;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.NotificationRequestAccepted;
import uk.gov.moj.cpp.progression.domain.NotificationRequestFailed;
import uk.gov.moj.cpp.progression.domain.NotificationRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper;
import uk.gov.moj.cpp.progression.domain.event.CaseAlreadyExistsInCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateRemoved;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.DefendantBailDocumentCreated;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetInvalidated;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetPreviouslyCompleted;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdateFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

@SuppressWarnings({"squid:S3776", "squid:MethodCyclomaticComplexity", "squid:S1948", "squid:S3457", "squid:S1192", "squid:CallToDeprecatedMethod"})
public class CaseAggregate implements Aggregate {

    private static final long serialVersionUID = 7L;
    private static final String HEARING_PAYLOAD_PROPERTY = "hearing";
    private static final String CROWN_COURT_HEARING_PROPERTY = "crownCourtHearing";
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAggregate.class);
    private static final String CASE_ID = "caseId";
    private static final String SENDING_SHEET_ALREADY_COMPLETED_MSG = "Sending sheet already completed, not allowed to perform add sentence hearing date  for case Id %s ";
    private final Set<UUID> caseIdsWithCompletedSendingSheet = new HashSet<>();

    private String courtCentreId;
    private String reference;
    private int arnCount = 0;
    private final Set<String> policeDefendantIds = new HashSet<>();
    private final Set<Defendant> defendants = new HashSet<>();
    private final Map<UUID, List<OffenceForDefendant>> offenceForDefendants = new HashMap<>();
    private final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
    private final Map<UUID, BailDocument> defendantsBailDocuments = new HashMap<>();
    private final Map<UUID, Person> personOnDefendantAdded = new HashMap<>();
    private final Map<UUID, Person> personForDefendant = new HashMap<>();
    private final Map<UUID, Interpreter> interpreterForDefendant = new HashMap<>();
    private final Map<UUID, String> bailStatusForDefendant = new HashMap<>();
    private final Map<UUID, String> solicitorFirmForDefendant = new HashMap<>();
    private final Map<UUID, LocalDate> custodyTimeLimitForDefendant = new HashMap<>();
    private final Map<UUID, List<uk.gov.justice.core.courts.Offence>> defendantCaseOffences = new HashMap<>();


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
                when(uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(DefendantBailDocumentCreated.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(SendingSheetPreviouslyCompleted.class)
                        .apply(e -> {
                            // Do Nothng
                        }), when(DefendantUpdated.class).apply(e ->
                        this.defendantsBailDocuments.put(e.getDefendantId(),
                                e.getBailDocument())
                ), when(SendingSheetCompleted.class).apply(e ->
                        caseIdsWithCompletedSendingSheet.add(e.getHearing().getCaseId())
                ),
                when(ProsecutionCaseCreated.class)
                        .apply(e -> {
                            final ProsecutionCaseIdentifier prosecutionCaseIdentifier = e.getProsecutionCase().getProsecutionCaseIdentifier();
                            if (Objects.nonNull(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
                                reference = e.getProsecutionCase().getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
                            }
                            if (Objects.nonNull(prosecutionCaseIdentifier.getCaseURN())) {
                                reference = e.getProsecutionCase().getProsecutionCaseIdentifier().getCaseURN();
                            }
                            if (Objects.nonNull(e.getProsecutionCase()) && !e.getProsecutionCase().getDefendants().isEmpty()) {
                                e.getProsecutionCase().getDefendants().forEach(d ->
                                        this.defendantCaseOffences.put(d.getId(), d.getOffences())
                                );
                            }
                        }
                ),
                when(ProsecutionCaseDefendantUpdated.class).apply(e -> {
                            //do nothing
                        }
                ),
                when(ProsecutionCaseOffencesUpdated.class).apply(e -> {
                            if (e.getDefendantCaseOffences().getOffences() != null && !e.getDefendantCaseOffences().getOffences().isEmpty()) {
                                this.defendantCaseOffences.put(e.getDefendantCaseOffences().getDefendantId(), e.getDefendantCaseOffences().getOffences());
                            }
                        }
                ),
                when(CourtApplicationCreated.class).apply(
                        e -> arnCount = e.getCount()
                ),
                when(DefendantsAddedToCourtProceedings.class).apply(
                        e ->
                            e.getDefendants().forEach(
                                    ed -> this.defendantCaseOffences.put( ed.getId(), ed.getOffences()) )
                ),
                otherwiseDoNothing());

    }

    public Stream<Object> addCaseToCrownCourt(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = UUID.fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
        final String centreId = jsonEnvelope.payloadAsJsonObject().getString("courtCentreId");
        if (centreId.equals(this.courtCentreId)) {
            LOGGER.info("Case already exists in crown court with Id %s", caseId);
            return apply(Stream.of(new CaseAlreadyExistsInCrownCourt(caseId, "Case already exists in crown court with Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createCaseAddedToCrownCourt(jsonEnvelope));
        return apply(streamBuilder.build());
    }


    public Stream<Object> requestPsrForDefendant(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = UUID.fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform requestPsrForDefendant  for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform requestPsrForDefendant after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createPsrForDefendantsRequested(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> sendingHearingCommittal(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = UUID.fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform sending hearing committal  for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform sending hearing committal after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createSendingCommittalHearingInformationAdded(jsonEnvelope));
        return apply(streamBuilder.build());
    }


    public Stream<Object> addSentenceHearingDate(final UUID caseId, final LocalDate sentenceHearingDate) {
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info(SENDING_SHEET_ALREADY_COMPLETED_MSG, caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform add sentence hearing date after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(new SentenceHearingDateAdded(sentenceHearingDate, caseId));
        return apply(streamBuilder.build());
    }

    public Stream<Object> addConvictionDateToOffence(final UUID caseId,
                                                     final UUID offenceId, final LocalDate convictionDate) {
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info(SENDING_SHEET_ALREADY_COMPLETED_MSG, caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform add sentence hearing date after sending sheet completed with case Id " + caseId)));
        }
        return apply(Stream.of(ConvictionDateAdded.builder()
                .withCaseId(caseId)
                .withOffenceId(offenceId)
                .withConvictionDate(convictionDate)
                .build()));
    }

    public Stream<Object> removeConvictionDateFromOffence(final UUID caseId, final UUID offenceId) {
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info(SENDING_SHEET_ALREADY_COMPLETED_MSG, caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform add sentence hearing date after sending sheet completed with case Id " + caseId)));
        }
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

            if (defendantsBailDocuments.get(defendantUUID) != null) {
                streamBuilder.add(new DefendantBailDocumentCreated(caseId,
                        defendantUUID,
                        defendantsBailDocuments.get(defendantUUID).getMaterialId(),
                        defendantsBailDocuments.get(defendantUUID).getId()));
            }

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


    public Stream<Object> updateOffencesForDefendant(final OffencesForDefendantUpdated offencesForDefendantUpdated) {
        final UUID caseId = offencesForDefendantUpdated.getCaseId();
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform update offences for defendant for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform update offences for defendant after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(offencesForDefendantUpdated);
        return apply(streamBuilder.build());
    }

    public Stream<Object> addDefendant(final AddDefendant addDefendantCommand) {
        final UUID defendantId = addDefendantCommand.getDefendantId();
        final UUID caseId = addDefendantCommand.getCaseId();
        final String caseUrn = addDefendantCommand.getCaseUrn();
        final String policeDefendantId = addDefendantCommand.getPoliceDefendantId();
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform add defendant for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform add defendant after sending sheet completed with case Id " + caseId)));
        }
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


    public Stream<Object> updateDefendant(
            final UpdateDefendantCommand updateDefendantCommandCommand) {
        final UUID caseId = updateDefendantCommandCommand.getCaseId();
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform update defendant for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform update defendant after sending sheet completed with case Id " + caseId)));
        }
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

    /*
      Edit case releated events (prosecution case)
     */

    public Stream<Object> createProsecutionCase(final ProsecutionCase prosecutionCase) {
        LOGGER.debug("Prosecution case is being refered To Court .");
        return apply(Stream.of(ProsecutionCaseCreated.prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build()));
    }

    public Stream<Object> defendantsAddedToCourtProcessdings(final List<uk.gov.justice.core.courts.Defendant> addedDefendants,
                                                             final List<ListHearingRequest> listHearingRequests) {
        LOGGER.debug("Defendants Added To CourtProcessdings");

        final List<uk.gov.justice.core.courts.Defendant> newDefendantsList =
                addedDefendants.stream().filter(d -> !this.defendantCaseOffences.containsKey(d.getId())).collect(toMap(d -> d.getId(), d->d, (i, d) -> d)).values().stream().collect(toList());

        if(newDefendantsList.isEmpty()) {
            return apply( Stream.of(DefendantsNotAddedToCourtProceedings.defendantsNotAddedToCourtProceedings()
                    .withDefendants(addedDefendants)
                    .withListHearingRequests(listHearingRequests)
                    .build())
            );
        }

        return apply( Stream.of(DefendantsAddedToCourtProceedings.defendantsAddedToCourtProceedings()
                .withDefendants(newDefendantsList)
                .withListHearingRequests(listHearingRequests)
                .build())
        );
    }

    public Stream<Object> updateDefendantDetails(final DefendantUpdate updatedDefendant) {
        LOGGER.debug("Defendant information is being updated.");
        return apply(Stream.of(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                .withDefendant(updatedDefendant).build()));
    }

    public Stream<Object> updateOffences(final List<uk.gov.justice.core.courts.Offence> offences, final UUID prosecutionCaseId, final UUID defendantId) {
        LOGGER.debug("Offences information is being updated.");
        final DefendantCaseOffences newDefendantCaseOffences = DefendantCaseOffences.defendantCaseOffences()
                .withOffences(offences)
                .withDefendantId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId
                ).build();
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                .withDefendantCaseOffences(newDefendantCaseOffences).build());

        if (this.defendantCaseOffences.containsKey(defendantId)) {
            final Optional<OffencesForDefendantChanged> offencesForDefendantChanged = DefendantHelper.getOffencesForDefendantChanged(offences, this.defendantCaseOffences.get(defendantId), prosecutionCaseId, defendantId);
            if (offencesForDefendantChanged.isPresent()) {
                streamBuilder.add(offencesForDefendantChanged.get());
            }
        }
        return apply(streamBuilder.build());

    }


    public Stream<Object> recordPrintRequest(final UUID caseId,
                                             final UUID notificationId,
                                             final UUID materialId) {
        return apply(Stream.of(new PrintRequested(notificationId, null, caseId, materialId)));
    }

    public Stream<Object> recordEmailRequest(final UUID caseId,
                                             final UUID materialId,
                                             final List<Notification> notifications) {
        return apply(Stream.of(new EmailRequested(caseId, materialId, null, notifications)));
    }

    public Stream<Object> recordNotificationRequestFailure(final UUID caseId,
                                                    final UUID notificationId,
                                                    final ZonedDateTime failedTime,
                                                    final String errorMessage,
                                                    final Optional<Integer> statusCode) {
        return apply(Stream.of(new NotificationRequestFailed(caseId, null, notificationId, failedTime, errorMessage, statusCode)));
    }

    public Stream<Object> recordNotificationRequestSuccess(final UUID caseId,
                                                    final UUID notificationId,
                                                    final ZonedDateTime sentTime) {
        return apply(Stream.of(new NotificationRequestSucceeded(caseId, null, notificationId, sentTime)));
    }

    public Stream<Object> recordNotificationRequestAccepted(final UUID caseId,
                                                     final UUID notificationId,
                                                     final ZonedDateTime acceptedTime) {
        return apply(Stream.of(new NotificationRequestAccepted(caseId, null, notificationId, acceptedTime)));
    }

    public Stream<Object> addConvictionDate(final UUID prosecutionCaseId, final UUID offenceId, final LocalDate convictionDate) {
        return apply(Stream.of(new uk.gov.justice.core.courts.ConvictionDateAdded(prosecutionCaseId, convictionDate, offenceId)));
    }

    public Stream<Object> removeConvictionDate(final UUID prosecutionCaseId, final UUID offenceId) {
        return apply(Stream.of(new uk.gov.justice.core.courts.ConvictionDateRemoved(prosecutionCaseId, offenceId)));
    }

    public Stream<Object> createCourtApplication(final CourtApplication courtApplication) {
        if (courtApplication.getRespondents() != null && !courtApplication.getRespondents().isEmpty()) {
            Optional<CourtApplicationRespondent> respondent = courtApplication.getRespondents().stream()
                    .filter(o -> o.getPartyDetails().getId().equals(courtApplication.getApplicant().getId()))
                    .findFirst();
            if (respondent.isPresent()) {
                return apply(Stream.of(CourtApplicationRejected.courtApplicationRejected()
                        .withApplicationId(courtApplication.getId().toString())
                        .withCaseId(courtApplication.getLinkedCaseId().toString())
                        .withDescription("Applicant cannot be respondent")
                        .build()));
            }
        }
        return apply(
                Stream.of(
                        CourtApplicationCreated.courtApplicationCreated()
                                .withCourtApplication(courtApplication)
                                .withCount(++arnCount)
                                .withArn(reference + "-" + arnCount)
                                .build()));
    }
}
