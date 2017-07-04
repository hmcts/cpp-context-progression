package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseProgressionAggregate implements Aggregate {

    public static final String CANNOT_ADD_ADDITIONAL_INFO = "Cannot add additional information without defendant ";
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProgressionAggregate.class);
    private final transient Set<Defendant> defendants = new HashSet<>();
    private transient ProgressionEventFactory progressionEventFactory = new ProgressionEventFactory();
    private UUID caseProgressionId;
    private Set<UUID> defendantIds = new HashSet<>();
    private boolean isAllDefendantReviewed;
    private boolean isAnyDefendantPending;
    private LocalDate sentenceHearingDate;
    private UUID sentenceHearingId;

    @Override
    public Object apply(final Object event) {
        return match(event).with(

                when(uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt.class)
                        .apply(e -> {
                            defendants.addAll(e.getDefendants());
                            defendantIds = defendants.stream().map(Defendant::getId)
                                    .collect(Collectors.toSet());
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(PreSentenceReportForDefendantsRequested.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent.class)
                        .apply(e -> {
                            // Do Nothng
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded.class)
                        .apply(e -> {
                            caseProgressionId = e.getCaseProgressionId();
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
                            caseProgressionId = e.getCaseProgressionId();
                            final Defendant defendant = defendants.stream()
                                    .filter(d -> d.getId()
                                            .equals(e.getDefendantId()))
                                    .findAny().get();
                            defendant.setSentenceHearingReviewDecision(true);
                            defendant.setIsAdditionalInfoAvilable(false);
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded.class)
                        .apply(e -> {
                            caseProgressionId = e.getCaseProgressionId();
                            this.sentenceHearingDate = e.getSentenceHearingDate();

                        }),
                when(uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateUpdated.class)
                        .apply(e -> {
                            caseProgressionId = e.getCaseProgressionId();
                            this.sentenceHearingDate = e.getSentenceHearingDate();

                        }),
                when(uk.gov.moj.cpp.progression.domain.event.SentenceHearingAdded.class)
                        .apply(e -> {
                            caseProgressionId = e.getCaseProgressionId();
                            this.sentenceHearingId = e.getSentenceHearingId();
                        }));

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
        if (!defendantIds.contains(defendantId)) {
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

        if (!defendantIds.contains(defendantId)) {
            LOGGER.warn("Cannot add additional information without defendant " + defendantId);
            return Stream.empty();
        }

        updateDefendantInfo(defendant.getAdditionalInformation() != null ? true : false, defendantId);

        // check if all defendant's reviewed
        checkAllDefendant();
        if (caseProgressionId == null) {
            caseProgressionId = defendant.getCaseProgressionId();
        }

        updateCaseStatus(streamBuilder);

        streamBuilder.add(progressionEventFactory.addDefendantEvent(defendant));
        return apply(streamBuilder.build());
    }

    private void updateCaseStatus(Stream.Builder<Object> streamBuilder) {
        if (isAllDefendantReviewed) {
            if (isAnyDefendantPending) {
                streamBuilder.add(new CasePendingForSentenceHearing(caseProgressionId,
                        CaseStatusEnum.PENDING_FOR_SENTENCING_HEARING, ZonedDateTime.now(ZoneOffset.UTC)));
            } else {
                streamBuilder.add(new CaseReadyForSentenceHearing(caseProgressionId,
                        CaseStatusEnum.READY_FOR_SENTENCING_HEARING, ZonedDateTime.now(ZoneOffset.UTC)));
            }
        }
    }

}
