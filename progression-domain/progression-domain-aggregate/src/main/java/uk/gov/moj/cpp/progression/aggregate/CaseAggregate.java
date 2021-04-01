package uk.gov.moj.cpp.progression.aggregate;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.empty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createCaseAddedToCrownCourt;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createPsrForDefendantsRequested;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createSendingCommittalHearingInformationAdded;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.isAllDefendantProceedingConcluded;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.offenceWithSexualOffenceReportingRestriction;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.updateOrderIndex;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.GRANTED;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.NO_VALUE;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.PENDING;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.REFUSED;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.WITHDRAWN;
import static uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationDisassociated.defendantDefenceOrganisationDisassociated;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.CaseCpsProsecutorUpdated;
import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.CaseMarkersUpdated;
import uk.gov.justice.core.courts.CaseNoteAdded;
import uk.gov.justice.core.courts.CaseNoteEdited;
import uk.gov.justice.core.courts.Cases;
import uk.gov.justice.core.courts.CpsProsecutorUpdated;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.DefendantPartialMatchCreated;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Defendants;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.DefendantsNotAddedToCourtProceedings;
import uk.gov.justice.core.courts.ExactMatchedDefendantSearchResultStored;
import uk.gov.justice.core.courts.FinancialDataAdded;
import uk.gov.justice.core.courts.FinancialMeansDeleted;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.HearingConfirmedCaseStatusUpdated;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.HearingUpdatedForPartialAllocation;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.PartialMatchedDefendantSearchResultStored;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseUpdateDefendantsWithMatchedRequested;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.ReceiveRepresentationOrderForDefendant;
import uk.gov.justice.cpp.progression.events.DefendantDefenceAssociationLocked;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.progression.courts.DefendantLegalaidStatusUpdated;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicateForCase;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;
import uk.gov.moj.cpp.progression.domain.CaseToUnlink;
import uk.gov.moj.cpp.progression.domain.CasesToLink;
import uk.gov.moj.cpp.progression.domain.MatchDefendant;
import uk.gov.moj.cpp.progression.domain.MatchedDefendant;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.NotificationRequestAccepted;
import uk.gov.moj.cpp.progression.domain.NotificationRequestFailed;
import uk.gov.moj.cpp.progression.domain.NotificationRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.UnmatchDefendant;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
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
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;
import uk.gov.moj.cpp.progression.events.CasesUnlinked;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociatedByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationDissociatedByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationDisassociated;
import uk.gov.moj.cpp.progression.events.DefendantLaaAssociated;
import uk.gov.moj.cpp.progression.events.DefendantMatched;
import uk.gov.moj.cpp.progression.events.DefendantUnmatched;
import uk.gov.moj.cpp.progression.events.LinkCases;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MatchedDefendants;
import uk.gov.moj.cpp.progression.events.MergeCases;
import uk.gov.moj.cpp.progression.events.RepresentationType;
import uk.gov.moj.cpp.progression.events.SplitCases;
import uk.gov.moj.cpp.progression.events.UnlinkedCases;
import uk.gov.moj.cpp.progression.events.ValidateLinkCases;
import uk.gov.moj.cpp.progression.events.ValidateMergeCases;
import uk.gov.moj.cpp.progression.events.ValidateSplitCases;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3776", "squid:MethodCyclomaticComplexity", "squid:S1948", "squid:S3457", "squid:S1192", "squid:CallToDeprecatedMethod"})
public class CaseAggregate implements Aggregate {

