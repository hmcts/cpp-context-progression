package uk.gov.moj.cpp.progression.aggregate;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.CotrPdfContent;
import uk.gov.justice.core.courts.CreateCotr;
import uk.gov.justice.core.courts.Defence;
import uk.gov.justice.core.courts.DefendantCotrServed;
import uk.gov.justice.core.courts.Fields;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.RequestCotrTask;
import uk.gov.justice.core.courts.ServeDefendantCotr;
import uk.gov.justice.cpp.progression.event.CotrArchived;
import uk.gov.justice.cpp.progression.event.CotrCreated;
import uk.gov.justice.cpp.progression.event.Defendants;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrServed;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrUpdated;
import uk.gov.justice.cpp.progression.event.UpdateProsecutionCotr;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.progression.courts.AddFurtherInfoDefenceCotrCommand;
import uk.gov.justice.progression.courts.AddFurtherInfoProsecutionCotr;
import uk.gov.justice.progression.courts.ChangeDefendantsCotr;
import uk.gov.justice.progression.courts.UpdateReviewNotes;
import uk.gov.justice.progression.event.CotrNotes;
import uk.gov.justice.progression.event.DefendantAddedToCotr;
import uk.gov.justice.progression.event.DefendantRemovedFromCotr;
import uk.gov.justice.progression.event.FurtherInfoForDefenceCotrAdded;
import uk.gov.justice.progression.event.FurtherInfoForProsecutionCotrAdded;
import uk.gov.justice.progression.event.ReviewNoteType;
import uk.gov.justice.progression.event.ReviewNotes;
import uk.gov.justice.progression.event.ReviewNotesUpdated;
import uk.gov.moj.cpp.progression.command.ServeProsecutionCotr;
import uk.gov.moj.cpp.progression.json.schemas.event.CotrTaskRequested;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CotrAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CotrAggregate.class);

    private static final long serialVersionUID = 102L;
    private static final String CTSC_ADMIN = "CTSC Admin";
    private static final String OPERATIONAL_DELIVERY_ADMIN_LISTING_OFFICER_CASE_PROGRESSION_OFFICER = "Operational Delivery Admin, Listing Officer, Case Progression officer";
    private static final String OPERATIONAL_DELIVERY_ADMIN = "Operational Delivery Admin";
    private static final String CASE_REFERENCE = "Case reference ";
    private static final String AND_TRIAL_DATE = " and Trial date ";
    private boolean archived = false;
    private int defendantNumber = 0;
    private UUID caseId;
    private String caseUrn;
    private LocalDate hearingDate;
    private JurisdictionType jurisdictionType;
    private String courtCenter;
    private Map<UUID, CotrPdfContent> defendantCotrPdfContent = new HashMap<>();


    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CotrArchived.class).apply(e -> this.archived = true),
                when(DefendantCotrServed.class).apply(e -> this.defendantCotrPdfContent.put(e.getDefendantId(), e.getPdfContent())),
                when(FurtherInfoForDefenceCotrAdded.class).apply(e -> this.defendantCotrPdfContent.put(e.getDefendantId(), e.getPdfContent())),
                when(CotrCreated.class).apply(e -> {
                    defendantNumber = e.getDefendants().size();
                    caseId = e.getCaseId();
                    caseUrn = e.getCaseUrn();
                    hearingDate = LocalDate.parse(e.getHearingDate());
                    jurisdictionType = e.getJurisdictionType();
                    courtCenter = e.getCourtCenter();
                }),
                when(DefendantAddedToCotr.class).apply(e -> defendantNumber++),
                otherwiseDoNothing());
    }

    public Stream<Object> createCotr(final CreateCotr createCotr) {
        final AtomicInteger dNumberCounter = new AtomicInteger();
        return apply(Stream.of(CotrCreated.cotrCreated()
                .withCotrId(createCotr.getCotrId())
                .withHearingId(createCotr.getHearingId())
                .withCaseId(createCotr.getCaseId())
                .withCaseUrn(createCotr.getCaseUrn())
                .withHearingDate(createCotr.getHearingDate())
                .withJurisdictionType(createCotr.getJurisdictionType())
                .withCourtCenter(createCotr.getCourtCenter())
                .withDefendants(createCotr.getDefendantIds().stream()
                        .map(defendantId -> Defendants.defendants()
                                .withId(defendantId)
                                .withDnumber(dNumberCounter.incrementAndGet())
                                .build())
                        .collect(Collectors.toList()))
                .withSubmissionId(createCotr.getSubmissionId())
                .withCertifyThatTheProsecutionIsTrialReady(createCotr.getCertifyThatTheProsecutionIsTrialReady())
                .withCertificationDate(createCotr.getCertificationDate())
                .withApplyForThePtrToBeVacated(createCotr.getApplyForThePtrToBeVacated())
                .withFormCompletedOnBehalfOfTheProsecutionBy(createCotr.getFormCompletedOnBehalfOfTheProsecutionBy())
                .withFurtherInformationToAssistTheCourt(createCotr.getFurtherInformationToAssistTheCourt())
                .withHasAllDisclosureBeenProvided(createCotr.getHasAllDisclosureBeenProvided())
                .withHasAllEvidenceToBeReliedOnBeenServed(createCotr.getHasAllEvidenceToBeReliedOnBeenServed())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(createCotr.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(createCotr.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(createCotr.getHaveEditedAbeInterviewsBeenPreparedAndAgreed())
                .withHaveInterpretersForWitnessesBeenArranged(createCotr.getHaveInterpretersForWitnessesBeenArranged())
                .withHaveOtherDirectionsBeenCompliedWith(createCotr.getHaveOtherDirectionsBeenCompliedWith())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(createCotr.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(createCotr.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(createCotr.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJury())
                .withIsTheTimeEstimateCorrect(createCotr.getIsTheTimeEstimateCorrect())
                .withLastRecordedTimeEstimate(createCotr.getLastRecordedTimeEstimate())
                .withFormDefendants(createCotr.getFormDefendants())
                .build()));
    }

    public Stream<Object> serveProsecutionCotr(final ServeProsecutionCotr serveProsecutionCotr) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final String roles = getRoles();
        String taskName = "Review Prosecution CoTR Task";
        int numberOfDays = 1;
        if (serveProsecutionCotr.getIsTheTimeEstimateCorrect() != null
                && serveProsecutionCotr.getIsTheTimeEstimateCorrect().getAnswer() != null
                && "N".equalsIgnoreCase(serveProsecutionCotr.getIsTheTimeEstimateCorrect().getAnswer())) {
            taskName = "Review Listing Task";
            numberOfDays = 2;
        }

        streamBuilder.add(CotrTaskRequested.cotrTaskRequested()
                .withCotrId(serveProsecutionCotr.getCotrId())
                .withNumberOfDays(numberOfDays)
                .withTaskName(taskName)
                .withComments(CASE_REFERENCE + caseUrn + AND_TRIAL_DATE + hearingDate)
                .withRoles(roles)
                .withCourtCode(courtCenter)
                .withOrganisationId(UUID.randomUUID())
                .withHearingDate(hearingDate != null ? hearingDate.toString() : null)
                .build());

        streamBuilder.add(ProsecutionCotrServed.prosecutionCotrServed()
                .withCotrId(serveProsecutionCotr.getCotrId())
                .withHearingId(serveProsecutionCotr.getHearingId())
                .withCertifyThatTheProsecutionIsTrialReady(serveProsecutionCotr.getCertifyThatTheProsecutionIsTrialReady())
                .withCertificationDate(serveProsecutionCotr.getCertificationDate())
                .withApplyForThePtrToBeVacated(serveProsecutionCotr.getApplyForThePtrToBeVacated())
                .withFormCompletedOnBehalfOfTheProsecutionBy(serveProsecutionCotr.getFormCompletedOnBehalfOfTheProsecutionBy())
                .withFurtherInformationToAssistTheCourt(serveProsecutionCotr.getFurtherInformationToAssistTheCourt())
                .withHasAllDisclosureBeenProvided(serveProsecutionCotr.getHasAllDisclosureBeenProvided())
                .withHasAllEvidenceToBeReliedOnBeenServed(serveProsecutionCotr.getHasAllEvidenceToBeReliedOnBeenServed())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(serveProsecutionCotr.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(serveProsecutionCotr.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(serveProsecutionCotr.getHaveEditedAbeInterviewsBeenPreparedAndAgreed())
                .withHaveInterpretersForWitnessesBeenArranged(serveProsecutionCotr.getHaveInterpretersForWitnessesBeenArranged())
                .withHaveOtherDirectionsBeenCompliedWith(serveProsecutionCotr.getHaveOtherDirectionsBeenCompliedWith())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(serveProsecutionCotr.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(serveProsecutionCotr.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(serveProsecutionCotr.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJury())
                .withIsTheTimeEstimateCorrect(serveProsecutionCotr.getIsTheTimeEstimateCorrect())
                .withLastRecordedTimeEstimate(serveProsecutionCotr.getLastRecordedTimeEstimate())
                .withSubmissionId(serveProsecutionCotr.getSubmissionId())
                .build());
        return apply(streamBuilder.build());
    }

    public Stream<Object> archiveCotr(final UUID cotrId) {
        if (archived) {
            LOGGER.info("COTR with id {} already archived", cotrId);
            return Stream.empty();
        }
        return apply(Stream.of(CotrArchived.cotrArchived().withCotrId(cotrId).build()));

    }

    public Stream<Object> requestCotrTask(final RequestCotrTask requestCotrTask) {

        return apply(Stream.of(CotrTaskRequested.cotrTaskRequested()
                .withCotrId(requestCotrTask.getCotrId())
                .withNumberOfDays(requestCotrTask.getNumberOfDays())
                .withTaskName(requestCotrTask.getTaskName())
                .withComments(CASE_REFERENCE + caseUrn + AND_TRIAL_DATE + hearingDate + " , " + requestCotrTask.getComments())
                .withRoles(requestCotrTask.getRoles())
                .withCourtCode(courtCenter)
                .withOrganisationId(UUID.randomUUID())
                .withHearingDate(hearingDate != null ? hearingDate.toString() : null)
                .build()));
    }

    public Stream<Object> serveDefendantCotr(final ServeDefendantCotr serveDefendantCotr) {
        //  COTR <trial date> <first name> <Last name> <DOB>
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final String roles = getRoles();
        String taskName = "Review Defence CoTR Task";
        int numberOfDays = 1;
        if (serveDefendantCotr.getCorrectEstimate() != null && !serveDefendantCotr.getCorrectEstimate()) {
            taskName = "Review Listing Task";
            numberOfDays = 2;
        }

        if (serveDefendantCotr.getIsWelshForm() != null && serveDefendantCotr.getIsWelshForm()) {
            streamBuilder.add(CotrTaskRequested.cotrTaskRequested()
                    .withCotrId(serveDefendantCotr.getCotrId())
                    .withNumberOfDays(numberOfDays)
                    .withTaskName("Welsh Translation request Task")
                    .withComments(CASE_REFERENCE + caseUrn + AND_TRIAL_DATE + hearingDate)
                    .withRoles(getRolesForWelsh())
                    .withCourtCode(courtCenter)
                    .withOrganisationId(UUID.randomUUID())
                    .withHearingDate(hearingDate != null ? hearingDate.toString() : null)
                    .build());
        }

        streamBuilder.add(CotrTaskRequested.cotrTaskRequested()
                .withCotrId(serveDefendantCotr.getCotrId())
                .withNumberOfDays(numberOfDays)
                .withTaskName(taskName)
                .withComments(CASE_REFERENCE + caseUrn + AND_TRIAL_DATE + hearingDate)
                .withRoles(roles)
                .withCourtCode(courtCenter)
                .withOrganisationId(UUID.randomUUID())
                .withHearingDate(hearingDate != null ? hearingDate.toString() : null)
                .build());

        streamBuilder.add(DefendantCotrServed.defendantCotrServed()
                .withCaseId(caseId)
                .withCotrId(serveDefendantCotr.getCotrId())
                .withDefendantId(serveDefendantCotr.getDefendantId())
                .withCorrectEstimate(serveDefendantCotr.getCorrectEstimate())
                .withIsWelshForm(serveDefendantCotr.getIsWelshForm())
                .withDefendantFormData(serveDefendantCotr.getDefendantFormData())
                .withPdfContent(serveDefendantCotr.getPdfContent())
                .withServedByName(serveDefendantCotr.getServedByName())
                .withTrailDate(convertDateToTextPatternDMMMUUUU(hearingDate))
                .build());
        return apply(streamBuilder.build());
    }


    public Stream<Object> changeDefendantsCotr(final ChangeDefendantsCotr changeDefendantsCotr) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (isNotEmpty(changeDefendantsCotr.getAddedDefendantIds())) {
            changeDefendantsCotr.getAddedDefendantIds().forEach(defendantId ->
                    streamBuilder.add(DefendantAddedToCotr.defendantAddedToCotr()
                            .withCotrId(changeDefendantsCotr.getCotrId())
                            .withHearingId(changeDefendantsCotr.getHearingId())
                            .withDefendantId(defendantId)
                            .withDefendantNumber(++defendantNumber)
                            .build())
            );
        }
        if (isNotEmpty(changeDefendantsCotr.getRemovedDefendantIds())) {
            changeDefendantsCotr.getRemovedDefendantIds().forEach(defendantId ->
                    streamBuilder.add(DefendantRemovedFromCotr.defendantRemovedFromCotr()
                            .withCotrId(changeDefendantsCotr.getCotrId())
                            .withHearingId(changeDefendantsCotr.getHearingId())
                            .withDefendantId(defendantId)
                            .build())
            );
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> addFurtherInfoForProsecutionCotr(final AddFurtherInfoProsecutionCotr addFurtherInfoProsecutionCotr) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final String roles = getRoles();
        final String taskName = "Review Prosecution CoTR Task";
        final int numberOfDays = 1;

        streamBuilder.add(CotrTaskRequested.cotrTaskRequested()
                .withCotrId(addFurtherInfoProsecutionCotr.getCotrId())
                .withNumberOfDays(numberOfDays)
                .withTaskName(taskName)
                .withComments(CASE_REFERENCE + caseUrn + AND_TRIAL_DATE + hearingDate)
                .withRoles(roles)
                .withCourtCode(courtCenter)
                .withOrganisationId(UUID.randomUUID())
                .withHearingDate(hearingDate != null ? hearingDate.toString() : null)
                .build());
        streamBuilder.add(FurtherInfoForProsecutionCotrAdded.furtherInfoForProsecutionCotrAdded()
                .withCotrId(addFurtherInfoProsecutionCotr.getCotrId())
                .withMessage(addFurtherInfoProsecutionCotr.getMessage())
                .withIsCertificationReady(addFurtherInfoProsecutionCotr.getIsCertificationReady())
                .withAddedBy(addFurtherInfoProsecutionCotr.getAddedBy())
                .withAddedByName(addFurtherInfoProsecutionCotr.getAddedByName())
                .build());
        return apply(streamBuilder.build());
    }

    public Stream<Object> addFurtherInfoForDefenceCotr(final AddFurtherInfoDefenceCotrCommand addFurtherInfoDefenceCotr) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final String roles = getRoles();
        final String taskName = "Review Defence CoTR Task";
        final int numberOfDays = 1;

        if (this.defendantCotrPdfContent.containsKey(addFurtherInfoDefenceCotr.getDefendantId())) {
            final CotrPdfContent cotrPdfContent = defendantCotrPdfContent.get(addFurtherInfoDefenceCotr.getDefendantId());
            if (cotrPdfContent.getDefence() != null) {
                final List<Defence> defenceList = new ArrayList<>(cotrPdfContent.getDefence());
                defenceList.add(Defence.defence()
                        .withFields(Arrays.asList(Fields.fields()
                                .withLabel("Question")
                                .withValue(addFurtherInfoDefenceCotr.getMessage()).build())).build());

                streamBuilder.add(FurtherInfoForDefenceCotrAdded.furtherInfoForDefenceCotrAdded()
                        .withCotrId(addFurtherInfoDefenceCotr.getCotrId())
                        .withCaseId(caseId)
                        .withDefendantId(addFurtherInfoDefenceCotr.getDefendantId())
                        .withMessage(addFurtherInfoDefenceCotr.getMessage())
                        .withIsCertificationReady(addFurtherInfoDefenceCotr.getIsCertificationReady())
                        .withAddedBy(addFurtherInfoDefenceCotr.getAddedBy())
                        .withAddedByName(addFurtherInfoDefenceCotr.getAddedByName())
                        .withPdfContent(CotrPdfContent.cotrPdfContent().withValuesFrom(cotrPdfContent).withDefence(defenceList).build())
                        .withTrailDate(convertDateToTextPatternDMMMUUUU(hearingDate))
                        .withIsWelshForm(addFurtherInfoDefenceCotr.getIsWelshForm())
                        .build());
            }
        }


        streamBuilder.add(CotrTaskRequested.cotrTaskRequested()
                .withCotrId(addFurtherInfoDefenceCotr.getCotrId())
                .withNumberOfDays(numberOfDays)
                .withTaskName(taskName)
                .withComments(CASE_REFERENCE + caseUrn + AND_TRIAL_DATE + hearingDate)
                .withRoles(roles)
                .withCourtCode(courtCenter)
                .withOrganisationId(UUID.randomUUID())
                .withHearingDate(hearingDate != null ? hearingDate.toString() : null)
                .build());

        if (addFurtherInfoDefenceCotr.getIsWelshForm() != null && addFurtherInfoDefenceCotr.getIsWelshForm()) {
            streamBuilder.add(CotrTaskRequested.cotrTaskRequested()
                    .withCotrId(addFurtherInfoDefenceCotr.getCotrId())
                    .withNumberOfDays(numberOfDays)
                    .withTaskName("Welsh Translation request Task")
                    .withComments(CASE_REFERENCE + caseUrn + AND_TRIAL_DATE + hearingDate)
                    .withRoles(getRolesForWelsh())
                    .withCourtCode(courtCenter)
                    .withOrganisationId(UUID.randomUUID())
                    .withHearingDate(hearingDate != null ? hearingDate.toString() : null)
                    .build());
        }


        return apply(streamBuilder.build());
    }

    public Stream<Object> updateReviewNotes(final UpdateReviewNotes updateReviewNotes) {
        return apply(Stream.of(ReviewNotesUpdated.reviewNotesUpdated()
                .withCotrId(updateReviewNotes.getCotrId())
                .withCotrNotes(convertToCotrNotes(updateReviewNotes.getCotrNotes()))
                .build()));
    }

    private List<CotrNotes> convertToCotrNotes(final List<uk.gov.justice.progression.courts.CotrNotes> cotrNotes) {
        return cotrNotes.stream()
                .map(cotrNote -> CotrNotes.cotrNotes()
                        .withReviewNoteType(ReviewNoteType.valueOf(cotrNote.getReviewNoteType().name()))
                        .withReviewNotes(convertToEventType(cotrNote.getReviewNotes()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ReviewNotes> convertToEventType(final List<uk.gov.justice.progression.courts.ReviewNotes> reviewNotes) {
        return reviewNotes.stream()
                .map(reviewNote -> ReviewNotes.reviewNotes()
                        .withId(reviewNote.getId())
                        .withComment(reviewNote.getComment())
                        .build())
                .collect(Collectors.toList());
    }

    private String getRoles() {
        return JurisdictionType.CROWN == this.jurisdictionType ? OPERATIONAL_DELIVERY_ADMIN_LISTING_OFFICER_CASE_PROGRESSION_OFFICER : CTSC_ADMIN;
    }

    private String getRolesForWelsh() {
        return JurisdictionType.CROWN == this.jurisdictionType ? OPERATIONAL_DELIVERY_ADMIN : CTSC_ADMIN;
    }

    private String convertDateToTextPatternDMMMUUUU(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("d MMM uuuu")) : null;
    }

    public Stream<Object> updateProsecutionCotr(final UpdateProsecutionCotr updateProsecutionCotr) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(ProsecutionCotrUpdated.prosecutionCotrUpdated()
                .withCotrId(updateProsecutionCotr.getCotrId())
                .withCertificationDate(updateProsecutionCotr.getCertificationDate())
                .withCertifyThatTheProsecutionIsTrialReady(updateProsecutionCotr.getCertifyThatTheProsecutionIsTrialReady())
                .withFormCompletedOnBehalfOfProsecutionBy(updateProsecutionCotr.getFormCompletedOnBehalfOfProsecutionBy())
                .withFurtherProsecutionInformationProvidedAfterCertification(updateProsecutionCotr.getFurtherProsecutionInformationProvidedAfterCertification())
                .withHearingId(updateProsecutionCotr.getHearingId())
                .withSubmissionId(updateProsecutionCotr.getSubmissionId())
                .build()
        );
        return apply(streamBuilder.build());
    }
}