    private static final long serialVersionUID = 6644269354919757994L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String HEARING_PAYLOAD_PROPERTY = "hearing";
    private static final String CROWN_COURT_HEARING_PROPERTY = "crownCourtHearing";
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAggregate.class);
    private static final String CASE_ID = "caseId";
    private static final String SENDING_SHEET_ALREADY_COMPLETED_MSG = "Sending sheet already completed, not allowed to perform add sentence hearing date  for case Id %s ";
    private static final String CASE_STATUS_EJECTED = "EJECTED";
    private static final String LAA_WITHDRAW_STATUS_CODE = "WD";
    private final Set<UUID> caseIdsWithCompletedSendingSheet = new HashSet<>();
    private final Set<String> policeDefendantIds = new HashSet<>();
    private final Set<UUID> hearingIds = new HashSet<>();
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
    private final Map<UUID, String> defendantLegalAidStatus = new HashMap<>();
    private final Map<UUID, List<UUID>> applicationFinancialDocs = new HashMap<>();
    private final Map<UUID, List<UUID>> defendantFinancialDocs = new HashMap<>();
    private final Map<UUID, List<Cases>> partialMatchedDefendants = new HashMap<>();
    private final Map<UUID, List<Cases>> exactMatchedDefendants = new HashMap<>();
    private final Map<UUID, Boolean> defendantProceedingConcluded = new HashMap<>();
    private final Set<UUID> matchedDefendantIds = new HashSet<>();
    private final Map<UUID, UUID> defendantAssociatedDefenceOrganisation = new HashMap<>();
    private String caseStatus;
    private String courtCentreId;
    private String reference;
    private UUID latestHearingId;
    private ProsecutionCase prosecutionCase;

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
                                    .collect(toList()));
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
                when(CaseEjected.class)
                        .apply(e ->
                                this.caseStatus = CASE_STATUS_EJECTED
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
                                    this.prosecutionCase = e.getProsecutionCase();
                                    this.caseStatus = e.getProsecutionCase().getCaseStatus();
                                    final ProsecutionCaseIdentifier prosecutionCaseIdentifier = e.getProsecutionCase().getProsecutionCaseIdentifier();
                                    if (nonNull(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
                                        reference = e.getProsecutionCase().getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
                                    }
                                    if (nonNull(prosecutionCaseIdentifier.getCaseURN())) {
                                        reference = e.getProsecutionCase().getProsecutionCaseIdentifier().getCaseURN();
                                    }
                                    if (nonNull(e.getProsecutionCase()) && !e.getProsecutionCase().getDefendants().isEmpty()) {
                                        e.getProsecutionCase().getDefendants().forEach(d -> {
                                            this.defendantCaseOffences.put(d.getId(), d.getOffences());
                                            this.defendantLegalAidStatus.put(d.getId(), NO_VALUE.getDescription());
                                        });
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
                                this.defendantLegalAidStatus.put(e.getDefendantCaseOffences().getDefendantId(), e.getDefendantCaseOffences().getLegalAidStatus());
                            }
                        }
                ),
                /*when(CourtApplicationCreated.class).apply(
                        e -> arnCount = e.getCount()
                ),*/
                when(DefendantsAddedToCourtProceedings.class).apply(
                        e ->
                        {
                            if (!e.getDefendants().isEmpty()) {
                                e.getDefendants().forEach(
                                        ed -> this.defendantCaseOffences.put(ed.getId(), ed.getOffences()));
                            }
                            this.prosecutionCase.getDefendants().add(e.getDefendants().get(0));
                        }
                ),
                when(CaseLinkedToHearing.class).apply(
                        e -> {
                            this.hearingIds.add(e.getHearingId());
                            latestHearingId = e.getHearingId();
                        }
                ),
                when(FinancialDataAdded.class).apply(this::populateFinancialData),
                when(FinancialMeansDeleted.class).apply(this::deleteFinancialData),
                when(CaseMarkersUpdated.class).apply(e -> {
                            //do nothing
                        }
                ),
                when(HearingResultedCaseUpdated.class).apply(this::updateDefendantProceedingConcludedAndCaseStatus),
                when(HearingConfirmedCaseStatusUpdated.class).apply(this::hearingConfirmedCaseStatusUpdated),
                when(DefendantDefenceOrganisationAssociated.class).apply(this::updateDefendantAssociatedDefenceOrganisation),
                when(DefendantDefenceOrganisationDisassociated.class).apply(this::removeDefendantAssociatedDefenceOrganisation),
                when(DefenceOrganisationDissociatedByDefenceContext.class).apply(this::removeDefendantAssociatedDefenceOrganisation),
                when(DefenceOrganisationAssociatedByDefenceContext.class).apply(this::updateDefendantAssociatedDefenceOrganisation),
                when(HearingMarkedAsDuplicateForCase.class).apply(this::onHearingMarkedAsDuplicateForCase),
                when(DefendantMatched.class).apply(
                        e -> this.matchedDefendantIds.add(e.getDefendantId())
                ),
                when(ExactMatchedDefendantSearchResultStored.class).apply(
                        e -> this.exactMatchedDefendants.put(e.getDefendantId(), e.getCases())
                ),
                when(PartialMatchedDefendantSearchResultStored.class).apply(
                        e -> this.partialMatchedDefendants.put(e.getDefendantId(), e.getCases())
                ),
                when(MasterDefendantIdUpdated.class).apply(
                        e -> this.exactMatchedDefendants.remove(e.getDefendantId())
                ),
                when(DefendantPartialMatchCreated.class).apply(
                        e -> this.partialMatchedDefendants.remove(e.getDefendantId())
                ),
                when(CaseCpsProsecutorUpdated.class).apply(this::updateProsecutionCaseIdentifier),
                otherwiseDoNothing());

    }

    public Map<UUID, List<UUID>> getApplicationFinancialDocs() {
        return applicationFinancialDocs;
    }

    public Map<UUID, List<UUID>> getDefendantFinancialDocs() {
        return defendantFinancialDocs;
    }

    private void deleteFinancialData(final FinancialMeansDeleted financialMeansDeleted) {
        this.defendantFinancialDocs.remove(financialMeansDeleted.getDefendantId());
    }

    private void updateDefendantProceedingConcludedAndCaseStatus(final HearingResultedCaseUpdated hearingResultedCaseUpdated) {
        hearingResultedCaseUpdated.getProsecutionCase().getDefendants().stream().forEach(defendant ->
                defendantProceedingConcluded.put(defendant.getId(), defendant.getProceedingsConcluded())
        );

    }

    private void populateFinancialData(final FinancialDataAdded financialDataAdded) {

        if (financialDataAdded.getApplicationId() != null) {
            this.applicationFinancialDocs.put(financialDataAdded.getApplicationId(), financialDataAdded.getMaterialIds());
        } else if (financialDataAdded.getDefendantId() != null) {
            this.defendantFinancialDocs.put(financialDataAdded.getDefendantId(), financialDataAdded.getMaterialIds());
        }
    }

    public Stream<Object> addCaseToCrownCourt(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
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
        final UUID caseId = fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform requestPsrForDefendant  for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform requestPsrForDefendant after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createPsrForDefendantsRequested(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> sendingHearingCommittal(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
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
                        final UUID defendantId = fromString(jdo.getString("id"));
                        final JsonArray jsonOffences = ((JsonObject) jd).getJsonArray("offences");
                        final Set<UUID> offenceIds = jsonOffences.stream().map(jo -> (fromString(((JsonObject) jo).getString("id")))).collect(Collectors.toSet());
                        incomingDefendantId2OffenceIds.put(defendantId, offenceIds);
                    }
            );
        }
        return incomingDefendantId2OffenceIds;
    }

    public Stream<Object> completeSendingSheet(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final UUID caseId =
                fromString(payload
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

    public Stream<Object> ejectCase(final UUID prosecutionCaseId, final String removalReason) {
        if (CASE_STATUS_EJECTED.equals(caseStatus)) {
            LOGGER.info("Case with id {} already ejected", prosecutionCaseId);
            return empty();
        }
        return apply(Stream.of(CaseEjected.caseEjected()
                .withProsecutionCaseId(prosecutionCaseId).withRemovalReason(removalReason).build()));
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

    public Stream<Object> createProsecutionCase(final ProsecutionCase prosecutionCase) {
        LOGGER.debug("Prosecution case is being referred To Court .");
        return apply(Stream.of(ProsecutionCaseCreated.prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build()));
    }

    public Stream<Object> defendantsAddedToCourtProceedings(final List<uk.gov.justice.core.courts.Defendant> addedDefendants,
                                                            final List<ListHearingRequest> listHearingRequests) {
        LOGGER.debug("Defendants Added To CourtProceedings");

        final List<uk.gov.justice.core.courts.Defendant> newDefendantsList =
                addedDefendants.stream().filter(d -> !this.defendantCaseOffences.containsKey(d.getId())).collect(toMap(uk.gov.justice.core.courts.Defendant::getId, d -> d, (i, d) -> d)).values().stream().collect(toList());

        if (newDefendantsList.isEmpty()) {
            return apply(Stream.of(DefendantsNotAddedToCourtProceedings.defendantsNotAddedToCourtProceedings()
                    .withDefendants(addedDefendants)
                    .withListHearingRequests(listHearingRequests)
                    .build())
            );
        }

        return apply(Stream.of(DefendantsAddedToCourtProceedings.defendantsAddedToCourtProceedings()
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


    public Stream<Object> updateCase(final ProsecutionCase prosecutionCase) {
        LOGGER.debug(" ProsecutionCase is being updated ");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final List<uk.gov.justice.core.courts.Defendant> updatedDefendants = new ArrayList<>();
        final boolean allDefendantProceedingConcluded = isAllDefendantProceedingConcluded(prosecutionCase, updatedDefendants);

        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase()
                .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withId(prosecutionCase.getId())
                .withDefendants(updatedDefendants)
                .withInitiationCode(prosecutionCase.getInitiationCode())
                .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                .withCpsOrganisation(prosecutionCase.getCpsOrganisation())
                .withIsCpsOrgVerifyError(prosecutionCase.getIsCpsOrgVerifyError())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .withCaseMarkers(prosecutionCase.getCaseMarkers())
                .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                .withRemovalReason(prosecutionCase.getRemovalReason())
                .withCaseStatus(allDefendantProceedingConcluded ? CaseStatusEnum.INACTIVE.getDescription() : prosecutionCase.getCaseStatus())
                .build();

        streamBuilder.add(HearingResultedCaseUpdated.hearingResultedCaseUpdated().withProsecutionCase(updatedProsecutionCase).build());

        return apply(streamBuilder.build());
    }

    public Stream<Object> recordUpdateDefendantDetailsWithMatched(final DefendantUpdate updatedDefendant, final UUID matchedDefendantHearingId) {
        LOGGER.debug("Update Defendant information with Matched Defendants are recorded");
        return apply(Stream.of(ProsecutionCaseUpdateDefendantsWithMatchedRequested.prosecutionCaseUpdateDefendantsWithMatchedRequested()
                .withDefendant(updatedDefendant)
                .withMatchedDefendantHearingId(matchedDefendantHearingId).build()));
    }

    public Stream<Object> updateOffences(final List<uk.gov.justice.core.courts.Offence> offences, final UUID prosecutionCaseId, final UUID defendantId, final Optional<List<JsonObject>> referenceDataOffences) {
        LOGGER.debug("Offences information is being updated.");
        final AtomicInteger maxOrderIndex = new AtomicInteger(Collections.max(this.defendantCaseOffences.get(defendantId), Comparator.comparing(uk.gov.justice.core.courts.Offence::getOrderIndex)).getOrderIndex());
        final List<uk.gov.justice.core.courts.Offence> updatedOffences = offences.stream().map(commandOffence -> {
            uk.gov.justice.core.courts.Offence offence;
            Optional<uk.gov.justice.core.courts.Offence> existingOffence = this.defendantCaseOffences.get(defendantId).stream().filter(o -> o.getId().equals(commandOffence.getId())).findFirst();
            if (existingOffence.isPresent()) {
                offence = updateOrderIndex(commandOffence, existingOffence.get().getOrderIndex());
            } else {
                offence = offenceWithSexualOffenceReportingRestriction(updateOrderIndex(commandOffence, maxOrderIndex.addAndGet(1)), referenceDataOffences);
            }
            return offence;
        }).collect(Collectors.toList());

        final DefendantCaseOffences newDefendantCaseOffences = DefendantCaseOffences.defendantCaseOffences()
                .withOffences(updatedOffences)
                .withDefendantId(defendantId)
                .withLegalAidStatus(defendantLegalAidStatus.get(defendantId))
                .withProsecutionCaseId(prosecutionCaseId)
                .build();
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                .withDefendantCaseOffences(newDefendantCaseOffences).build());

        if (this.defendantCaseOffences.containsKey(defendantId)) {
            final Optional<OffencesForDefendantChanged> offencesForDefendantChanged = DefendantHelper.getOffencesForDefendantChanged(updatedOffences, this.defendantCaseOffences.get(defendantId), prosecutionCaseId, defendantId, referenceDataOffences);
            offencesForDefendantChanged.ifPresent(streamBuilder::add);
        }
        return apply(streamBuilder.build());

    }

    public Stream<Object> updateCaseMarkers(final List<Marker> caseMarkers, final UUID prosecutionCaseId, final UUID hearingId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(CaseMarkersUpdated.caseMarkersUpdated()
                .withCaseMarkers(caseMarkers)
                .withProsecutionCaseId(prosecutionCaseId)
                .withHearingId(hearingId)
                .build()
        );

        return apply(streamBuilder.build());
    }

    public Stream<Object> updateCaseStatus(final ProsecutionCase prosecutionCase, final String caseStatus) {
        LOGGER.debug(" ProsecutionCase  updateCaseStatus is being updated ");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(HearingConfirmedCaseStatusUpdated.hearingConfirmedCaseStatusUpdated()
                .withProsecutionCase(prosecutionCase)
                .withCaseStatus(caseStatus)
                .build());
        return apply(streamBuilder.build());

    }

    public Stream<Object> recordPrintRequest(final UUID caseId,
                                             final UUID notificationId,
                                             final UUID materialId,
                                             boolean postage) {
        return apply(Stream.of(new PrintRequested(notificationId, null, caseId, materialId, postage)));
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
        return apply(Stream.of(new NotificationRequestFailed(caseId, null, null, notificationId, failedTime, errorMessage, statusCode)));
    }

    public Stream<Object> recordNotificationRequestSuccess(final UUID caseId,
                                                           final UUID notificationId,
                                                           final ZonedDateTime sentTime,
                                                           final ZonedDateTime completedAt) {
        return apply(Stream.of(new NotificationRequestSucceeded(caseId, null, null, notificationId, sentTime, completedAt)));
    }

    public Stream<Object> recordNotificationRequestAccepted(final UUID caseId,
                                                            final UUID materialId,
                                                            final UUID notificationId,
                                                            final ZonedDateTime acceptedTime) {
        return apply(Stream.of(new NotificationRequestAccepted(caseId, null, materialId, notificationId, acceptedTime)));
    }

    public Stream<Object> addConvictionDate(final UUID prosecutionCaseId, final UUID offenceId, final LocalDate convictionDate) {
        return apply(Stream.of(uk.gov.justice.core.courts.ConvictionDateAdded.convictionDateAdded()
                .withCaseId(prosecutionCaseId)
                .withConvictionDate(convictionDate)
                .withOffenceId(offenceId)
                .build()));
    }

    public Stream<Object> removeConvictionDate(final UUID prosecutionCaseId, final UUID offenceId) {
        return apply(Stream.of(uk.gov.justice.core.courts.ConvictionDateRemoved.convictionDateRemoved()
                .withCaseId(prosecutionCaseId)
                .withOffenceId(offenceId)
                .build()));
    }

    public Stream<Object> linkProsecutionCaseToHearing(final UUID hearingId, final UUID caseId) {
        return apply(Stream.of(CaseLinkedToHearing.caseLinkedToHearing()
                .withHearingId(hearingId).withCaseId(caseId).build()));
    }

    public Stream<Object> addFinancialMeansData(final UUID prosecutionCaseId, final UUID defendantId, final UUID applicationId, final Material material) {

        final List<UUID> materialIds = new ArrayList<>();
        materialIds.add(material.getId());
        return apply(Stream.of(FinancialDataAdded.financialDataAdded()
                .withCaseId(prosecutionCaseId)
                .withApplicationId(applicationId)
                .withDefendantId(defendantId)
                .withMaterialIds(materialIds)
                .build())
        );
    }

    public Stream<Object> deleteFinancialMeansData(final UUID defendantId, final UUID caseId) {
        final List<UUID> materialIds = this.defendantFinancialDocs.get(defendantId);
        return apply(Stream.of(FinancialMeansDeleted.financialMeansDeleted()
                .withDefendantId(defendantId)
                .withMaterialIds(materialIds)
                .withCaseId(caseId)
                .build())
        );

    }

    public Stream<Object> recordLAAReferenceForOffence(final UUID prosecutionCaseId, final UUID defendantId, final UUID offenceId, final LaaReference laaReference) {
        LOGGER.debug("LAA reference is recorded for Offence.");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.defendantCaseOffences.containsKey(defendantId)) {
            final List<uk.gov.justice.core.courts.Offence> offencesList = defendantCaseOffences.get(defendantId);
            final List<uk.gov.justice.core.courts.Offence> updatedOffenceList = new ArrayList<>(offencesList);
            final Optional<uk.gov.justice.core.courts.Offence> optionalOffence = offencesList.stream().filter(offence -> offence.getId().equals(offenceId)).findAny();
            if (optionalOffence.isPresent()) {
                final uk.gov.justice.core.courts.Offence matchingOffence = optionalOffence.get();
                updatedOffenceList.remove(matchingOffence);
                final uk.gov.justice.core.courts.Offence updatedOffence = uk.gov.justice.core.courts.Offence.offence()
                        .withNotifiedPlea(matchingOffence.getNotifiedPlea())
                        .withId(matchingOffence.getId())
                        .withArrestDate(matchingOffence.getArrestDate())
                        .withChargeDate(matchingOffence.getChargeDate())
                        .withConvictionDate(matchingOffence.getConvictionDate())
                        .withCount(nonNull(matchingOffence.getCount()) ? matchingOffence.getCount() : 0)
                        .withEndDate(matchingOffence.getEndDate())
                        .withOffenceDefinitionId(matchingOffence.getOffenceDefinitionId())
                        .withOffenceFacts(matchingOffence.getOffenceFacts())
                        .withOffenceLegislation(matchingOffence.getOffenceLegislation())
                        .withOffenceLegislationWelsh(matchingOffence.getOffenceLegislationWelsh())
                        .withOffenceTitle(matchingOffence.getOffenceTitle())
                        .withOffenceTitleWelsh(matchingOffence.getOffenceTitleWelsh())
                        .withModeOfTrial(matchingOffence.getModeOfTrial())
                        .withOffenceCode(matchingOffence.getOffenceCode())
                        .withOrderIndex(matchingOffence.getOrderIndex())
                        .withStartDate(matchingOffence.getStartDate())
                        .withWording(matchingOffence.getWording())
                        .withWordingWelsh(matchingOffence.getWordingWelsh())
                        .withId(matchingOffence.getId())
                        .withOffenceCode(matchingOffence.getOffenceCode())
                        .withOffenceDefinitionId(matchingOffence.getOffenceDefinitionId())
                        .withOffenceFacts(matchingOffence.getOffenceFacts())
                        .withOffenceLegislation(matchingOffence.getOffenceLegislation())
                        .withOffenceLegislationWelsh(matchingOffence.getOffenceLegislationWelsh())
                        .withWording(matchingOffence.getWording())
                        .withCount(matchingOffence.getCount())
                        .withStartDate(matchingOffence.getStartDate())
                        .withLaaApplnReference(laaReference)
                        .withOffenceDateCode(matchingOffence.getOffenceDateCode())
                        .withCommittingCourt(matchingOffence.getCommittingCourt())
                        .build();
                updatedOffenceList.add(updatedOffence);
                final String legalAidStatus = getDefendantLevelLegalStatus(updatedOffenceList);
                final DefendantCaseOffences newDefendantCaseOffences = DefendantCaseOffences.defendantCaseOffences()
                        .withOffences(updatedOffenceList)
                        .withDefendantId(defendantId)
                        .withLegalAidStatus(legalAidStatus)
                        .withProsecutionCaseId(prosecutionCaseId)
                        .build();
                final DefendantLegalaidStatusUpdated defendantLegalaidStatusUpdated = DefendantLegalaidStatusUpdated.defendantLegalaidStatusUpdated()
                        .withCaseId(prosecutionCaseId)
                        .withDefendantId(defendantId)
                        .withLegalAidStatus(legalAidStatus)
                        .build();

                final Optional<OffencesForDefendantChanged> offencesForDefendantChanged = DefendantHelper.getOffencesForDefendantUpdated(Arrays.asList(updatedOffence), offencesList, prosecutionCaseId, defendantId);
                if (offencesForDefendantChanged.isPresent()) {
                    streamBuilder.add(ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                            .withDefendantCaseOffences(newDefendantCaseOffences).build());
                    streamBuilder.add(offencesForDefendantChanged.get());
                    streamBuilder.add(defendantLegalaidStatusUpdated);
                }

                if (LAA_WITHDRAW_STATUS_CODE.equalsIgnoreCase(laaReference.getStatusCode()) && !GRANTED.getDescription().equals(legalAidStatus)
                        && this.defendantAssociatedDefenceOrganisation.containsKey(defendantId)) {
                    final DefendantDefenceOrganisationDisassociated defendantDefenceOrganisationDisassociated = defendantDefenceOrganisationDisassociated()
                            .withDefendantId(defendantId)
                            .withOrganisationId(defendantAssociatedDefenceOrganisation.get(defendantId))
                            .withCaseId(prosecutionCaseId)
                            .build();
                    streamBuilder.add(DefendantDefenceOrganisationChanged.defendantDefenceOrganisationChanged()
                            .withProsecutionCaseId(prosecutionCaseId)
                            .withDefendantId(defendantId)
                            .build());
                    streamBuilder.add(defendantDefenceOrganisationDisassociated);
                    streamBuilder.add(DefendantDefenceAssociationLocked.defendantDefenceAssociationLocked()
                            .withDefendantId(defendantId)
                            .withProsecutionCaseId(prosecutionCaseId)
                            .withLockedByRepOrder(false)
                            .build());
                }
            }
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> receiveRepresentationOrderForDefendant(final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant, final LaaReference laaReference,
                                                                 final UUID organisationId, final String organisationName, final String associatedOrganisationId, final uk.gov.justice.core.courts.Defendant defendant) {
        LOGGER.debug("Receive Representation Order for Defendant.");
        final UUID defendantId = receiveRepresentationOrderForDefendant.getDefendantId();
        final UUID prosecutionCaseId = receiveRepresentationOrderForDefendant.getProsecutionCaseId();
        final UUID offenceId = receiveRepresentationOrderForDefendant.getOffenceId();
        final String laaContractNumber = receiveRepresentationOrderForDefendant.getDefenceOrganisation().getLaaContractNumber();
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.defendantCaseOffences.containsKey(defendantId)) {
            final List<uk.gov.justice.core.courts.Offence> offencesList = defendantCaseOffences.get(defendantId);
            final List<uk.gov.justice.core.courts.Offence> updatedOffenceList = new ArrayList<>(offencesList);
            final Optional<uk.gov.justice.core.courts.Offence> optionalOffence = offencesList.stream().filter(offence -> offence.getId().equals(offenceId)).findAny();
            if (optionalOffence.isPresent()) {
                final uk.gov.justice.core.courts.Offence matchingOffence = optionalOffence.get();
                updatedOffenceList.remove(matchingOffence);
                final uk.gov.justice.core.courts.Offence updatedOffence = uk.gov.justice.core.courts.Offence.offence()
                        .withNotifiedPlea(matchingOffence.getNotifiedPlea())
                        .withId(matchingOffence.getId())
                        .withArrestDate(matchingOffence.getArrestDate())
                        .withChargeDate(matchingOffence.getChargeDate())
                        .withConvictionDate(matchingOffence.getConvictionDate())
                        .withCount(nonNull(matchingOffence.getCount()) ? matchingOffence.getCount() : 0)
                        .withEndDate(matchingOffence.getEndDate())
                        .withOffenceDefinitionId(matchingOffence.getOffenceDefinitionId())
                        .withOffenceFacts(matchingOffence.getOffenceFacts())
                        .withOffenceLegislation(matchingOffence.getOffenceLegislation())
                        .withOffenceLegislationWelsh(matchingOffence.getOffenceLegislationWelsh())
                        .withOffenceTitle(matchingOffence.getOffenceTitle())
                        .withOffenceTitleWelsh(matchingOffence.getOffenceTitleWelsh())
                        .withModeOfTrial(matchingOffence.getModeOfTrial())
                        .withOffenceCode(matchingOffence.getOffenceCode())
                        .withOrderIndex(matchingOffence.getOrderIndex())
                        .withStartDate(matchingOffence.getStartDate())
                        .withWording(matchingOffence.getWording())
                        .withWordingWelsh(matchingOffence.getWordingWelsh())
                        .withId(matchingOffence.getId())
                        .withOffenceCode(matchingOffence.getOffenceCode())
                        .withOffenceDefinitionId(matchingOffence.getOffenceDefinitionId())
                        .withOffenceFacts(matchingOffence.getOffenceFacts())
                        .withOffenceLegislation(matchingOffence.getOffenceLegislation())
                        .withOffenceLegislationWelsh(matchingOffence.getOffenceLegislationWelsh())
                        .withWording(matchingOffence.getWording())
                        .withCount(matchingOffence.getCount())
                        .withStartDate(matchingOffence.getStartDate())
                        .withLaaApplnReference(laaReference)
                        .withOffenceDateCode(matchingOffence.getOffenceDateCode())
                        .withCommittingCourt(matchingOffence.getCommittingCourt())
                        .build();
                updatedOffenceList.add(updatedOffence);
            }
            final String legalAidStatus = getDefendantLevelLegalStatus(updatedOffenceList);
            final DefendantLegalaidStatusUpdated defendantLegalaidStatusUpdated = DefendantLegalaidStatusUpdated.defendantLegalaidStatusUpdated()
                    .withCaseId(prosecutionCaseId)
                    .withDefendantId(defendantId)
                    .withLegalAidStatus(legalAidStatus)
                    .build();
            final DefendantCaseOffences newDefendantCaseOffences = DefendantCaseOffences.defendantCaseOffences()
                    .withOffences(updatedOffenceList)
                    .withDefendantId(defendantId)
                    .withLegalAidStatus(legalAidStatus)
                    .withProsecutionCaseId(receiveRepresentationOrderForDefendant.getProsecutionCaseId())
                    .build();
            final Optional<OffencesForDefendantChanged> offencesForDefendantChanged = DefendantHelper.getOffencesForDefendantUpdated(updatedOffenceList, offencesList, prosecutionCaseId, defendantId);
            handleDefenceOrganisationAssociationAndDisassociation(organisationId, organisationName, associatedOrganisationId, prosecutionCaseId, defendantId, laaContractNumber, streamBuilder);
            final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                    .withDefenceOrganisation(receiveRepresentationOrderForDefendant.getDefenceOrganisation())
                    .withIsAssociatedByLAA(true)
                    .withFundingType(FundingType.REPRESENTATION_ORDER)
                    .withApplicationReference(receiveRepresentationOrderForDefendant.getApplicationReference())
                    .withAssociationStartDate(receiveRepresentationOrderForDefendant.getEffectiveStartDate())
                    .withAssociationEndDate(receiveRepresentationOrderForDefendant.getEffectiveEndDate())
                    .build();
            if (offencesForDefendantChanged.isPresent()) {
                streamBuilder.add(ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                        .withDefendantCaseOffences(newDefendantCaseOffences).build());
                streamBuilder.add(offencesForDefendantChanged.get());
                streamBuilder.add(defendantLegalaidStatusUpdated);
                streamBuilder.add(DefendantDefenceOrganisationChanged.defendantDefenceOrganisationChanged()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantId(defendantId)
                        .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                        .build());
                streamBuilder.add(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                        .withDefendant(DefendantUpdate.defendantUpdate()
                                .withId(defendantId)
                                .withMasterDefendantId(defendant.getMasterDefendantId())
                                .withProsecutionCaseId(prosecutionCaseId)
                                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                                .withIsYouth(defendant.getIsYouth())
                                .withOffences(updatedOffenceList)
                                .withJudicialResults(defendant.getDefendantCaseJudicialResults())
                                .withCroNumber(defendant.getCroNumber())
                                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                                .withWitnessStatement(defendant.getWitnessStatement())
                                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                                .withPncId(defendant.getPncId())
                                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                                .withMitigationWelsh(defendant.getMitigationWelsh())
                                .withMitigation(defendant.getMitigation())
                                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                                .withAssociatedPersons(defendant.getAssociatedPersons())
                                .withAliases(defendant.getAliases())
                                .withPersonDefendant(defendant.getPersonDefendant())
                                .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                                .build())
                        .build());

            }
            streamBuilder.add(DefendantDefenceAssociationLocked.defendantDefenceAssociationLocked()
                    .withDefendantId(defendantId)
                    .withProsecutionCaseId(prosecutionCaseId)
                    .withLockedByRepOrder(true)
                    .build());

        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> receiveAssociateDefenceOrganisation(final String organisationName, final UUID defendantId, final UUID prosecutionCaseId, final String laaContractNumber, final ZonedDateTime startTime, final String representationType, final OrganisationDetails organisationDetails) {
        LOGGER.debug("Receive AssociateDefenceOrganisation");

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.defendantCaseOffences.containsKey(defendantId) && !isAlreadyAssociated(defendantId)) {

            final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                    .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                            .withLaaContractNumber(laaContractNumber)
                            .withOrganisation(Organisation.organisation()
                                    .withAddress(Address.address()
                                            .withAddress1(organisationDetails.getAddressLine1())
                                            .withAddress2(organisationDetails.getAddressLine2())
                                            .withAddress3(organisationDetails.getAddressLine3())
                                            .withAddress4(organisationDetails.getAddressLine4())
                                            .withPostcode(organisationDetails.getAddressPostcode())
                                            .build())
                                    .withName(organisationName)
                                    .build())
                            .build())
                    .withIsAssociatedByLAA(false)
                    .withFundingType(FundingType.valueOf(representationType))
                    .withAssociationStartDate(LocalDate.from(startTime))
                    .build();

            streamBuilder.add(DefendantDefenceOrganisationChanged.defendantDefenceOrganisationChanged()
                    .withProsecutionCaseId(prosecutionCaseId)
                    .withDefendantId(defendantId)
                    .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                    .build());

            streamBuilder.add(DefenceOrganisationAssociatedByDefenceContext.defenceOrganisationAssociatedByDefenceContext()
                    .withDefendantId(defendantId)
                    .withLaaContractNumber(laaContractNumber)
                    .withOrganisationId(organisationDetails.getId())
                    .withOrganisationName(organisationName)
                    .withRepresentationType(RepresentationType.valueOf(representationType))
                    .withStartDate(startTime)
                    .build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> receiveDisAssociateDefenceOrganisation(final UUID defendantId, final UUID prosecutionCaseId, final UUID organisationId) {
        LOGGER.debug("Receive DisAssociateDefenceOrganisation");

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.defendantCaseOffences.containsKey(defendantId)) {


            streamBuilder.add(DefendantDefenceOrganisationChanged.defendantDefenceOrganisationChanged()
                    .withProsecutionCaseId(prosecutionCaseId)
                    .withDefendantId(defendantId)
                    .build());

            streamBuilder.add(DefenceOrganisationDissociatedByDefenceContext.defenceOrganisationDissociatedByDefenceContext()
                    .withCaseId(prosecutionCaseId)
                    .withDefendantId(defendantId)
                    .withOrganisationId(organisationId)
                    .build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> addNote(final UUID caseId, final String note,final boolean isPinned, final String firstName, final String lastName) {
        return apply(Stream.of(CaseNoteAdded.caseNoteAdded()
                .withCaseId(caseId)
                .withNote(note)
                .withIsPinned(isPinned)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withCreatedDateTime(ZonedDateTime.now())
                .build()));
    }

    public Stream<Object> editNote(final UUID caseId, final UUID caseNoteId, final Boolean isPinned) {
        return apply(Stream.of(CaseNoteEdited.caseNoteEdited()
                .withCaseId(caseId)
                .withCaseNoteId(caseNoteId)
                .withIsPinned(isPinned)
                .build()));
    }

    public Stream<Object> aggregateExactMatchedDefendantSearchResult(final UUID defendantId, final List<Cases> cases) {
        return apply(Stream.of(ExactMatchedDefendantSearchResultStored.exactMatchedDefendantSearchResultStored()
                .withDefendantId(defendantId)
                .withCases(cases)
                .build()));
    }

    public Stream<Object> aggregatePartialMatchedDefendantSearchResult(final UUID defendantId, final List<Cases> cases) {
        return apply(Stream.of(PartialMatchedDefendantSearchResultStored.partialMatchedDefendantSearchResultStored()
                .withDefendantId(defendantId)
                .withCases(cases)
                .build()));
    }

    public Stream<Object> storeMatchedDefendants(final UUID prosecutionCaseId) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final Map<UUID, List<Cases>> partiallyMatchedDefendants = this.partialMatchedDefendants;
        final Map<UUID, List<Cases>> fullyMatchedDefendants = this.exactMatchedDefendants;

        for (final Map.Entry<UUID, List<Cases>> defendantEntry : partiallyMatchedDefendants.entrySet()) {
            // Partial matched defendant events
            final uk.gov.justice.core.courts.Defendant defendant = prosecutionCase.getDefendants().stream()
                    .filter(d -> d.getId().equals(defendantEntry.getKey()))
                    .findFirst().orElseThrow(() -> new RuntimeException("Defendant not found"));
            streamBuilder.add(DefendantPartialMatchCreated.defendantPartialMatchCreated()
                    .withDefendantId(defendant.getId())
                    .withProsecutionCaseId(prosecutionCaseId)
                    .withDefendantName(getDefendantName(defendant))
                    .withCaseReference(reference)
                    .withPayload(transformToPartialMatchDefendantPayload(defendant, prosecutionCaseId, defendantEntry.getValue()))
                    .withCaseReceivedDatetime(defendant.getCourtProceedingsInitiated())
                    .build());
        }

        for (final Map.Entry<UUID, List<Cases>> defendantEntry : fullyMatchedDefendants.entrySet()) {
            // Fully matched defendant events
            streamBuilder.add(MasterDefendantIdUpdated.masterDefendantIdUpdated()
                    .withProsecutionCaseId(prosecutionCaseId)
                    .withDefendantId(defendantEntry.getKey())
                    .withHearingId(latestHearingId)
                    .withMatchedDefendants(transformToExactMatchedDefendants(defendantEntry.getValue()))
                    .build());
        }
        return apply(streamBuilder.build());
    }

    private String getDefendantName(final uk.gov.justice.core.courts.Defendant defendant) {
        final uk.gov.justice.core.courts.Person personDetails = defendant.getPersonDefendant().getPersonDetails();
        return Stream.of(personDetails.getFirstName(), personDetails.getMiddleName(), personDetails.getLastName())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));

    }

    private void addToJsonObjectNullSafe(final JsonObjectBuilder jsonObjectBuilder, final String key, final String value) {
        if (nonNull(value)) {
            jsonObjectBuilder.add(key, value);
        }
    }

    private String transformToPartialMatchDefendantPayload(final uk.gov.justice.core.courts.Defendant defendant, final UUID prosecutionCaseId, final List<Cases> casesList) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("defendantId", defendant.getId().toString());
        jsonObjectBuilder.add("masterDefendantId", defendant.getMasterDefendantId().toString());
        jsonObjectBuilder.add("prosecutionCaseId", prosecutionCaseId.toString());
        jsonObjectBuilder.add("caseReference", reference);
        jsonObjectBuilder.add("courtProceedingsInitiated", ZONE_DATETIME_FORMATTER.format(defendant.getCourtProceedingsInitiated()));
        final uk.gov.justice.core.courts.Person personDetails = defendant.getPersonDefendant().getPersonDetails();
        addToJsonObjectNullSafe(jsonObjectBuilder, "firstName", personDetails.getFirstName());
        addToJsonObjectNullSafe(jsonObjectBuilder, "middleName", personDetails.getMiddleName());
        jsonObjectBuilder.add("lastName", personDetails.getLastName());
        if (nonNull(personDetails.getDateOfBirth())) {
            addToJsonObjectNullSafe(jsonObjectBuilder, "dateOfBirth", FORMATTER.format(personDetails.getDateOfBirth()));
        }
        addToJsonObjectNullSafe(jsonObjectBuilder, "pncId", defendant.getPncId());
        addToJsonObjectNullSafe(jsonObjectBuilder, "croNumber", defendant.getCroNumber());
        if (nonNull(defendant.getPersonDefendant().getPersonDetails().getAddress())) {
            addAddress(defendant.getPersonDefendant().getPersonDetails().getAddress(), jsonObjectBuilder);
        }
        jsonObjectBuilder.add("defendantsMatchedCount", casesList.size());

        final JsonArrayBuilder jsonDefendantsMatchedBuilder = Json.createArrayBuilder();
        casesList.forEach(cases -> convertToJsonArray(jsonDefendantsMatchedBuilder, cases, cases.getDefendants()));
        jsonObjectBuilder.add("defendantsMatched", jsonDefendantsMatchedBuilder.build());
        return jsonObjectBuilder.build().toString();
    }

    private JsonArrayBuilder convertToJsonArray(final JsonArrayBuilder jsonArrayBuilder, final Cases cases, final List<Defendants> defendants) {
        defendants.forEach(defendant -> {
            final JsonObjectBuilder defendantJsonObjectBuilder = Json.createObjectBuilder();
            defendantJsonObjectBuilder.add("defendantId", defendant.getDefendantId());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "masterDefendantId", defendant.getMasterDefendantId());
            defendantJsonObjectBuilder.add("courtProceedingsInitiated", ZONE_DATETIME_FORMATTER.format(defendant.getCourtProceedingsInitiated()));
            defendantJsonObjectBuilder.add("caseReference", cases.getCaseReference());
            defendantJsonObjectBuilder.add("prosecutionCaseId", cases.getProsecutionCaseId());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "firstName", defendant.getFirstName());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "middleName", defendant.getMiddleName());
            defendantJsonObjectBuilder.add("lastName", defendant.getLastName());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "dateOfBirth", defendant.getDateOfBirth());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "pncId", defendant.getPncId());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "croNumber", defendant.getCroNumber());
            if (nonNull(defendant.getAddress())) {
                addAddress(defendant.getAddress(), defendantJsonObjectBuilder);
            }
            jsonArrayBuilder.add(defendantJsonObjectBuilder.build());
        });
        return jsonArrayBuilder;
    }

    private void addAddress(final Address address, final JsonObjectBuilder jsonObjectBuilder) {
        final JsonObjectBuilder addressJsonObjectBuilder = Json.createObjectBuilder();
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine1", address.getAddress1());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine2", address.getAddress2());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine3", address.getAddress3());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine4", address.getAddress4());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine5", address.getAddress5());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "postcode", address.getPostcode());
        jsonObjectBuilder.add("address", addressJsonObjectBuilder.build());
    }

    public Stream<Object> matchPartiallyMatchedDefendants(final MatchDefendant matchDefendant) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (matchedDefendantIds.contains(matchDefendant.getDefendantId())) {
            streamBuilder.add(DefendantMatched.defendantMatched()
                    .withDefendantId(matchDefendant.getDefendantId())
                    .withHasDefendantAlreadyBeenDeleted(TRUE)
                    .build());
        } else {
            streamBuilder.add(DefendantMatched.defendantMatched()
                    .withDefendantId(matchDefendant.getDefendantId())
                    .withHasDefendantAlreadyBeenDeleted(FALSE)
                    .build());
            streamBuilder.add(MasterDefendantIdUpdated.masterDefendantIdUpdated()
                    .withProsecutionCaseId(matchDefendant.getProsecutionCaseId())
                    .withDefendantId(matchDefendant.getDefendantId())
                    .withHearingId(latestHearingId)
                    .withMatchedDefendants(transform(matchDefendant.getMatchedDefendants()))
                    .build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> unmatchDefendants(final UnmatchDefendant unmatchDefendant) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        unmatchDefendant.getUnmatchedDefendants()
                .forEach(d -> streamBuilder.add(DefendantUnmatched.defendantUnmatched()
                        .withDefendantId(d.getDefendantId())
                        .withProsecutionCaseId(d.getProsecutionCaseId())
                        .build())
                );
        return apply(streamBuilder.build());
    }

    public Stream<Object> validateLinkSplitOrMergeStreams(final List<CasesToLink> casesToLink) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        casesToLink.forEach(ctl -> {

                    if (ctl.getLinkType().toString().equals(LinkType.LINK.name())) {
                        streamBuilder.add(ValidateLinkCases.validateLinkCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());

                    } else if (ctl.getLinkType().toString().equals(LinkType.SPLIT.name())) {
                        streamBuilder.add(ValidateSplitCases.validateSplitCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());

                    } else if (ctl.getLinkType().toString().equals(LinkType.MERGE.name())) {
                        streamBuilder.add(ValidateMergeCases.validateMergeCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());
                    }
                }
        );
        return apply(streamBuilder.build());
    }

    public Stream<Object> processLinkSplitOrMergeStreams(final List<CasesToLink> casesToLink) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        casesToLink.forEach(ctl -> {

                    if (ctl.getLinkType().toString().equals(LinkType.LINK.name())) {
                        streamBuilder.add(LinkCases.linkCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());

                    } else if (ctl.getLinkType().toString().equals(LinkType.SPLIT.name())) {
                        streamBuilder.add(SplitCases.splitCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());

                    } else if (ctl.getLinkType().toString().equals(LinkType.MERGE.name())) {
                        streamBuilder.add(MergeCases.mergeCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());
                    }
                }
        );
        return apply(streamBuilder.build());
    }

    public Stream<Object> unlinkCases(final UUID prosecutionCaseId, final String prosecutionCaseUrn, final List<CaseToUnlink> casesToUnlink) {
        final CasesUnlinked casesUnlinked = CasesUnlinked.casesUnlinked()
                .withProsecutionCaseId(prosecutionCaseId)
                .withProsecutionCaseUrn(prosecutionCaseUrn)
                .withUnlinkedCases(
                        casesToUnlink.stream()
                                .map(ctu -> UnlinkedCases.unlinkedCases()
                                        .withCaseId(ctu.getCaseId())
                                        .withCaseUrn(ctu.getCaseUrn())
                                        .withLinkGroupId(ctu.getLinkGroupId())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();

        return apply(Stream.builder()
                .add(casesUnlinked)
                .build());
    }

    public Stream<Object> extendHearing(final HearingListingNeeds hearingListingNeeds, final Boolean isAdjourned, final UUID extendHearingFrom, final Boolean isPartiallyAllocated, final List<UUID> shadowListedOffences) {
        LOGGER.debug("hearing has been extended");
        return apply(Stream.of(
                HearingExtended.hearingExtended()
                        .withExtendedHearingFrom(extendHearingFrom)
                        .withHearingRequest(hearingListingNeeds)
                        .withIsAdjourned(isAdjourned)
                        .withIsPartiallyAllocated(isPartiallyAllocated)
                        .withShadowListedOffences(shadowListedOffences)
                        .build()));
    }

    public Stream<Object> updateHearingForPartialAllocation(final UUID hearingId, final List<ProsecutionCasesToRemove> prosecutionCasesToRemove) {
        LOGGER.debug("hearing has been updated for partial allocation");
        return apply(Stream.of(HearingUpdatedForPartialAllocation.hearingUpdatedForPartialAllocation()
                .withHearingId(hearingId)
                .withProsecutionCasesToRemove(prosecutionCasesToRemove)
                .build()));
    }

    public Stream<Object> markHearingAsDuplicate(final UUID hearingId, final UUID caseId, final List<UUID> defendantIds) {
        return apply(Stream.of(HearingMarkedAsDuplicateForCase.hearingMarkedAsDuplicateForCase()
                .withCaseId(caseId)
                .withHearingId(hearingId)
                .withDefendantIds(defendantIds)
                .build()));
    }

    public Stream<Object> updateCaseProsecutorDetails(final ProsecutionCaseIdentifier prosecutionCaseIdentifier,
                                                      final String oldCpsProsecutor) {
        LOGGER.debug("update case Prosecution details for caseId");
        final UUID caseId = this.prosecutionCase.getId();
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(caseId)
                .withAddress(prosecutionCaseIdentifier.getAddress())
                .withCaseURN(this.prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                .withOldCpsProsecutor(oldCpsProsecutor)
                .withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode())
                .withProsecutionAuthorityId(prosecutionCaseIdentifier.getProsecutionAuthorityId())
                .withProsecutionAuthorityName(prosecutionCaseIdentifier.getProsecutionAuthorityName())
                .withProsecutionAuthorityReference(this.prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())
                .withContact(prosecutionCaseIdentifier.getContact())
                .withMajorCreditorCode(prosecutionCaseIdentifier.getMajorCreditorCode())
                .withProsecutionAuthorityOUCode(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode())
                .withIsCpsOrgVerifyError(false)// this method is called for valid cps organisation so we should set the error flag as false.
                .build());

        if(oldCpsProsecutor != null && !oldCpsProsecutor.isEmpty()) {
            streamBuilder.add(CpsProsecutorUpdated.cpsProsecutorUpdated()
                    .withProsecutionCaseId(caseId)
                    .withOldCpsProsecutor(oldCpsProsecutor)
                    .withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode())
                    .build());
        }

        return apply(streamBuilder.build());
    }

    public ProsecutionCase getProsecutionCase(){
        return this.prosecutionCase;
    }

    private List<MatchedDefendants> transform(final List<MatchedDefendant> matchedDefendants) {
        return matchedDefendants.stream()
                .map(matchedDefendant -> MatchedDefendants.matchedDefendants()
                        .withDefendantId(matchedDefendant.getDefendantId())
                        .withProsecutionCaseId(matchedDefendant.getProsecutionCaseId())
                        .withMasterDefendantId(matchedDefendant.getMasterDefendantId())
                        .withCourtProceedingsInitiated(matchedDefendant.getCourtProceedingsInitiated())
                        .build())
                .collect(toList());
    }

    private List<MatchedDefendants> transformToExactMatchedDefendants(final List<Cases> casesList) {
        final List<MatchedDefendants> matchedDefendantsList = new ArrayList<>();
        casesList
                .forEach(cases -> cases.getDefendants()
                        .forEach(def -> matchedDefendantsList.add(MatchedDefendants.matchedDefendants()
                                .withProsecutionCaseId(UUID.fromString(cases.getProsecutionCaseId()))
                                .withDefendantId(UUID.fromString(def.getDefendantId()))
                                .withMasterDefendantId(UUID.fromString(def.getMasterDefendantId()))
                                .withCourtProceedingsInitiated(def.getCourtProceedingsInitiated())
                                .build())
                        ));
        return matchedDefendantsList;
    }

    private void handleDefenceOrganisationAssociationAndDisassociation(final UUID organisationId, final String organisationName, final String alreadyAssociatedOrganisationId,
                                                                       final UUID prosecutionCaseId, final UUID defendantId, final String laaContractNumber, final Stream.Builder<Object> streamBuilder) {

        if (organisationId == null && alreadyAssociatedOrganisationId != null) {
            LOGGER.error("Organisation not set up for LAA Contract Number {}", laaContractNumber);
            //Raise event to store orphaned defendants so that it can be fetched when organisation is Set up.
            streamBuilder.add(DefendantLaaAssociated.defendantLaaAssociated()
                    .withDefendantId(defendantId)
                    .withLaaContractNumber(laaContractNumber)
                    .withIsAssociatedByLAA(false)
                    .build());

            //disassociate the existing defence organisation
            streamBuilder.add(defendantDefenceOrganisationDisassociated()
                    .withDefendantId(defendantId)
                    .withCaseId(prosecutionCaseId)
                    .withOrganisationId(fromString(alreadyAssociatedOrganisationId))
                    .build());
        } else if (organisationId != null && alreadyAssociatedOrganisationId != null) {
            if (!organisationId.toString().equals(alreadyAssociatedOrganisationId)) {
                // Disassociate the existing defence organisation and associate the one which is linked with one from payload
                streamBuilder.add(defendantDefenceOrganisationDisassociated()
                        .withDefendantId(defendantId)
                        .withCaseId(prosecutionCaseId)
                        .withOrganisationId(fromString(alreadyAssociatedOrganisationId))
                        .build());
                streamBuilder.add(DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                        .withDefendantId(defendantId)
                        .withOrganisationId(organisationId)
                        .withOrganisationName(organisationName)
                        .withLaaContractNumber(laaContractNumber)
                        .withRepresentationType(RepresentationType.REPRESENTATION_ORDER)
                        .build());
            } else {
                if(!isAlreadyAssociatedToOrganisation(defendantId, organisationId)) {
                    // Handle wrong payload case, when it is not actually associated by payload says organisation is associated.
                    streamBuilder.add(DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                            .withDefendantId(defendantId)
                            .withLaaContractNumber(laaContractNumber)
                            .withOrganisationId(organisationId)
                            .withOrganisationName(organisationName)
                            .withRepresentationType(RepresentationType.REPRESENTATION_ORDER)
                            .build());
                }
                else {
                    LOGGER.info("Organisation for LAA Contract Number {} is already associated", laaContractNumber);
                }

            }
        } else {
            if (organisationId != null) {
                // Associate the one which is linked with one from payload
                if(!isAlreadyAssociatedToOrganisation(defendantId, organisationId)) {
                    streamBuilder.add(DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                            .withDefendantId(defendantId)
                            .withLaaContractNumber(laaContractNumber)
                            .withOrganisationId(organisationId)
                            .withOrganisationName(organisationName)
                            .withRepresentationType(RepresentationType.REPRESENTATION_ORDER)
                            .build());
                }
            } else {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Organisation not set up for LAA Contract Number {} and there is no existing association for defendant {} ", laaContractNumber, defendantId.toString());
                }
                //Raise event to store orphaned defendants so that it can be fetched when organisation is Set up.
                streamBuilder.add(DefendantLaaAssociated.defendantLaaAssociated()
                        .withDefendantId(defendantId)
                        .withLaaContractNumber(laaContractNumber)
                        .withIsAssociatedByLAA(false)
                        .build());
            }
        }
    }

    private String getDefendantLevelLegalStatus(final List<uk.gov.justice.core.courts.Offence> offencesList) {
        final List<String> defendantLevelStatusList = offencesList.stream().filter(offence -> nonNull(offence.getLaaApplnReference()))
                .map(uk.gov.justice.core.courts.Offence::getLaaApplnReference)
                .map(LaaReference::getOffenceLevelStatus).collect(toList());
        if (defendantLevelStatusList.stream().anyMatch
                (defendantLevelStatus -> defendantLevelStatus != null && defendantLevelStatus.equals(GRANTED.getDescription()))) {
            return GRANTED.getDescription();
        } else if (defendantLevelStatusList.stream().allMatch(defendantLevelStatus -> defendantLevelStatus != null && defendantLevelStatus.equals(REFUSED.getDescription()))) {
            return REFUSED.getDescription();
        } else if (defendantLevelStatusList.stream().allMatch(defendantLevelStatus -> defendantLevelStatus != null && defendantLevelStatus.equals(WITHDRAWN.getDescription()))) {
            return WITHDRAWN.getDescription();
        } else if (defendantLevelStatusList.stream().allMatch(defendantLevelStatus -> defendantLevelStatus != null && defendantLevelStatus.equals(PENDING.getDescription()))) {
            return PENDING.getDescription();
        } else {
            return NO_VALUE.getDescription();
        }

    }

    private void hearingConfirmedCaseStatusUpdated(final HearingConfirmedCaseStatusUpdated hearingConfirmedCaseStatusUpdated) {
        this.caseStatus = hearingConfirmedCaseStatusUpdated.getProsecutionCase().getCaseStatus();
    }

    private void removeDefendantAssociatedDefenceOrganisation(final DefendantDefenceOrganisationDisassociated defendantDefenceOrganisationDisassociated) {
        removeFromOrganisationMap(defendantDefenceOrganisationDisassociated.getDefendantId());
    }

    private void removeDefendantAssociatedDefenceOrganisation(final DefenceOrganisationDissociatedByDefenceContext defenceOrganisationDissociatedByDefenceContext) {
        removeFromOrganisationMap(defenceOrganisationDissociatedByDefenceContext.getDefendantId());
    }

    private void removeFromOrganisationMap(final UUID defendantId) {
        this.defendantAssociatedDefenceOrganisation.remove(defendantId);
    }

    private void updateDefendantAssociatedDefenceOrganisation(final DefendantDefenceOrganisationAssociated defendantDefenceOrganisationAssociated) {
        this.defendantAssociatedDefenceOrganisation.put(defendantDefenceOrganisationAssociated.getDefendantId(), defendantDefenceOrganisationAssociated.getOrganisationId());
    }

    private void updateDefendantAssociatedDefenceOrganisation(final DefenceOrganisationAssociatedByDefenceContext defenceOrganisationAssociatedByDefenceContext) {
        this.defendantAssociatedDefenceOrganisation.put(defenceOrganisationAssociatedByDefenceContext.getDefendantId(), defenceOrganisationAssociatedByDefenceContext.getOrganisationId());
    }

    private void onHearingMarkedAsDuplicateForCase(final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase) {
        this.hearingIds.remove(hearingMarkedAsDuplicateForCase.getHearingId());
    }

    private boolean isAlreadyAssociated(final UUID defendantId) {
        return !isNull(this.defendantAssociatedDefenceOrganisation.get(defendantId));
    }

    private boolean isAlreadyAssociatedToOrganisation(final UUID defendantId, final UUID organisationId) {
        return this.defendantAssociatedDefenceOrganisation.containsKey(defendantId) && this.defendantAssociatedDefenceOrganisation.get(defendantId).equals(organisationId);
    }

    private void updateProsecutionCaseIdentifier(final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated) {
        ProsecutionCaseIdentifier newProsecutionCaseIdentifier;
        if(!isNull(caseCpsProsecutorUpdated.getIsCpsOrgVerifyError()) && Boolean.TRUE.equals(caseCpsProsecutorUpdated.getIsCpsOrgVerifyError())){
            newProsecutionCaseIdentifier = this.prosecutionCase.getProsecutionCaseIdentifier();
        }else{
            newProsecutionCaseIdentifier = ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                    .withProsecutionAuthorityCode(caseCpsProsecutorUpdated.getProsecutionAuthorityCode())
                    .withCaseURN(caseCpsProsecutorUpdated.getCaseURN())
                    .withProsecutionAuthorityId(caseCpsProsecutorUpdated.getProsecutionAuthorityId())
                    .withProsecutionAuthorityReference(caseCpsProsecutorUpdated.getProsecutionAuthorityReference())
                    .withProsecutionAuthorityName(caseCpsProsecutorUpdated.getProsecutionAuthorityName())
                    .withAddress(caseCpsProsecutorUpdated.getAddress())
                    .withProsecutionAuthorityOUCode(caseCpsProsecutorUpdated.getProsecutionAuthorityOUCode())
                    .withContact(caseCpsProsecutorUpdated.getContact())
                    .withMajorCreditorCode(caseCpsProsecutorUpdated.getMajorCreditorCode())
                    .build();
        }

        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase()
                .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                .withProsecutionCaseIdentifier(newProsecutionCaseIdentifier)
                .withId(prosecutionCase.getId())
                .withDefendants(prosecutionCase.getDefendants())
                .withInitiationCode(prosecutionCase.getInitiationCode())
                .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                .withCpsOrganisation(prosecutionCase.getCpsOrganisation())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .withCaseMarkers(prosecutionCase.getCaseMarkers())
                .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                .withRemovalReason(prosecutionCase.getRemovalReason())
                .withCaseStatus(prosecutionCase.getCaseStatus())
                .withIsCpsOrgVerifyError(caseCpsProsecutorUpdated.getIsCpsOrgVerifyError())
                .build();
        this.prosecutionCase = updatedProsecutionCase;

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = this.prosecutionCase.getProsecutionCaseIdentifier();
        if (nonNull(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
            reference = caseCpsProsecutorUpdated.getProsecutionAuthorityReference();
        }
        if (nonNull(prosecutionCaseIdentifier.getCaseURN())) {
            reference = caseCpsProsecutorUpdated.getCaseURN();
        }
    }

    public Stream<Object> updateCpsOrganisationInvalid() {
        final UUID caseId = this.prosecutionCase.getId();
       final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(caseId)
                .withProsecutionAuthorityCode(this.prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                .withIsCpsOrgVerifyError(true).build());

        return apply(streamBuilder.build());
    }
}